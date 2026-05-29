package com.lhy.wcwt.compat;

import org.jetbrains.annotations.Nullable;

/**
 * Optional compatibility with JustEnoughCharacters.
 * Falls back to plain case-insensitive substring matching when JEC is absent.
 */
public final class JecSearchCompat {
    private static @Nullable Boolean available;
    private static @Nullable java.lang.reflect.Method containsMethod;

    private JecSearchCompat() {
    }

    public static boolean contains(@Nullable String text, @Nullable String query) {
        if (text == null) {
            return false;
        }
        if (query == null || query.isEmpty()) {
            return true;
        }

        tryInit();
        if (Boolean.TRUE.equals(available) && containsMethod != null) {
            try {
                Object result = containsMethod.invoke(null, text, query);
                if (result instanceof Boolean matched && matched) {
                    return true;
                }
                String lowerText = text.toLowerCase(java.util.Locale.ROOT);
                String lowerQuery = query.toLowerCase(java.util.Locale.ROOT);
                Object lowerResult = containsMethod.invoke(null, lowerText, lowerQuery);
                if (lowerResult instanceof Boolean matched && matched) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Fall back to plain contains below.
            }
        }

        return text.toLowerCase(java.util.Locale.ROOT)
                .contains(query.toLowerCase(java.util.Locale.ROOT));
    }

    private static void tryInit() {
        if (available != null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName("me.towdium.jecharacters.utils.Match");
            containsMethod = clazz.getMethod("contains", CharSequence.class, CharSequence.class);
            available = true;
        } catch (Throwable ignored) {
            containsMethod = null;
            available = false;
        }
    }
}
