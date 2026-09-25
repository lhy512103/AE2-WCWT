package com.lhy.wcwt.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import appeng.helpers.patternprovider.PatternContainer;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatternProviderIdsTest {
    @Test
    void idFollowsTheProviderNotItsListPosition() {
        PatternContainer first = provider();
        PatternContainer second = provider();
        long secondId = PatternProviderIds.idOf(second);

        PatternContainer added = provider();
        List<PatternContainer> reordered = List.of(added, second, first);

        assertEquals(secondId, PatternProviderIds.idOf(second));
        assertSame(second, PatternProviderIds.find(reordered, secondId));
    }

    @Test
    void idsAreUniqueAndNeverResolveToAnotherProvider() {
        PatternContainer removed = provider();
        PatternContainer remaining = provider();
        long removedId = PatternProviderIds.idOf(removed);

        assertNotEquals(removedId, PatternProviderIds.idOf(remaining));
        assertNull(PatternProviderIds.find(List.of(remaining), removedId));
    }

    @Test
    void nonPositiveIdsResolveToNothing() {
        PatternContainer provider = provider();
        assertNull(PatternProviderIds.find(List.of(provider), 0L));
        assertNull(PatternProviderIds.find(List.of(provider), -1L));
    }

    private static PatternContainer provider() {
        return (PatternContainer) Proxy.newProxyInstance(
                PatternContainer.class.getClassLoader(),
                new Class<?>[] {PatternContainer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "provider@" + System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
