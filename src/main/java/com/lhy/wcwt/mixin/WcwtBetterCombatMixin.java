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

/** Keeps Better Combat's temporary hand writes paired with Inventory.getSelected's virtual hand. */
@Pseudo
@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
public abstract class WcwtBetterCombatMixin {
    @Redirect(
            method = "swapHandAttributes(Lnet/minecraft/world/entity/player/Player;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
            require = 4,
            allow = 4)
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
