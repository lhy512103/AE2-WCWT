package com.lhy.wcwt.compat;

import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.ManualWorkspaceModePacket;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class WcwtManualWorkspaceRecipeSwitch {
    private WcwtManualWorkspaceRecipeSwitch() {
    }

    public static void switchForTransfer(WirelessComprehensiveWorkTerminalMenu menu, EncodingMode encodingMode) {
        if (!WcwtClientConfig.autoSwitchManualWorkspaceOnRecipeTransfer()) {
            return;
        }
        var target = targetMode(encodingMode);
        if (target == null || menu.getManualWorkspaceMode() == target) {
            return;
        }
        menu.setManualWorkspaceMode(target);
        PacketDistributor.sendToServer(new ManualWorkspaceModePacket(target.ordinal()));
    }

    @Nullable
    private static WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode targetMode(EncodingMode encodingMode) {
        return switch (encodingMode) {
            case SMITHING_TABLE -> WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.SMITHING;
            case CRAFTING, PROCESSING, STONECUTTING -> WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING;
        };
    }
}
