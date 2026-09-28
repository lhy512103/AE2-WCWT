package com.lhy.wcwt.mixin;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.lhy.wcwt.helpers.WcwtToolkitAccess;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item menus opened from a toolkit cell keep the same stack instance in that cell, not in a player
 * inventory slot. Identity-match the extra-bar cells so AE2 does not close the menu or overwrite
 * the vanilla hotbar.
 */
@Mixin(ItemMenuHost.class)
public abstract class WcwtItemMenuHostMixin {
    @Shadow(remap = false)
    public abstract Player getPlayer();

    @Shadow(remap = false)
    public abstract ItemStack getItemStack();

    @Inject(method = "ensureItemStillInSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void wcwt$ensureToolkitItem(CallbackInfoReturnable<Boolean> cir) {
        ItemStack expected = getItemStack();
        if (expected.isEmpty()) {
            return;
        }
        Player player = getPlayer();
        for (int i = 0; i < WcwtToolkitAccess.HOTBAR_SLOTS; i++) {
            if (WcwtToolkitHotbarState.stackAt(player, i) == expected) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
