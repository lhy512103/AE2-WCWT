package com.lhy.wcwt.compat;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class CrystalScienceCompat {
    private static final String MOD_ID = "ae2cs";
    private static volatile boolean initialized;
    private static volatile Method encodeMethod;

    private CrystalScienceCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack encodeResonating(ItemStack sourcePattern) {
        if (!available() || sourcePattern == null || sourcePattern.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!initialized) {
            initialized = true;
            try {
                Class<?> cls = Class.forName("io.github.lounode.ae2cs.common.me.crafting.ResonatingPatternDetails");
                encodeMethod = cls.getMethod("encode", ItemStack.class);
            } catch (Throwable ignored) {
                encodeMethod = null;
            }
        }
        if (encodeMethod == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object encoded = encodeMethod.invoke(null, sourcePattern);
            return encoded instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }
}
