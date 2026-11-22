package com.lhy.wcwt.compat;

import com.lhy.wcwt.WcwtMod;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class JecSearchCompat {
    private static final String MOD_ID = "jecharacters";
    private static volatile boolean initialized;
    private static volatile boolean broken;
    private static volatile Method matchContains;

    private JecSearchCompat() {
    }

    public static boolean contains(@Nullable String text, @Nullable String query) {
        if (text == null) {
            return false;
        }
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (!broken && ModList.get().isLoaded(MOD_ID) && invokeContains(text, query)) {
            return true;
        }
        return text.toLowerCase(java.util.Locale.ROOT)
                .contains(query.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean invokeContains(String text, String query) {
        if (!initialized) {
            initialized = true;
            try {
                Class<?> matchClass = Class.forName("me.towdium.jecharacters.utils.Match");
                matchContains = matchClass.getMethod("contains", String.class, CharSequence.class);
            } catch (Throwable e) {
                broken = true;
                WcwtMod.LOGGER.warn("WCWT: Just Enough Characters search integration disabled, its API changed", e);
                return false;
            }
        }
        if (matchContains == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(matchContains.invoke(null, text, query))
                    || Boolean.TRUE.equals(matchContains.invoke(null,
                            text.toLowerCase(java.util.Locale.ROOT),
                            query.toLowerCase(java.util.Locale.ROOT)));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
