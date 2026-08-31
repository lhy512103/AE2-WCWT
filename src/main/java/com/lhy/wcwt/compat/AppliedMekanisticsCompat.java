package com.lhy.wcwt.compat;

import appeng.api.stacks.GenericStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class AppliedMekanisticsCompat {
    private static final String MEKANISM = "mekanism";
    private static final String APPMEK = "appmek";

    private AppliedMekanisticsCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MEKANISM) && ModList.get().isLoaded(APPMEK);
    }

    @Nullable
    public static GenericStack fromChemicalStack(@Nullable Object raw) {
        if (!available() || raw == null) {
            return null;
        }
        return Impl.fromStack(raw);
    }

    @Nullable
    public static GenericStack fromChemicalKey(@Nullable Object rawKey, long amount) {
        if (!available() || rawKey == null) {
            return null;
        }
        return Impl.fromKey(rawKey, amount);
    }

    private static final class Impl {
        private Impl() {
        }

        @Nullable
        static GenericStack fromStack(Object raw) {
            try {
                if (!(raw instanceof mekanism.api.chemical.ChemicalStack stack) || stack.isEmpty()) {
                    return null;
                }
                var key = me.ramidzkh.mekae2.ae2.MekanismKey.of(stack);
                return key == null ? null : new GenericStack(key, Math.max(1L, stack.getAmount()));
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Nullable
        static GenericStack fromKey(Object rawKey, long amount) {
            try {
                if (!(rawKey instanceof mekanism.api.chemical.Chemical chemical)) {
                    return null;
                }
                var holder = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical);
                var stack = new mekanism.api.chemical.ChemicalStack(holder, Math.max(1L, amount));
                if (stack.isEmpty()) {
                    return null;
                }
                var key = me.ramidzkh.mekae2.ae2.MekanismKey.of(stack);
                return key == null ? null : new GenericStack(key, Math.max(1L, amount));
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
