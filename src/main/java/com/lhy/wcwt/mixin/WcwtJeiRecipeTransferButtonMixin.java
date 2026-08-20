package com.lhy.wcwt.mixin;

import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import mezz.jei.gui.recipes.RecipeTransferButtonController;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = RecipeTransferButtonController.class, remap = false)
public abstract class WcwtJeiRecipeTransferButtonMixin {
    private static final int LOCKED_GRID_BORDER_COLOR = 0xFFFF0000;
    private static final int LOCKED_GRID_BORDER_THICKNESS = 1;

    @Shadow
    private RecipesGui recipesGui;

    @Inject(method = "drawExtras", at = @At("TAIL"))
    private void wcwt$drawLockedCraftingGridBorder(GuiGraphics guiGraphics, Rect2i buttonArea,
                                                   int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (recipesGui == null || buttonArea == null || !WcwtClientConfig.lockedCraftingGridJeiTransferBorder()) {
            return;
        }
        AbstractContainerMenu menu = recipesGui.getParentContainerMenu();
        if (!(menu instanceof WirelessComprehensiveWorkTerminalMenu wcwt)
                || wcwt.getMenuHost() == null
                || !wcwt.getMenuHost().isCraftingGridLocked()) {
            return;
        }
        int x = buttonArea.getX();
        int y = buttonArea.getY();
        int x2 = x + buttonArea.getWidth();
        int y2 = y + buttonArea.getHeight();
        int t = LOCKED_GRID_BORDER_THICKNESS;
        guiGraphics.fill(RenderType.guiOverlay(), x, y, x2, y + t, LOCKED_GRID_BORDER_COLOR);
        guiGraphics.fill(RenderType.guiOverlay(), x, y2 - t, x2, y2, LOCKED_GRID_BORDER_COLOR);
        guiGraphics.fill(RenderType.guiOverlay(), x, y, x + t, y2, LOCKED_GRID_BORDER_COLOR);
        guiGraphics.fill(RenderType.guiOverlay(), x2 - t, y, x2, y2, LOCKED_GRID_BORDER_COLOR);
    }
}
