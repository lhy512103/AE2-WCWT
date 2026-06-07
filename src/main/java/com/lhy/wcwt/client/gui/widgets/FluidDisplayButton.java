package com.lhy.wcwt.client.gui.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 流体显示过滤按钮
 * 控制ME库存只显示流体类型
 */
public class FluidDisplayButton extends KeyTypeDisplayButton {
    public FluidDisplayButton(OnPress onPress) {
        super(32, onPress);
    }
    
    @Override
    protected List<Component> tooltipForState(boolean checked) {
        if (checked) {
            return List.of(
                Component.translatable("gui.wcwt.filter.fluid.enabled"),
                Component.translatable("gui.wcwt.filter.fluid.enabled.desc")
            );
        } else {
            return List.of(
                Component.translatable("gui.wcwt.filter.fluid.disabled"),
                Component.translatable("gui.wcwt.filter.fluid.disabled.desc")
            );
        }
    }
}

