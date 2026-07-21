package com.lhy.wcwt.client.gui;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    public static int drawString(GuiGraphics graphics, Font font, Component text,
                                 float x, float y, int color, boolean shadow) {
        if (!isEnglish()) {
            return graphics.drawString(font, text, Math.round(x), Math.round(y), color, shadow);
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(ENGLISH_SCALE, ENGLISH_SCALE, 1.0F);
        int width = graphics.drawString(font, text, 0, 0, color, shadow);
        pose.popPose();
        return Math.round(width * ENGLISH_SCALE);
    }

    public static int drawString(GuiGraphics graphics, Font font, String text,
                                 float x, float y, int color, boolean shadow) {
        if (!isEnglish()) {
            return graphics.drawString(font, text, Math.round(x), Math.round(y), color, shadow);
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(ENGLISH_SCALE, ENGLISH_SCALE, 1.0F);
        int width = graphics.drawString(font, text, 0, 0, color, shadow);
        pose.popPose();
        return Math.round(width * ENGLISH_SCALE);
    }
}
