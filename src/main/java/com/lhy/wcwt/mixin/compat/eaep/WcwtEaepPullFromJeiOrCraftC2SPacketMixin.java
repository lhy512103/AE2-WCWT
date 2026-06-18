package com.lhy.wcwt.mixin.compat.eaep;

import appeng.api.stacks.GenericStack;
import com.extendedae_plus.network.PullFromJeiOrCraftC2SPacket;
import com.lhy.wcwt.compat.eaep.WcwtEaepJeiCraftingCompat;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PullFromJeiOrCraftC2SPacket.class, remap = false)
public abstract class WcwtEaepPullFromJeiOrCraftC2SPacketMixin {
    @Shadow
    @Final
    private GenericStack stack;

    @Inject(method = "lambda$handle$1", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wcwt$handleCurioWcwt(NetworkEvent.Context context, PullFromJeiOrCraftC2SPacket packet,
                                            CallbackInfo ci) {
        GenericStack stack = ((WcwtEaepPullFromJeiOrCraftC2SPacketMixin) (Object) packet).stack;
        if (WcwtEaepJeiCraftingCompat.handlePullFromJeiOrCraft(context.getSender(), stack)) {
            ci.cancel();
        }
    }
}
