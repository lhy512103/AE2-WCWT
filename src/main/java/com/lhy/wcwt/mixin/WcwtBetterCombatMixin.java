package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps Better Combat's temporary hand writes paired with Inventory.getSelected's virtual hand.
 *
 * <p>Better Combat 2.4 writes the hand slot four times in {@code swapHandAttributes}. The redirect is
 * optional so a Better Combat build with a different shape loses this pairing instead of refusing to
 * start the game; {@code expect} still flags the mismatch in development.
 */
@Pseudo
@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
public abstract class WcwtBetterCombatMixin {
    @Redirect(
            method = "swapHandAttributes(Lnet/minecraft/world/entity/player/Player;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
            require = 0,
            expect = 4)
    private static Object wcwt$setSwappedHand(NonNullList<ItemStack> list, int index, Object stack,
                                             Player player, boolean useOffHand, Runnable action) {
        var inventory = player.getInventory();
        if (list == inventory.items && index == inventory.selected
                && WcwtToolkitHand.isOverrideActive(player)) {
            return WcwtToolkitHotbarState.swapSelectedToolkitForAttributes(player, (ItemStack) stack);
        }
        return list.set(index, (ItemStack) stack);
    }
}
