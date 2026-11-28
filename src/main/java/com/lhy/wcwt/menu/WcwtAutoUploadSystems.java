package com.lhy.wcwt.menu;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Counts logical backing systems before any insertion or duplicate handling can mutate state. */
final class WcwtAutoUploadSystems {
    private final Set<Object> systems = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Object> meteoriteProviders = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean ecoAvailable;
    private boolean uncertainSystem;

    void add(Object system) {
        if (system != null) {
            systems.add(system);
        }
    }

    void markUncertain() {
        uncertainSystem = true;
    }

    void addEco(Object storage) {
        if (storage == null) {
            markUncertain();
        } else {
            ecoAvailable = true;
        }
    }

    void addMeteoriteProvider(Object provider) {
        meteoriteProviders.add(provider);
    }

    <T> T uploadIfUnique(Supplier<T> upload, Supplier<T> uploadToEco, T noTarget, T ambiguous) {
        if (uncertainSystem || (ecoAvailable && !systems.isEmpty())) {
            return ambiguous;
        }
        if (ecoAvailable) {
            T result = uploadToEco.get();
            if (!Objects.equals(result, noTarget)) {
                return result;
            }
        }
        return switch (systems.size() + meteoriteProviders.size()) {
            case 0 -> noTarget;
            case 1 -> upload.get();
            default -> ambiguous;
        };
    }
}
