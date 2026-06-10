package com.lhy.wcwt.mixin.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class WcwtMinecraftPickBlockMixin {
    private static final boolean WCWT_DEBUG_PICK_BLOCK = Boolean.getBoolean("wcwt.debug.magnet");

    @Shadow
    public LocalPlayer player;

    @Inject(method = "pickBlock", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"))
    private void wcwt$pickBlockFromNetwork(CallbackInfo ci, @Local ItemStack picked, @Local int slot) {
        if (player == null || player.getAbilities().instabuild || player.isSpectator()
                || picked.isEmpty() || slot != -1) {
            return;
        }

        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client sending request for {}", picked);
        }
        WcwtWirelessFeatures.pickBlock(picked.copy());
    }
}
