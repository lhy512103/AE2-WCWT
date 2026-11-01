package com.lhy.wcwt.compat.plus;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlusAutoUploadSettingsClientTest {
    @Test
    void readsThePrivateLiveSettingOnEveryEncodeRequest() {
        ProviderScreen.autoUploadUniqueMatchEnabled = true;
        assertTrue(readSetting());
        ProviderScreen.autoUploadUniqueMatchEnabled = false;
        assertFalse(readSetting());
        ProviderScreen.autoUploadUniqueMatchEnabled = true;
        assertTrue(readSetting());
    }

    @Test
    void readsTheModernPublicConfigValueOnEveryEncodeRequest() {
        ModernConfig.UNIQUE_PROVIDER_AUTO_UPLOAD_ENABLE.value = true;
        assertTrue(PlusPatternUploadApi.readModernUniqueUploadSetting(ModernConfig.class));
        ModernConfig.UNIQUE_PROVIDER_AUTO_UPLOAD_ENABLE.value = false;
        assertFalse(PlusPatternUploadApi.readModernUniqueUploadSetting(ModernConfig.class));
    }

    public static final class ModernConfig {
        public static final BooleanConfigValue UNIQUE_PROVIDER_AUTO_UPLOAD_ENABLE = new BooleanConfigValue();
    }

    public static final class BooleanConfigValue {
        boolean value;

        public boolean get() {
            return value;
        }
    }

    @Test
    void wcwtSettingCanDisableUploadWithoutResolvingTheEaepScreen() {
        AtomicInteger lookups = new AtomicInteger();
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(false, true, () -> {
            lookups.incrementAndGet();
            return Optional.of(ProviderScreen.class);
        }));
        assertEquals(0, lookups.get());
    }

    @Test
    void absentEaepKeepsWcwtUploadAndNeverLoadsAClientScreen() {
        assertTrue(PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, false, () -> {
            throw new AssertionError("Absent EAEP must not resolve its client screen");
        }));
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(false, false, Optional::empty));
    }

    @Test
    void unavailableScreenOrReflectionFailureDisablesAutomaticUpload() {
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, true, Optional::empty));
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, true, () -> {
            throw new LinkageError("Client class unavailable");
        }));
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, true,
                () -> Optional.of(MissingSettingScreen.class)));
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, true,
                () -> Optional.of(IncompatibleSettingScreen.class)));
        assertFalse(PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, true,
                () -> Optional.of(InstanceSettingScreen.class)));
    }

    private static boolean readSetting() {
        return PlusAutoUploadSettingsClient.effectiveUploadEnabled(true, true,
                () -> Optional.of(ProviderScreen.class));
    }

    private static final class ProviderScreen {
        private static boolean autoUploadUniqueMatchEnabled = true;
    }

    private static final class MissingSettingScreen {
    }

    private static final class IncompatibleSettingScreen {
        private static String autoUploadUniqueMatchEnabled = "true";
    }

    private static final class InstanceSettingScreen {
        private boolean autoUploadUniqueMatchEnabled = true;
    }
}
