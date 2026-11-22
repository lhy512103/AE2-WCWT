package com.lhy.wcwt.mixin;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class WcwtPlayerMixin {
    @Inject(method = "setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void wcwt$setMainHand(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (slot != EquipmentSlot.MAINHAND) {
            return;
        }
        if ((Object) this instanceof Player player && WcwtToolkitHand.isOverrideActive(player)) {
            WcwtToolkitHotbarState.setSelectedToolkit(player, stack);
            ci.cancel();
        }
    }
}
