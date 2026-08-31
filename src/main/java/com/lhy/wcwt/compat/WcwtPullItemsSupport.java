package com.lhy.wcwt.compat;

import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.client.Minecraft;

public final class WcwtPullItemsSupport {
    private WcwtPullItemsSupport() {
    }

    public static boolean shouldShowExtraButton() {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return false;
        }
        var player = Minecraft.getInstance().player;
        return player != null && player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu;
    }
}
