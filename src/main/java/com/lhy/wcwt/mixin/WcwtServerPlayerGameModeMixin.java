package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 与 AE2WTLib 一致：对方块使用物品（如放置方块消耗物品）后尝试补货。
 */
@Mixin(ServerPlayerGameMode.class)
public class WcwtServerPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void wcwt$restockAfterUseItemOn(ServerPlayer player, Level level, ItemStack original, InteractionHand hand,
            BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        WcwtWirelessFeatures.restock(player, original.copy(), player.getItemInHand(hand),
                stack -> player.setItemInHand(hand, stack));
    }
}
