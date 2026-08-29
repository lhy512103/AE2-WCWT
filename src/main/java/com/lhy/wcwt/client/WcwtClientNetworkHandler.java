package com.lhy.wcwt.client;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import com.lhy.wcwt.network.OpenEaepProviderSelectScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class WcwtClientNetworkHandler {
    private static final String EAEP_PROVIDER_SELECT_SCREEN =
            "com.extendedae_plus.client.screen.ProviderSelectScreen";

    private WcwtClientNetworkHandler() {
    }

    public static void openEaepProviderSelectScreen(OpenEaepProviderSelectScreenPacket packet) {
        var ids = new ArrayList<Long>();
        var names = new ArrayList<Component>();
        var emptySlots = new ArrayList<Integer>();
        for (var entry : packet.entries()) {
            ids.add(entry.providerId());
            names.add(Component.literal(entry.providerName()));
            emptySlots.add(entry.emptySlots());
        }
        Minecraft minecraft = Minecraft.getInstance();
        WcwtReflect.construct("extendedae_plus", EAEP_PROVIDER_SELECT_SCREEN,
                        new Class<?>[]{Screen.class, List.class, List.class, List.class},
                        minecraft.screen, ids, names, emptySlots)
                .filter(Screen.class::isInstance)
                .map(Screen.class::cast)
                .ifPresent(minecraft::setScreen);
    }
}
