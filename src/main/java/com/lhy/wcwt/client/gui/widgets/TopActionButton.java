package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class TopActionButton extends Button implements ITooltip {
    private static final ResourceLocation AE2_CHECKBOX =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/checkbox.png");
    private static final int RADIO_UNCHECKED_U = 28;
    private static final int RADIO_UNCHECKED_V = 0;
    private static final int RADIO_FOCUS_U = 42;
    private static final int RADIO_FOCUS_V = 0;
    private static final int RADIO_SIZE = 14;

    private final ResourceLocation iconTexture;
    private final int iconU;
    private final int iconV;
    private final int iconW;
    private final int iconH;
    private final int textureW;
    private final int textureH;

    public TopActionButton(ResourceLocation iconTexture, int iconU, int iconV, int iconW, int iconH,
                           int textureW, int textureH, Component tooltip, OnPress onPress) {
        super(0, 0, 13, 12, tooltip, onPress, Button.DEFAULT_NARRATION);
        this.iconTexture = iconTexture;
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconW = iconW;
        this.iconH = iconH;
        this.textureW = textureW;
        this.textureH = textureH;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        boolean focus = !active || isHoveredOrFocused();
        int bgU = focus ? RADIO_FOCUS_U : RADIO_UNCHECKED_U;
        int bgV = focus ? RADIO_FOCUS_V : RADIO_UNCHECKED_V;
        guiGraphics.blit(AE2_CHECKBOX, getX(), getY(), width, height, bgU, bgV,
                RADIO_SIZE, RADIO_SIZE, 64, 64);

        int iconSize = Math.min(width, height) - 2;
        float scale = Math.min((float) iconSize / iconW, (float) iconSize / iconH);
        int drawW = Math.round(iconW * scale);
        int drawH = Math.round(iconH * scale);
        int pressOffsetY = isHoveredOrFocused() ? 1 : 0;
        int iconX = getX() + (width - drawW) / 2;
        int iconY = getY() + (height - drawH) / 2 + pressOffsetY;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(iconX, iconY, 0);
        pose.scale(scale, scale, 1.0f);
        guiGraphics.blit(iconTexture, 0, 0, iconU, iconV, iconW, iconH, textureW, textureH);
        pose.popPose();
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(getMessage());
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}
