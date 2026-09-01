package com.lhy.wcwt.compat.plus;

import net.neoforged.fml.ModList;

public final class PlusPresence {
    public static final String MOD_ID = "extendedae_plus";

    private PlusPresence() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }
}
