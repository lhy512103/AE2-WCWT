package com.lhy.wcwt.client.gui.panels;
import com.lhy.wcwt.compat.CosmeticArmorReworkedBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 装饰盔甲面板
 * 包含4个装饰装备槽和显示开关
 */
public class CosmeticArmorPanel extends ExtendedUIPanel {
    private static final ResourceLocation PANEL_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_cosmetic_armor.png");
    private static final ResourceLocation STATES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");
    private static final int[] COSMETIC_SLOTS = {3, 2, 1, 0};
    private static final int TOGGLE_X = 3;
    private static final int TOGGLE_Y = 15;
    private static final int SLOT_SPACING = 18;
    private static final String[] SLOT_IDS = {
            "DECORATIVE_HELMET",
            "DECORATIVE_ARMOR",
            "DECORATIVE_SHIN_GUARDS",
            "DECORATIVE_BOOTS"
    };
    private final ExtendedPanelLayout layout = ExtendedPanelLayout.load("wcwt_cosmetic_armor.json");
    private final ExtendedPanelLayout.Rect[] slots = {
            new ExtendedPanelLayout.Rect(4, 16, 0, 0),
            new ExtendedPanelLayout.Rect(22, 16, 0, 0),
            new ExtendedPanelLayout.Rect(40, 16, 0, 0),
            new ExtendedPanelLayout.Rect(58, 16, 0, 0)
    };
    
    public CosmeticArmorPanel(int x, int y) {
        super(x, y, 80, 38);
    }
    
    @Override
    public void init() {
        children.clear();

        var returnButton = layout.widget("return_button", new ExtendedPanelLayout.Rect(width - 23, -7, 20, 20));
        configureReturnButton(width - returnButton.left(), returnButton.top(), returnButton.width(), returnButton.height());
        for (int i = 0; i < slots.length; i++) {
            slots[i] = layout.slot(SLOT_IDS[i], slots[i]);
        }
        createReturnButton();
    }
    
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        // 渲染面板背景纹理
        guiGraphics.blit(PANEL_TEXTURE, x, y, 0, 0, width, height, 256, 256);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染显示开关图标
        // 暗图标位置: (192, 0) 大小5x5
        // 亮图标位置: (208, 0) 大小5x5
        for (int i = 0; i < 4; i++) {
            int iconX = x + TOGGLE_X + i * SLOT_SPACING;
            int iconY = y + TOGGLE_Y;
            int texX = isSkinArmor(i) ? 208 : 192;

            guiGraphics.blit(STATES_TEXTURE, iconX, iconY, texX, 0, 5, 5, 256, 256);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || !CosmeticArmorReworkedBridge.isLoaded()) {
            return false;
        }
        int index = getToggleIndex(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        toggleDisplay(index);
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    public void toggleDisplay(int index) {
        Player player = Minecraft.getInstance().player;
        if (player != null && index >= 0 && index < COSMETIC_SLOTS.length) {
            int slot = COSMETIC_SLOTS[index];
            boolean next = !CosmeticArmorReworkedBridge.isSkinArmor(player, slot);
            CosmeticArmorReworkedBridge.setSkinArmor(player, slot, next);
        }
    }

    public int getSlotRelativeX(int index) {
        return slots[index].left();
    }

    public int getSlotRelativeY(int index) {
        return slots[index].top();
    }

    private boolean isSkinArmor(int index) {
        Player player = Minecraft.getInstance().player;
        return player != null
                && index >= 0
                && index < COSMETIC_SLOTS.length
                && CosmeticArmorReworkedBridge.isSkinArmor(player, COSMETIC_SLOTS[index]);
    }

    private int getToggleIndex(double mouseX, double mouseY) {
        int relX = (int) Math.floor(mouseX - x);
        int relY = (int) Math.floor(mouseY - y);
        for (int i = 0; i < 4; i++) {
            int iconX = TOGGLE_X + i * SLOT_SPACING;
            if (relX >= iconX && relX < iconX + 5 && relY >= TOGGLE_Y && relY < TOGGLE_Y + 5) {
                return i;
            }
        }
        return -1;
    }
}
