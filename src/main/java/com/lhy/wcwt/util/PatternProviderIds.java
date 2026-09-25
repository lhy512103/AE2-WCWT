package com.lhy.wcwt.util;

import appeng.helpers.patternprovider.PatternContainer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-side ids for pattern providers that stay attached to the provider object itself.
 *
 * <p>An id is never reused, so a client mapping made before a provider was added, removed or renamed
 * resolves to nothing instead of to whichever provider now sits at the same list position.
 */
public final class PatternProviderIds {
    private static final Map<PatternContainer, Long> IDS = new WeakHashMap<>();
    private static long nextId = 1L;

    private PatternProviderIds() {
    }

    public static synchronized long idOf(PatternContainer provider) {
        Long id = IDS.get(provider);
        if (id == null) {
            id = nextId++;
            IDS.put(provider, id);
        }
        return id;
    }

    @Nullable
    public static PatternContainer find(List<PatternContainer> providers, long id) {
        if (id <= 0) {
            return null;
        }
        for (var provider : providers) {
            if (idOf(provider) == id) {
                return provider;
            }
        }
        return null;
    }
}
