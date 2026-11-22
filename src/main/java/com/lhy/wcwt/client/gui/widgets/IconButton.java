package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.util.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class IconButton extends Button implements ITooltip {

    private final Identifier texture;
    private final int normalU;
    private final int normalV;
    private final int hoverU;
    private final int hoverV;
    private final int texW;
    private final int texH;
    private List<Component> tooltipMessages;
    @Nullable
    private Supplier<Icon> overlayIcon;
    private int overlayOffsetY = 0;
    private boolean scaleTextureToButton = true;
    private boolean pressOffsetOnHover = true;
    private boolean useAE2ToolbarBg = false;

    public IconButton(int x, int y, int w, int h,
                      int normalU, int normalV,
                      int hoverU, int hoverV,
                      int texW, int texH,
                      Identifier texture,
                      Component tooltip,
                      OnPress onPress) {
        super(x, y, w, h, tooltip != null ? tooltip : Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.texture = texture;
        this.normalU = normalU;
        this.normalV = normalV;
        this.hoverU = hoverU;
        this.hoverV = hoverV;
        this.texW = texW;
        this.texH = texH;
        this.tooltipMessages = tooltip != null ? List.of(tooltip) : List.of();
    }

    public IconButton setTooltipLines(List<Component> tooltipMessages) {
        this.tooltipMessages = tooltipMessages != null ? List.copyOf(tooltipMessages) : List.of();
        return this;
    }

    public IconButton setOverlayIcon(@Nullable Supplier<Icon> overlayIcon) {
        this.overlayIcon = overlayIcon;
        return this;
    }

    public IconButton setOverlayOffsetY(int overlayOffsetY) {
        this.overlayOffsetY = overlayOffsetY;
        return this;
    }

    public IconButton useAE2ToolbarBackground() {
        this.useAE2ToolbarBg = true;
        return this;
    }

    public IconButton drawTextureUnscaledCentered() {
        this.scaleTextureToButton = false;
        return this;
    }

    public IconButton disableHoverPressOffset() {
        this.pressOffsetOnHover = false;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        int pressOffsetY = pressOffsetOnHover && isHoveredOrFocused() ? 1 : 0;

        if (useAE2ToolbarBg) {
            Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER : Icon.TOOLBAR_BUTTON_BACKGROUND;
            float scaleX = (float) width / 18f;
            float scaleY = (float) height / 20f;
            var pose = guiGraphics.pose();
            pose.pushMatrix();
            pose.translate(getX(), getY());
            pose.scale(scaleX, scaleY);
            Blitter.icon(bgIcon).dest(0, 0).blit(guiGraphics);
            pose.popMatrix();
        } else {
            int u = isHovered() ? hoverU : normalU;
            int v = isHovered() ? hoverV : normalV;
            if (!scaleTextureToButton) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY() + pressOffsetY, u, v, texW, texH, 256, 256);
            } else if (texW == width && texH == height) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), u, v, texW, texH, 256, 256);
            } else {
                float scaleX = (float) width / texW;
                float scaleY = (float) height / texH;
                var pose = guiGraphics.pose();
                pose.pushMatrix();
                pose.translate(getX(), getY());
                pose.scale(scaleX, scaleY);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, u, v, texW, texH, 256, 256);
                pose.popMatrix();
            }
        }

        if (overlayIcon != null) {
            Icon icon = overlayIcon.get();
            if (icon != null) {
                float iconScale = Math.min((float) Math.max(1, width - 2) / icon.width,
                        (float) Math.max(1, height - 2) / icon.height);
                int renderW = (int) (icon.width * iconScale);
                int renderH = (int) (icon.height * iconScale);
                int iconX = getX() + (width - renderW) / 2;
                int iconY = getY() + (height - renderH) / 2 + overlayOffsetY;
                if (iconScale == 1f) {
                    Blitter.icon(icon).dest(iconX, iconY).blit(guiGraphics);
                } else {
                    var pose = guiGraphics.pose();
                    pose.pushMatrix();
                    pose.translate(iconX, iconY);
                    pose.scale(iconScale, iconScale);
                    Blitter.icon(icon).dest(0, 0).blit(guiGraphics);
                    pose.popMatrix();
                }
            }
        }
    }

    @Override
    public List<Component> getTooltipMessage() {
        return tooltipMessages;
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
