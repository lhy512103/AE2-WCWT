package com.lhy.wcwt;

import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ArrowNockEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public class WcwtWirelessFeatureEvents {
    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtWirelessFeatures.restock(player, event.getItem(), event.getResultStack(), event::setResultStack);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            ItemStack item = event.getItemStack();
            WcwtWirelessFeatures.restock(player, item, item, stack -> player.setItemInHand(event.getHand(), stack));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            ItemStack item = event.getItemStack();
            WcwtWirelessFeatures.restock(player, item, item, stack -> player.setItemInHand(event.getHand(), stack));
        }
    }

    @SubscribeEvent
    public static void onArrowNock(ArrowNockEvent event) {
        if (!event.hasAmmo()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack projectile = player.getProjectile(event.getBow());
            WcwtWirelessFeatures.restock(player, projectile, projectile, stack -> {
            });
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (!event.hasAmmo()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack projectile = player.getProjectile(event.getBow());
            WcwtWirelessFeatures.restock(player, projectile, projectile, stack -> {
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (event.canPickup().isFalse()) {
            return;
        }
        var entity = event.getItemEntity();
        var player = event.getPlayer();
        if (event.canPickup().isDefault()) {
            if (entity.hasPickUpDelay()) {
                return;
            }
        }
        if (WcwtWirelessFeatures.insertPickupIntoME(entity, player)) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
