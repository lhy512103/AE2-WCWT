package com.lhy.wcwt.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The dependency-free identity and slot mapping used by the pattern-management screen. */
final class PatternManagementLayout {
    static final int EMPTY_SLOT_SUMMARY = -1;

    private PatternManagementLayout() {
    }

    static <T, G> Map<G, List<T>> groupByTerminalGroup(Stream<T> providers, Function<T, G> terminalGroup) {
        // Keep the complete group as the key: labels and bare hash codes are not identities.
        return providers.collect(Collectors.groupingBy(terminalGroup, LinkedHashMap::new, Collectors.toList()));
    }

    static CompactSlots compactSlots(int inventorySize, IntPredicate occupied) {
        var slots = new ArrayList<Integer>();
        int emptySlots = 0;
        for (int slot = 0; slot < inventorySize; slot++) {
            if (occupied.test(slot)) {
                slots.add(slot);
            } else {
                emptySlots++;
            }
        }
        if (emptySlots > 0) {
            slots.add(EMPTY_SLOT_SUMMARY);
        }
        return new CompactSlots(slots, emptySlots);
    }

    static int resolveSlot(int displayedSlot, int inventorySize, IntPredicate occupied) {
        if (displayedSlot == EMPTY_SLOT_SUMMARY) {
            // Resolve on each interaction, since prediction or server updates can fill the previous hole.
            for (int slot = 0; slot < inventorySize; slot++) {
                if (!occupied.test(slot)) {
                    return slot;
                }
            }
            return -1;
        }
        return displayedSlot >= 0 && displayedSlot < inventorySize ? displayedSlot : -1;
    }

    record CompactSlots(List<Integer> slots, int emptySlotCount) {
    }
}
