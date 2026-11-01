package com.lhy.wcwt.compat.plus;

import com.lhy.wcwt.compat.reflect.WcwtReflect;

import java.util.Optional;
import java.util.function.Supplier;

/** Reads EAEP's live client setting only when the client sends an automatic encoding request. */
public final class PlusAutoUploadSettingsClient {
    private static final String PROVIDER_SCREEN = "com.extendedae_plus.client.screen.ProviderSelectScreen";

    private PlusAutoUploadSettingsClient() {
    }

    public static boolean effectiveUploadEnabled(boolean wcwtUploadEnabled) {
        return effectiveUploadEnabled(wcwtUploadEnabled, PlusPresence.available(),
                () -> WcwtReflect.findClass(PlusPresence.MOD_ID, PROVIDER_SCREEN));
    }

    static boolean effectiveUploadEnabled(boolean wcwtUploadEnabled, boolean plusAvailable,
                                          Supplier<Optional<Class<?>>> screenClass) {
        if (!wcwtUploadEnabled) {
            return false;
        }
        if (!plusAvailable) {
            return true;
        }
        try {
            // Cache member lookup in WcwtReflect, never the value: EAEP can toggle it while WCWT stays open.
            return screenClass.get()
                    .flatMap(owner -> WcwtReflect.findDeclaredField(owner, "autoUploadUniqueMatchEnabled"))
                    .flatMap(field -> WcwtReflect.readField(null, field))
                    .filter(Boolean.class::isInstance)
                    .map(Boolean.class::cast)
                    .orElse(false);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
