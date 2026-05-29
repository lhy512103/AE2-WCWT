package com.lhy.wcwt.client.gui.panels;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import com.lhy.wcwt.WcwtMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展UI面板基类
 * 所有扩展UI面板都应该继承这个类
 */
public abstract class ExtendedUIPanel implements Renderable, GuiEventListener {
    protected static final ResourceLocation STATES_TEXTURE = 
        com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "textures/guis/wcwt_states.png");
    
    protected int x, y;
    protected int width, height;
    protected boolean visible = false;
    protected Button returnButton;
    protected List<GuiEventListener> children = new ArrayList<>();
    protected Runnable onCloseAction = () -> setVisible(false);
    protected int returnRight = 23;
    protected int returnTop = -8;
    protected int returnWidth = 20;
    protected int returnHeight = 20;
    
    public ExtendedUIPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * 初始化面板
     * 在这里创建按钮和其他组件
     */
    public abstract void init();
    
    /**
     * 渲染面板背景
     */
    protected abstract void renderBackground(GuiGraphics guiGraphics);
    
    /**
     * 渲染面板内容
     */
    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        // 渲染背景
        renderBackground(guiGraphics);
        
        // 渲染内容
        renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染返回按钮
        if (returnButton != null) {
            returnButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // 渲染子组件
        for (var child : children) {
            if (child instanceof Renderable renderable) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }

    public Rect2i getBounds() {
        return new Rect2i(x, y, width, height);
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        updateReturnButtonPosition();
    }

    public void setOnCloseAction(Runnable onCloseAction) {
        this.onCloseAction = onCloseAction;
        if (this.returnButton != null) {
            createReturnButton();
        }
    }

    public void configureReturnButton(int right, int top, int width, int height) {
        this.returnRight = right;
        this.returnTop = top;
        this.returnWidth = width;
        this.returnHeight = height;
        if (this.returnButton != null) {
            createReturnButton();
        }
    }

    protected void createReturnButton() {
        this.returnButton = new ReturnButton(
                x + width - returnRight,
                y + returnTop,
                returnWidth,
                returnHeight,
                onCloseAction);
    }

    protected void updateReturnButtonPosition() {
        if (this.returnButton instanceof ReturnButton rb) {
            rb.setPosition(x + width - returnRight, y + returnTop);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        if (returnButton != null && returnButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        for (var child : children) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // ⚠️ 关键：vanilla AbstractWidget.mouseReleased 在 button==0 时无条件返回 true（不查边界）。
        // 如果这里像 mouseClicked 那样裸跑 `for(child) child.mouseReleased(...)`，
        // 任意鼠标抬起都会被第一个子按钮"吞掉"，导致 Screen.mouseReleased 不再调用 slotClicked，
        // 玩家拿着任何物品都无法放回 / 丢弃。
        // 所以仿照 vanilla ContainerEventHandler.mouseReleased，只转发给鼠标实际命中的子组件。
        if (returnButton != null && returnButton.isMouseOver(mouseX, mouseY)
                && returnButton.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        for (var child : children) {
            if (child.isMouseOver(mouseX, mouseY) && child.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible) return false;

        // 同 mouseReleased：AbstractWidget.mouseDragged 也是无条件返回 true，必须先查边界。
        for (var child : children) {
            if (child.isMouseOver(mouseX, mouseY)
                    && child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void setFocused(boolean focused) {
        // 由子类实现
    }
    
    @Override
    public boolean isFocused() {
        return false;
    }
    
    /**
     * 返回按钮
     */
    protected static class ReturnButton extends Button implements ITooltip {
        public ReturnButton(int x, int y, int width, int height, Runnable onClose) {
            super(x, y, width, height, Component.translatable("gui.wcwt.return"),
                  btn -> onClose.run(), Button.DEFAULT_NARRATION);
        }
        
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!this.visible) return;

            float scaleX = this.width / 20.0f;
            float scaleY = this.height / 20.0f;

            var pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(getX(), getY(), 0);
            pose.scale(scaleX, scaleY, 1.0f);

            var bg = isHovered() ? Icon.TAB_BUTTON_BACKGROUND_FOCUS : Icon.TAB_BUTTON_BACKGROUND;
            bg.getBlitter().dest(0, 0).blit(guiGraphics);
            Icon.BACK.getBlitter().dest(2, 1).blit(guiGraphics);

            pose.popPose();
        }
        
        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.wcwt.return"));
        }
        
        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(getX(), getY(), this.width, this.height);
        }
        
        @Override
        public boolean isTooltipAreaVisible() {
            return this.visible;
        }

        public void setPosition(int x, int y) {
            setX(x);
            setY(y);
        }
    }
}

