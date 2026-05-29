package com.lhy.wcwt;

import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public class WcwtWirelessFeatureEvents {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            WcwtWirelessFeatures.tickPlayerMagnet(player);
        }
    }

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
    public static void onItemPickup(EntityItemPickupEvent event) {
        var entity = event.getItem();
        var player = event.getEntity();
        if (entity.hasPickUpDelay()) {
            return;
        }
        if (WcwtWirelessFeatures.insertPickupIntoME(entity, player)) {
            event.setCanceled(true);
            event.setResult(Result.DENY);
        }
    }
}
