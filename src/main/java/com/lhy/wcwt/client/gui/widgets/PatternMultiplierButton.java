package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 样板倍增/除法按钮
 * 用于调整样板编码中的数量
 */
public class PatternMultiplierButton extends Button implements ITooltip {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");
    
    public enum MultiplierType {
        SWAP("⇄", "gui.wcwt.multiplier.swap"),       // 主副切换
        TIMES_2("×2", "gui.wcwt.multiplier.times2"), // x2
        TIMES_3("×3", "gui.wcwt.multiplier.times3"), // x3
        TIMES_5("×5", "gui.wcwt.multiplier.times5"), // x5
        EQUALS_1("=1", "gui.wcwt.multiplier.equals1"), // =1
        DIVIDE_2("÷2", "gui.wcwt.multiplier.divide2"), // /2
        DIVIDE_3("÷3", "gui.wcwt.multiplier.divide3"), // /3
        DIVIDE_5("÷5", "gui.wcwt.multiplier.divide5"); // /5
        
        private final String display;
        private final String translationKey;
        
        MultiplierType(String display, String translationKey) {
            this.display = display;
            this.translationKey = translationKey;
        }
        
        public String getDisplay() {
            return display;
        }
        
        public String getTranslationKey() {
            return translationKey;
        }
    }
    
    private final MultiplierType type;
    
    public PatternMultiplierButton(MultiplierType type, OnPress onPress) {
        super(0, 0, 17, 16, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.type = type;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        boolean pressedLook = isHovered();
        
        // 选择底图纹理（17x16）
        int bgSrcX = pressedLook ? 224 : 192;
        int bgSrcY = 160;
        
        // 渲染底图（纹理区域17x16，与按钮尺寸匹配）
        guiGraphics.blit(TEXTURE, getX(), getY(), bgSrcX, bgSrcY, 17, 16, 256, 256);
        
        // 在按钮上绘制文本
        var font = net.minecraft.client.Minecraft.getInstance().font;
        var text = type.getDisplay();
        int textColor = pressedLook ? 0xA0A0A0 : 0xFFFFFF;
        
        // 居中绘制文本
        int textX = getX() + (17 - font.width(text)) / 2;
        int textY = getY() + (16 - font.lineHeight) / 2 + (pressedLook ? 1 : 0);
        guiGraphics.drawString(font, text, textX, textY, textColor);
    }
    
    @Override
    public List<Component> getTooltipMessage() {
        return List.of(Component.translatable(type.getTranslationKey()));
    }
    
    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), 17, 16);
    }
    
    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
    
    public MultiplierType getType() {
        return type;
    }
}
