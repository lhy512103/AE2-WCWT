package com.lhy.wcwt.client.gui.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 其他类型显示过滤按钮
 * 控制ME库存显示物品和流体以外的类型（如魔源、灵魂等）
 * 这是一个新功能，原版无线终端没有
 */
public class OtherTypesDisplayButton extends KeyTypeDisplayButton {
    public OtherTypesDisplayButton(OnPress onPress) {
        super(32, onPress);
    }
    
    @Override
    protected List<Component> tooltipForState(boolean checked) {
        if (checked) {
            return List.of(
                Component.translatable("gui.wcwt.filter.other.enabled"),
                Component.translatable("gui.wcwt.filter.other.enabled.desc")
            );
        } else {
            return List.of(
                Component.translatable("gui.wcwt.filter.other.disabled"),
                Component.translatable("gui.wcwt.filter.other.disabled.desc")
            );
        }
    }
}

