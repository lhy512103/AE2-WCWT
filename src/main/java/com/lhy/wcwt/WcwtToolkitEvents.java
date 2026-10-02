package com.lhy.wcwt;

import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.helpers.WcwtToolkitSync;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

/**
 * Equipping from the extra bar is not a vanilla hotbar swap: vanilla would clear the live cell with
 * {@code copyAndClear}. Cancel {@code use()} on both sides so the client cannot predict it, and
 * swap copies on the server only.
 */
@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public final class WcwtToolkitEvents {
    private WcwtToolkitEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!WcwtToolkitHand.isOverrideActive(player)) {
            return;
        }
        InteractionResult result = equipFromSelected(player, !player.level().isClientSide());
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player player && WcwtToolkitHand.isOverrideActive(player)) {
            ItemStack toMain = event.getItemSwappedToMainHand();
            if (!toMain.isEmpty() && !ToolkitItemRules.isBaseToolkitCandidate(toMain)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * @return {@code null} when the held item is not equipment, so vanilla {@code use()} runs. Creative
     * keeps the hand item only when the armor slot was empty, like vanilla; a worn piece is always
     * returned so it is never deleted.
     */
    @Nullable
    private static InteractionResult equipFromSelected(Player player, boolean apply) {
        ItemStack hand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand.isEmpty() || !(hand.getItem() instanceof Equipable)) {
            return null;
        }
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(hand);
        ItemStack worn = player.getItemBySlot(slot);
        if (ItemStack.matches(hand, worn) || preventsArmorChange(player, worn)) {
            return InteractionResult.FAIL;
        }
        if (!apply) {
            return InteractionResult.SUCCESS;
        }
        ItemStack equipped = hand.copy();
        ItemStack displaced = worn.isEmpty() ? ItemStack.EMPTY : worn.copy();
        boolean keepHand = player.isCreative() && worn.isEmpty();
        player.setItemSlot(slot, equipped);
        if (!keepHand) {
            WcwtToolkitHotbarState.setSelectedToolkit(player, displaced);
        }
        player.awardStat(Stats.ITEM_USED.get(equipped.getItem()));
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            WcwtToolkitSync.sendNow(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean preventsArmorChange(Player player, ItemStack worn) {
        return !worn.isEmpty()
                && !player.isCreative()
                && EnchantmentHelper.hasBindingCurse(worn);
    }
}
