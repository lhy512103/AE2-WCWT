package com.lhy.wcwt.compat.emi;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.itemlists.TransferHelper;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.client.WcwtFavorites;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.compat.WcwtRecipeTransferCommon;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket.RequestedIngredient;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import com.lhy.wcwt.pull.WcwtStackMatching;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.WeakHashMap;

/**
 * EMI 对 WCWT 的官方 recipe handler 接入。
 *
 * <p>保持改动尽量小，优先复用现有 JEI/WCWT 传输链路：
 * 未锁定合成网格 -> 走编码区填充；
 * 锁定合成网格 -> 走 WCWT 现有从 ME 拉料的网络包。
 */
public class WcwtEmiRecipeHandler implements EmiRecipeHandler<WirelessComprehensiveWorkTerminalMenu> {
    private static final Map<WirelessComprehensiveWorkTerminalMenu, CachedPreview> LOCKED_PREVIEW_CACHE = new WeakHashMap<>();

    private record CachedPreview(EmiRecipe recipe, EncodingMode mode, int inventoryHash, long tick,
            PreviewResult result) {
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<WirelessComprehensiveWorkTerminalMenu> screen) {
        List<EmiStack> inventory = new ArrayList<>();
        WirelessComprehensiveWorkTerminalMenu menu = screen.getMenu();

        for (ItemStack stack : menu.getPlayerInventory().items) {
            if (!stack.isEmpty()) {
                inventory.add(EmiStack.of(stack.copy()));
            }
        }
        if (!menu.getCarried().isEmpty()) {
            inventory.add(EmiStack.of(menu.getCarried().copy()));
        }

        return new EmiPlayerInventory(inventory);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return false;
        }
        EncodingMode mode = getTransferMode(recipe);
        return mode == EncodingMode.CRAFTING
                || mode == EncodingMode.PROCESSING
                || mode == EncodingMode.SMITHING_TABLE
                || mode == EncodingMode.STONECUTTING;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<WirelessComprehensiveWorkTerminalMenu> context) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return false;
        }
        if (context.getType() != EmiCraftContext.Type.FILL_BUTTON) {
            return false;
        }

        WirelessComprehensiveWorkTerminalMenu menu = context.getScreenHandler();
        EncodingMode mode = getTransferMode(recipe);
        if (mode != EncodingMode.CRAFTING
                && mode != EncodingMode.PROCESSING
                && mode != EncodingMode.SMITHING_TABLE
                && mode != EncodingMode.STONECUTTING) {
            return false;
        }

        if (isCraftingGridLocked(menu)) {
            return hasNonEmptyIngredients(recipe.getInputs(), Integer.MAX_VALUE);
        }

        if (mode == EncodingMode.PROCESSING) {
            return hasNonEmptyIngredients(recipe.getInputs(), Integer.MAX_VALUE)
                    || hasNonEmptyIngredients(recipe.getOutputs(), Integer.MAX_VALUE);
        }

        int inputLimit = mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE;
        return hasNonEmptyIngredients(recipe.getInputs(), inputLimit);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe,
                                                   EmiCraftContext<WirelessComprehensiveWorkTerminalMenu> context) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return EmiRecipeHandler.super.getTooltip(recipe, context);
        }
        if (context.getType() != EmiCraftContext.Type.FILL_BUTTON || !supportsRecipe(recipe)) {
            return EmiRecipeHandler.super.getTooltip(recipe, context);
        }

        WirelessComprehensiveWorkTerminalMenu menu = context.getScreenHandler();
        List<Component> tooltip = null;

        if (isCraftingGridLocked(menu)) {
            PreviewResult preview = buildLockedGridPreview(menu, recipe);
            if (preview.inputCount() > 0) {
                tooltip = createLockedGridTooltip(preview, getTransferMode(recipe) != EncodingMode.CRAFTING);
            }
        } else {
            Set<AEKey> availableNetworkKeys = collectAvailableNetworkKeys(menu);
            boolean anyHighlighted = recipe.getInputs().stream()
                    .anyMatch(ingredient -> isAvailableFromNetwork(availableNetworkKeys, ingredient));
            if (anyHighlighted) {
                tooltip = TransferHelper.createEncodingTooltip(true, true);
            }
        }

        if (tooltip == null) {
            return EmiRecipeHandler.super.getTooltip(recipe, context);
        }
        return tooltip.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
    }

    @Override
    public void render(EmiRecipe recipe,
                       EmiCraftContext<WirelessComprehensiveWorkTerminalMenu> context,
                       List<Widget> widgets,
                       GuiGraphics draw) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return;
        }
        if (context.getType() != EmiCraftContext.Type.FILL_BUTTON || !supportsRecipe(recipe)) {
            return;
        }

        WirelessComprehensiveWorkTerminalMenu menu = context.getScreenHandler();
        Map<Integer, SlotWidget> inputSlots = getRecipeInputSlots(recipe, widgets);
        if (inputSlots.isEmpty()) {
            return;
        }

        if (isCraftingGridLocked(menu)) {
            PreviewResult preview = buildLockedGridPreview(menu, recipe);
            renderMissingAndCraftableSlotOverlays(inputSlots, draw, preview.missingSlots(), preview.craftableSlots());
            return;
        }

        Set<AEKey> availableNetworkKeys = collectAvailableNetworkKeys(menu);
        if (availableNetworkKeys.isEmpty()) {
            return;
        }

        for (var entry : inputSlots.entrySet()) {
            if (isAvailableFromNetwork(availableNetworkKeys, recipe.getInputs().get(entry.getKey()))) {
                renderSlotOverlay(entry.getValue(), draw, TransferHelper.BLUE_SLOT_HIGHLIGHT_COLOR);
            }
        }
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<WirelessComprehensiveWorkTerminalMenu> context) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return false;
        }
        WirelessComprehensiveWorkTerminalMenu menu = context.getScreenHandler();
        if (!supportsRecipe(recipe)) {
            return false;
        }

        if (isCraftingGridLocked(menu)) {
            EncodingMode mode = getTransferMode(recipe);
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            List<RequestedIngredient> requestedIngredients = collectRequestedIngredients(menu, recipe);
            if (mode == EncodingMode.PROCESSING
                    && menu.getManualWorkspaceMode() == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING) {
                requestedIngredients = mergeProcessingIngredients(requestedIngredients);
            }
            if (requestedIngredients.isEmpty()) {
                return false;
            }
            boolean allowShiftMaxTransfer = mode != EncodingMode.CRAFTING;
            boolean maxTransfer = allowShiftMaxTransfer && (context.getAmount() > 1 || Screen.hasShiftDown());
            boolean craftMissing = Screen.hasControlDown()
                    || context.getDestination() != EmiCraftContext.Destination.NONE;
            PacketDistributor.sendToServer(new WcwtPullRecipeInputsPacket(maxTransfer, craftMissing,
                    requestedIngredients, menu.getManualWorkspaceMode().ordinal()));
            return true;
        }

        EncodingMode mode = getTransferMode(recipe);
        WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
        List<Widget> widgets = collectRecipeWidgets(recipe);
        var priorityContext = createPriorityContext(menu);
        List<@Nullable GenericStack> inputs = collectEncodingInputs(priorityContext, recipe, widgets);
        List<@Nullable GenericStack> outputs = collectEncodingOutputs(recipe);
        if (inputs.stream().allMatch(Objects::isNull) && outputs.stream().allMatch(Objects::isNull)) {
            return false;
        }

        WcwtRecipeTransferCommon.updateEaepProviderSearchKey(recipe, recipe.getBackingRecipe() != null
                ? recipe.getBackingRecipe().value() : null, mode);
        PacketDistributor.sendToServer(new JeiCraftingTransferPacket(inputs, outputs, false, mode));
        return true;
    }

    private static boolean isCraftingGridLocked(WirelessComprehensiveWorkTerminalMenu menu) {
        return menu.getMenuHost() != null && menu.getMenuHost().isCraftingGridLocked();
    }

    private static PreviewResult buildLockedGridPreview(WirelessComprehensiveWorkTerminalMenu menu, EmiRecipe recipe) {
        EncodingMode mode = getTransferMode(recipe);
        int inventoryHash = playerInventoryHash(menu);
        long tick = currentClientTick();
        CachedPreview cached = LOCKED_PREVIEW_CACHE.get(menu);
        if (cached != null && cached.recipe == recipe && cached.mode == mode
                && cached.inventoryHash == inventoryHash && cached.tick == tick) {
            return cached.result;
        }

        PreviewResult result = computeLockedGridPreview(menu, recipe, mode);
        LOCKED_PREVIEW_CACHE.put(menu, new CachedPreview(recipe, mode, inventoryHash, tick, result));
        return result;
    }

    private static PreviewResult computeLockedGridPreview(WirelessComprehensiveWorkTerminalMenu menu, EmiRecipe recipe,
            EncodingMode mode) {
        var reservedTerminalAmounts = new Object2IntOpenHashMap<AEItemKey>();
        var playerItems = menu.getPlayerInventory().items;
        var reservedPlayerItems = new int[playerItems.size()];
        Set<Integer> missingSlots = new HashSet<>();
        Set<Integer> craftableSlots = new HashSet<>();
        boolean anyResolved = false;

        int inputCount = 0;
        for (int index = 0; index < recipe.getInputs().size(); index++) {
            EmiIngredient emiIngredient = recipe.getInputs().get(index);
            List<ItemStack> alternatives = narrowSpecificAlternativesToDisplayed(getAlternativeStacks(emiIngredient));
            if (alternatives.isEmpty()) {
                continue;
            }
            Ingredient ingredient = Ingredient.of(alternatives.stream().map(ItemStack::copy));
            inputCount++;

            int maxStackSize = alternatives.stream().mapToInt(ItemStack::getMaxStackSize).max().orElse(64);
            int requiredCount = mode == EncodingMode.CRAFTING
                    ? 1
                    : Math.max(1, (int) Math.min(emiIngredient.getAmount(), maxStackSize));
            int remaining = requiredCount;
            boolean craftable = false;

            for (int slot = 0; slot < playerItems.size() && remaining > 0; slot++) {
                if (menu.isPlayerInventorySlotLocked(slot)) {
                    continue;
                }

                var stack = playerItems.get(slot);
                int available = stack.getCount() - reservedPlayerItems[slot];
                if (available <= 0 || !WcwtStackMatching.matchesAnyAlternative(stack, alternatives, ingredient)) {
                    continue;
                }
                int reserved = Math.min(available, remaining);
                reservedPlayerItems[slot] += reserved;
                remaining -= reserved;
                anyResolved = true;
            }

            int terminalReserved = WcwtStackMatching.reserveClientRepoStoredIngredient(menu, alternatives, ingredient,
                    reservedTerminalAmounts, remaining);
            remaining -= terminalReserved;
            anyResolved |= terminalReserved > 0;

            if (remaining > 0 && WcwtStackMatching.hasClientRepoCraftableIngredient(menu, alternatives, ingredient)) {
                craftable = true;
                anyResolved = true;
                remaining = 0;
            }

            if (remaining > 0) {
                missingSlots.add(index);
            }
            if (craftable) {
                craftableSlots.add(index);
            }
        }

        return new PreviewResult(missingSlots, craftableSlots, anyResolved, inputCount);
    }

    private static int playerInventoryHash(WirelessComprehensiveWorkTerminalMenu menu) {
        var items = menu.getPlayerInventory().items;
        int hash = 1;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            long stackHash = stack.isEmpty() ? 0L : ItemStack.hashItemAndComponents(stack);
            hash = 31 * hash + Long.hashCode(stackHash);
            hash = 31 * hash + stack.getCount();
            hash = 31 * hash + (menu.isPlayerInventorySlotLocked(slot) ? 1 : 0);
        }
        return hash;
    }

    private static long currentClientTick() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    private static List<Component> createLockedGridTooltip(PreviewResult preview, boolean allowShiftMaxTransfer) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(ItemModText.MOVE_ITEMS.text());

        if (!preview.craftableSlots().isEmpty()) {
            var line = Screen.hasControlDown() ? ItemModText.WILL_CRAFT.text() : ItemModText.CTRL_CLICK_TO_CRAFT.text();
            tooltip.add(line.withStyle(net.minecraft.ChatFormatting.BLUE));
        }

        if (!preview.missingSlots().isEmpty()) {
            var line = preview.anyResolved() ? ItemModText.MISSING_ITEMS.text() : ItemModText.NO_ITEMS.text();
            tooltip.add(line.withStyle(net.minecraft.ChatFormatting.RED));
        }

        if (allowShiftMaxTransfer) {
            tooltip.add(Component.translatable("message.wcwt.pull_shift_hint")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return tooltip;
    }

    private static boolean hasNonEmptyIngredients(List<? extends EmiIngredient> ingredients, int limit) {
        int checked = 0;
        for (var ingredient : ingredients) {
            if (checked++ >= limit) {
                break;
            }
            if (ingredient != null && !ingredient.isEmpty()) {
                return true;
            }
        }
        return false;
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

    private static Set<AEKey> collectAvailableNetworkKeys(WirelessComprehensiveWorkTerminalMenu menu) {
        var repo = menu.getClientRepo();
        if (repo == null) {
            return Set.of();
        }

        Set<AEKey> availableKeys = new HashSet<>();
        for (var entry : repo.getAllEntries()) {
            if (entry.getWhat() != null && (entry.getStoredAmount() > 0 || entry.isCraftable())) {
                availableKeys.add(entry.getWhat());
            }
        }
        return availableKeys;
    }

    private static boolean isAvailableFromNetwork(Set<AEKey> availableNetworkKeys, EmiIngredient ingredient) {
        return ingredient.getEmiStacks().stream().anyMatch(emiIngredient -> {
            var stack = toGenericStack(emiIngredient, 1);
            return stack != null && availableNetworkKeys.contains(stack.what());
        });
    }

    private static List<ItemStack> getAlternativeStacks(EmiIngredient ingredient) {
        List<ItemStack> alternatives = new ArrayList<>();
        for (var emiStack : ingredient.getEmiStacks()) {
            ItemStack stack = emiStack.getItemStack();
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copy = stack.copy();
            if (!containsEquivalentStack(alternatives, copy)) {
                alternatives.add(copy);
            }
        }
        return alternatives;
    }

    private static List<ItemStack> narrowSpecificAlternativesToDisplayed(List<ItemStack> alternatives) {
        if (!WcwtStackMatching.requiresExactItemKeyMatch(alternatives)) {
            return alternatives;
        }
        // Collect all alternatives with specific NBT data first
        List<ItemStack> specific = new ArrayList<>();
        for (ItemStack alt : alternatives) {
            if (alt != null && !alt.isEmpty() && WcwtStackMatching.hasSpecificData(alt)) {
                specific.add(alt.copy());
            }
        }
        if (!specific.isEmpty()) {
            return specific;
        }
        // Fallback: no specific alternatives found, return first non-empty
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty()) {
                return List.of(alternative.copy());
            }
        }
        return List.of();
    }

    private static Map<Integer, SlotWidget> getRecipeInputSlots(EmiRecipe recipe, List<Widget> widgets) {
        Map<Integer, SlotWidget> inputSlots = new IdentityHashMap<>();
        for (int i = 0; i < recipe.getInputs().size(); i++) {
            EmiIngredient ingredient = recipe.getInputs().get(i);
            for (var widget : widgets) {
                if (widget instanceof SlotWidget slot && slot.getRecipe() == null && slot.getStack() == ingredient) {
                    inputSlots.put(i, slot);
                    break;
                }
            }
        }

        if (inputSlots.size() == recipe.getInputs().size()) {
            return inputSlots;
        }

        // JEMI/部分第三方 EMI 配方不会复用 recipe.getInputs() 里的同一个 Ingredient 实例，
        // 这时按对象 identity 匹配会漏掉高亮槽位。回退到按输入槽出现顺序映射，
        // 与 JEI 的 slotView 顺序语义保持一致。
        List<SlotWidget> orderedInputSlots = new ArrayList<>();
        for (var widget : widgets) {
            if (widget instanceof SlotWidget slot && slot.getRecipe() == null) {
                orderedInputSlots.add(slot);
            }
        }

        inputSlots.clear();
        int count = Math.min(recipe.getInputs().size(), orderedInputSlots.size());
        for (int i = 0; i < count; i++) {
            inputSlots.put(i, orderedInputSlots.get(i));
        }
        return inputSlots;
    }

    private static void renderSlotOverlay(@Nullable SlotWidget slot, GuiGraphics guiGraphics, int color) {
        if (slot == null) {
            return;
        }

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 400);
        Bounds bounds = slot.getBounds();
        guiGraphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1, color);
        poseStack.popPose();
    }

    private static void renderMissingAndCraftableSlotOverlays(Map<Integer, SlotWidget> inputSlots,
                                                              GuiGraphics guiGraphics,
                                                              Set<Integer> missingSlots,
                                                              Set<Integer> craftableSlots) {
        for (var entry : inputSlots.entrySet()) {
            boolean missing = missingSlots.contains(entry.getKey());
            boolean craftable = craftableSlots.contains(entry.getKey());
            if (!missing && !craftable) {
                continue;
            }
            renderSlotOverlay(entry.getValue(), guiGraphics,
                    missing ? TransferHelper.RED_SLOT_HIGHLIGHT_COLOR : TransferHelper.BLUE_SLOT_HIGHLIGHT_COLOR);
        }
    }

    private static List<@Nullable GenericStack> collectEncodingInputs(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                                      EmiRecipe recipe,
                                                                      List<Widget> widgets) {
        EncodingMode mode = getTransferMode(recipe);
        List<@Nullable GenericStack> sparseInputs = new ArrayList<>();
        int limit = mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE;
        int count = Math.min(recipe.getInputs().size(), limit);
        for (int i = 0; i < count; i++) {
            sparseInputs.add(toGenericStack(priorityContext, recipe.getInputs().get(i)));
        }
        if (mode == EncodingMode.PROCESSING) {
            if (containsAnyStack(sparseInputs)) {
                return sparseInputs.stream().filter(Objects::nonNull).toList();
            }
            var backingRecipe = recipe.getBackingRecipe() != null ? recipe.getBackingRecipe().value() : null;
            if (backingRecipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basinRecipe) {
                List<@Nullable GenericStack> fallback = new ArrayList<>();
                for (Ingredient ingredient : basinRecipe.getIngredients()) {
                    fallback.add(WcwtRecipeTransferCommon.toBestGenericStack(priorityContext, ingredient, List.of()));
                }
                for (var fluidIngredient : basinRecipe.getFluidIngredients()) {
                    fallback.add(WcwtRecipeTransferCommon.toGenericStack(fluidIngredient));
                }
                return fallback.stream().filter(Objects::nonNull).toList();
            }
            return List.of();
        }
        return sparseInputs;
    }

    private static List<Widget> collectRecipeWidgets(EmiRecipe recipe) {
        SimpleWidgetCollector collector = new SimpleWidgetCollector(recipe.getDisplayWidth(), recipe.getDisplayHeight());
        recipe.addWidgets(collector);
        return collector.widgets();
    }

    private static List<@Nullable GenericStack> collectEncodingOutputs(EmiRecipe recipe) {
        if (getTransferMode(recipe) != EncodingMode.PROCESSING) {
            return List.of();
        }
        List<@Nullable GenericStack> displayed = recipe.getOutputs().stream()
                .map(stack -> toGenericStack(stack, stack.getAmount()))
                .filter(Objects::nonNull)
                .toList();
        if (!displayed.isEmpty()) {
            return displayed;
        }
        var backingRecipe = recipe.getBackingRecipe() != null ? recipe.getBackingRecipe().value() : null;
        if (backingRecipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basinRecipe) {
            List<@Nullable GenericStack> fallback = new ArrayList<>();
            for (var result : basinRecipe.getRollableResults()) {
                fallback.add(GenericStack.fromItemStack(result.getStack().copy()));
            }
            for (FluidStack fluidStack : basinRecipe.getFluidResults()) {
                if (!fluidStack.isEmpty()) {
                    fallback.add(GenericStack.fromFluidStack(fluidStack.copy()));
                }
            }
            return fallback.stream().filter(Objects::nonNull).toList();
        }
        return displayed;
    }

    private static boolean containsAnyStack(List<@Nullable GenericStack> stacks) {
        return stacks.stream().anyMatch(Objects::nonNull);
    }

    private static List<RequestedIngredient> collectRequestedIngredients(WirelessComprehensiveWorkTerminalMenu menu,
                                                                         EmiRecipe recipe) {
        var priorityContext = createPriorityContext(menu);
        if (getTransferMode(recipe) == EncodingMode.CRAFTING) {
            List<Widget> widgets = collectRecipeWidgets(recipe);
            Map<Integer, SlotWidget> inputSlots = getRecipeInputSlots(recipe, widgets);
            List<RequestedIngredient> requested = new ArrayList<>();
            for (var slotEntry : getOrderedCraftingInputSlots(inputSlots).entrySet()) {
                RequestedIngredient ingredient = toRequestedIngredient(priorityContext,
                        recipe.getInputs().get(slotEntry.getKey()),
                        slotEntry.getKey());
                if (ingredient != null) {
                    requested.add(new RequestedIngredient(ingredient.alternatives(), 1, ingredient.slotIndex()));
                }
            }
            if (!requested.isEmpty()) {
                return requested;
            }
        }
        return recipe.getInputs().stream()
                .filter(ingredient -> !ingredient.isEmpty())
                .map(ingredient -> toRequestedIngredient(priorityContext, ingredient, -1))
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<RequestedIngredient> mergeProcessingIngredients(List<RequestedIngredient> ingredients) {
        List<RequestedIngredient> merged = new ArrayList<>();
        for (RequestedIngredient ingredient : ingredients) {
            if (ingredient.alternatives().isEmpty()) {
                continue;
            }
            ItemStack representative = ingredient.alternatives().getFirst();
            boolean mergedExisting = false;
            for (int i = 0; i < merged.size(); i++) {
                RequestedIngredient existing = merged.get(i);
                if (ItemStack.isSameItemSameComponents(existing.alternatives().getFirst(), representative)) {
                    merged.set(i, new RequestedIngredient(existing.alternatives(),
                            existing.count() + ingredient.count(), -1));
                    mergedExisting = true;
                    break;
                }
            }
            if (!mergedExisting) {
                merged.add(new RequestedIngredient(ingredient.alternatives(), ingredient.count(), -1));
            }
        }
        return merged;
    }

    @Nullable
    private static RequestedIngredient toRequestedIngredient(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                             EmiIngredient ingredient,
                                                             int slotIndex) {
        List<ItemStack> visibleAlternatives = new ArrayList<>();
        ItemStack displayed = ingredient.getEmiStacks().stream()
                .map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty())
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
        if (!displayed.isEmpty()) {
            visibleAlternatives.add(displayed);
        }
        for (var emiStack : ingredient.getEmiStacks()) {
            ItemStack stack = emiStack.getItemStack();
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copy = stack.copy();
            if (containsEquivalentStack(visibleAlternatives, copy)) {
                continue;
            }
            visibleAlternatives.add(copy);
        }
        if (visibleAlternatives.isEmpty()) {
            return null;
        }
        visibleAlternatives = narrowSpecificAlternativesToDisplayed(visibleAlternatives);
        Ingredient wideIngredient = Ingredient.of(visibleAlternatives.stream().map(ItemStack::copy));
        if (priorityContext.hasFavoritePriorities()) {
            ItemStack favorited = WcwtRecipeTransferCommon.chooseFavoritedItem(
                    wideIngredient, visibleAlternatives, priorityContext.favoritePriorities());
            if (!favorited.isEmpty()) {
                int count = Math.max(1, (int) Math.min(Integer.MAX_VALUE, ingredient.getAmount()));
                return new RequestedIngredient(
                        orderAlternativesWithPreferredFirst(priorityContext, favorited, visibleAlternatives),
                        count, slotIndex);
            }
        }
        if (WcwtClientConfig.preferJeiBookmarksForPatternEncoding() && priorityContext.hasBookmarkPriorities()) {
            ItemStack bookmarked = WcwtRecipeTransferCommon.chooseBookmarkedItem(
                    wideIngredient, visibleAlternatives, priorityContext.bookmarkPriorities());
            if (!bookmarked.isEmpty()) {
                int count = Math.max(1, (int) Math.min(Integer.MAX_VALUE, ingredient.getAmount()));
                return new RequestedIngredient(
                        orderAlternativesWithPreferredFirst(priorityContext, bookmarked, visibleAlternatives),
                        count, slotIndex);
            }
        }
        ItemStack best = WcwtIngredientPriorities.chooseBestItemForEncoding(priorityContext, wideIngredient,
                visibleAlternatives);
        if (best.isEmpty()) {
            return null;
        }
        int count = Math.max(1, (int) Math.min(Integer.MAX_VALUE, ingredient.getAmount()));
        return new RequestedIngredient(orderAlternativesWithPreferredFirst(priorityContext, best, visibleAlternatives),
                count, slotIndex);
    }

    private static Map<Integer, SlotWidget> getOrderedCraftingInputSlots(Map<Integer, SlotWidget> inputSlots) {
        return inputSlots.values().stream()
                .filter(slot -> slot != null && slot.getStack() != null && !slot.getStack().isEmpty())
                .sorted(java.util.Comparator
                        .comparingInt((SlotWidget slot) -> slot.getBounds().y())
                        .thenComparingInt(slot -> slot.getBounds().x()))
                .limit(9)
                .collect(java.util.stream.Collectors.toMap(
                        slot -> {
                            Bounds bounds = slot.getBounds();
                            int col = Math.max(0, Math.min(2, bounds.x() / 18));
                            int row = Math.max(0, Math.min(2, bounds.y() / 18));
                            return row * 3 + col;
                        },
                        slot -> slot,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new));
    }

    @Nullable
    private static GenericStack toGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                               EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }

        List<GenericStack> candidates = new ArrayList<>();
        List<ItemStack> itemCandidates = new ArrayList<>();
        for (var emiStack : ingredient.getEmiStacks()) {
            GenericStack candidate = toGenericStack(emiStack, ingredient.getAmount());
            if (candidate != null) {
                candidates.add(candidate);
            }
            ItemStack itemStack = emiStack.getItemStack();
            if (!itemStack.isEmpty()) {
                itemCandidates.add(itemStack.copy());
            }
        }
        if (priorityContext.hasFavoritePriorities()) {
            GenericStack favorited = WcwtRecipeTransferCommon.chooseFavoritedStack(candidates,
                    priorityContext.favoritePriorities());
            if (favorited != null) {
                return favorited;
            }
        }
        if (WcwtClientConfig.preferJeiBookmarksForPatternEncoding() && priorityContext.hasBookmarkPriorities()) {
            var priorities = priorityContext.bookmarkPriorities();
            if (!priorities.isEmpty()) {
                GenericStack bookmarked = candidates.stream()
                        .filter(candidate -> candidate.what() != null && priorities.containsKey(candidate.what()))
                        .min(java.util.Comparator.comparingInt(candidate -> priorities.get(candidate.what())))
                        .orElse(null);
                if (bookmarked != null) {
                    return bookmarked;
                }
            }
        }
        if (!itemCandidates.isEmpty()) {
            ItemStack best = WcwtIngredientPriorities.chooseBestItemForEncoding(priorityContext,
                    Ingredient.of(itemCandidates.stream().map(ItemStack::copy)), itemCandidates);
            if (!best.isEmpty()) {
                return GenericStack.fromItemStack(best.copyWithCount(
                        Math.max(1, (int) Math.min(Integer.MAX_VALUE, ingredient.getAmount()))));
            }
        }
        return WcwtIngredientPriorities.chooseBestGenericStackForEncoding(priorityContext, candidates);
    }

    private static WcwtIngredientPriorities.PriorityContext createPriorityContext(WirelessComprehensiveWorkTerminalMenu menu) {
        Map<AEKey, Integer> bookmarkPriorities = WcwtClientConfig.preferJeiBookmarksForPatternEncoding()
                ? WcwtRecipeTransferCommon.getEmiFavoritePriorities()
                : Map.of();
        return WcwtIngredientPriorities.createContext(menu, bookmarkPriorities, getWcwtFavoritePriorities());
    }

    private static Map<AEKey, Integer> getWcwtFavoritePriorities() {
        if (!WcwtClientConfig.preferWcwtFavoritesForRecipeTransfer()) {
            return Map.of();
        }
        var favorites = WcwtFavorites.getFavoritedKeys();
        if (favorites.isEmpty()) {
            return Map.of();
        }
        Map<AEKey, Integer> priorities = new java.util.LinkedHashMap<>(favorites.size());
        int index = 0;
        for (AEKey key : favorites) {
            if (key != null) {
                priorities.putIfAbsent(key, index);
            }
            index++;
        }
        return priorities;
    }

    @Nullable
    private static GenericStack toGenericStack(EmiStack stack, long amount) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        GenericStack fluid = convertFluid(stack, amount);
        if (fluid != null) {
            return fluid;
        }
        GenericStack chemical = convertMekanismChemical(stack.getKey(), amount);
        if (chemical != null) {
            return chemical;
        }
        ItemStack displayed = stack.getItemStack();
        if (displayed.isEmpty()) {
            return null;
        }
        return GenericStack.fromItemStack(displayed.copyWithCount(
                Math.max(1, (int) Math.min(Integer.MAX_VALUE, amount))));
    }

    private record SimpleWidgetCollector(int width, int height, List<Widget> widgets) implements WidgetHolder {
        private SimpleWidgetCollector(int width, int height) {
            this(width, height, new ArrayList<>());
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public <T extends Widget> T add(T widget) {
            widgets.add(widget);
            return widget;
        }
    }

    private static boolean containsEquivalentStack(List<ItemStack> stacks, ItemStack candidate) {
        for (var existing : stacks) {
            if (ItemStack.isSameItemSameComponents(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> orderAlternativesWithPreferredFirst(
            WcwtIngredientPriorities.PriorityContext priorityContext,
            ItemStack preferred,
            List<ItemStack> alternatives) {
        List<ItemStack> ordered = new ArrayList<>();
        if (!preferred.isEmpty()) {
            ordered.add(preferred.copy());
        }
        for (ItemStack alternative : WcwtIngredientPriorities.sortItemAlternatives(priorityContext, alternatives)) {
            if (!containsEquivalentStack(ordered, alternative)) {
                ordered.add(alternative.copy());
            }
        }
        return ordered;
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

    private record PreviewResult(Set<Integer> missingSlots,
                                 Set<Integer> craftableSlots,
                                 boolean anyResolved,
                                 int inputCount) {
        private static PreviewResult previewOnly() {
            return new PreviewResult(Set.of(), Set.of(), false, 0);
        }

        private boolean anyMissingOrCraftable() {
            return !missingSlots.isEmpty() || !craftableSlots.isEmpty();
        }
    }

}
