package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import appeng.util.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 使用 AE 原版左侧工具栏按钮底图，只覆盖 WCWT 自定义显示元件图标。
 */
public class ViewCellsToggleButton extends IconButton {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");

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
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }

        boolean pressed = isHovered() || isFocused();
        int yOffset = pressed ? 1 : 0;
        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
        Blitter.icon(bgIcon)
                .dest(getX() - 1, getY() + yOffset, 18, 20)
                .blit(guiGraphics);

        int iconU = viewCellsVisible.getAsBoolean() ? ICON_U_VISIBLE : ICON_U_HIDDEN;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY() + 1 + yOffset,
                iconU, ICON_V, ICON_W, ICON_H, 256, 256);
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
