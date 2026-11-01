package com.lhy.wcwt.client;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Run through patternManagementRegressionTest, or compile with PatternManagementLayout using JDK 17. */
public final class PatternManagementLayoutTest {
    public static void main(String[] args) {
        groupsUseFullIdentity();
        summaryTracksRealEmptySlots();
        summaryHandlesEmptyAndFullProviders();
        explicitSlotsKeepTheirInventoryIndices();
        System.out.println("PatternManagementLayoutTest: 4 regression cases passed");
    }

    private static void groupsUseFullIdentity() {
        // Stand-ins for the upstream (icon, name, tooltip) record; deliberately collide every hash.
        var nuclear = new Group("nuclearcraft:alloy_smelter", "AlloySmelter", List.of());
        var enderio = new Group("enderio:alloy_smelter", "AlloySmelter", List.of());
        var nuclearCopy = new Group("nuclearcraft:alloy_smelter", "AlloySmelter", List.of());
        var upgraded = new Group("nuclearcraft:alloy_smelter", "AlloySmelter", List.of("Upgrade"));
        var providers = List.of(new Provider(nuclear, "AlloySmelter"),
                new Provider(enderio, "AlloySmelter"),
                new Provider(nuclearCopy, "Alias"),
                new Provider(upgraded, "AlloySmelter"));
        var groups = PatternManagementLayout.groupByTerminalGroup(providers.stream()
                .sorted(Comparator.comparing(Provider::displayName)), Provider::group);
        expect(3, groups.size(), "Different machines or tooltips must not merge, even with identical labels/hashes");
        expect(List.of(providers.get(2), providers.get(0)), groups.get(nuclear),
                "Equal terminal groups must merge despite different aliases and object instances");
        expect(List.of(providers.get(1)), groups.get(enderio), "Other machine keeps its own providers");
        expect(List.of(nuclearCopy, enderio, upgraded), List.copyOf(groups.keySet()),
                "Headers retain the screen's sorted encounter order");
    }

    private static void summaryTracksRealEmptySlots() {
        boolean[] occupied = {true, false, true, false, false};
        var before = PatternManagementLayout.compactSlots(occupied.length, slot -> occupied[slot]);
        expect(List.of(0, 2, -1), before.slots(), "Only occupied slots and one summary are displayed");
        expect(3, before.emptySlotCount(), "Summary counts all actual holes");
        int first = PatternManagementLayout.resolveSlot(-1, occupied.length, slot -> occupied[slot]);
        expect(1, first, "Summary inserts into the first actual hole, not its displayed column");
        occupied[first] = true; // Same occupancy change used by the screen's existing insert prediction/server refresh.
        var after = PatternManagementLayout.compactSlots(occupied.length, slot -> occupied[slot]);
        expect(List.of(0, 1, 2, -1), after.slots(), "Inserted pattern becomes a normal visible slot");
        expect(2, after.emptySlotCount(), "Insertion decrements the summary count");
        int next = PatternManagementLayout.resolveSlot(-1, occupied.length, slot -> occupied[slot]);
        expect(3, next, "A second click rechecks current occupancy instead of reusing a filled slot");
        occupied[next] = true;
        occupied[4] = true;
        expect(-1, PatternManagementLayout.resolveSlot(-1, occupied.length, slot -> occupied[slot]),
                "Stale summary must not target any occupied slot when the provider becomes full");
        expect(List.of(0, 1, 2, 3, 4),
                PatternManagementLayout.compactSlots(occupied.length, slot -> occupied[slot]).slots(),
                "The summary disappears once all holes are filled");
        occupied[2] = false;
        expect(2, PatternManagementLayout.resolveSlot(-1, occupied.length, slot -> occupied[slot]),
                "Extraction makes the newly empty real slot available again");
    }

    private static void summaryHandlesEmptyAndFullProviders() {
        var empty = PatternManagementLayout.compactSlots(9, slot -> false);
        expect(List.of(-1), empty.slots(), "Entirely empty providers expose an interactive summary");
        expect(9, empty.emptySlotCount(), "Empty provider capacity remains visible");
        expect(0, PatternManagementLayout.resolveSlot(-1, 9, slot -> false), "An empty provider inserts at zero");
        expect(0, PatternManagementLayout.compactSlots(9, slot -> true).emptySlotCount(),
                "A full provider has no empty slots");
        expect(List.of(), PatternManagementLayout.compactSlots(0, slot -> false).slots(),
                "Zero-capacity providers expose no summary");
        expect(-1, PatternManagementLayout.resolveSlot(-1, 0, slot -> false), "Zero capacity has no insert target");
        expect(0, PatternManagementLayout.resolveSlot(-1, 2, slot -> false),
                "Another provider's summary resolves only against that provider's own inventory");
    }

    private static void explicitSlotsKeepTheirInventoryIndices() {
        expect(7, PatternManagementLayout.resolveSlot(7, 9, slot -> true),
                "Normal compacted slots retain their original inventory index");
        expect(4, PatternManagementLayout.resolveSlot(4, 9, slot -> false),
                "Uncompacted empty slots keep their directly clicked target");
        expect(-1, PatternManagementLayout.resolveSlot(9, 9, slot -> false), "Out-of-range slots are rejected");
        expect(-1, PatternManagementLayout.resolveSlot(-2, 9, slot -> false), "Only the defined summary sentinel resolves");
    }

    private static void expect(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private record Group(String icon, String name, List<String> tooltip) {
        @Override
        public int hashCode() {
            return 37;
        }
    }

    private record Provider(Group group, String displayName) {
    }
}
