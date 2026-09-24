package com.lhy.wcwt;

import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.helpers.WcwtToolkitStore;
import com.lhy.wcwt.helpers.WcwtToolkitSync;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public final class WcwtToolkitEvents {
    private WcwtToolkitEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtToolkitSync.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtToolkitSync.sendFull(player);
        }
    }

    /** Respawn and dimension travel give the client a fresh player with an empty toolkit copy. */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtToolkitSync.sendFull(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtToolkitSync.sendFull(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtToolkitStore.persist(player);
        }
    }

    /**
     * Equipping from the extra bar is not a vanilla hotbar swap: vanilla would clear the live cell with
     * {@code copyAndClear}. Cancel {@code use()} on both sides so the client cannot predict it, and
     * swap copies on the server only.
     */
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
        EquipmentSlot slot = player.getEquipmentSlotForItem(hand);
        if (!player.canUseSlot(slot)) {
            return InteractionResult.PASS;
        }
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
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            WcwtToolkitSync.sendNow(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean preventsArmorChange(Player player, ItemStack worn) {
        return !worn.isEmpty()
                && !player.isCreative()
                && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }
}
