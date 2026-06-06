package com.lhy.wcwt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import com.lhy.wcwt.helpers.WcwtToolkitNetworkToolSupport;
import net.minecraft.world.entity.player.Player;

@Mixin(NetworkToolItem.class)
public abstract class WcwtNetworkToolItemMixin {
    @Inject(method = "findNetworkToolInv", at = @At("RETURN"), cancellable = true, remap = false)
    private static void wcwt$findNetworkToolInToolkit(Player player,
            CallbackInfoReturnable<NetworkToolMenuHost> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        var host = WcwtToolkitNetworkToolSupport.findToolkitNetworkToolHost(player);
        if (host != null) {
            cir.setReturnValue(host);
        }
    }
}
