package com.lhy.wcwt.compat.emi;

import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** EMI stack conversion facade (EMI is not available on Minecraft 26.1.2). */
public final class EmiStackCompat {
    private EmiStackCompat() {
    }

    public static boolean available() {
        return false;
    }

    @Nullable
    public static GenericStack convert(@Nullable Object raw) {
        return null;
    }

    public static List<GenericStack> stacksFromIngredient(@Nullable Object ingredient) {
        return List.of();
    }
}
