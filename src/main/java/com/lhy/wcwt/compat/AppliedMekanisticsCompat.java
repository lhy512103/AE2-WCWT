package com.lhy.wcwt.compat;

import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

/** Compatibility facade for Applied Mekanistics (not yet available on Minecraft 26.1.2). */
public final class AppliedMekanisticsCompat {
    private AppliedMekanisticsCompat() {
    }

    public static boolean available() {
        return false;
    }

    @Nullable
    public static GenericStack fromChemicalStack(@Nullable Object raw) {
        return null;
    }

    @Nullable
    public static GenericStack fromChemicalKey(@Nullable Object rawKey, long amount) {
        return null;
    }
}
