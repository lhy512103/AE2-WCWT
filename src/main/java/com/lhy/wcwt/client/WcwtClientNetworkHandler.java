package com.lhy.wcwt.client;

import com.lhy.wcwt.network.OpenEaepProviderSelectScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public final class WcwtClientNetworkHandler {
    private WcwtClientNetworkHandler() {
    }

    public static void openEaepProviderSelectScreen(OpenEaepProviderSelectScreenPacket packet) {
        try {
            Class<?> screenClass = Class.forName("com.extendedae_plus.client.screen.ProviderSelectScreen");
            Constructor<?> constructor = screenClass.getConstructor(
                    net.minecraft.client.gui.screens.Screen.class, java.util.List.class,
                    java.util.List.class, java.util.List.class);
            var ids = new ArrayList<Long>();
            var names = new ArrayList<Component>();
            var emptySlots = new ArrayList<Integer>();
            for (var entry : packet.entries()) {
                ids.add(entry.providerId());
                names.add(Component.literal(entry.providerName()));
                emptySlots.add(entry.emptySlots());
            }
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen((net.minecraft.client.gui.screens.Screen) constructor.newInstance(
                    minecraft.screen, ids, names, emptySlots));
        } catch (Throwable ignored) {
        }
    }
}
