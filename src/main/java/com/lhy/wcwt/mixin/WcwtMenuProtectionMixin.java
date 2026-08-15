package com.lhy.wcwt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import net.minecraft.world.inventory.Slot;

@Mixin(AEBaseMenu.class)
public abstract class WcwtMenuProtectionMixin {
    @Inject(method = "isPlayerSideSlot", at = @At("HEAD"), cancellable = true)
    private void wcwt$protectSpecialSlots(Slot slot, CallbackInfoReturnable<Boolean> cir) {
        AEBaseMenu menu = (AEBaseMenu) (Object) this;
        SlotSemantic semantic = menu.getSlotSemantic(slot);
        if (semantic == null) {
            return;
        }
        if (semantic == SlotSemantics.BLANK_PATTERN
                || semantic == SlotSemantics.ENCODED_PATTERN
                || semantic == SlotSemantics.VIEW_CELL
                || isWcwtSpecialSemantic(semantic)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isWcwtSpecialSemantic(SlotSemantic semantic) {
        String name = semantic.toString();
        return name.contains("wcwt_cell_upgrade")
                || name.contains("wcwt_pattern_cache")
                || name.contains("wcwt_storage_cell")
                || name.contains("wcwt_pattern_preview");
    }
}
