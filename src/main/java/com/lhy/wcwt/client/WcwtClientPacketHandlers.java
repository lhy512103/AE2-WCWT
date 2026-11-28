package com.lhy.wcwt.client;

import com.lhy.wcwt.network.PatternProviderFocusPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only handlers for clientbound packets. Kept out of the packet classes so that
 * common code never links against client classes on a dedicated server.
 */
public final class WcwtClientPacketHandlers {
    private WcwtClientPacketHandlers() {
    }

    public static void handlePatternProviderFocus(PatternProviderFocusPacket packet) {
        if (Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen) {
            screen.focusPatternProviderSlot(packet.providerId(), packet.slot());
        }
    }

    public static void handlePatternProviderList(PatternProviderListPacket packet) {
        if (Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen) {
            screen.updatePatternProviders(packet.entries(), packet.resolvedSearchText());
        }
    }
}
