package com.lhy.wcwt.client.gui.widgets;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Rects;
import appeng.client.gui.Tooltip;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;
import appeng.api.upgrades.Upgrades;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 元件工作台升级槽滚动面板。
 *
 * <p>布局沿用滚动升级槽列，背景使用 WCWT 自己的 wcwt_components.png 切片。
 * 不复用原类的原因是原类默认把第一个槽当作量子桥奇点槽，并且 tooltip 固定读取终端本体升级；
 * 元件升级槽需要完全按 {@code ICellWorkbenchItem#getUpgrades(stack)} 返回的 inventory 来决定可用槽数。
 */
public class CellScrollingUpgradesPanel implements ICompositeWidget {
    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);

    private final List<Slot> slots;
    private final Scrollbar scrollbar;
    private final Supplier<ItemStack> cellStackSupplier;

    private Point screenOrigin = Point.ZERO;
    private Point scrollbarOffset = new Point(WcwtUpgradeSlotBackground.SCROLLBAR_X, WcwtUpgradeSlotBackground.SCROLLBAR_Y);
    private int x;
    private int y;
    private int maxRows = 2;
    private boolean visible = false;

    public CellScrollingUpgradesPanel(List<Slot> slots, WidgetContainer widgets, Supplier<ItemStack> cellStackSupplier) {
        this.slots = slots;
        this.cellStackSupplier = cellStackSupplier;
        this.scrollbar = widgets.addScrollBar("cellUpgradeScrollbar", Scrollbar.SMALL);
        this.scrollbar.setCaptureMouseWheel(false);
        setScrollbarRange();
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = Math.max(1, maxRows);
        setScrollbarRange();
        updateScrollbarHeight();
        updateScrollbarPosition();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            scrollbar.setVisible(false);
            hideAllSlots();
        } else {
            setScrollbarRange();
            updateScrollbarHeight();
            updateScrollbarPosition();
        }
    }

    public void setScrollbarOffset(Point scrollbarOffset) {
        this.scrollbarOffset = scrollbarOffset;
        updateScrollbarPosition();
    }

    @Override
    public boolean isVisible() {
        return visible && getEnabledSlotCount() > 0;
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
        if (!visible || slotCount <= 0) {
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
        if (!visible) {
            hideAllSlots();
            return;
        }

        setScrollbarRange();
        updateScrollbarHeight();
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
        if (!visible) {
            return;
        }
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

    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        if (!visible || getEnabledSlotCount() <= 0) {
            return null;
        }
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        var stack = cellStackSupplier.get();
        if (!stack.isEmpty() && stack.getItem() instanceof ICellWorkbenchItem cellItem) {
            lines.addAll(Upgrades.getTooltipLinesForMachine(cellItem));
        }
        return new Tooltip(lines);
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        return visible && scrolling() && scrollbar.onMouseWheel(mousePos, delta);
    }

    private void updateScrollbarPosition() {
        scrollbar.setPosition(new Point(x + scrollbarOffset.getX(), y + scrollbarOffset.getY()));
    }

    private void updateScrollbarHeight() {
        scrollbar.setHeight(Math.max(0, getVisibleSlotCount() * WcwtUpgradeSlotBackground.SLOT_SIZE - 2));
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
        scrollbar.setVisible(visible && scrolling());
    }

    private void hideAllSlots() {
        for (Slot rawSlot : slots) {
            if (rawSlot instanceof AppEngSlot slot) {
                slot.setActive(false);
            }
            setSlotPosition(rawSlot, HIDDEN_SLOT_POS.getX(), HIDDEN_SLOT_POS.getY());
        }
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        slot.x = x;
        slot.y = y;
    }
}
