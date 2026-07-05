package com.lhy.wcwt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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

    @Inject(method = "isValidForSlot", at = @At("HEAD"), cancellable = true)
    private void wcwt$allowImportExportCards(Slot slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        AEBaseMenu menu = (AEBaseMenu) (Object) this;
        SlotSemantic semantic = menu.getSlotSemantic(slot);
        if (semantic == null || stack.isEmpty()) {
            return;
        }
        if (semantic != SlotSemantics.UPGRADE && !isWcwtUpgradeSemantic(semantic)) {
            return;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && "ae2importexportcard".equals(key.getNamespace())
                && (key.getPath().contains("import_card") || key.getPath().contains("export_card"))) {
            cir.setReturnValue(true);
        }
    }

    private static boolean isWcwtSpecialSemantic(SlotSemantic semantic) {
        String name = semantic.toString();
        return name.contains("wcwt_cell_upgrade")
                || name.contains("wcwt_pattern_cache")
                || name.contains("wcwt_storage_cell")
                || name.contains("wcwt_pattern_preview");
    }

    private static boolean isWcwtUpgradeSemantic(SlotSemantic semantic) {
        String name = semantic.toString();
        return name.contains("ae2wtlib")
                || name.contains("wcwt_cell_upgrade");
    }
}
