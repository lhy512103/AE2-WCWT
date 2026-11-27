package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Marks item use from the selected extra-bar cell, so a menu it opens can be checked in {@code openMenu}. */
@Mixin(ServerPlayerGameMode.class)
public abstract class WcwtToolkitUseItemMixin {
    @WrapMethod(method = "useItem")
    private InteractionResult wcwt$markToolkitUse(ServerPlayer player, Level level, ItemStack stack,
                                                  InteractionHand hand, Operation<InteractionResult> original) {
        boolean previous = WcwtToolkitHand.beginToolkitUse(player, hand);
        try {
            return original.call(player, level, stack, hand);
        } finally {
            WcwtToolkitHand.endToolkitUse(player, previous);
        }
    }
}
