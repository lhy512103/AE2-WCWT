package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Checks a menu opened by the selected extra-bar item before its open-screen packet is sent; a refused
 * menu is handled like {@code createMenu} returning null.
 */
@Mixin(ServerPlayer.class)
public abstract class WcwtToolkitOpenMenuMixin {
    @ModifyExpressionValue(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"))
    private AbstractContainerMenu wcwt$admitToolkitMenu(AbstractContainerMenu menu) {
        return WcwtToolkitHand.admitMenu((ServerPlayer) (Object) this, menu);
    }
}
