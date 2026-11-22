package com.lhy.wcwt.compat.plus;

import com.lhy.wcwt.compat.reflect.WcwtReflect;

import java.util.Optional;
import java.util.function.Supplier;

/** Reads EAEP's live client setting only when the client sends an automatic encoding request. */
public final class PlusAutoUploadSettingsClient {
    private PlusAutoUploadSettingsClient() {
    }

    public static boolean effectiveUploadEnabled(boolean wcwtUploadEnabled) {
        if (!wcwtUploadEnabled) {
            return false;
        }
        try {
            return !PlusPresence.available() || PlusPatternUploadApi.effectiveUniqueUploadEnabled();
        } catch (Throwable ignored) {
            return false;
        }
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
