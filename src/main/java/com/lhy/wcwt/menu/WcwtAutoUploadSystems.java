package com.lhy.wcwt.menu;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;

/** Counts logical backing systems before any insertion or duplicate handling can mutate state. */
final class WcwtAutoUploadSystems {
    private final Set<Object> systems = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean uncertainSystem;

    void add(Object system) {
        if (system != null) {
            systems.add(system);
        }
    }

    void markUncertain() {
        uncertainSystem = true;
    }

    <T> T uploadIfUnique(Supplier<T> upload, T noTarget, T ambiguous) {
        if (uncertainSystem) {
            return ambiguous;
        }
        return switch (systems.size()) {
            case 0 -> noTarget;
            case 1 -> upload.get();
            default -> ambiguous;
        };
    }
}
