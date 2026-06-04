package com.lhy.wcwt.client.gui.widgets;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Rects;
import appeng.client.gui.Tooltip;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * AE2 原版 VIEW_CELL 面板的 WCWT 皮肤版。
 *
 * <p>显示元件槽位按终端可见行数滚动，背景与 WTLib 升级面板一样，
 * 根据是否需要滑块自动切换两套 wcwt_components 样式。
 */
public class WcwtViewCellsPanel implements ICompositeWidget {
    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);

    private final List<Slot> slots;
    private final Supplier<List<Component>> tooltipSupplier;
    private final Scrollbar scrollbar;

    private Point screenOrigin = Point.ZERO;
    private Point scrollbarOffset = new Point(WcwtUpgradeSlotBackground.SCROLLBAR_X, WcwtUpgradeSlotBackground.SCROLLBAR_Y);
    private int x;
    private int y;
    private int maxRows = 2;

    public WcwtViewCellsPanel(List<Slot> slots, WidgetContainer widgets, Supplier<List<Component>> tooltipSupplier) {
        this.slots = slots;
        this.tooltipSupplier = tooltipSupplier;
        this.scrollbar = widgets.addScrollBar("viewCellsScrollbar", Scrollbar.SMALL);
        this.scrollbar.setCaptureMouseWheel(false);
        setScrollbarRange();
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = Math.max(1, maxRows);
        setScrollbarRange();
        scrollbar.setHeight(Math.max(0, getVisibleSlotCount() * WcwtUpgradeSlotBackground.SLOT_SIZE - 2));
    }

    public void setScrollbarOffset(Point scrollbarOffset) {
        this.scrollbarOffset = scrollbarOffset;
        updateScrollbarPosition();
    }

    @Override
    public void setPosition(Point position) {
        x = position.getX();
        y = position.getY();
        updateScrollbarPosition();
    }

    @Override
    public void setSize(int width, int height) {
    }

    @Override
    public Rect2i getBounds() {
        int slotCount = getVisibleSlotCount();
        if (slotCount <= 0) {
            return new Rect2i(x, y, 0, 0);
        }
        return new Rect2i(x, y, WcwtUpgradeSlotBackground.width(scrolling()),
                WcwtUpgradeSlotBackground.height(slotCount));
    }

    @Override
    public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        screenOrigin = Point.fromTopLeft(bounds);
        updateScrollbarPosition();
    }

    @Override
    public void updateBeforeRender() {
        setScrollbarRange();
        updateScrollbarPosition();

        int slotOriginX = x + WcwtUpgradeSlotBackground.SLOT_X;
        int slotOriginY = y + WcwtUpgradeSlotBackground.SLOT_Y;
        int currentFirstSlot = scrollbar.getCurrentScroll();
        int enabledIndex = 0;

        for (Slot rawSlot : slots) {
            if (!(rawSlot instanceof AppEngSlot slot)) {
                continue;
            }

            if (!slot.isSlotEnabled()) {
                slot.setActive(false);
                setSlotPosition(rawSlot, HIDDEN_SLOT_POS.getX(), HIDDEN_SLOT_POS.getY());
                continue;
            }

            boolean slotVisible = enabledIndex >= currentFirstSlot && enabledIndex < currentFirstSlot + maxRows;
            slot.setActive(slotVisible);
            if (slotVisible) {
                setSlotPosition(rawSlot, slotOriginX, slotOriginY);
                slotOriginY += WcwtUpgradeSlotBackground.SLOT_SIZE;
            } else {
                setSlotPosition(rawSlot, HIDDEN_SLOT_POS.getX(), HIDDEN_SLOT_POS.getY());
            }
            enabledIndex++;
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        int slotCount = getVisibleSlotCount();
        if (slotCount <= 0) {
            return;
        }

        WcwtUpgradeSlotBackground.draw(guiGraphics, screenOrigin.getX() + x, screenOrigin.getY() + y, slotCount,
                scrolling());
    }

    @Override
    public void addExclusionZones(List<Rect2i> exclusionZones, Rect2i screenBounds) {
        var bounds = getBounds();
        if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            exclusionZones.add(Rects.expand(Rects.move(bounds, screenBounds.getX(), screenBounds.getY()), 2));
        }
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        if (getEnabledSlotCount() <= 0) {
            return null;
        }

        var tooltip = tooltipSupplier.get();
        return tooltip.isEmpty() ? null : new Tooltip(tooltip);
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        return scrolling() && scrollbar.onMouseWheel(mousePos, delta);
    }

    @Override
    public boolean isVisible() {
        return getEnabledSlotCount() > 0;
    }

    private void updateScrollbarPosition() {
        scrollbar.setPosition(new Point(x + scrollbarOffset.getX(), y + scrollbarOffset.getY()));
    }

    private int getEnabledSlotCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot && appEngSlot.isSlotEnabled()) {
                count++;
            }
        }
        return count;
    }

    private int getVisibleSlotCount() {
        return Math.min(maxRows, getEnabledSlotCount());
    }

    private boolean scrolling() {
        return getEnabledSlotCount() > maxRows;
    }

    private void setScrollbarRange() {
        scrollbar.setRange(0, Math.max(0, getEnabledSlotCount() - getVisibleSlotCount()), 1);
        scrollbar.setVisible(scrolling());
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        slot.x = x;
        slot.y = y;
    }
}
