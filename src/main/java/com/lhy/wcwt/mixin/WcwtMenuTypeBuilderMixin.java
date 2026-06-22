package com.lhy.wcwt.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MenuTypeBuilder.class, remap = false)
public abstract class WcwtMenuTypeBuilderMixin {
    @Unique
    private MenuLocator wcwt$clientLocator;

    @Inject(method = "fromNetwork", at = @At("HEAD"))
    private void wcwt$clearClientLocator(int containerId, Inventory inv, FriendlyByteBuf packetBuf,
            CallbackInfoReturnable<AEBaseMenu> cir) {
        wcwt$clientLocator = null;
    }

    @Redirect(method = "fromNetwork", at = @At(value = "INVOKE",
            target = "Lappeng/menu/locator/MenuLocators;readFromPacket(Lnet/minecraft/network/FriendlyByteBuf;)Lappeng/menu/locator/MenuLocator;"))
    private MenuLocator wcwt$captureClientLocator(FriendlyByteBuf packetBuf) {
        MenuLocator locator = MenuLocators.readFromPacket(packetBuf);
        wcwt$clientLocator = locator;
        return locator;
    }

    @Inject(method = "fromNetwork", at = @At("RETURN"))
    private void wcwt$setClientLocator(int containerId, Inventory inv, FriendlyByteBuf packetBuf,
            CallbackInfoReturnable<AEBaseMenu> cir) {
        MenuLocator locator = wcwt$clientLocator;
        wcwt$clientLocator = null;
        AEBaseMenu menu = cir.getReturnValue();
        if (locator != null && menu != null && menu.getLocator() == null) {
            menu.setLocator(locator);
        }
    }
}
