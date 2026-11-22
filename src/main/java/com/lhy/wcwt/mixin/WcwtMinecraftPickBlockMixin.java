package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class WcwtMinecraftPickBlockMixin {
    @Shadow
    public LocalPlayer player;
    @Shadow
    public ClientLevel level;
    @Shadow
    public HitResult hitResult;

    @Inject(method = "pickBlockOrEntity", at = @At("TAIL"))
    private void wcwt$pickBlockOrEntity(CallbackInfo ci) {
        if (player == null || level == null || hitResult == null
                || player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        ItemStack picked = ItemStack.EMPTY;
        if (hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            picked = level.getBlockState(blockHit.getBlockPos())
                    .getCloneItemStack(level, blockHit.getBlockPos(), true);
        } else if (hitResult instanceof EntityHitResult entityHit) {
            ItemStack stack = entityHit.getEntity().getPickResult();
            if (stack != null) {
                picked = stack;
            }
        }
        if (picked.isEmpty()) {
            return;
        }
        if (player.getInventory().findSlotMatchingItem(picked) == -1) {
            WcwtWirelessFeatures.pickBlock(picked);
        }
    }
}
