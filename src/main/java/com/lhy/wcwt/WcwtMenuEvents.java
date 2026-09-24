package com.lhy.wcwt;

import com.lhy.wcwt.helpers.WcwtRemoteMenuAccess;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public final class WcwtMenuEvents {
    private WcwtMenuEvents() {
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WcwtRemoteMenuAccess.clear(player, event.getContainer());
        }
    }
}
