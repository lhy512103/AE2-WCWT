package com.lhy.wcwt.compat;

import org.jetbrains.annotations.Nullable;

/**
 * Optional compatibility with JustEnoughCharacters.
 * Falls back to plain case-insensitive substring matching when JEC is absent.
 */
public final class JecSearchCompat {
    private static @Nullable Boolean available;
    private static @Nullable java.lang.reflect.Method containsMethod;
    private static @Nullable Object containsTarget;

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
                if (invokeContains(text, query)) {
                    return true;
                }
                String lowerText = text.toLowerCase(java.util.Locale.ROOT);
                String lowerQuery = query.toLowerCase(java.util.Locale.ROOT);
                if (invokeContains(lowerText, lowerQuery)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Fall back to plain contains below.
            }
        }

        return text.toLowerCase(java.util.Locale.ROOT)
                .contains(query.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean invokeContains(String text, String query) throws ReflectiveOperationException {
        Object result = containsMethod.invoke(containsTarget, text, query);
        return result instanceof Boolean matched && matched;
    }

    private static void tryInit() {
        if (available != null) {
            return;
        }
        if (tryStaticContains("me.towdium.jecharacters.utils.Match", String.class, CharSequence.class)
                || tryStaticContains("me.towdium.jecharacters.utils.Match", CharSequence.class, CharSequence.class)
                || tryPinInInstanceContains()) {
            available = true;
            return;
        }
        containsMethod = null;
        containsTarget = null;
        available = false;
    }

    private static boolean tryStaticContains(String className, Class<?> first, Class<?> second) {
        try {
            Class<?> clazz = Class.forName(className);
            containsMethod = clazz.getMethod("contains", first, second);
            containsTarget = null;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryPinInInstanceContains() {
        try {
            Class<?> clazz = Class.forName("me.towdium.pinin.PinIn");
            containsMethod = clazz.getMethod("contains", String.class, String.class);
            containsTarget = clazz.getConstructor().newInstance();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
