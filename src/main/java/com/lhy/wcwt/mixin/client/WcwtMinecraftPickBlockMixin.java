package com.lhy.wcwt.mixin.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public abstract class WcwtMinecraftPickBlockMixin {
    private static final boolean WCWT_DEBUG_PICK_BLOCK = Boolean.getBoolean("wcwt.debug.magnet");

    @Shadow
    public LocalPlayer player;

    @Inject(method = "pickBlock", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void wcwt$pickBlockFromNetwork(CallbackInfo ci, boolean creative, BlockEntity blockEntity,
                                           HitResult.Type hitType, ItemStack picked,
                                           Inventory inventory, int slot) {
        if (player == null || creative || player.isSpectator() || picked.isEmpty() || slot != -1) {
            return;
        }

        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client sending request for {}", picked);
        }
        WcwtWirelessFeatures.pickBlock(picked.copy());
    }
}
