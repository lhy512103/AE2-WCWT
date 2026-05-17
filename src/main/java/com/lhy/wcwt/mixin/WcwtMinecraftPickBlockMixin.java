package com.lhy.wcwt.mixin;

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
    @Shadow
    public LocalPlayer player;

    @Inject(method = "pickBlock", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"))
    private void wcwt$pickBlock(CallbackInfo ci, @Local ItemStack itemstack, @Local int i) {
        if (player == null || player.getAbilities().instabuild || player.isSpectator() || i != -1) {
            return;
        }
        WcwtWirelessFeatures.pickBlock(itemstack);
    }
}
