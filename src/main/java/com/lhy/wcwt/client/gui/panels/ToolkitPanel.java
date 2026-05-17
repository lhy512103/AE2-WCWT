package com.lhy.wcwt.client.gui.panels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 工具包面板
 * 工具包收纳区，布局/滑块坐标由 JSON 软编码。
 */
public class ToolkitPanel extends ExtendedUIPanel {
    private static final ResourceLocation PANEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_curios.png");
    private static final ResourceLocation STATES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");

    public static final int SLOT_SIZE = 18;
    public static final int VISIBLE_ROWS = 9;
    public static final int DEFAULT_COLUMNS = 6;
    public static final int DEFAULT_SLOTS_ANCHOR_X = 4;
    public static final int DEFAULT_SLOTS_ANCHOR_Y = 16;
    public static final int DEFAULT_SCROLLBAR_X = 118;
    public static final int DEFAULT_SCROLLBAR_Y = 15;
    public static final int DEFAULT_SCROLLBAR_HEIGHT = 160;

    private final ExtendedPanelLayout layout = ExtendedPanelLayout.load("wcwt_toolkit.json");
    private ExtendedPanelLayout.Rect toolkitSlot =
            new ExtendedPanelLayout.Rect(DEFAULT_SLOTS_ANCHOR_X, DEFAULT_SLOTS_ANCHOR_Y, 0, 0);
    private ExtendedPanelLayout.Rect scrollbar =
            new ExtendedPanelLayout.Rect(DEFAULT_SCROLLBAR_X, DEFAULT_SCROLLBAR_Y, 0, DEFAULT_SCROLLBAR_HEIGHT);
    private int columns = DEFAULT_COLUMNS;
    private int firstVisibleSlot;
    private int slotIconCount = 11;
    private int slotIconOffsetX;
    private int slotIconOffsetY;
    private int slotIconTextureU = 48;
    private int slotIconTextureV = 16;
    private int slotIconWidth = 16;
    private int slotIconHeight = 16;
    private int slotIconTextureStepX = 16;
    private int slotIconTextureStepY;

    public ToolkitPanel(int x, int y) {
        super(x, y, 134, 186);
    }

    @Override
    public void init() {
        children.clear();

        var returnButton = layout.widget("return_button", new ExtendedPanelLayout.Rect(111, -8, 20, 20));
        configureReturnButton(width - returnButton.left(), returnButton.top(), returnButton.width(), returnButton.height());
        toolkitSlot = layout.slot("WCWT_TOOLKIT", toolkitSlot);
        scrollbar = layout.widget("toolkit_scrollbar", scrollbar);
        columns = layout.slotColumns("WCWT_TOOLKIT", DEFAULT_COLUMNS);
        slotIconCount = layout.widgetInt("toolkit_slot_icons", "count", slotIconCount);
        slotIconOffsetX = layout.widgetInt("toolkit_slot_icons", "offsetX", slotIconOffsetX);
        slotIconOffsetY = layout.widgetInt("toolkit_slot_icons", "offsetY", slotIconOffsetY);
        slotIconTextureU = layout.widgetInt("toolkit_slot_icons", "textureU", slotIconTextureU);
        slotIconTextureV = layout.widgetInt("toolkit_slot_icons", "textureV", slotIconTextureV);
        slotIconWidth = layout.widgetInt("toolkit_slot_icons", "width", slotIconWidth);
        slotIconHeight = layout.widgetInt("toolkit_slot_icons", "height", slotIconHeight);
        slotIconTextureStepX = layout.widgetInt("toolkit_slot_icons", "stepX", slotIconTextureStepX);
        slotIconTextureStepY = layout.widgetInt("toolkit_slot_icons", "stepY", slotIconTextureStepY);
        createReturnButton();
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.blit(PANEL_TEXTURE, x, y, 0, 0, width, height, 256, 256);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font,
                Component.translatable("gui.wcwt.extended_ui.toolkit"),
                x + 4, y + 3, 0x404040, false);
        renderSlotIcons(guiGraphics);
    }

    private void renderSlotIcons(GuiGraphics guiGraphics) {
        int visibleSlots = columns * VISIBLE_ROWS;
        for (int slotIndex = 0; slotIndex < slotIconCount; slotIndex++) {
            if (slotIndex < firstVisibleSlot || slotIndex >= firstVisibleSlot + visibleSlots) {
                continue;
            }
            int visibleIndex = slotIndex - firstVisibleSlot;
            int iconX = x + getSlotAnchorX() + (visibleIndex % columns) * SLOT_SIZE + slotIconOffsetX;
            int iconY = y + getSlotAnchorY() + (visibleIndex / columns) * SLOT_SIZE + slotIconOffsetY;
            int iconU = slotIconTextureU + slotIndex * slotIconTextureStepX;
            int iconV = slotIconTextureV + slotIndex * slotIconTextureStepY;
            guiGraphics.blit(STATES_TEXTURE, iconX, iconY, iconU, iconV, slotIconWidth, slotIconHeight, 256, 256);
        }
    }

    public void setFirstVisibleSlot(int firstVisibleSlot) {
        this.firstVisibleSlot = Math.max(0, firstVisibleSlot);
    }

    public int getColumns() {
        return columns;
    }

    public int getSlotAnchorX() {
        return toolkitSlot.left();
    }

    public int getSlotAnchorY() {
        return toolkitSlot.top();
    }

    public int getScrollbarX() {
        return scrollbar.left();
    }

    public int getScrollbarY() {
        return scrollbar.top();
    }

    public int getScrollbarHeight() {
        return scrollbar.height();
    }
}
