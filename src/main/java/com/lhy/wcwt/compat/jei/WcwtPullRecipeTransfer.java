package com.lhy.wcwt.compat.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
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
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import com.lhy.wcwt.pull.WcwtPullIngredientOrdering;

/** JEI「+」从 ME 拉配方原料：锁定合成网格时生效；编码逻辑见 {@link WcwtRecipeTransferHandler}。 */
public final class WcwtPullRecipeTransfer {

    private WcwtPullRecipeTransfer() {
    }

    public static IRecipeTransferError transfer(WirelessComprehensiveWorkTerminalMenu menu, Object recipeIgnored,
            IRecipeSlotsView recipeSlots, Player player,
            boolean maxTransfer, boolean doTransfer, IRecipeTransferHandlerHelper transferHelper) {
        boolean effectiveMaxTransfer = maxTransfer || Screen.hasShiftDown();
        boolean craftMissing = Screen.hasControlDown();

        if (!doTransfer) {
            if (hasAnyInput(recipeSlots)) {
                return new TerminalPullTransferError(findTransferPreview(menu, recipeSlots), craftMissing);
            }
            return null;
        }

        List<RequestedIngredient> requestedIngredients = collectRequestedIngredients(menu, recipeSlots);
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
        return collectInputSlots(recipeSlots).stream()
                .filter(WcwtPullRecipeTransfer::hasItemStack)
                .map(slotView -> toRequestedIngredient(menu, slotView))
                .filter(ingredient -> !ingredient.alternatives().isEmpty())
                .toList();
    }

    private static PreviewSlots findTransferPreview(MEStorageMenu container, IRecipeSlotsView recipeSlots) {
        var reservedTerminalAmounts = new Object2IntOpenHashMap<>();
        var playerItems = container.getPlayerInventory().items;
        var reservedPlayerItems = new int[playerItems.size()];
        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        List<IRecipeSlotView> craftableSlots = new ArrayList<>();
        boolean anyResolved = false;

        for (IRecipeSlotView slotView : collectInputSlots(recipeSlots)) {
            var ingredient = toIngredient(slotView);
            if (ingredient.isEmpty()) {
                continue;
            }

            int requiredCount = getDisplayedStack(slotView).getCount();
            requiredCount = Math.max(requiredCount, 1);

            boolean missing = false;
            boolean craftable = false;
            for (int i = 0; i < requiredCount; i++) {
                boolean found = false;
                for (int slot = 0; slot < playerItems.size(); slot++) {
                    if (container.isPlayerInventorySlotLocked(slot)) {
                        continue;
                    }

                    var stack = playerItems.get(slot);
                    if (stack.getCount() - reservedPlayerItems[slot] > 0 && ingredient.test(stack)) {
                        reservedPlayerItems[slot]++;
                        found = true;
                        anyResolved = true;
                        break;
                    }
                }

                if (!found && container.hasIngredient(ingredient, reservedTerminalAmounts)) {
                    reservedTerminalAmounts.merge(ingredient, 1, Integer::sum);
                    found = true;
                    anyResolved = true;
                }

                if (!found && hasCraftableAlternative(container, ingredient)) {
                    craftable = true;
                    found = true;
                    anyResolved = true;
                }

                if (!found) {
                    missing = true;
                }
            }

            if (missing) {
                missingSlots.add(slotView);
            }
            if (craftable) {
                craftableSlots.add(slotView);
            }
        }

        return new PreviewSlots(missingSlots, craftableSlots, anyResolved);
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

    private static boolean hasCraftableAlternative(MEStorageMenu container, Ingredient ingredient) {
        var clientRepo = container.getClientRepo();
        if (clientRepo == null) {
            return false;
        }

        for (var entry : clientRepo.getAllEntries()) {
            if (!entry.isCraftable() || !(entry.getWhat() instanceof AEItemKey what)) {
                continue;
            }

            for (ItemStack alternative : ingredient.getItems()) {
                if (what.matches(alternative)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasItemStack(IRecipeSlotView slotView) {
        return slotView.getDisplayedItemStack().isPresent() || slotView.getItemStacks().findAny().isPresent();
    }

    private static Ingredient toIngredient(IRecipeSlotView slotView) {
        return Ingredient.of(slotView.getItemStacks().map(ItemStack::copy));
    }

    private static RequestedIngredient toRequestedIngredient(WirelessComprehensiveWorkTerminalMenu menu,
                                                             IRecipeSlotView slotView) {
        List<ItemStack> alternatives = chooseRequestedAlternative(menu, slotView);
        int count = Math.max(getDisplayedStack(slotView).getCount(), 1);
        return new RequestedIngredient(alternatives, count);
    }

    private static List<ItemStack> chooseRequestedAlternative(WirelessComprehensiveWorkTerminalMenu menu,
                                                              IRecipeSlotView slotView) {
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
        if (visibleAlternatives.isEmpty()) {
            return List.of();
        }
        Ingredient ingredient = Ingredient.of(visibleAlternatives.stream().map(ItemStack::copy));
        ItemStack best = WcwtIngredientPriorities.chooseBestItem(menu, ingredient, visibleAlternatives);
        return best.isEmpty() ? List.of() : List.of(best);
    }

    private static boolean containsEquivalentStack(List<ItemStack> stacks, ItemStack candidate) {
        for (ItemStack existing : stacks) {
            if (ItemStack.isSameItemSameComponents(existing, candidate)) {
                return true;
            }
        }
        return false;
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

        private TerminalPullTransferError(PreviewSlots preview, boolean craftMissing) {
            this.preview = preview;
            this.craftMissing = craftMissing;
        }

        private static TerminalPullTransferError previewOnly(boolean craftMissing) {
            return new TerminalPullTransferError(new PreviewSlots(List.of(), List.of(), false), craftMissing);
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
            tooltip.add(Component.translatable("message.wcwt.pull_shift_hint").withStyle(ChatFormatting.GRAY));
        }

        @Override
        public int getMissingCountHint() {
            return 0;
        }
    }
}
