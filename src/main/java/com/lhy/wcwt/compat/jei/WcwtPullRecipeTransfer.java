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
import com.lhy.wcwt.pull.WcwtPullIngredientOrdering;

/** JEI「+」从 ME 拉配方原料：锁定合成网格时生效；编码逻辑见 {@link WcwtRecipeTransferHandler}。 */
public final class WcwtPullRecipeTransfer {

    private WcwtPullRecipeTransfer() {
    }

    public static IRecipeTransferError transfer(WirelessComprehensiveWorkTerminalMenu menu, Object recipeIgnored,
            IRecipeSlotsView recipeSlots, Player player,
            boolean maxTransfer, boolean doTransfer, IRecipeTransferHandlerHelper transferHelper) {
        boolean craftMissing = Screen.hasControlDown();

        List<RequestedIngredient> requestedIngredients = collectRequestedIngredients(recipeSlots);
        if (requestedIngredients.isEmpty()) {
            return transferHelper.createUserErrorWithTooltip(
                    Component.translatable("message.wcwt.pull_no_inputs"));
        }

        if (!doTransfer) {
            var preview = findTransferPreview(menu, recipeSlots);
            if (preview.anyMissingOrCraftable()) {
                return new TerminalPullTransferError(preview, craftMissing);
            }
            return null;
        }

        PacketDistributor.sendToServer(new WcwtPullRecipeInputsPacket(maxTransfer, craftMissing, requestedIngredients));
        return null;
    }

    private static List<RequestedIngredient> collectRequestedIngredients(IRecipeSlotsView recipeSlots) {
        return collectInputSlots(recipeSlots).stream()
                .filter(WcwtPullRecipeTransfer::hasItemStack)
                .map(WcwtPullRecipeTransfer::toRequestedIngredient)
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

    private static RequestedIngredient toRequestedIngredient(IRecipeSlotView slotView) {
        List<ItemStack> alternatives = WcwtPullIngredientOrdering.preferSpecificComponentsFirst(slotView.getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .distinct()
                .toList());
        int count = Math.max(getDisplayedStack(slotView).getCount(), 1);
        return new RequestedIngredient(alternatives, count);
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

        @Override
        public Type getType() {
            return preview.anyCraftable() || preview.canIgnoreMissing() ? Type.COSMETIC : Type.USER_FACING;
        }

        @Override
        public int getButtonHighlightColor() {
            if (preview.anyMissing()) {
                return ORANGE_BUTTON_HIGHLIGHT_COLOR;
            }
            if (preview.anyCraftable()) {
                return BLUE_BUTTON_HIGHLIGHT_COLOR;
            }
            return 0;
        }

        @Override
        public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
            var poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(recipeX, recipeY, 0);

            for (IRecipeSlotView slot : preview.missingSlots()) {
                slot.drawHighlight(guiGraphics, RED_SLOT_HIGHLIGHT_COLOR);
            }
            for (IRecipeSlotView slot : preview.craftableSlots()) {
                slot.drawHighlight(guiGraphics, BLUE_SLOT_HIGHLIGHT_COLOR);
            }

            poseStack.popPose();
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip) {
            tooltip.add(ItemModText.MOVE_ITEMS.text());

            if (preview.anyCraftable()) {
                var line = craftMissing ? ItemModText.WILL_CRAFT.text() : ItemModText.CTRL_CLICK_TO_CRAFT.text();
                tooltip.add(line.withStyle(ChatFormatting.BLUE));
            }

            if (preview.anyMissing()) {
                var line = preview.canIgnoreMissing()
                        ? ItemModText.MISSING_ITEMS.text()
                        : ItemModText.NO_ITEMS.text();
                tooltip.add(line.withStyle(ChatFormatting.RED));
            }

            tooltip.add(Component.translatable("message.wcwt.pull_shift_hint").withStyle(ChatFormatting.GRAY));
        }

        @Override
        public int getMissingCountHint() {
            return preview.totalSize();
        }
    }
}
