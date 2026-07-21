package com.lhy.wcwt.client.gui.panels;
import com.lhy.wcwt.client.gui.WcwtTextRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Curios饰品面板
 * 显示饰品槽位网格（6列）和滚动条
 */
public class CuriosPanel extends ExtendedUIPanel {
    private static final ResourceLocation PANEL_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_curios.png");
    
    // 布局常量
    public static final int SLOT_SIZE = 18;
    public static final int VISIBLE_ROWS = 9;
    public static final int DEFAULT_COLUMNS = 6;
    public static final int DEFAULT_SLOTS_ANCHOR_X = 4;
    public static final int DEFAULT_SLOTS_ANCHOR_Y = 16;
    public static final int DEFAULT_SCROLLBAR_X = 116;
    public static final int DEFAULT_SCROLLBAR_Y = 15;
    public static final int DEFAULT_SCROLLBAR_HEIGHT = 160;

    private final ExtendedPanelLayout layout = ExtendedPanelLayout.load("wcwt_curios.json");
    private ExtendedPanelLayout.Rect curiosSlot =
            new ExtendedPanelLayout.Rect(DEFAULT_SLOTS_ANCHOR_X, DEFAULT_SLOTS_ANCHOR_Y, 0, 0);
    private ExtendedPanelLayout.Rect scrollbar =
            new ExtendedPanelLayout.Rect(DEFAULT_SCROLLBAR_X, DEFAULT_SCROLLBAR_Y, 0, DEFAULT_SCROLLBAR_HEIGHT);
    private int columns = DEFAULT_COLUMNS;

    private int selectedSlot = -1; // 高级编码模式下选中的槽位
    private boolean advancedCodingMode = false; // 是否处于高级编码模式
    
    public CuriosPanel(int x, int y) {
        super(x, y, 134, 186);
    }
    
    @Override
    public void init() {
        children.clear();

        var returnButton = layout.widget("return_button", new ExtendedPanelLayout.Rect(111, -8, 20, 20));
        configureReturnButton(width - returnButton.left(), returnButton.top(), returnButton.width(), returnButton.height());
        curiosSlot = layout.slot("AE_CURIOS", curiosSlot);
        scrollbar = layout.widget("curios_scrollbar", scrollbar);
        columns = layout.slotColumns("AE_CURIOS", DEFAULT_COLUMNS);
        createReturnButton();
    }
    
    /**
     * 设置高级编码模式
     */
    public void setAdvancedCodingMode(boolean enabled) {
        this.advancedCodingMode = enabled;
    }
    
    /**
     * 设置选中的槽位
     */
    public void setSelectedSlot(int slot) {
        this.selectedSlot = slot;
    }
    
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        // 渲染面板背景纹理
        guiGraphics.blit(PANEL_TEXTURE, x, y, 0, 0, width, height, 256, 256);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染标题
        var font = Minecraft.getInstance().font;
        WcwtTextRendering.drawString(guiGraphics, font,
            Component.translatable("gui.wcwt.extended_ui.curios"),
            x + 4, y + 3, 0x404040, false);
        
        // 槽位现在由 Screen 按 AE_CURIOS 真实 Slot 渲染，这里只画面板与标题。
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // 关闭按钮等：先交给基类处理
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 处理槽位点击（高级编码模式下选择槽位）
        if (advancedCodingMode) {
            int relX = (int)mouseX - (x + getSlotAnchorX());
            int relY = (int)mouseY - (y + getSlotAnchorY());

            if (relX >= 0 && relX < getColumns() * SLOT_SIZE && relY >= 0 && relY < VISIBLE_ROWS * SLOT_SIZE) {
                int col = relX / SLOT_SIZE;
                int row = relY / SLOT_SIZE;
                int slotIndex = row * getColumns() + col;

                if (slotIndex >= 0) {
                    selectedSlot = slotIndex;
                    // TODO: 发送网络包通知服务端选择了该槽位
                    return true;
                }
            }
        }

        return false;
    }

    public int getColumns() {
        return columns;
    }

    public int getSlotAnchorX() {
        return curiosSlot.left();
    }

    public int getSlotAnchorY() {
        return curiosSlot.top();
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
