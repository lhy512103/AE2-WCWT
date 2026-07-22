package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtRemoteMenuAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class WcwtRemoteMenuMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"),
            require = 0)
    private boolean wcwt$keepValidatedRemoteMenuOpen(boolean original) {
        if (original) {
            return true;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        return WcwtRemoteMenuAccess.keepsMenuValid(self, self.containerMenu);
    }
}
