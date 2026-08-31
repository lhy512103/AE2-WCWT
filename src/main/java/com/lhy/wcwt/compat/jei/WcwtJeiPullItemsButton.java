package com.lhy.wcwt.compat.jei;

import appeng.client.gui.Icon;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.compat.WcwtPullItemsSupport;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Player;

public final class WcwtJeiPullItemsButton implements IRecipeButtonControllerFactory {
    static IRecipeTransferHandlerHelper helper;

    @Override
    public <T> IIconButtonController createButtonController(IRecipeLayoutDrawable<T> recipeLayout) {
        return new Controller(recipeLayout);
    }

    private static final class Controller implements IIconButtonController {
        private final IRecipeLayoutDrawable<?> recipeLayout;
        private IRecipeTransferError preview;

        private final IDrawable icon = new IDrawable() {
            @Override
            public int getWidth() {
                return 10;
            }

            @Override
            public int getHeight() {
                return 10;
            }

            @Override
            public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                int highlight = preview == null ? 0 : preview.getButtonHighlightColor();
                if (highlight != 0 && WcwtPullRecipeTransfer.canActivate(preview)) {
                    graphics.fill(xOffset - 1, yOffset - 1, xOffset + 11, yOffset + 11, highlight);
                }
                Icon.CRAFT_HAMMER.getBlitter().dest(xOffset, yOffset, 10, 10).blit(graphics);
            }
        };

        private Controller(IRecipeLayoutDrawable<?> recipeLayout) {
            this.recipeLayout = recipeLayout;
        }

        @Override
        public void initState(IButtonState state) {
            state.setIcon(icon);
            state.setVisible(false);
            state.setActive(false);
        }

        @Override
        public void updateState(IButtonState state) {
            preview = createPreview();
            boolean show = preview != null
                    && WcwtPullItemsSupport.shouldShowExtraButton()
                    && WcwtPullRecipeTransfer.hasPullableInputs(recipeLayout.getRecipeSlotsView());
            state.setVisible(show);
            state.setActive(show && WcwtPullRecipeTransfer.canActivate(preview));
        }

        @Override
        public void drawExtras(GuiGraphics graphics, Rect2i buttonArea, int mouseX, int mouseY, float partialTicks) {
            if (preview == null || !WcwtPullItemsSupport.shouldShowExtraButton()) {
                return;
            }
            if (mouseX < buttonArea.getX() || mouseX >= buttonArea.getX() + buttonArea.getWidth()
                    || mouseY < buttonArea.getY() || mouseY >= buttonArea.getY() + buttonArea.getHeight()) {
                return;
            }
            var recipeRect = recipeLayout.getRect();
            preview.showError(graphics, mouseX, mouseY, recipeLayout.getRecipeSlotsView(),
                    recipeRect.getX(), recipeRect.getY());
        }

        @Override
        public boolean onPress(IJeiUserInput input) {
            if (!WcwtPullItemsSupport.shouldShowExtraButton() || !WcwtPullRecipeTransfer.canActivate(preview)) {
                return false;
            }
            if (input.isSimulate()) {
                return true;
            }
            var player = Minecraft.getInstance().player;
            if (player == null || !(player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)
                    || helper == null) {
                return false;
            }
            EncodingMode mode = WcwtRecipeTransferHandler.getTransferMode(
                    recipeLayout.getRecipe(), recipeLayout.getRecipeSlotsView());
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            WcwtPullRecipeTransfer.transfer(menu, recipeLayout.getRecipe(), recipeLayout.getRecipeSlotsView(),
                    player, false, true, helper, mode != EncodingMode.CRAFTING);
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                screen.onClose();
            }
            return true;
        }

        @Override
        public void getTooltips(ITooltipBuilder tooltip) {
            if (preview != null) {
                preview.getTooltip(tooltip);
            }
        }

        private IRecipeTransferError createPreview() {
            if (helper == null || !WcwtPullItemsSupport.shouldShowExtraButton()
                    || WcwtRecipeTransferHandler.shouldSkipTransferAnalysis(recipeLayout.getRecipe())) {
                return null;
            }
            Player player = Minecraft.getInstance().player;
            if (player == null || !(player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
                return null;
            }
            var slots = recipeLayout.getRecipeSlotsView();
            if (!WcwtPullRecipeTransfer.hasPullableInputs(slots)) {
                return null;
            }
            EncodingMode mode = WcwtRecipeTransferHandler.getTransferMode(recipeLayout.getRecipe(), slots);
            return WcwtPullRecipeTransfer.transfer(menu, recipeLayout.getRecipe(), slots, player, false, false,
                    helper, mode != EncodingMode.CRAFTING);
        }
    }
}
