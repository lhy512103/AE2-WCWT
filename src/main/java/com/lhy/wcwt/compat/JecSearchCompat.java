package com.lhy.wcwt.compat;

import com.lhy.wcwt.WcwtMod;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class JecSearchCompat {
    private static final String MOD_ID = "jecharacters";
    /** Set once the Just Enough Characters API fails to link, so every searched entry does not retry it. */
    private static volatile boolean broken;

    private JecSearchCompat() {
    }

    public static boolean contains(@Nullable String text, @Nullable String query) {
        if (text == null) {
            return false;
        }
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (!broken && ModList.get().isLoaded(MOD_ID) && Impl.contains(text, query)) {
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
            } catch (LinkageError e) {
                broken = true;
                WcwtMod.LOGGER.warn("WCWT: Just Enough Characters search integration disabled, its API changed", e);
                return false;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
    }
}
