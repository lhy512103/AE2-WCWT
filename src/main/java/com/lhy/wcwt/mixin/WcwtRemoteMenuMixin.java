package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtRemoteMenuAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class WcwtRemoteMenuMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean wcwt$keepValidatedRemoteMenuOpen(AbstractContainerMenu menu, Player player) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        return WcwtRemoteMenuAccess.keepsMenuValid(self, menu) || menu.stillValid(player);
    }
}
