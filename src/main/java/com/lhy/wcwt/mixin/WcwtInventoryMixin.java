package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class WcwtInventoryMixin {
    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void wcwt$getSelected(CallbackInfoReturnable<ItemStack> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (WcwtToolkitHotbarState.isToolkitSelected(inventory.player)) {
            cir.setReturnValue(WcwtToolkitHotbarState.getSelectedToolkit(inventory.player));
        }
    }

    @Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
    private void wcwt$removeFromSelected(boolean all, CallbackInfoReturnable<ItemStack> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (!WcwtToolkitHotbarState.isToolkitSelected(inventory.player)) {
            return;
        }
        ItemStack selected = WcwtToolkitHotbarState.getSelectedToolkit(inventory.player);
        if (selected.isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack removed = selected.copyWithCount(all ? selected.getCount() : 1);
        selected.shrink(removed.getCount());
        WcwtToolkitHotbarState.setSelectedToolkit(inventory.player, selected);
        cir.setReturnValue(removed);
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void wcwt$getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (WcwtToolkitHotbarState.isToolkitSelected(inventory.player)) {
            cir.setReturnValue(WcwtToolkitHotbarState.getSelectedToolkit(inventory.player).getDestroySpeed(state));
        }
    }
}
