package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class WcwtMinecraftPickBlockMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public ClientLevel level;

    @Inject(method = "pickBlock", at = @At("HEAD"))
    private void wcwt$pickBlock(CallbackInfo ci) {
        if (player == null || player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }

        ItemStack requestedStack = wcwt$getRequestedStack();
        if (requestedStack.isEmpty()) {
            return;
        }

        int slot = player.getInventory().findSlotMatchingItem(requestedStack);
        if (slot != -1) {
            return;
        }

        WcwtWirelessFeatures.pickBlock(requestedStack);
    }

    private ItemStack wcwt$getRequestedStack() {
        if (hitResult == null) {
            return ItemStack.EMPTY;
        }

        return switch (hitResult.getType()) {
            case BLOCK -> wcwt$getBlockPickStack((BlockHitResult) hitResult);
            case ENTITY -> wcwt$getEntityPickStack((EntityHitResult) hitResult);
            case MISS -> ItemStack.EMPTY;
        };
    }

    private ItemStack wcwt$getBlockPickStack(BlockHitResult hitResult) {
        if (level == null) {
            return ItemStack.EMPTY;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        return state.getBlock().getCloneItemStack(level, pos, state);
    }

    private static ItemStack wcwt$getEntityPickStack(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        ItemStack stack = entity.getPickResult();
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
