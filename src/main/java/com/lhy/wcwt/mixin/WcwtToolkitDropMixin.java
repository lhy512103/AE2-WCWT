package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla {@code ServerPlayer#drop(boolean)} writes {@code getSelected()} into the remote copy of the
 * vanilla hotbar slot, which would desync that slot with the toolkit cell. Extra-bar drops skip it.
 */
@Mixin(ServerPlayer.class)
public abstract class WcwtToolkitDropMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void wcwt$dropToolkit(boolean dropStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!WcwtToolkitHand.isOverrideActive(player)) {
            return;
        }
        WcwtToolkitHotbarState.dropSelected(player, dropStack);
        cir.setReturnValue(true);
    }
}
