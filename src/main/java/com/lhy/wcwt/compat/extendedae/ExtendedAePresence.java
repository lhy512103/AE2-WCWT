package com.lhy.wcwt.compat.extendedae;

import net.neoforged.fml.ModList;

public final class ExtendedAePresence {
    public static final String MOD_ID = "extendedae";

    private ExtendedAePresence() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }
}
