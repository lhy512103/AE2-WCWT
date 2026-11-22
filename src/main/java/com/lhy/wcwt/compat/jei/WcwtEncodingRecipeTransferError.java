package com.lhy.wcwt.compat.jei;

import appeng.integration.modules.itemlists.TransferHelper;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Cosmetic slot highlights + AE2 encoding tooltip when JEI + targets WCWT pattern encode area. */
public record WcwtEncodingRecipeTransferError(List<IRecipeSlotView> craftableSlots) implements IRecipeTransferError {

    @Override
    public Type getType() {
        return Type.COSMETIC;
    }

    @Override
    public int getButtonHighlightColor() {
        return 0;
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
            for (IRecipeSlotView slotView : craftableSlots) {
                slotView.drawHighlight(guiGraphics, TransferHelper.BLUE_SLOT_HIGHLIGHT_COLOR);
            }
        } finally {
            poseStack.popMatrix();
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip) {
        tooltip.addAll(TransferHelper.createEncodingTooltip(!craftableSlots.isEmpty(), true));
    }
}
