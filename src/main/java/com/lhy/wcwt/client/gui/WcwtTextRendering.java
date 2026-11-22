package com.lhy.wcwt.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class WcwtTextRendering {
    public static final float ENGLISH_SCALE = 7.0F / 9.0F;

    private static final Map<ScreenStyle, Map<Text, Float>> ORIGINAL_STYLE_SCALES = new WeakHashMap<>();

    private WcwtTextRendering() {
    }

    public static boolean isEnglish() {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        return language != null && language.regionMatches(true, 0, "en_", 0, 3);
    }

    /**
     * 面板标题等文字的默认颜色。走 ScreenStyle 调色板（wcwt_palette.json），
     * 这样资源包（如内置暗色材质）可以覆盖文字颜色。
     */
    public static int defaultTextColor() {
        if (Minecraft.getInstance().screen instanceof AEBaseScreen<?> aeScreen) {
            var color = aeScreen.getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR);
            if (color != null) {
                return color.toARGB();
            }
        }
        return 0xFF404040;
    }

    public static float scale(float baseScale) {
        return isEnglish() ? baseScale * ENGLISH_SCALE : baseScale;
    }

    public static ScreenStyle applyToStyle(ScreenStyle style) {
        synchronized (ORIGINAL_STYLE_SCALES) {
            Map<Text, Float> originalScales = ORIGINAL_STYLE_SCALES.computeIfAbsent(style, current -> {
                Map<Text, Float> scales = new IdentityHashMap<>();
                current.getText().values().forEach(text -> scales.put(text, text.getScale()));
                return scales;
            });
            float languageScale = isEnglish() ? ENGLISH_SCALE : 1.0F;
            originalScales.forEach((text, originalScale) -> text.setScale(originalScale * languageScale));
        }
        return style;
    }

    private static int ensureOpaqueArgb(int color) {
        return (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color;
    }

    public static int drawString(GuiGraphicsExtractor graphics, Font font, Component text,
                                 float x, float y, int color, boolean shadow) {
        int argb = ensureOpaqueArgb(color);
        int width = font.width(text);
        if (!isEnglish()) {
            graphics.text(font, text, Math.round(x), Math.round(y), argb, shadow);
            return width;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(ENGLISH_SCALE, ENGLISH_SCALE);
        graphics.text(font, text, 0, 0, argb, shadow);
        pose.popMatrix();
        return Math.round(width * ENGLISH_SCALE);
    }

    public static int drawString(GuiGraphicsExtractor graphics, Font font, String text,
                                 float x, float y, int color, boolean shadow) {
        int argb = ensureOpaqueArgb(color);
        int width = font.width(text);
        if (!isEnglish()) {
            graphics.text(font, text, Math.round(x), Math.round(y), argb, shadow);
            return width;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(ENGLISH_SCALE, ENGLISH_SCALE);
        graphics.text(font, text, 0, 0, argb, shadow);
        pose.popMatrix();
        return Math.round(width * ENGLISH_SCALE);
    }
}
