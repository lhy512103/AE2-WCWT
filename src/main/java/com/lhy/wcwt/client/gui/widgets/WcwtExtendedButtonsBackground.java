package com.lhy.wcwt.client.gui.widgets;

import com.lhy.wcwt.util.ResourceLocationCompat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class WcwtExtendedButtonsBackground {
    public static final Style DEFAULT_STYLE = new Style(
            34,
            69, 0, 28,
            104, 28, 21,
            69, 49, 24,
            7, 7,
            7, 7,
            7, 7);

    private static final ResourceLocation TEXTURE =
            ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_components.png");
    private static final int TEXTURE_SIZE = 256;

    private WcwtExtendedButtonsBackground() {
    }

    public static int height(int buttonCount, Style style) {
        if (buttonCount <= 0) {
            return 0;
        }
        if (buttonCount == 1) {
            return style.topHeight();
        }
        return style.topHeight() + (buttonCount - 2) * style.middleHeight() + style.bottomHeight();
    }

    public static void draw(GuiGraphics guiGraphics, int x, int y, int buttonCount, Style style) {
        if (buttonCount <= 0) {
            return;
        }

        if (buttonCount == 1) {
            blit(guiGraphics, x, y, style.topU(), style.topV(), style.width(), style.topHeight());
            return;
        }

        blit(guiGraphics, x, y, style.topU(), style.topV(), style.width(), style.topHeight());
        int drawY = y + style.topHeight();
        for (int i = 0; i < buttonCount - 2; i++) {
            blit(guiGraphics, x, drawY, style.middleU(), style.middleV(), style.width(), style.middleHeight());
            drawY += style.middleHeight();
        }
        blit(guiGraphics, x, drawY, style.bottomU(), style.bottomV(), style.width(), style.bottomHeight());
    }

    private static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(TEXTURE, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public record Style(
            int width,
            int topU,
            int topV,
            int topHeight,
            int middleU,
            int middleV,
            int middleHeight,
            int bottomU,
            int bottomV,
            int bottomHeight,
            int firstButtonX,
            int firstButtonY,
            int middleButtonX,
            int middleButtonY,
            int lastButtonX,
            int lastButtonY) {
    }
}
