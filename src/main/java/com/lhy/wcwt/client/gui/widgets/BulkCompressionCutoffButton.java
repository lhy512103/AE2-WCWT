package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.util.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class BulkCompressionCutoffButton extends Button implements ITooltip {
    public static final int WIDTH = 12;
    public static final int HEIGHT = 12;
    private static final float ICON_SCALE = WIDTH / 16.0F;

    private final Consumer<Boolean> onCycle;
    private ItemStack item = ItemStack.EMPTY;

    public BulkCompressionCutoffButton(Consumer<Boolean> onCycle) {
        super(0, 0, WIDTH, HEIGHT,
                Component.translatable("gui.tooltips.megacells.CompressionCutoff"),
                btn -> {
                },
                Button.DEFAULT_NARRATION);
        this.onCycle = onCycle;
        this.visible = false;
        this.active = false;
    }

    public void setItem(ItemStack item) {
        this.item = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
        this.visible = !this.item.isEmpty();
        this.active = this.visible;
    }

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int button = event.button();
        if (!visible || !active || (button != 0 && button != 1) || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        playDownSound(Minecraft.getInstance().getSoundManager());
        onCycle.accept(button == 1);
        return true;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        int pressOffsetY = isHoveredOrFocused() ? 1 : 0;
        var pose = guiGraphics.pose();

        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER : Icon.TOOLBAR_BUTTON_BACKGROUND;
        pose.pushMatrix();
        pose.translate(getX(), getY());
        pose.scale((float) width / 18.0F, (float) height / 20.0F);
        Blitter.icon(bgIcon).dest(0, 0).blit(guiGraphics);
        pose.popMatrix();

        pose.pushMatrix();
        pose.translate(getX(), getY() + pressOffsetY);
        pose.scale(ICON_SCALE, ICON_SCALE);
        guiGraphics.item(item, 0, 0);
        pose.popMatrix();
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (item.isEmpty()) {
            return List.of(Component.translatable("gui.tooltips.megacells.CompressionCutoff"));
        }
        return List.of(Component.translatable("gui.tooltips.megacells.CompressionCutoff"), item.getHoverName());
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
