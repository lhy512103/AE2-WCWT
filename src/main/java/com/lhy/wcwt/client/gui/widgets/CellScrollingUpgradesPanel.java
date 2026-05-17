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
import de.mari_023.ae2wtlib.api.gui.UpgradeBackground;
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
 * <p>布局/背景沿用 WTLib 的 ScrollingUpgradesPanel：一个 18px 槽列 + 可选小滑块。
 * 不复用原类的原因是原类默认把第一个槽当作量子桥奇点槽，并且 tooltip 固定读取终端本体升级；
 * 元件升级槽需要完全按 {@code ICellWorkbenchItem#getUpgrades(stack)} 返回的 inventory 来决定可用槽数。
 */
public class CellScrollingUpgradesPanel implements ICompositeWidget {
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 5;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);

    private final List<Slot> slots;
    private final Scrollbar scrollbar;
    private final Supplier<ItemStack> cellStackSupplier;

    private Point screenOrigin = Point.ZERO;
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
        scrollbar.setHeight(Math.max(0, getVisibleSlotCount() * SLOT_SIZE - 2));
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        scrollbar.setVisible(visible && scrolling());
        if (!visible) {
            hideAllSlots();
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
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
        int height = 2 * PADDING + slotCount * SLOT_SIZE;
        int width = 2 * PADDING + SLOT_SIZE + (scrolling() ? SCROLLBAR_WIDTH : 0);
        return new Rect2i(x, y, width, height);
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
        updateScrollbarPosition();

        int slotOriginX = x + 1;
        int slotOriginY = y + PADDING + 1;
        int currentFirstSlot = scrollbar.getCurrentScroll();
        int enabledIndex = 0;

        for (Slot rawSlot : slots) {
            if (!(rawSlot instanceof AppEngSlot slot)) {
                continue;
            }

            if (!slot.isSlotEnabled()) {
                slot.setActive(false);
                rawSlot.x = HIDDEN_SLOT_POS.getX();
                rawSlot.y = HIDDEN_SLOT_POS.getY();
                continue;
            }

            boolean slotVisible = enabledIndex >= currentFirstSlot && enabledIndex < currentFirstSlot + maxRows;
            slot.setActive(slotVisible);
            if (slotVisible) {
                rawSlot.x = slotOriginX;
                rawSlot.y = slotOriginY;
                slotOriginY += SLOT_SIZE;
            } else {
                rawSlot.x = HIDDEN_SLOT_POS.getX();
                rawSlot.y = HIDDEN_SLOT_POS.getY();
            }
            enabledIndex++;
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        int slotCount = getVisibleSlotCount();
        if (!visible || slotCount <= 0) {
            return;
        }

        int slotOriginX = screenOrigin.getX() + x;
        int slotOriginY = screenOrigin.getY() + y + PADDING;
        UpgradeBackground bg = UpgradeBackground.get(scrolling());

        bg.top().getBlitter().dest(slotOriginX, slotOriginY - PADDING).blit(guiGraphics);
        for (int i = 1; i < slotCount - 1; i++) {
            bg.middle().getBlitter().dest(slotOriginX, slotOriginY + i * SLOT_SIZE).blit(guiGraphics);
        }
        bg.bottom().getBlitter().dest(slotOriginX, slotOriginY + (slotCount - 1) * SLOT_SIZE).blit(guiGraphics);
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
        // WTLib 的升级槽背景图中，滑块轨道位于面板左边缘 +19px。
        // 之前用了 +24px，导致手柄整体偏到轨道右侧。
        scrollbar.setPosition(new Point(x + SLOT_SIZE + 1, y + PADDING + 1));
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
            rawSlot.x = HIDDEN_SLOT_POS.getX();
            rawSlot.y = HIDDEN_SLOT_POS.getY();
        }
    }
}
