package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Registers WCWT HUD content at a stable vanilla GUI-layer position. */
public final class WcwtClientGuiLayers {
    private WcwtClientGuiLayers() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_bar"),
                WcwtToolkitHud::renderGuiLayer);
    }
}
