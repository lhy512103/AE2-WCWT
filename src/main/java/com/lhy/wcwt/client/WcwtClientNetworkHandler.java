package com.lhy.wcwt.client;

import com.google.common.collect.Maps;
import com.extendedae_plus.client.screen.ProviderSelectScreen;
import com.lhy.wcwt.network.OpenEaepProviderSelectScreenPacket;
import com.lhy.wcwt.network.PatternProviderFocusPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.WcwtRestockAmountsPacket;
import com.lhy.wcwt.network.WcwtUpdateRestockPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public static void openEaepProviderSelectScreen(OpenEaepProviderSelectScreenPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Long> ids = new ArrayList<>(packet.entries().size());
        List<String> names = new ArrayList<>(packet.entries().size());
        List<Integer> emptySlots = new ArrayList<>(packet.entries().size());
        for (var entry : packet.entries()) {
            ids.add(entry.providerId());
            names.add(entry.providerName());
            emptySlots.add(entry.emptySlots());
        }
        minecraft.setScreen(new ProviderSelectScreen(minecraft.screen, ids, names, emptySlots));
    }
}
