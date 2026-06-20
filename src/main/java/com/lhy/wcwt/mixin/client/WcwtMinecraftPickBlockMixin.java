package com.lhy.wcwt.mixin.client;

import appeng.api.stacks.AEItemKey;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
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

    @Shadow
    public HitResult hitResult;

    @Shadow
    public Options options;

    private boolean wcwt$sentPickBlockThisCall;
    private boolean wcwt$pickKeyWasDown;
    private int wcwt$lastSentPickBlockTick = -1;

    @Inject(method = "handleKeybinds", at = @At("TAIL"))
    private void wcwt$pickBlockKeyFallback(CallbackInfo ci) {
        if (player == null || options == null || options.keyPickItem == null) {
            wcwt$pickKeyWasDown = false;
            return;
        }

        boolean pickKeyDown = options.keyPickItem.isDown();
        if (pickKeyDown && !wcwt$pickKeyWasDown) {
            if (wcwt$lastSentPickBlockTick == player.tickCount) {
                wcwt$pickKeyWasDown = true;
                return;
            }
            if (WCWT_DEBUG_PICK_BLOCK) {
                WcwtMod.LOGGER.info(
                        "WCWT pick-block debug: client pick key pressed fallback player={}, hitResult={}, terminals={}",
                        player.getScoreboardName(),
                        hitResult == null ? "<null>" : hitResult.getType(),
                        WcwtWirelessFeatures.describePickBlockTerminals(player));
            }
            wcwt$tryPickBlockBeforeOtherOverrides();
        }
        wcwt$pickKeyWasDown = pickKeyDown;
    }

    @Inject(method = "pickBlock", at = @At("HEAD"))
    private void wcwt$logPickBlockEntry(CallbackInfo ci) {
        wcwt$sentPickBlockThisCall = player != null && wcwt$lastSentPickBlockTick == player.tickCount;
        if (!WCWT_DEBUG_PICK_BLOCK) {
            return;
        }
        WcwtMod.LOGGER.info("WCWT pick-block debug: client pickBlock entered player={}, instabuild={}, spectator={}, hitResult={}",
                player == null ? "<null>" : player.getScoreboardName(),
                player != null && player.getAbilities().instabuild,
                player != null && player.isSpectator(),
                hitResult == null ? "<null>" : hitResult.getType());
        if (wcwt$sentPickBlockThisCall) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client pickBlock early path skipped because fallback already sent this tick");
        }
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
        if (wcwt$sentPickBlockThisCall) {
            return;
        }
        boolean topUp = wcwt$shouldTopUpHeldStack(picked);
        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info(
                    "WCWT pick-block debug: client after vanilla lookup picked={}, slot={}, topUp={}, willSend={}",
                    wcwt$describeStack(picked), slot, topUp,
                    player != null && !player.getAbilities().instabuild && !player.isSpectator()
                            && !picked.isEmpty() && (slot == -1 || topUp));
        }
        if (player == null || player.getAbilities().instabuild || player.isSpectator()
                || picked.isEmpty() || (slot != -1 && !topUp)) {
            return;
        }

        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client sending request for {}", picked);
        }
        wcwt$sentPickBlockThisCall = true;
        wcwt$lastSentPickBlockTick = player.tickCount;
        WcwtWirelessFeatures.pickBlock(picked.copy());
    }

    private void wcwt$tryPickBlockBeforeOtherOverrides() {
        if (player == null) {
            wcwt$debugEarlySkip("player missing");
            return;
        }
        if (player.getAbilities().instabuild) {
            wcwt$debugEarlySkip("creative instabuild");
            return;
        }
        if (player.isSpectator()) {
            wcwt$debugEarlySkip("spectator");
            return;
        }
        if (hitResult == null) {
            wcwt$debugEarlySkip("hitResult missing");
            return;
        }
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            wcwt$debugEarlySkip("hitResult type " + hitResult.getType());
            return;
        }
        if (!WcwtWirelessFeatures.hasPickBlockTerminal(player)) {
            wcwt$debugEarlySkip("no WCWT with pick_block enabled; " + WcwtWirelessFeatures.describePickBlockTerminals(player));
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) {
            wcwt$debugEarlySkip("target block is air at " + pos);
            return;
        }

        ItemStack picked = state.getBlock().getCloneItemStack(state, blockHit, player.level(), pos, player);
        int slot = picked.isEmpty() ? -1 : player.getInventory().findSlotMatchingItem(picked);
        boolean topUp = wcwt$shouldTopUpHeldStack(picked);
        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info(
                    "WCWT pick-block debug: client early lookup picked={}, slot={}, topUp={}, willSend={}",
                    wcwt$describeStack(picked), slot, topUp, !picked.isEmpty() && (slot == -1 || topUp));
        }
        if (picked.isEmpty() || (slot != -1 && !topUp)) {
            return;
        }

        wcwt$sentPickBlockThisCall = true;
        wcwt$lastSentPickBlockTick = player.tickCount;
        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client early sending request picked={}, pos={}, block={}",
                    wcwt$describeStack(picked), pos, BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
        WcwtWirelessFeatures.pickBlock(picked.copy());
    }

    /**
     * Matches EAEP's pick-from-wireless trigger: when the main hand already holds the same
     * item and the stack is not full, the pick should still be sent so the server tops it up
     * from the ME network instead of doing nothing because the item is "already in inventory".
     */
    private boolean wcwt$shouldTopUpHeldStack(ItemStack picked) {
        if (player == null || picked == null || picked.isEmpty()) {
            return false;
        }
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty() || hand.getCount() >= hand.getMaxStackSize()) {
            return false;
        }
        AEItemKey handKey = AEItemKey.of(hand);
        return handKey != null && handKey.equals(AEItemKey.of(picked));
    }

    private static void wcwt$debugEarlySkip(String reason) {
        if (WCWT_DEBUG_PICK_BLOCK) {
            WcwtMod.LOGGER.info("WCWT pick-block debug: client early skipped: {}", reason);
        }
    }

    private static String wcwt$describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
