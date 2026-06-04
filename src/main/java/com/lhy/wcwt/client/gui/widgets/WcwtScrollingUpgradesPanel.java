package com.lhy.wcwt.client.gui.widgets;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Rects;
import appeng.client.gui.Tooltip;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;
import appeng.menu.slot.AppEngSlot;
import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WcwtScrollingUpgradesPanel implements ICompositeWidget {
    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);

    private final List<Slot> slots;
    private final Supplier<IUpgradeInventory> upgrades;
    private final Scrollbar scrollbar;
    private final List<Component> tooltips;

    private Point screenOrigin = Point.ZERO;
    private Point scrollbarOffset = new Point(WcwtUpgradeSlotBackground.SCROLLBAR_X, WcwtUpgradeSlotBackground.SCROLLBAR_Y);
    private int x;
    private int y;
    private int maxRows = 2;

    public WcwtScrollingUpgradesPanel(List<Slot> slots, IUpgradeableObject upgradeableObject, WidgetContainer widgets,
            Supplier<IUpgradeInventory> upgrades) {
        this.slots = slots;
        this.upgrades = upgrades;
        this.tooltips = Upgrades.getTooltipLinesForMachine(upgradeableObject.getUpgrades().getUpgradableItem());
        this.tooltips.add(0, GuiText.CompatibleUpgrades.text());

        this.scrollbar = widgets.addScrollBar("upgradeScrollbar", Scrollbar.SMALL);
        this.scrollbar.setCaptureMouseWheel(false);
        setScrollbarRange();
    }

    public void setMaxRows(int rows) {
        maxRows = Math.max(1, rows);
        setScrollbarRange();
        scrollbar.setHeight(Math.max(0, getVisibleSlotCount() * WcwtUpgradeSlotBackground.SLOT_SIZE - 2));
        updateScrollbarPosition();
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
        return new Rect2i(x, y, slotCount > 0 ? WcwtUpgradeSlotBackground.width(scrolling()) : 0,
                WcwtUpgradeSlotBackground.height(slotCount));
    }

    @Override
    public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        screenOrigin = Point.fromTopLeft(bounds);
        updateScrollbarPosition();
    }

    @Override
    public void updateBeforeRender() {
        int currentFirstSlot = scrollbar.getCurrentScroll();
        setScrollbarRange();
        updateScrollbarPosition();

        int slotOriginY = y + WcwtUpgradeSlotBackground.SLOT_Y;
        int enabledIndex = 0;
        for (Slot rawSlot : slots) {
            if (!(rawSlot instanceof AppEngSlot slot)) {
                continue;
            }

            if (rawSlot == firstSlot() && singularitySlotHidden()) {
                slot.setSlotEnabled(false);
                slot.setActive(false);
                setSlotPosition(rawSlot, HIDDEN_SLOT_POS.getX(), HIDDEN_SLOT_POS.getY());
                continue;
            }

            boolean slotVisible = enabledIndex >= currentFirstSlot && enabledIndex < currentFirstSlot + maxRows;
            slot.setSlotEnabled(true);
            slot.setActive(slotVisible);
            if (slotVisible && slot.isActive()) {
                setSlotPosition(slot, x + WcwtUpgradeSlotBackground.SLOT_X, slotOriginY);
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
        if (getUpgradeSlotCount() == 0) {
            return null;
        }

        return new Tooltip(tooltips);
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        return scrolling() && scrollbar.onMouseWheel(mousePos, delta);
    }

    private void updateScrollbarPosition() {
        scrollbar.setPosition(new Point(x + scrollbarOffset.getX(), y + scrollbarOffset.getY()));
    }

    private boolean singularitySlotHidden() {
        var first = firstSlot();
        return first instanceof AppEngSlot slot && isDisabledSlotEmpty(slot)
                && !AE2wtlibAPI.hasQuantumBridgeCard(upgrades);
    }

    private Slot firstSlot() {
        return slots.isEmpty() ? null : slots.get(0);
    }

    private boolean isDisabledSlotEmpty(AppEngSlot slot) {
        boolean enabled = slot.isSlotEnabled();
        slot.setSlotEnabled(true);
        ItemStack stack = slot.getItem();
        slot.setSlotEnabled(enabled);
        return stack.isEmpty();
    }

    private void setScrollbarRange() {
        scrollbar.setRange(0, Math.max(0, getUpgradeSlotCount() - getVisibleSlotCount()), 1);
        scrollbar.setVisible(scrolling());
    }

    private int getUpgradeSlotCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot) {
                count++;
            }
        }
        if (singularitySlotHidden()) {
            count--;
        }
        return count;
    }

    private int getVisibleSlotCount() {
        return Math.min(maxRows, getUpgradeSlotCount());
    }

    private boolean scrolling() {
        return getUpgradeSlotCount() > maxRows;
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        slot.x = x;
        slot.y = y;
    }
}
