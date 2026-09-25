package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Client-only mod entrypoint for GUI-layer registration. */
@Mod(value = WcwtMod.MOD_ID, dist = Dist.CLIENT)
public final class WcwtClientMod {
    public WcwtClientMod(IEventBus modEventBus) {
        modEventBus.addListener(WcwtClientGuiLayers::register);
    }
}
