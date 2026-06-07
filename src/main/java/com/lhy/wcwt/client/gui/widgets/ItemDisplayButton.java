package com.lhy.wcwt.client.gui.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 物品显示过滤按钮
 * 控制ME库存只显示物品类型
 *
 * 历史坑：原实现引用 {@code minecraft:textures/gui/checkbox.png}，该纹理在 1.21 已被
 * Mojang 移除，每帧都会抛 FileNotFoundException 中断 GuiGraphics 的渲染批次，导致
 * 周边 tooltip 出现"羽化"。改用 AE2 的 Icon.TYPE_FILTER_ITEMS / Icon.TOOLBAR_BUTTON_BACKGROUND
 * 后即可彻底消除这个异常。
 */
public class ItemDisplayButton extends KeyTypeDisplayButton {
    public ItemDisplayButton(OnPress onPress) {
        super(32, onPress);
    }
    
    @Override
    protected List<Component> tooltipForState(boolean checked) {
        if (checked) {
            return List.of(
                Component.translatable("gui.wcwt.filter.item.enabled"),
                Component.translatable("gui.wcwt.filter.item.enabled.desc")
            );
        } else {
            return List.of(
                Component.translatable("gui.wcwt.filter.item.disabled"),
                Component.translatable("gui.wcwt.filter.item.disabled.desc")
            );
        }
    }
}

