package com.lhy.wcwt.mixin.compat.eaep;

import com.lhy.wcwt.compat.jei.WcwtEaepBookmarkOrder;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.extendedae_plus.client.InputEvents", remap = false)
public abstract class WcwtEaepInputEventsMixin {
    @Inject(method = "onMouseButtonPre", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wcwt$handleBookmarkOrderBeforeEaep(ScreenEvent.MouseButtonPressed.Pre event, CallbackInfo ci) {
        // WCWTが注文操作として受理したクリックだけEAEP側の重複処理を止める。
        if (WcwtEaepBookmarkOrder.handleMouseButtonPre(event)) {
            ci.cancel();
        }
    }
}
