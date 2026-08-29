package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Optional compatibility with JustEnoughCharacters.
 * Falls back to plain case-insensitive substring matching when JEC is absent.
 */
public final class JecSearchCompat {
    private static final String MATCH_CLASS = "me.towdium.jecharacters.utils.Match";
    private static @Nullable Boolean available;
    private static @Nullable Method containsMethod;

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
            if (invokeContains(text, query) || invokeContains(
                    text.toLowerCase(java.util.Locale.ROOT),
                    query.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }

        return text.toLowerCase(java.util.Locale.ROOT)
                .contains(query.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean invokeContains(String text, String query) {
        return WcwtReflect.invoke(null, containsMethod, text, query)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    private static void tryInit() {
        if (available != null) {
            return;
        }
        containsMethod = WcwtReflect.findMethod("jecharacters", MATCH_CLASS, "contains",
                CharSequence.class, CharSequence.class).orElse(null);
        available = containsMethod != null;
    }
}
