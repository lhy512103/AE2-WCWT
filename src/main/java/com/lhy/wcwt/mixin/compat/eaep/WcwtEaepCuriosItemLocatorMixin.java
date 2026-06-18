package com.lhy.wcwt.mixin.compat.eaep;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.extendedae_plus.menu.locator.CuriosItemLocator;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtCurioLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CuriosItemLocator.class, remap = false)
public abstract class WcwtEaepCuriosItemLocatorMixin {
    @Shadow
    @Final
    private String slotId;

    @Shadow
    @Final
    private int index;

    @Inject(method = "locate", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void wcwt$locateWcwtHost(Player player, Class<T> hostInterface, CallbackInfoReturnable<T> cir) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            if (!slotId.equals(curio.identifier()) || index != curio.slotIndex()) {
                continue;
            }

            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (!(stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal)) {
                return;
            }

            var locator = new WcwtCurioLocator(slotId, index);
            ItemMenuHost menuHost = terminal.getMenuHost(player, locator, stack);
            if (hostInterface.isInstance(menuHost)) {
                cir.setReturnValue(hostInterface.cast(menuHost));
            }
            return;
        }
    }
}
