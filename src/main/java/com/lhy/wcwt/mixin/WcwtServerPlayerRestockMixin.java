package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 与 AE2WTLib 一致：丢物品（Q）后从 ME 补满当前选中槽，否则会出现 HUD 显示网络存量但丢几个就没了的割裂感。
 */
@Mixin(ServerPlayer.class)
public class WcwtServerPlayerRestockMixin {

    @Inject(method = "drop(Z)Z", at = @At("TAIL"))
    private void wcwt$restockAfterDrop(boolean dropStack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        int slot = self.getInventory().selected;
        ItemStack inHand = self.getInventory().getItem(slot);
        if (inHand.isEmpty()) {
            return;
        }
        WcwtWirelessFeatures.restock(self, inHand, inHand, s -> self.getInventory().setItem(slot, s));
    }
}
