package com.lhy.wcwt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import com.lhy.wcwt.helpers.WcwtToolkitNetworkToolSupport;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.world.entity.player.Player;

@Mixin(NetworkToolItem.class)
public abstract class WcwtNetworkToolItemMixin {
    @Inject(method = "findNetworkToolInv", at = @At("RETURN"), cancellable = true, remap = false)
    private static void wcwt$findNetworkToolInToolkit(Player player,
            CallbackInfoReturnable<NetworkToolMenuHost> cir) {
        var originalHost = cir.getReturnValue();
        if (originalHost != null
                && originalHost.getItemStack().getItem() instanceof WirelessComprehensiveWorkTerminalItem) {
            // WCWT 是终端宿主，不是 AE2 Network Tool。只有卡槽包卡或工具包里的真实网络工具
            // 才能进入其它容器的 ToolboxMenu；否则 AE2 原逻辑会把 WCWT 自身误当成网络工具。
            cir.setReturnValue(null);
            originalHost = null;
        }

        var cardBackedHost = WcwtToolkitNetworkToolSupport.findCardBackedNetworkToolHost(player);
        if (cardBackedHost != null) {
            cir.setReturnValue(cardBackedHost);
            return;
        }
        if (originalHost != null) {
            return;
        }
        var toolkitHost = WcwtToolkitNetworkToolSupport.findToolkitNetworkToolHost(player);
        if (toolkitHost != null) {
            cir.setReturnValue(toolkitHost);
        }
    }
}
