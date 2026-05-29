package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
public class ItemDisplayButton extends Button implements ITooltip {
    private static final ResourceLocation CHECKBOX_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/checkbox.png");
    private static final int SWITCH_W = 22;
    private static final int SWITCH_H = 12;

    private boolean checked = false;
    
    public ItemDisplayButton(OnPress onPress) {
        super(0, 0, 18, 20, Component.empty(), onPress, Button.DEFAULT_NARRATION);
    }
    
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
    
    public boolean isChecked() {
        return checked;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        int srcX = isHovered() ? 22 : 0;
        int srcY = checked ? 40 : 28;
        guiGraphics.blit(CHECKBOX_TEXTURE, getX(), getY(), width, height,
                srcX, srcY, SWITCH_W, SWITCH_H, 64, 64);
    }
    
    @Override
    public List<Component> getTooltipMessage() {
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
    
    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }
    
    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}

