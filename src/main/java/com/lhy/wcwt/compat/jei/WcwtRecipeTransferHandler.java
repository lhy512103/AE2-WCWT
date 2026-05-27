package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.parts.encoding.EncodingMode;
import appeng.util.CraftingRecipeUtil;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class WcwtRecipeTransferHandler
        implements IUniversalRecipeTransferHandler<WirelessComprehensiveWorkTerminalMenu> {

    private final IRecipeTransferHandlerHelper transferHelper;

    public WcwtRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }

    private static boolean isCraftingGridLocked(WirelessComprehensiveWorkTerminalMenu menu) {
        return menu.getMenuHost() != null && menu.getMenuHost().isCraftingGridLocked();
    }

    @Override
    public Class<? extends WirelessComprehensiveWorkTerminalMenu> getContainerClass() {
        return WirelessComprehensiveWorkTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<WirelessComprehensiveWorkTerminalMenu>> getMenuType() {
        return Optional.of(ModMenus.WCWT_MENU.get());
    }

    @Override
    public IRecipeTransferError transferRecipe(WirelessComprehensiveWorkTerminalMenu menu, Object recipe,
                                               IRecipeSlotsView recipeSlots, Player player,
                                               boolean maxTransfer, boolean doTransfer) {
        RecipeHolder<?> recipeHolder = recipe instanceof RecipeHolder<?> holder ? holder : null;
        Recipe<?> minecraftRecipe = recipeHolder != null ? recipeHolder.value()
                : recipe instanceof Recipe<?> directRecipe ? directRecipe
                : null;
        EncodingMode mode = getTransferMode(minecraftRecipe, recipeSlots);

        if (isCraftingGridLocked(menu)) {
            if (doTransfer) {
                WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            }
            return WcwtPullRecipeTransfer.transfer(menu, recipe, recipeSlots, player, maxTransfer, doTransfer,
                    transferHelper);
        }

        boolean encodingRecipe = mode != EncodingMode.PROCESSING;

        List<@Nullable GenericStack> inputs = encodingRecipe
                ? collectCraftingLikeInputs(menu, recipeHolder, minecraftRecipe, recipeSlots, mode)
                : collectStacksPreservingSlots(menu, recipeSlots, RecipeIngredientRole.INPUT, Integer.MAX_VALUE);
        List<@Nullable GenericStack> outputs = encodingRecipe ? List.of()
                : collectStacksPreservingSlots(menu, recipeSlots, RecipeIngredientRole.OUTPUT, Integer.MAX_VALUE);
        if (mode == EncodingMode.PROCESSING) {
            inputs = compactProcessingIngredientOrder(inputs);
            outputs = compactProcessingIngredientOrder(outputs);
        }
        if (inputs.stream().allMatch(Objects::isNull) && outputs.stream().allMatch(Objects::isNull)) {
            return null;
        }

        if (!doTransfer) {
            int inputHighlightLimit = mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE;
            var craftableSlots = findCraftableEncodingSlots(menu, recipeSlots, inputHighlightLimit);
            return new WcwtEncodingRecipeTransferError(craftableSlots);
        }

        if (doTransfer) {
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            updateEaepProviderSearchKey(recipe, minecraftRecipe, mode);
            PacketDistributor.sendToServer(new JeiCraftingTransferPacket(inputs, outputs, false, mode));
        }
        return null;
    }

    /**
     * JEI encoding preview: blue slots ME can satisfy (network stock or autocraft), aligned with terminal usability.
     */
    public static List<IRecipeSlotView> findCraftableEncodingSlots(WirelessComprehensiveWorkTerminalMenu menu,
                                                                   IRecipeSlotsView slotsView,
                                                                   int maxInputSlots) {
        var repo = menu.getClientRepo();
        if (repo == null) {
            return List.of();
        }
        var networkKeys = repo.getAllEntries().stream()
                .filter(e -> e.getWhat() != null && (e.getStoredAmount() > 0 || e.isCraftable()))
                .map(GridInventoryEntry::getWhat)
                .collect(Collectors.toSet());

        var stream = slotsView.getSlotViews(RecipeIngredientRole.INPUT).stream();
        if (maxInputSlots < Integer.MAX_VALUE) {
            stream = stream.limit(maxInputSlots);
        }
        return stream
                .filter(slotView -> slotView.getAllIngredients().anyMatch(ingredient -> {
                    GenericStack stack = toGenericStack(ingredient);
                    return stack != null && networkKeys.contains(stack.what());
                }))
                .toList();
    }

    public static void updateEaepProviderSearchKey(Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode) {
        try {
            Class<?> utilClass = Class.forName("com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil");
            if (mode != EncodingMode.PROCESSING) {
                utilClass.getMethod("presetCraftingProviderSearchKey").invoke(null);
                return;
            }

            String name = null;
            if (recipe != null) {
                Object value = utilClass.getMethod("mapRecipeTypeToSearchKey", Recipe.class).invoke(null, recipe);
                if (value instanceof String text) {
                    name = text;
                }
            }
            if ((name == null || name.isBlank()) && recipeBase != null) {
                Object value = utilClass.getMethod("deriveSearchKeyFromUnknownRecipe", Object.class).invoke(null,
                        recipeBase);
                if (value instanceof String text) {
                    name = text;
                }
            }
            if (name != null && !name.isBlank()) {
                utilClass.getMethod("setLastProcessingName", String.class).invoke(null, name);
            }
        } catch (Throwable ignored) {
        }
    }

    private static EncodingMode getTransferMode(@Nullable Recipe<?> recipe, IRecipeSlotsView slotsView) {
        if (recipe != null && EncodingHelper.isSupportedCraftingRecipe(recipe)) {
            if (recipe.getType() == RecipeType.STONECUTTING) {
                return EncodingMode.STONECUTTING;
            }
            if (recipe.getType() == RecipeType.SMITHING) {
                return EncodingMode.SMITHING_TABLE;
            }
            return EncodingMode.CRAFTING;
        }

        return EncodingMode.PROCESSING;
    }

    static List<@Nullable GenericStack> collectCraftingLikeInputs(WirelessComprehensiveWorkTerminalMenu menu,
                                                                  @Nullable RecipeHolder<?> recipeHolder,
                                                                  @Nullable Recipe<?> recipe,
                                                                  IRecipeSlotsView recipeSlots,
                                                                  EncodingMode mode) {
        if (recipe != null && recipeHolder != null) {
            List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                    .limit(mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE)
                    .toList();
            if (mode == EncodingMode.CRAFTING) {
                var ingredients = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
                List<@Nullable GenericStack> resolved = new ArrayList<>(ingredients.size());
                for (int slot = 0; slot < ingredients.size(); slot++) {
                    resolved.add(toBestGenericStack(menu, ingredients.get(slot), inputSlots, slot));
                }
                return resolved;
            }
            var ingredients = CraftingRecipeUtil.getIngredients(recipe);
            List<@Nullable GenericStack> resolved = new ArrayList<>(ingredients.size());
            for (int slot = 0; slot < ingredients.size(); slot++) {
                resolved.add(toBestGenericStack(menu, ingredients.get(slot), inputSlots, slot));
            }
            return resolved;
        }

        return collectStacksPreservingSlots(menu, recipeSlots, RecipeIngredientRole.INPUT,
                mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE);
    }

    /**
     * JEI 处理配方往往在输入/输出槽之间留空位以对齐布局；拉取到 WCWT 编码区时应保持材料先后顺序，
     * 去掉中间的 {@code null}，从样板网格左上角起连续排列（与原版样板终端常见体验一致）。
     */
    private static List<@Nullable GenericStack> compactProcessingIngredientOrder(List<@Nullable GenericStack> sparse) {
        List<@Nullable GenericStack> dense = new ArrayList<>();
        for (GenericStack stack : sparse) {
            if (stack != null) {
                dense.add(stack);
            }
        }
        return dense;
    }

    private static List<@Nullable GenericStack> collectStacksPreservingSlots(WirelessComprehensiveWorkTerminalMenu menu,
                                                                             IRecipeSlotsView recipeSlots,
                                                                             RecipeIngredientRole role,
                                                                             int limit) {
        return recipeSlots.getSlotViews(role).stream()
                .limit(limit)
                .map(slotView -> toBestGenericStack(menu, slotView))
                .toList();
    }

    @Nullable
    private static GenericStack toBestGenericStack(WirelessComprehensiveWorkTerminalMenu menu,
                                                   Ingredient ingredient,
                                                   List<IRecipeSlotView> inputSlots,
                                                   int slot) {
        if (ingredient.isEmpty()) {
            return null;
        }

        List<ItemStack> visibleAlternatives = slot < inputSlots.size()
                ? inputSlots.get(slot).getItemStacks().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList()
                : List.of();
        ItemStack best = WcwtIngredientPriorities.chooseBestItem(menu, ingredient, visibleAlternatives);
        return best.isEmpty() ? null : GenericStack.fromItemStack(best.copyWithCount(1));
    }

    @Nullable
    private static GenericStack toBestGenericStack(WirelessComprehensiveWorkTerminalMenu menu, IRecipeSlotView slotView) {
        List<GenericStack> candidates = slotView.getAllIngredients()
                .map(WcwtRecipeTransferHandler::toGenericStack)
                .filter(Objects::nonNull)
                .toList();
        GenericStack best = WcwtIngredientPriorities.chooseBestGenericStack(menu, candidates);
        return best != null ? best : toGenericStack(getDisplayedStack(slotView));
    }

    private static ITypedIngredient<?> getDisplayedStack(IRecipeSlotView slotView) {
        return slotView.getDisplayedIngredient()
                .or(() -> slotView.getAllIngredients().findFirst())
                .orElse(null);
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

    @Nullable
    private static GenericStack toGenericStack(@Nullable ITypedIngredient<?> ingredient) {
        if (ingredient == null) {
            return null;
        }
        GenericStack converted = convertWithAe2JeiIntegration(ingredient);
        if (converted != null) {
            return converted;
        }

        Object raw = ingredient.getIngredient();
        if (raw instanceof ItemStack stack && !stack.isEmpty()) {
            return GenericStack.fromItemStack(stack.copyWithCount(1));
        }
        if (raw instanceof FluidStack fluid && !fluid.isEmpty()) {
            return GenericStack.fromFluidStack(fluid.copy());
        }
        return convertMekanismChemical(raw);
    }

    @Nullable
    private static GenericStack convertWithAe2JeiIntegration(ITypedIngredient<?> ingredient) {
        try {
            Class<?> convertersClass =
                    Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters");
            Method getConverter = convertersClass.getMethod("getConverter", IIngredientType.class);
            Object converter = getConverter.invoke(null, ingredient.getType());
            if (converter == null) {
                return null;
            }
            Method getStackFromIngredient = converter.getClass().getMethod("getStackFromIngredient", Object.class);
            Object converted = getStackFromIngredient.invoke(converter, ingredient.getIngredient());
            return converted instanceof GenericStack stack ? stack : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    private static GenericStack convertMekanismChemical(Object raw) {
        try {
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            if (!chemicalStackClass.isInstance(raw)) {
                return null;
            }
            Class<?> keyClass = Class.forName("me.ramidzkh.mekae2.ae2.MekanismKey");
            Method of = keyClass.getMethod("of", chemicalStackClass);
            Object key = of.invoke(null, raw);
            if (!(key instanceof appeng.api.stacks.AEKey aeKey)) {
                return null;
            }
            Method getAmount = chemicalStackClass.getMethod("getAmount");
            long amount = ((Number) getAmount.invoke(raw)).longValue();
            return new GenericStack(aeKey, Math.max(1, amount));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
