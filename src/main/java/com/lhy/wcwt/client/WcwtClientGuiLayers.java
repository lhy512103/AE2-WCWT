package com.lhy.wcwt.client;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/** Registers WCWT HUD content at a stable vanilla GUI-overlay position. */
public final class WcwtClientGuiLayers {
    private WcwtClientGuiLayers() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "toolkit_bar",
                WcwtToolkitHud::renderOverlay);
    }
}
