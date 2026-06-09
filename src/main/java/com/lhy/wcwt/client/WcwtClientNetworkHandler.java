package com.lhy.wcwt.client;

import com.google.common.collect.Maps;
import com.lhy.wcwt.network.PatternProviderFocusPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.WcwtRestockAmountsPacket;
import com.lhy.wcwt.network.WcwtUpdateRestockPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

import java.util.HashMap;

public final class WcwtClientNetworkHandler {
    private WcwtClientNetworkHandler() {
    }

    public static void handlePatternProviderList(PatternProviderListPacket packet) {
        if (Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen) {
            screen.updatePatternProviders(packet.entries());
        }
    }

    public static void handlePatternProviderFocus(PatternProviderFocusPacket packet) {
        if (Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen) {
            screen.focusPatternProviderSlot(packet.providerId(), packet.slot());
        }
    }

    public static void handleRestockAmounts(WcwtRestockAmountsPacket packet) {
        HashMap<Item, Long> map = Maps.newHashMapWithExpectedSize(packet.items().size());
        packet.items().forEach((item, count) -> map.put(item.value(), count));
        WcwtRestockState.update(packet.enabled(), map);
    }

    public static void handleUpdateRestock(WcwtUpdateRestockPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && packet.slot() >= 0 && packet.slot() < player.getInventory().getContainerSize()) {
            player.getInventory().getItem(packet.slot()).setCount(packet.amount());
        }
    }
}
