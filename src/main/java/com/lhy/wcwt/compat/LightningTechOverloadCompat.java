package com.lhy.wcwt.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Compatibility facade for AE2 Lightning Tech overload patterns
 * (AE2LT 2.x overload API is not yet present in 26.1.2 alpha).
 */
public final class LightningTechOverloadCompat {
    private LightningTechOverloadCompat() {
    }

    public static boolean available() {
        return false;
    }

    public static boolean isOverloadPattern(ItemStack stack) {
        return false;
    }

    public static ItemStack convertToOverload(ItemStack sourcePattern, Level level,
                                              HolderLookup.Provider registries,
                                              int[] inputIdOnlySlots, int[] outputIdOnlySlots) {
        return ItemStack.EMPTY;
    }

    public static Set<Integer> inputIdOnlySlots(ItemStack stack) {
        return Set.of();
    }

    public static Set<Integer> outputIdOnlySlots(ItemStack stack) {
        return Set.of();
    }
}
