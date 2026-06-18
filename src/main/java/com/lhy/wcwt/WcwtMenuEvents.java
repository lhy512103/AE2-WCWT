package com.lhy.wcwt;

import appeng.menu.AEBaseMenu;
import com.lhy.wcwt.menu.locator.WcwtEmbeddedTerminalLocator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

@EventBusSubscriber(modid = WcwtMod.MOD_ID)
public final class WcwtMenuEvents {
    private WcwtMenuEvents() {
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof AEBaseMenu menu
                && menu.getLocator() instanceof WcwtEmbeddedTerminalLocator locator) {
            locator.flush(event.getEntity());
        }
    }
}
