package com.lhy.wcwt.compat;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class CrystalScienceCompat {
    private static final String MOD_ID = "ae2cs";

    private CrystalScienceCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack encodeResonating(ItemStack sourcePattern) {
        if (!available() || sourcePattern == null || sourcePattern.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return Impl.encode(sourcePattern);
    }

    private static final class Impl {
        private Impl() {
        }

        static ItemStack encode(ItemStack sourcePattern) {
            try {
                ItemStack encoded = io.github.lounode.ae2cs.common.me.crafting.ResonatingPatternDetails
                        .encode(sourcePattern);
                return encoded == null ? ItemStack.EMPTY : encoded;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
    }
}
