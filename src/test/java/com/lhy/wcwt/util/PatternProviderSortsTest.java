package com.lhy.wcwt.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class PatternProviderSortsTest {
    @Test
    void sortsByGroupNameIgnoringCase() {
        var b = stub("beta");
        var a = stub("Alpha");
        var c = stub("charlie");
        List<PatternContainer> providers = new ArrayList<>(List.of(b, a, c));

        PatternProviderSorts.sortStable(providers);

        assertEquals(List.of(a, b, c), providers);
    }

    @Test
    void keepsInsertionOrderForEqualKeys() {
        var first = stub("same");
        var second = stub("same");
        List<PatternContainer> providers = new ArrayList<>(List.of(first, second));

        PatternProviderSorts.sortStable(providers);

        assertEquals(List.of(first, second), providers);
    }

    @Test
    void providersWithoutAGroupSortFirst() {
        var named = stub("alpha");
        var unnamed = stub(null);
        List<PatternContainer> providers = new ArrayList<>(List.of(named, unnamed));

        PatternProviderSorts.sortStable(providers);

        assertEquals(List.of(unnamed, named), providers);
    }

    private static PatternContainer stub(@Nullable String name) {
        return new PatternContainer() {
            @Override
            public IGrid getGrid() {
                return null;
            }

            @Override
            public InternalInventory getTerminalPatternInventory() {
                return null;
            }

            @Override
            public PatternContainerGroup getTerminalGroup() {
                return name == null ? null : new PatternContainerGroup(null, Component.literal(name), List.of());
            }
        };
    }
}
