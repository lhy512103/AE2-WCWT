package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 使用 AE 原版左侧工具栏按钮底图，只覆盖 WCWT 自定义显示元件图标。
 */
public class ViewCellsToggleButton extends IconButton {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");

    private static final int ICON_U_VISIBLE = 144;
    private static final int ICON_U_HIDDEN = 160;
    private static final int ICON_V = 32;
    private static final int ICON_W = 16;
    private static final int ICON_H = 16;

    private final BooleanSupplier viewCellsVisible;

    public ViewCellsToggleButton(BooleanSupplier viewCellsVisible, OnPress onPress) {
        super(onPress);
        this.viewCellsVisible = viewCellsVisible;
        setMessage(Component.translatable("gui.wcwt.view_cells.visible"));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }

        boolean pressed = isHovered() || isFocused();
        int yOffset = pressed ? 1 : 0;
        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
        bgIcon.getBlitter()
                .dest(getX() - 1, getY() + yOffset, 18, 20)
                .zOffset(2)
                .blit(guiGraphics);

        int iconU = viewCellsVisible.getAsBoolean() ? ICON_U_VISIBLE : ICON_U_HIDDEN;
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 3);
        guiGraphics.blit(TEXTURE, getX(), getY() + 1 + yOffset, iconU, ICON_V, ICON_W, ICON_H, 256, 256);
        pose.popPose();
    }

    @Override
    protected Icon getIcon() {
        return Icon.TOOLBAR_BUTTON_BACKGROUND;
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(Component.translatable(viewCellsVisible.getAsBoolean()
                ? "gui.wcwt.view_cells.visible"
                : "gui.wcwt.view_cells.hidden"));
    }
}
