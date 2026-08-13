package com.lhy.wcwt.mixin.client;

import java.util.List;

import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpgradesPanel.class, remap = false)
public abstract class WcwtUpgradesPanelLayoutMixin {
    private static final int SLOTS_PER_COLUMN = 8;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_OFFSET = 8;

    @Shadow
    @Final
    private List<Slot> slots;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void wcwt$alignOverflowSlotsWithBackground(CallbackInfo ci) {
        int enabledSlotCount = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot && appEngSlot.isSlotEnabled()) {
                enabledSlotCount++;
            }
        }
        if (enabledSlotCount <= SLOTS_PER_COLUMN) {
            return;
        }

        int index = 0;
        for (Slot slot : slots) {
            if (!(slot instanceof AppEngSlot appEngSlot) || !appEngSlot.isSlotEnabled() || !slot.isActive()) {
                continue;
            }

            slot.x = x + SLOT_OFFSET + index / SLOTS_PER_COLUMN * SLOT_SIZE;
            slot.y = y + SLOT_OFFSET + index % SLOTS_PER_COLUMN * SLOT_SIZE;
            index++;
        }
    }
}