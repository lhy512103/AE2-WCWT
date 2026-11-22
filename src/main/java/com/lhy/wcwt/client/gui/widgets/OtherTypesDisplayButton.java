package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 其他类型显示过滤按钮
 * 控制ME库存显示物品和流体以外的类型（如魔源、灵魂等）
 * 这是一个新功能，原版无线终端没有
 */
public class OtherTypesDisplayButton extends Button implements ITooltip {
    private static final Identifier CHECKBOX_TEXTURE =
            Identifier.fromNamespaceAndPath("ae2", "textures/guis/checkbox.png");
    private static final int SWITCH_W = 22;
    private static final int SWITCH_H = 12;

    private boolean checked = false;
    
    public OtherTypesDisplayButton(OnPress onPress) {
        super(0, 0, 18, 20, Component.empty(), onPress, Button.DEFAULT_NARRATION);
    }
    
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
    
    public boolean isChecked() {
        return checked;
    }
    
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        int srcX = isHovered() ? 22 : 0;
        int srcY = checked ? 40 : 28;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CHECKBOX_TEXTURE, getX(), getY(),
                srcX, srcY, width, height, SWITCH_W, SWITCH_H, 64, 64);
    }
    
    @Override
    public List<Component> getTooltipMessage() {
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
    
    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }
    
    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}
