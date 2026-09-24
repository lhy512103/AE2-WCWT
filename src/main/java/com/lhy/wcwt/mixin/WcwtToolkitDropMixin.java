package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Vanilla {@code ServerPlayer#drop(boolean)} writes {@code getSelected()} into the remote copy of the
 * vanilla hotbar slot, which would desync that slot with the toolkit cell. Extra-bar drops skip it.
 */
@Mixin(ServerPlayer.class)
public abstract class WcwtToolkitDropMixin {
    @WrapMethod(method = "drop(Z)Z")
    private boolean wcwt$dropToolkit(boolean dropStack, Operation<Boolean> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!WcwtToolkitHand.isOverrideActive(player)) {
            return original.call(dropStack);
        }
        WcwtToolkitHotbarState.dropSelected(player, dropStack);
        return true;
    }
}
