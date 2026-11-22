package com.lhy.wcwt.client.gui.panels;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.util.Icon;
import com.lhy.wcwt.WcwtMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展UI面板基类
 * 所有扩展UI面板都应该继承这个类
 */
public abstract class ExtendedUIPanel implements Renderable, GuiEventListener {
    protected static final Identifier STATES_TEXTURE =
            Identifier.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/wcwt_states.png");

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

    public abstract void init();

    protected abstract void renderBackground(GuiGraphicsExtractor guiGraphics);

    protected abstract void renderContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick);

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        renderBackground(guiGraphics);
        renderContent(guiGraphics, mouseX, mouseY, partialTick);

        if (returnButton != null) {
            returnButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (var child : children) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!visible) return false;

        if (returnButton != null && returnButton.mouseClicked(event, isDoubleClick)) {
            return true;
        }

        for (var child : children) {
            if (child.mouseClicked(event, isDoubleClick)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!visible) return false;

        double mouseX = event.x();
        double mouseY = event.y();
        if (returnButton != null && returnButton.isMouseOver(mouseX, mouseY)
                && returnButton.mouseReleased(event)) {
            return true;
        }
        for (var child : children) {
            if (child.isMouseOver(mouseX, mouseY) && child.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!visible) return false;

        double mouseX = event.x();
        double mouseY = event.y();
        for (var child : children) {
            if (child.isMouseOver(mouseX, mouseY)
                    && child.mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    protected static class ReturnButton extends Button implements ITooltip {
        public ReturnButton(int x, int y, int width, int height, Runnable onClose) {
            super(x, y, width, height, Component.translatable("gui.wcwt.return"),
                    btn -> onClose.run(), Button.DEFAULT_NARRATION);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
            if (!this.visible) return;

            float scaleX = this.width / 20.0f;
            float scaleY = this.height / 20.0f;

            var pose = guiGraphics.pose();
            pose.pushMatrix();
            pose.translate(getX(), getY());
            pose.scale(scaleX, scaleY);

            var bg = isHovered() ? Icon.TAB_BUTTON_BACKGROUND_FOCUS : Icon.TAB_BUTTON_BACKGROUND;
            Blitter.icon(bg).dest(0, 0).blit(guiGraphics);
            Blitter.icon(Icon.BACK).dest(2, 1).blit(guiGraphics);

            pose.popMatrix();
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
