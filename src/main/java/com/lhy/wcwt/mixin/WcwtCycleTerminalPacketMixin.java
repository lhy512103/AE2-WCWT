package com.lhy.wcwt.mixin;

import appeng.menu.locator.ItemMenuHostLocator;
import com.lhy.wcwt.menu.locator.WcwtToolkitItemLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "de.mari_023.ae2wtlib.networking.CycleTerminalPacket", remap = false)
public abstract class WcwtCycleTerminalPacketMixin {
    @Inject(method = "getHeldUniversalTerminalLocator", at = @At("HEAD"), cancellable = true)
    private static void wcwt$locateHeldToolkitTerminal(Player player, CallbackInfoReturnable<ItemMenuHostLocator> cir) {
        var locator = WcwtToolkitItemLocator.forHand(player, InteractionHand.MAIN_HAND);
        if (locator != null && locator.locateItem(player).getItem() instanceof ItemWUT) {
            cir.setReturnValue(locator);
        }
    }
}
