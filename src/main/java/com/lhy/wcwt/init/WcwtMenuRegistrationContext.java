package com.lhy.wcwt.init;

import java.util.function.Supplier;

import net.minecraft.world.inventory.MenuType;

/** Keeps AE2's menu setup while leaving registry ownership to Forge's DeferredRegister. */
public final class WcwtMenuRegistrationContext {
    private static final ThreadLocal<Boolean> DEFERRED_REGISTRATION =
            ThreadLocal.withInitial(() -> false);

    private WcwtMenuRegistrationContext() {
    }

    public static <T extends MenuType<?>> T buildForDeferredRegister(Supplier<T> factory) {
        boolean previous = DEFERRED_REGISTRATION.get();
        DEFERRED_REGISTRATION.set(true);
        try {
            return factory.get();
        } finally {
            DEFERRED_REGISTRATION.set(previous);
        }
    }

    public static boolean isBuildingForDeferredRegister() {
        return DEFERRED_REGISTRATION.get();
    }
}
