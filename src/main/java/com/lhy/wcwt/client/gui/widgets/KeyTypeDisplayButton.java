package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

abstract class KeyTypeDisplayButton extends Button implements ITooltip {
    private static final ResourceLocation WCWT_STATES_TEXTURE =
            ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_states.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int ICON_W = 16;
    private static final int ICON_H = 16;
    private static final int BUTTON_W = 22;
    private static final int BUTTON_H = 12;
    private static final int DISABLED_U = 176;
    private static final int DISABLED_HOVER_U = 192;
    private static final int ENABLED_U = 224;
    private static final int ENABLED_HOVER_U = 208;

    private final int iconV;
    private boolean checked;

    KeyTypeDisplayButton(int iconV, OnPress onPress) {
        super(0, 0, BUTTON_W, BUTTON_H, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.iconV = iconV;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }
        int srcX = checked ? ENABLED_U : DISABLED_U;
        if (isHoveredOrFocused()) {
            srcX = checked ? ENABLED_HOVER_U : DISABLED_HOVER_U;
        }
        guiGraphics.blit(WCWT_STATES_TEXTURE, getX(), getY(), width, height,
                srcX, iconV, ICON_W, ICON_H, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    protected abstract List<Component> tooltipForState(boolean checked);

    @Override
    public List<Component> getTooltipMessage() {
        return tooltipForState(checked);
    }
}
