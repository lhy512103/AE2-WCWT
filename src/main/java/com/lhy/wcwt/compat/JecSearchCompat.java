package com.lhy.wcwt.compat;

import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class JecSearchCompat {
    private static final String MOD_ID = "jecharacters";

    private JecSearchCompat() {
    }

    public static boolean contains(@Nullable String text, @Nullable String query) {
        if (text == null) {
            return false;
        }
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (ModList.get().isLoaded(MOD_ID) && Impl.contains(text, query)) {
            return true;
        }
        return text.toLowerCase(java.util.Locale.ROOT)
                .contains(query.toLowerCase(java.util.Locale.ROOT));
    }

    private static final class Impl {
        private Impl() {
        }

        static boolean contains(String text, String query) {
            try {
                return me.towdium.jecharacters.utils.Match.contains(text, (CharSequence) query)
                        || me.towdium.jecharacters.utils.Match.contains(
                                text.toLowerCase(java.util.Locale.ROOT),
                                (CharSequence) query.toLowerCase(java.util.Locale.ROOT));
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
