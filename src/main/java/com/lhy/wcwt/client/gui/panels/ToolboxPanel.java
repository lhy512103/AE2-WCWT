package com.lhy.wcwt.client.gui.panels;

import com.lhy.wcwt.client.gui.WcwtAe2Textures;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 卡槽箱面板
 * 显示 AE 网络工具提供的升级卡 3x3 卡槽
 */
public class ToolboxPanel extends ExtendedUIPanel {
    private final ExtendedPanelLayout layout = ExtendedPanelLayout.load("wcwt_toolbox.json");
    private static final int DEFAULT_SLOT_SPACING_X = 17;
    private static final int DEFAULT_SLOT_SPACING_Y = 18;
    private ExtendedPanelLayout.Rect toolboxSlot =
            new ExtendedPanelLayout.Rect(8, 20, 0, 0);
    private int slotSpacingX = DEFAULT_SLOT_SPACING_X;
    private int slotSpacingY = DEFAULT_SLOT_SPACING_Y;
    
    public ToolboxPanel(int x, int y) {
        super(x, y, 59, 66);
    }
    
    @Override
    public void init() {
        children.clear();

        var returnButton = layout.widget("return_button", new ExtendedPanelLayout.Rect(13, -5, 12, 12));
        configureReturnButton(width - returnButton.left(), returnButton.top(), returnButton.width(), returnButton.height());
        toolboxSlot = layout.slot("TOOLBOX", toolboxSlot);
        slotSpacingX = layout.slotInt("TOOLBOX", "slotSpacingX", DEFAULT_SLOT_SPACING_X);
        slotSpacingY = layout.slotInt("TOOLBOX", "slotSpacingY", DEFAULT_SLOT_SPACING_Y);
        createReturnButton();
    }
    
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        // 渲染卡槽箱背景
        // 纹理位置: (69, 62, 59, 66) in extra_panels.png
        guiGraphics.blit(WcwtAe2Textures.extraPanels(), x, y, 69, 62, width, height,
                WcwtAe2Textures.EXTRA_PANELS_WIDTH, WcwtAe2Textures.EXTRA_PANELS_HEIGHT);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 卡槽箱的内容由 AE2 的 ToolboxMenu 管理
        // 槽位会自动渲染
    }

    public int getSlotRelativeX() {
        return toolboxSlot.left();
    }

    public int getSlotRelativeY() {
        return toolboxSlot.top();
    }

    public int getSlotSpacingX() {
        return slotSpacingX;
    }

    public int getSlotSpacingY() {
        return slotSpacingY;
    }
    
}

