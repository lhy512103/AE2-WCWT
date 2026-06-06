package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.Icon;
import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * AE2 toolbar button for showing or hiding the view-cell panel.
 */
public class ViewCellsToggleButton extends appeng.client.gui.widgets.IconButton {
    private static final ResourceLocation TEXTURE =
            ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_states.png");

    private static final int ICON_U_ON = 144;
    private static final int ICON_U_OFF = 160;
    private static final int ICON_V = 32;
    private static final int ICON_W = 16;
    private static final int ICON_H = 16;

    private final BooleanSupplier visibleState;

    public ViewCellsToggleButton(BooleanSupplier visibleState, OnPress onPress) {
        super(onPress);
        this.visibleState = visibleState;
        refreshMessage();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }

        refreshMessage();
        if (isFocused()) {
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), -1);
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + height, -1);
            guiGraphics.fill(getX() + width, getY(), getX() + width + 1, getY() + height, -1);
            guiGraphics.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, -1);
        }

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 2);
        Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).blit(guiGraphics);
        pose.translate(0, 0, 1);
        guiGraphics.blit(TEXTURE, getX(), getY(), visibleState.getAsBoolean() ? ICON_U_ON : ICON_U_OFF,
                ICON_V, ICON_W, ICON_H, 256, 256);
        pose.popPose();
    }

    @Override
    protected Icon getIcon() {
        return Icon.TOOLBAR_BUTTON_BACKGROUND;
    }

    public void refreshMessage() {
        setMessage(Component.translatable(visibleState.getAsBoolean()
                ? "gui.wcwt.view_cells.visible"
                : "gui.wcwt.view_cells.hidden"));
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(getMessage());
    }
}
