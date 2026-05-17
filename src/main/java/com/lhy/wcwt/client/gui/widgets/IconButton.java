package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * 通用图标按钮：
 * - 从某张精灵图的两组 UV 中读取 normal / hover 两种状态
 * - 支持把原始尺寸的纹理缩放绘制到任意宽高（例：把 24×24 的纹理画进 12×12 的按钮里）
 * - 实现 AE2 的 {@link ITooltip}，自动接入 AE2 的 tooltip 渲染管线
 *
 * 命名约定：
 *   normalU/V + texW/texH  ⇒ 默认状态在精灵图中的矩形
 *   hoverU/V  + texW/texH  ⇒ 悬停状态在精灵图中的矩形
 *   width/height           ⇒ 按钮在屏幕上的实际尺寸（可与 texW/H 不同 → 走缩放路径）
 */
public class IconButton extends Button implements ITooltip {

    private final ResourceLocation texture;
    private final int normalU;
    private final int normalV;
    private final int hoverU;
    private final int hoverV;
    private final int texW;
    private final int texH;
    private List<Component> tooltipMessages;
    /** 可选叠加图标（如 AE2 的 COG/CLEAR/COPY_MODE_ON）。supplier 形式以支持 toggle 类按钮动态切换图标。 */
    @Nullable
    private Supplier<Icon> overlayIcon;
    private int overlayOffsetY = 0;

    public IconButton(int x, int y, int w, int h,
                      int normalU, int normalV,
                      int hoverU, int hoverV,
                      int texW, int texH,
                      ResourceLocation texture,
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

    /** 设置叠加图标。null 表示无叠加；用 supplier 支持运行时切换（如 COPY_MODE_ON ↔ OFF）。 */
    public IconButton setOverlayIcon(@Nullable Supplier<Icon> overlayIcon) {
        this.overlayIcon = overlayIcon;
        return this;
    }

    public IconButton setOverlayOffsetY(int overlayOffsetY) {
        this.overlayOffsetY = overlayOffsetY;
        return this;
    }

    /**
     * 使用 AE2 Toolbar 按钮底图替代 wcwt_states.png 里的底图。
     * 设置后，renderWidget 将用 Icon.TOOLBAR_BUTTON_BACKGROUND（normal/hover）来绘制底图，
     * 忽略 normalU/V、hoverU/V、texture 字段。
     */
    private boolean useAE2ToolbarBg = false;

    public IconButton useAE2ToolbarBackground() {
        this.useAE2ToolbarBg = true;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        int pressOffsetY = isHoveredOrFocused() ? 1 : 0;

        // ── 底图 ──
        if (useAE2ToolbarBg) {
            // 使用 AE2 Toolbar 按钮底图（18×20），缩放到按钮实际尺寸
            Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER : Icon.TOOLBAR_BUTTON_BACKGROUND;
            float scaleX = (float) width / 18f;
            float scaleY = (float) height / 20f;
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(getX(), getY(), 0);
            pose.scale(scaleX, scaleY, 1.0f);
            bgIcon.getBlitter().dest(0, 0).blit(guiGraphics);
            pose.popPose();
        } else {
            int u = isHovered() ? hoverU : normalU;
            int v = isHovered() ? hoverV : normalV;
            if (texW == width && texH == height) {
                guiGraphics.blit(texture, getX(), getY(), u, v, texW, texH, 256, 256);
            } else {
                float scaleX = (float) width / texW;
                float scaleY = (float) height / texH;
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(getX(), getY(), 0);
                pose.scale(scaleX, scaleY, 1.0f);
                guiGraphics.blit(texture, 0, 0, u, v, texW, texH, 256, 256);
                pose.popPose();
            }
        }

        // ── 叠加 AE2 图标（居中，按实际图标尺寸缩放到按钮 12×12 / 16×16 内部）──
        if (overlayIcon != null) {
            Icon icon = overlayIcon.get();
            if (icon != null) {
                // 图标始终缩放到按钮内部
                float iconScale = Math.min((float) width / icon.width, (float) height / icon.height);
                int renderW = (int)(icon.width  * iconScale);
                int renderH = (int)(icon.height * iconScale);
                int iconX = getX() + (width  - renderW) / 2;
                int iconY = getY() + (height - renderH) / 2 + overlayOffsetY + pressOffsetY;
                if (iconScale == 1f) {
                    icon.getBlitter().dest(iconX, iconY).blit(guiGraphics);
                } else {
                    PoseStack pose = guiGraphics.pose();
                    pose.pushPose();
                    pose.translate(iconX, iconY, 0);
                    pose.scale(iconScale, iconScale, 1.0f);
                    icon.getBlitter().dest(0, 0).blit(guiGraphics);
                    pose.popPose();
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
