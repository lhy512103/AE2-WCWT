package com.lhy.wcwt.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WcwtClientConfigTest {
    @Test
    void earlyClientTicksDoNotReadOrCacheUnloadedToolkitConfig() {
        WcwtClientConfig.SPEC.acceptConfig(null);
        try {
            // NeoForge deliberately rejects raw reads before loading the config.
            assertThrows(IllegalStateException.class, WcwtClientConfig.SHOW_TOOLKIT_HOTBARS::get);
            for (int tick = 0; tick < 20; tick++) {
                assertFalse(WcwtClientConfig.showToolkitHotbars());
            }
            // The HUD guard must not populate NeoForge's config cache with a fallback value.
            assertThrows(IllegalStateException.class, WcwtClientConfig.SHOW_TOOLKIT_HOTBARS::get);
        } finally {
            WcwtClientConfig.SPEC.acceptConfig(null);
        }
    }
}
