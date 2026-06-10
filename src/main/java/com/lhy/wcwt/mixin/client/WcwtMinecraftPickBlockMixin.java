package com.lhy.wcwt.mixin.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private static final boolean WCWT_DEBUG_PICK_BLOCK =
            Boolean.getBoolean("wcwt.debug.pickBlock")
                    || Boolean.getBoolean("wcwt.debug.toolkit")
                    || Boolean.getBoolean("wcwt.debug.magnet");

    @Shadow
    public LocalPlayer player;

    @Inject(method = "pickBlock", at = @At("HEAD"))
    private void wcwt$logPickBlockEntry(CallbackInfo ci) {
        if (!WCWT_DEBUG_PICK_BLOCK) {
            return;
        }
        WcwtMod.LOGGER.info("WCWT pick-block debug: client pickBlock entered player={}, instabuild={}, spectator={}",
                player == null ? "<null>" : player.getScoreboardName(),
                player != null && player.getAbilities().instabuild,
                player != null && player.isSpectator());
    }

    @Inject(method = "pickBlock", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void wcwt$pickBlockFromNetwork(CallbackInfo ci,
            boolean creative,
            BlockEntity blockEntity,
            HitResult.Type hitResultType,
            ItemStack picked,
            Inventory inventory,
            int slot) {
        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info(
                    "WCWT pick-block debug: client after vanilla lookup picked={}, slot={}, willSend={}",
                    wcwt$describeStack(picked), slot,
                    player != null && !player.getAbilities().instabuild && !player.isSpectator()
                            && !picked.isEmpty() && slot == -1);
        }
        if (player == null || player.getAbilities().instabuild || player.isSpectator()
                || picked.isEmpty() || slot != -1) {
            return;
        }

        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client sending request for {}", picked);
        }
        WcwtWirelessFeatures.pickBlock(picked.copy());
    }

    private static String wcwt$describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
