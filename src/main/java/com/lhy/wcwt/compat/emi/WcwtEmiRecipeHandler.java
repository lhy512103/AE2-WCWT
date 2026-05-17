package com.lhy.wcwt.compat.emi;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.jei.WcwtRecipeTransferHandler;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket.RequestedIngredient;
import com.lhy.wcwt.pull.WcwtPullIngredientOrdering;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EMI 对 WCWT 的官方 recipe handler 接入。
 *
 * <p>保持改动尽量小，优先复用现有 JEI/WCWT 传输链路：
 * 未锁定合成网格 -> 走编码区填充；
 * 锁定合成网格 -> 走 WCWT 现有从 ME 拉料的网络包。
 */
public class WcwtEmiRecipeHandler implements EmiRecipeHandler<WirelessComprehensiveWorkTerminalMenu> {

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<WirelessComprehensiveWorkTerminalMenu> screen) {
        List<EmiStack> inventory = new ArrayList<>();
        WirelessComprehensiveWorkTerminalMenu menu = screen.getMenu();

        for (ItemStack stack : menu.getPlayerInventory().items) {
            if (!stack.isEmpty()) {
                inventory.add(EmiStack.of(stack.copy()));
            }
        }

        var repo = menu.getClientRepo();
        if (repo != null) {
            for (GridInventoryEntry entry : repo.getAllEntries()) {
                if (!(entry.getWhat() instanceof AEItemKey itemKey)) {
                    continue;
                }
                long amount = Math.max(1, entry.getStoredAmount());
                inventory.add(EmiStack.of(itemKey.toStack((int) Math.min(amount, itemKey.getMaxStackSize())), amount));
            }
        }

        return new EmiPlayerInventory(inventory);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        EncodingMode mode = getTransferMode(recipe);
        return mode == EncodingMode.CRAFTING
                || mode == EncodingMode.PROCESSING
                || mode == EncodingMode.SMITHING_TABLE
                || mode == EncodingMode.STONECUTTING;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<WirelessComprehensiveWorkTerminalMenu> context) {
        WirelessComprehensiveWorkTerminalMenu menu = context.getScreenHandler();
        if (!supportsRecipe(recipe)) {
            return false;
        }

        if (isCraftingGridLocked(menu)) {
            return !collectRequestedIngredients(recipe).isEmpty();
        }

        List<@Nullable GenericStack> inputs = collectEncodingInputs(recipe);
        List<@Nullable GenericStack> outputs = collectEncodingOutputs(recipe);
        return inputs.stream().anyMatch(Objects::nonNull) || outputs.stream().anyMatch(Objects::nonNull);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<WirelessComprehensiveWorkTerminalMenu> context) {
        WirelessComprehensiveWorkTerminalMenu menu = context.getScreenHandler();
        if (!supportsRecipe(recipe)) {
            return false;
        }

        if (isCraftingGridLocked(menu)) {
            List<RequestedIngredient> requestedIngredients = collectRequestedIngredients(recipe);
            if (requestedIngredients.isEmpty()) {
                return false;
            }
            boolean maxTransfer = context.getAmount() > 1;
            boolean craftMissing = context.getDestination() != EmiCraftContext.Destination.NONE;
            PacketDistributor.sendToServer(new WcwtPullRecipeInputsPacket(maxTransfer, craftMissing, requestedIngredients));
            return true;
        }

        EncodingMode mode = getTransferMode(recipe);
        List<@Nullable GenericStack> inputs = collectEncodingInputs(recipe);
        List<@Nullable GenericStack> outputs = collectEncodingOutputs(recipe);
        if (inputs.stream().allMatch(Objects::isNull) && outputs.stream().allMatch(Objects::isNull)) {
            return false;
        }

        WcwtRecipeTransferHandler.updateEaepProviderSearchKey(recipe, recipe.getBackingRecipe() != null
                ? recipe.getBackingRecipe().value() : null, mode);
        PacketDistributor.sendToServer(new JeiCraftingTransferPacket(inputs, outputs, false, mode));
        return true;
    }

    private static boolean isCraftingGridLocked(WirelessComprehensiveWorkTerminalMenu menu) {
        return menu.getMenuHost() != null && menu.getMenuHost().isCraftingGridLocked();
    }

    private static EncodingMode getTransferMode(EmiRecipe recipe) {
        if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree()) {
            return EncodingMode.CRAFTING;
        }
        if (recipe.getCategory() == VanillaEmiRecipeCategories.SMITHING) {
            return EncodingMode.SMITHING_TABLE;
        }
        if (recipe.getCategory() == VanillaEmiRecipeCategories.STONECUTTING) {
            return EncodingMode.STONECUTTING;
        }
        return EncodingMode.PROCESSING;
    }

    private static List<@Nullable GenericStack> collectEncodingInputs(EmiRecipe recipe) {
        EncodingMode mode = getTransferMode(recipe);
        List<@Nullable GenericStack> sparseInputs = recipe.getInputs().stream()
                .map(WcwtEmiRecipeHandler::toGenericStack)
                .limit(mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE)
                .toList();
        if (mode == EncodingMode.PROCESSING) {
            return sparseInputs.stream().filter(Objects::nonNull).toList();
        }
        return sparseInputs;
    }

    private static List<@Nullable GenericStack> collectEncodingOutputs(EmiRecipe recipe) {
        if (getTransferMode(recipe) != EncodingMode.PROCESSING) {
            return List.of();
        }
        return recipe.getOutputs().stream()
                .map(WcwtEmiRecipeHandler::toGenericStack)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<RequestedIngredient> collectRequestedIngredients(EmiRecipe recipe) {
        return recipe.getInputs().stream()
                .filter(ingredient -> !ingredient.isEmpty())
                .map(WcwtEmiRecipeHandler::toRequestedIngredient)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    private static RequestedIngredient toRequestedIngredient(EmiIngredient ingredient) {
        List<ItemStack> alternatives = WcwtPullIngredientOrdering.preferSpecificComponentsFirst(
                ingredient.getEmiStacks().stream()
                        .map(EmiStack::getItemStack)
                        .filter(stack -> !stack.isEmpty())
                        .map(ItemStack::copy)
                        .distinct()
                        .toList());
        if (alternatives.isEmpty()) {
            return null;
        }
        int count = Math.max(1, (int) Math.min(Integer.MAX_VALUE, ingredient.getAmount()));
        return new RequestedIngredient(alternatives, count);
    }

    @Nullable
    private static GenericStack toGenericStack(EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }

        GenericStack direct = tryConvertDirectSingleIngredient(ingredient);
        if (direct != null) {
            return direct;
        }

        Set<Object> keys = ingredient.getEmiStacks().stream()
                .filter(stack -> !stack.isEmpty())
                .map(EmiStack::getKey)
                .collect(Collectors.toSet());
        if (keys.size() > 1) {
            return toGenericStack(Ingredient.of(ingredient.getEmiStacks().stream()
                    .map(EmiStack::getItemStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(ItemStack[]::new)));
        }
        ItemStack displayed = ingredient.getEmiStacks().stream()
                .map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty())
                .findFirst()
                .orElse(ItemStack.EMPTY);
        if (!displayed.isEmpty()) {
            return GenericStack.fromItemStack(displayed.copyWithCount(Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                    ingredient.getAmount()))));
        }
        return null;
    }

    @Nullable
    private static GenericStack tryConvertDirectSingleIngredient(EmiIngredient ingredient) {
        List<EmiStack> stacks = ingredient.getEmiStacks().stream()
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (stacks.size() != 1) {
            return null;
        }
        EmiStack stack = stacks.get(0);
        GenericStack fluid = convertFluid(stack, ingredient.getAmount());
        if (fluid != null) {
            return fluid;
        }
        GenericStack chemical = convertMekanismChemical(stack.getKey(), ingredient.getAmount());
        if (chemical != null) {
            return chemical;
        }
        return null;
    }

    @Nullable
    private static GenericStack convertFluid(EmiStack stack, long amount) {
        Fluid fluid = stack.getKeyOfType(Fluid.class);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return null;
        }
        FluidStack neoFluidStack = new FluidStack(fluid.builtInRegistryHolder(),
                (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount)),
                stack.getComponentChanges());
        if (neoFluidStack.isEmpty()) {
            return null;
        }
        return GenericStack.fromFluidStack(neoFluidStack);
    }

    @Nullable
    private static GenericStack convertMekanismChemical(Object rawKey, long amount) {
        try {
            Class<?> chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
            if (!chemicalClass.isInstance(rawKey)) {
                return null;
            }
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Class<?> keyClass = Class.forName("me.ramidzkh.mekae2.ae2.MekanismKey");
            Method getStack = chemicalClass.getMethod("getStack", long.class);
            Object chemicalStack = getStack.invoke(rawKey, Math.max(1L, amount));
            if (!chemicalStackClass.isInstance(chemicalStack)) {
                return null;
            }
            Method of = keyClass.getMethod("of", chemicalStackClass);
            Object aeKey = of.invoke(null, chemicalStack);
            if (!(aeKey instanceof AEKey key)) {
                return null;
            }
            return new GenericStack(key, Math.max(1L, amount));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    private static GenericStack toGenericStack(Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return null;
        }
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0 || items[0].isEmpty()) {
            return null;
        }
        return GenericStack.fromItemStack(items[0].copyWithCount(1));
    }
}
