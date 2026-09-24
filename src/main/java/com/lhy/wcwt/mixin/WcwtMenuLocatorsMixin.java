package com.lhy.wcwt.mixin;

import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.menu.locator.WcwtToolkitItemLocator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 finds the hand item in the player inventory by identity and throws "Could not find item held
 * in hand" when it is a toolkit cell, which kicked the player. Item menus opened from the extra bar
 * are located on the toolkit instead.
 */
@Mixin(MenuLocators.class)
public abstract class WcwtMenuLocatorsMixin {
    @Inject(method = "forHand", at = @At("HEAD"), cancellable = true)
    private static void wcwt$forHand(Player player, InteractionHand hand,
                                     CallbackInfoReturnable<ItemMenuHostLocator> cir) {
        ItemMenuHostLocator locator = WcwtToolkitItemLocator.forHand(player, hand);
        if (locator != null) {
            cir.setReturnValue(locator);
        }
    }

    @Inject(method = "forItemUseContext", at = @At("HEAD"), cancellable = true)
    private static void wcwt$forItemUseContext(UseOnContext context,
                                               CallbackInfoReturnable<ItemMenuHostLocator> cir) {
        ItemMenuHostLocator locator = WcwtToolkitItemLocator.forUse(context);
        if (locator != null) {
            cir.setReturnValue(locator);
        }
    }
}
