package com.lhy.wcwt.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.network.PacketDistributor;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import appeng.api.stacks.AEItemKey;
import appeng.core.localization.ItemModText;
import appeng.menu.me.common.MEStorageMenu;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket.RequestedIngredient;
import com.lhy.wcwt.compat.WcwtRecipeTransferCommon;
import com.lhy.wcwt.client.WcwtFavorites;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import com.lhy.wcwt.pull.WcwtStackMatching;
import appeng.parts.encoding.EncodingMode;

/** JEI「+」从 ME 拉配方原料：锁定合成网格时生效；编码逻辑见 {@link WcwtRecipeTransferHandler}。 */
public final class WcwtPullRecipeTransfer {

    private WcwtPullRecipeTransfer() {
    }

    public static IRecipeTransferError transfer(WirelessComprehensiveWorkTerminalMenu menu, Object recipeIgnored,
            IRecipeSlotsView recipeSlots, Player player,
            boolean maxTransfer, boolean doTransfer, IRecipeTransferHandlerHelper transferHelper,
            boolean allowShiftMaxTransfer) {
        boolean effectiveMaxTransfer = allowShiftMaxTransfer && (maxTransfer || Screen.hasShiftDown());
        boolean craftMissing = Screen.hasControlDown();

        if (!doTransfer) {
            if (hasAnyInput(recipeSlots)) {
                return new TerminalPullTransferError(findTransferPreview(menu, recipeSlots), craftMissing,
                        allowShiftMaxTransfer);
            }
            return null;
        }

        List<RequestedIngredient> requestedIngredients = collectRequestedIngredients(menu, recipeSlots);
        if (menu.getManualWorkspaceMode() == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING
                && WcwtRecipeTransferHandler.getTransferMode(recipeIgnored, recipeSlots) == EncodingMode.PROCESSING) {
            requestedIngredients = mergeProcessingIngredients(requestedIngredients);
        }
        if (requestedIngredients.isEmpty()) {
            return transferHelper.createUserErrorWithTooltip(
                    Component.translatable("message.wcwt.pull_no_inputs"));
        }

        PacketDistributor.sendToServer(new WcwtPullRecipeInputsPacket(effectiveMaxTransfer, craftMissing, requestedIngredients,
                menu.getManualWorkspaceMode().ordinal()));
        return null;
    }

    private static List<RequestedIngredient> collectRequestedIngredients(WirelessComprehensiveWorkTerminalMenu menu,
                                                                         IRecipeSlotsView recipeSlots) {
        List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        List<RequestedIngredient> result = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < inputs.size(); slotIndex++) {
            IRecipeSlotView slotView = inputs.get(slotIndex);
            if (!hasItemStack(slotView)) {
                continue;
            }
            RequestedIngredient ingredient = toRequestedIngredient(menu, slotView, slotIndex);
            if (!ingredient.alternatives().isEmpty()) {
                result.add(ingredient);
            }
        }
        return result;
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

    private static final byte MISSING = 1;
    private static final byte CRAFTABLE = 2;

    /** 库存/可合成变化无显式版本号，用每槽 tick TTL 兜底刷新（0.5s）。 */
    private static final long PREVIEW_TTL_TICKS = 10L;

    private static final Map<IRecipeSlotsView, CachedPreview> PREVIEW_CACHE = new WeakHashMap<>();

    private record CachedPreview(byte[] flags, boolean anyResolved, int invHash, long computedAtTick) {
    }

    /**
     * JEI 每 tick（每个屏上布局）都会调一次 doTransfer=false 预览且不缓存，故按 {@link IRecipeSlotsView}
     * 对象身份（{@code RecipeLayout} 每 tick 返回同一实例）缓存判定：背包未变且未超 TTL 时直接复用，
     * 避开重复的 {@code Ingredient.of} 候选展开 / 全背包扫描 / {@code getByIngredient}。
     * 缓存只存每槽判定位，高亮用的 slotView 取自当次调用，避免过期引用画错位与 WeakHashMap key 被 pin。
     */
    private static PreviewSlots findTransferPreview(MEStorageMenu container, IRecipeSlotsView recipeSlots) {
        List<IRecipeSlotView> inputs = collectInputSlots(recipeSlots);

        int invHash = playerInventoryHash(container);
        long tick = currentClientTick();

        CachedPreview cached = PREVIEW_CACHE.get(recipeSlots);
        if (cached != null
                && cached.flags.length == inputs.size()
                && cached.invHash == invHash
                && tick - cached.computedAtTick < PREVIEW_TTL_TICKS) {
            return assemble(inputs, cached.flags, cached.anyResolved);
        }

        byte[] flags = new byte[inputs.size()];
        boolean anyResolved = computeFlags(container, inputs, flags);
        PREVIEW_CACHE.put(recipeSlots, new CachedPreview(flags, anyResolved, invHash, tick));
        return assemble(inputs, flags, anyResolved);
    }

    private static boolean computeFlags(MEStorageMenu container, List<IRecipeSlotView> inputs, byte[] flags) {
        var reservedTerminalAmounts = new Object2IntOpenHashMap<AEItemKey>();
        var playerItems = container.getPlayerInventory().items;
        var reservedPlayerItems = new int[playerItems.size()];
        boolean anyResolved = false;

        for (int idx = 0; idx < inputs.size(); idx++) {
            IRecipeSlotView slotView = inputs.get(idx);
            List<ItemStack> alternatives = narrowSpecificAlternativesToDisplayed(getAlternativeStacks(slotView));
            if (alternatives.isEmpty()) {
                continue;
            }
            Ingredient ingredient = Ingredient.of(alternatives.stream().map(ItemStack::copy));

            int requiredCount = Math.max(getDisplayedStack(slotView).getCount(), 1);

            boolean missing = false;
            boolean craftable = false;
            for (int i = 0; i < requiredCount; i++) {
                boolean found = false;
                for (int slot = 0; slot < playerItems.size(); slot++) {
                    if (container.isPlayerInventorySlotLocked(slot)) {
                        continue;
                    }

                    var stack = playerItems.get(slot);
                    if (stack.getCount() - reservedPlayerItems[slot] > 0
                            && WcwtStackMatching.matchesAnyAlternative(stack, alternatives, ingredient)) {
                        reservedPlayerItems[slot]++;
                        found = true;
                        anyResolved = true;
                        break;
                    }
                }

                if (!found && WcwtStackMatching.reserveClientRepoStoredIngredient(container, alternatives, ingredient,
                        reservedTerminalAmounts)) {
                    found = true;
                    anyResolved = true;
                }

                if (!found && WcwtStackMatching.hasClientRepoCraftableIngredient(container, alternatives, ingredient)) {
                    craftable = true;
                    found = true;
                    anyResolved = true;
                }

                if (!found) {
                    missing = true;
                }
            }

            byte f = 0;
            if (missing) {
                f |= MISSING;
            }
            if (craftable) {
                f |= CRAFTABLE;
            }
            flags[idx] = f;
        }

        return anyResolved;
    }

    private static PreviewSlots assemble(List<IRecipeSlotView> inputs, byte[] flags, boolean anyResolved) {
        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        List<IRecipeSlotView> craftableSlots = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            byte f = flags[i];
            if ((f & MISSING) != 0) {
                missingSlots.add(inputs.get(i));
            }
            if ((f & CRAFTABLE) != 0) {
                craftableSlots.add(inputs.get(i));
            }
        }
        return new PreviewSlots(missingSlots, craftableSlots, anyResolved);
    }

    private static int playerInventoryHash(MEStorageMenu container) {
        var items = container.getPlayerInventory().items;
        int h = 1;
        for (int slot = 0; slot < items.size(); slot++) {
            var stack = items.get(slot);
            long stackHash = stack.isEmpty() ? 0L : ItemStack.hashItemAndComponents(stack);
            h = 31 * h + Long.hashCode(stackHash);
            h = 31 * h + stack.getCount();
            h = 31 * h + (container.isPlayerInventorySlotLocked(slot) ? 1 : 0);
        }
        return h;
    }

    private static long currentClientTick() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    private static List<IRecipeSlotView> collectInputSlots(IRecipeSlotsView recipeSlots) {
        return recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .filter(WcwtPullRecipeTransfer::hasItemStack)
                .toList();
    }

    private static boolean hasAnyInput(IRecipeSlotsView recipeSlots) {
        return recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .anyMatch(WcwtPullRecipeTransfer::hasItemStack);
    }

    private static boolean hasItemStack(IRecipeSlotView slotView) {
        return slotView.getDisplayedItemStack().isPresent() || slotView.getItemStacks().findAny().isPresent();
    }

    private static RequestedIngredient toRequestedIngredient(WirelessComprehensiveWorkTerminalMenu menu,
                                                             IRecipeSlotView slotView,
                                                             int slotIndex) {
        List<ItemStack> alternatives = chooseRequestedAlternative(menu, slotView);
        int count = Math.max(getDisplayedStack(slotView).getCount(), 1);
        return new RequestedIngredient(alternatives, count, slotIndex);
    }

    private static List<ItemStack> chooseRequestedAlternative(WirelessComprehensiveWorkTerminalMenu menu,
                                                              IRecipeSlotView slotView) {
        List<ItemStack> visibleAlternatives = narrowSpecificAlternativesToDisplayed(getAlternativeStacks(slotView));
        if (visibleAlternatives.isEmpty()) {
            return List.of();
        }
        Ingredient ingredient = Ingredient.of(visibleAlternatives.stream().map(ItemStack::copy));
        var priorityContext = WcwtIngredientPriorities.createContext(menu, Map.of(), getWcwtFavoritePriorities());
        ItemStack best = WcwtRecipeTransferCommon.chooseFavoritedItem(
                ingredient, visibleAlternatives, priorityContext.favoritePriorities());
        if (best.isEmpty()) {
            best = WcwtIngredientPriorities.chooseBestItem(priorityContext, ingredient, visibleAlternatives);
        }
        return orderAlternativesWithPreferredFirst(priorityContext, best, visibleAlternatives);
    }

    private static List<ItemStack> getAlternativeStacks(IRecipeSlotView slotView) {
        List<ItemStack> visibleAlternatives = new ArrayList<>();
        ItemStack displayed = getDisplayedStack(slotView);
        if (!displayed.isEmpty()) {
            visibleAlternatives.add(displayed.copy());
        }
        slotView.getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .forEach(stack -> {
                    ItemStack copy = stack.copy();
                    if (!containsEquivalentStack(visibleAlternatives, copy)) {
                        visibleAlternatives.add(copy);
                    }
                });
        return visibleAlternatives;
    }

    private static List<ItemStack> narrowSpecificAlternativesToDisplayed(List<ItemStack> alternatives) {
        if (!WcwtStackMatching.requiresExactItemKeyMatch(alternatives)) {
            return alternatives;
        }
        // Collect all alternatives with specific NBT data
        List<ItemStack> specific = new ArrayList<>();
        for (ItemStack alt : alternatives) {
            if (alt != null && !alt.isEmpty() && WcwtStackMatching.hasSpecificData(alt)) {
                specific.add(alt.copy());
            }
        }
        if (!specific.isEmpty()) {
            return specific;
        }
        // Fallback: no specific-alternatives found, return first non-empty
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty()) {
                return List.of(alternative.copy());
            }
        }
        return List.of();
    }

    private static boolean containsEquivalentStack(List<ItemStack> stacks, ItemStack candidate) {
        for (ItemStack existing : stacks) {
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

    private static Map<appeng.api.stacks.AEKey, Integer> getWcwtFavoritePriorities() {
        if (!WcwtClientConfig.preferWcwtFavoritesForRecipeTransfer()) {
            return Map.of();
        }
        var favorites = WcwtFavorites.getFavoritedKeys();
        if (favorites.isEmpty()) {
            return Map.of();
        }
        Map<appeng.api.stacks.AEKey, Integer> priorities = new java.util.LinkedHashMap<>(favorites.size());
        int index = 0;
        for (appeng.api.stacks.AEKey key : favorites) {
            if (key != null) {
                priorities.putIfAbsent(key, index);
            }
            index++;
        }
        return priorities;
    }

    private static ItemStack getDisplayedStack(IRecipeSlotView slotView) {
        return slotView.getDisplayedItemStack()
                .or(() -> slotView.getItemStacks().findFirst())
                .orElse(ItemStack.EMPTY);
    }

    private record PreviewSlots(List<IRecipeSlotView> missingSlots, List<IRecipeSlotView> craftableSlots, boolean anyResolved) {
        public boolean anyMissing() {
            return !missingSlots.isEmpty();
        }

        public boolean anyCraftable() {
            return !craftableSlots.isEmpty();
        }

        public boolean anyMissingOrCraftable() {
            return anyMissing() || anyCraftable();
        }

        public boolean canIgnoreMissing() {
            return anyMissing() && anyResolved;
        }

        public int totalSize() {
            return missingSlots.size() + craftableSlots.size();
        }
    }

    private static final class TerminalPullTransferError implements IRecipeTransferError {
        private static final int RED_SLOT_HIGHLIGHT_COLOR = 0x66FF0000;
        private static final int BLUE_SLOT_HIGHLIGHT_COLOR = 0x400000FF;
        private static final int BLUE_BUTTON_HIGHLIGHT_COLOR = 0x804545FF;
        private static final int ORANGE_BUTTON_HIGHLIGHT_COLOR = 0x80FFA500;

        private final PreviewSlots preview;
        private final boolean craftMissing;
        private final boolean allowShiftMaxTransfer;

        private TerminalPullTransferError(PreviewSlots preview, boolean craftMissing, boolean allowShiftMaxTransfer) {
            this.preview = preview;
            this.craftMissing = craftMissing;
            this.allowShiftMaxTransfer = allowShiftMaxTransfer;
        }

        private static TerminalPullTransferError previewOnly(boolean craftMissing, boolean allowShiftMaxTransfer) {
            return new TerminalPullTransferError(new PreviewSlots(List.of(), List.of(), false), craftMissing,
                    allowShiftMaxTransfer);
        }

        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public int getButtonHighlightColor() {
            if (!preview.anyMissingOrCraftable()) {
                return BLUE_BUTTON_HIGHLIGHT_COLOR;
            }
            return preview.anyMissing() ? ORANGE_BUTTON_HIGHLIGHT_COLOR : BLUE_BUTTON_HIGHLIGHT_COLOR;
        }

        @Override
        public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
            var poseStack = guiGraphics.pose();
            poseStack.pushPose();
            try {
                poseStack.translate(recipeX, recipeY, 0);
                for (IRecipeSlotView slotView : preview.missingSlots()) {
                    slotView.drawHighlight(guiGraphics, RED_SLOT_HIGHLIGHT_COLOR);
                }
                for (IRecipeSlotView slotView : preview.craftableSlots()) {
                    if (!preview.missingSlots().contains(slotView)) {
                        slotView.drawHighlight(guiGraphics, BLUE_SLOT_HIGHLIGHT_COLOR);
                    }
                }
            } finally {
                poseStack.popPose();
            }
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip) {
            tooltip.add(ItemModText.MOVE_ITEMS.text());
            if (preview.anyCraftable()) {
                var line = craftMissing ? ItemModText.WILL_CRAFT.text() : ItemModText.CTRL_CLICK_TO_CRAFT.text();
                tooltip.add(line.withStyle(ChatFormatting.BLUE));
            }
            if (preview.anyMissing()) {
                var line = preview.canIgnoreMissing() ? ItemModText.MISSING_ITEMS.text() : ItemModText.NO_ITEMS.text();
                tooltip.add(line.withStyle(ChatFormatting.RED));
            }
            if (allowShiftMaxTransfer) {
                tooltip.add(Component.translatable("message.wcwt.pull_shift_hint").withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        public int getMissingCountHint() {
            return 0;
        }
    }
}
