package com.lhy.wcwt.compat.jei;

import appeng.integration.modules.itemlists.TransferHelper;
import appeng.menu.me.items.CraftingTermMenu;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Mirrors AE2 REI crafting transfer: COSMETIC error so the plus button stays usable while missing items
 * are highlighted (red / blue for craftables) and AE2 localized tooltips explain the behaviour.
 */
public final class WcwtPartialRecipeTransferError implements IRecipeTransferError {

    private final CraftingTermMenu.MissingIngredientSlots missing;
    private final List<IRecipeSlotView> slotViewsByIngredientIndex;

    public WcwtPartialRecipeTransferError(CraftingTermMenu.MissingIngredientSlots missing,
                                          List<IRecipeSlotView> slotViewsByIngredientIndex) {
        this.missing = missing;
        this.slotViewsByIngredientIndex = slotViewsByIngredientIndex;
    }

    @Override
    public Type getType() {
        return Type.COSMETIC;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip) {
        tooltip.addAll(TransferHelper.createCraftingTooltip(missing, Minecraft.getInstance().hasControlDown(), true));
    }

    @Override
    public int getButtonHighlightColor() {
        return missing.anyMissing()
                ? TransferHelper.ORANGE_PLUS_BUTTON_COLOR
                : TransferHelper.BLUE_PLUS_BUTTON_COLOR;
    }

    @Override
    public void showError(GuiGraphicsExtractor guiGraphics,
                          int mouseX,
                          int mouseY,
                          IRecipeSlotsView recipeSlotsView,
                          int recipeX,
                          int recipeY) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(recipeX, recipeY);
        try {
            for (var guiSlot : missing.missingSlots()) {
                drawSlotHighlight(guiGraphics, guiSlot, TransferHelper.RED_SLOT_HIGHLIGHT_COLOR);
            }
            for (var guiSlot : missing.craftableSlots()) {
                drawSlotHighlight(guiGraphics, guiSlot, TransferHelper.BLUE_SLOT_HIGHLIGHT_COLOR);
            }
        } finally {
            poseStack.popMatrix();
        }
    }

    private void drawSlotHighlight(GuiGraphicsExtractor guiGraphics, int ingredientIndex, int color) {
        if (ingredientIndex < 0 || ingredientIndex >= slotViewsByIngredientIndex.size()) {
            return;
        }
        slotViewsByIngredientIndex.get(ingredientIndex).drawHighlight(guiGraphics, color);
    }
}
