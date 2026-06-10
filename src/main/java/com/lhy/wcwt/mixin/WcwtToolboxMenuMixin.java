package com.lhy.wcwt.mixin;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ToolboxMenu;
import com.lhy.wcwt.helpers.WcwtToolkitNetworkToolMenuHost;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToolboxMenu.class)
public abstract class WcwtToolboxMenuMixin {
    @Shadow(remap = false)
    @Final
    private AEBaseMenu menu;

    @Shadow(remap = false)
    @Final
    private NetworkToolMenuHost inv;

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lappeng/menu/AEBaseMenu;lockPlayerInventorySlot(I)V"), remap = false)
    private void wcwt$skipPlayerSlotLockForCuriosToolkit(AEBaseMenu menu, int invSlot) {
        if (inv instanceof WcwtToolkitNetworkToolMenuHost wcwtHost && wcwtHost.isCuriosBacked()) {
            return;
        }
        menu.lockPlayerInventorySlot(invSlot);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void wcwt$validateCuriosToolkitWithoutPlayerSlot(CallbackInfo ci) {
        if (!(inv instanceof WcwtToolkitNetworkToolMenuHost wcwtHost) || !wcwtHost.isCuriosBacked()) {
            return;
        }

        if (!wcwtHost.isSourceStillPresent()) {
            menu.setValidMenu(false);
        }
        ci.cancel();
    }
}
