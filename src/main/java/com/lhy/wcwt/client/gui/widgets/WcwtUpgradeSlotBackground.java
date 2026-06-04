package com.lhy.wcwt.client.gui.widgets;

import com.lhy.wcwt.util.ResourceLocationCompat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class WcwtUpgradeSlotBackground {
    public static final int WIDTH = 39;
    public static final int FIXED_WIDTH = 28;
    public static final int SLOT_SIZE = 18;
    public static final int ITEM_SLOT_SIZE = 16;
    public static final int SLOT_X = 6;
    public static final int SLOT_Y = 8;
    public static final int SCROLLBAR_X = 29;
    public static final int SCROLLBAR_Y = 8;

    private static final ResourceLocation TEXTURE =
            ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_components.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int TOP_HEIGHT = 25;
    private static final int MIDDLE_HEIGHT = 18;
    private static final int BOTTOM_HEIGHT = 25;
    private static final int SINGLE_BOTTOM_MARGIN_HEIGHT = 8;
    private static final int SCROLLING_U = 0;
    private static final int SCROLLING_MIDDLE_Y = 25;
    private static final int SCROLLING_BOTTOM_Y = 43;
    private static final int SCROLLING_SINGLE_BOTTOM_MARGIN_Y = 60;
    private static final int FIXED_U = 40;
    private static final int FIXED_MIDDLE_Y = 25;
    private static final int FIXED_BOTTOM_Y = 43;
    private static final int FIXED_SINGLE_BOTTOM_MARGIN_Y = 60;

    private WcwtUpgradeSlotBackground() {
    }

    static int height(int slotCount) {
        if (slotCount <= 0) {
            return 0;
        }
        return slotCount == 1 ? SLOT_Y + ITEM_SLOT_SIZE + SINGLE_BOTTOM_MARGIN_HEIGHT : 18 * slotCount + 14;
    }

    public static int width(boolean scrolling) {
        return scrolling ? WIDTH : FIXED_WIDTH;
    }

    static void draw(GuiGraphics guiGraphics, int x, int y, int slotCount, boolean scrolling) {
        if (slotCount <= 0) {
            return;
        }

        int u = scrolling ? SCROLLING_U : FIXED_U;
        int width = width(scrolling);
        int middleY = scrolling ? SCROLLING_MIDDLE_Y : FIXED_MIDDLE_Y;
        int bottomY = scrolling ? SCROLLING_BOTTOM_Y : FIXED_BOTTOM_Y;
        int singleBottomMarginY = scrolling ? SCROLLING_SINGLE_BOTTOM_MARGIN_Y : FIXED_SINGLE_BOTTOM_MARGIN_Y;

        if (slotCount == 1) {
            blit(guiGraphics, x, y, u, 0, width, SLOT_Y + ITEM_SLOT_SIZE);
            blit(guiGraphics, x, y + SLOT_Y + ITEM_SLOT_SIZE, u, singleBottomMarginY,
                    width, SINGLE_BOTTOM_MARGIN_HEIGHT);
            return;
        }

        blit(guiGraphics, x, y, u, 0, width, TOP_HEIGHT);
        int drawY = y + TOP_HEIGHT;
        for (int i = 0; i < slotCount - 2; i++) {
            blit(guiGraphics, x, drawY, u, middleY, width, MIDDLE_HEIGHT);
            drawY += MIDDLE_HEIGHT;
        }
        blit(guiGraphics, x, drawY, u, bottomY, width, BOTTOM_HEIGHT);
    }

    private static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(TEXTURE, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
