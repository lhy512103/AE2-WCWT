package com.lhy.wcwt.mixin.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class WcwtMinecraftPickBlockMixin {
    private static final boolean WCWT_DEBUG_PICK_BLOCK = Boolean.getBoolean("wcwt.debug.magnet");

    @Shadow
    public HitResult hitResult;

    @Shadow
    public LocalPlayer player;

    @Shadow
    public ClientLevel level;

    @Inject(method = "pickBlock", at = @At("HEAD"))
    private void wcwt$pickBlockFromNetwork(CallbackInfo ci) {
        if (player == null || level == null || player.getAbilities().instabuild
                || !(hitResult instanceof BlockHitResult hit)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        ItemStack picked = state.getCloneItemStack(hitResult, level, pos, player);
        if (picked.isEmpty() || player.getInventory().findSlotMatchingItem(picked) != -1) {
            return;
        }

        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client sending request for {}", picked);
        }
        WcwtWirelessFeatures.pickBlock(picked.copy());
    }
}
