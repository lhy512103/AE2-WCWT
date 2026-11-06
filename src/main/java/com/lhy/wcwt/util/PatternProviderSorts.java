package com.lhy.wcwt.util;

import appeng.helpers.patternprovider.PatternContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PatternProviderSorts {
    private static final Comparator<SortKey> ORDER = Comparator
            .comparing(SortKey::groupName)
            .thenComparing(SortKey::dimension)
            .thenComparingLong(SortKey::blockPos)
            .thenComparingInt(SortKey::face)
            .thenComparing(SortKey::className);

    private PatternProviderSorts() {
    }

    /**
     * Sorts providers by terminal group name, then dimension, position, face and class. Every key is computed
     * once per provider up front: {@code getTerminalGroup()} resolves the neighbouring machines on each call,
     * so a comparator that re-derives it on every comparison costs N log N such lookups.
     */
    public static void sortStable(List<PatternContainer> providers) {
        if (providers.size() < 2) {
            return;
        }
        List<SortKey> keys = new ArrayList<>(providers.size());
        for (PatternContainer provider : providers) {
            keys.add(new SortKey(provider, groupNameKey(provider), dimensionKey(provider), blockPosKey(provider),
                    faceKey(provider), provider.getClass().getName()));
        }
        keys.sort(ORDER);
        for (int i = 0; i < keys.size(); i++) {
            providers.set(i, keys.get(i).provider());
        }
    }

    private static String groupNameKey(PatternContainer provider) {
        var group = provider.getTerminalGroup();
        return group == null ? "" : group.name().getString().toLowerCase(Locale.ROOT);
    }

    private static String dimensionKey(PatternContainer provider) {
        if (provider instanceof BlockEntity be && be.getLevel() != null) {
            ResourceKey<Level> key = be.getLevel().dimension();
            return key.location().toString();
        }
        if (provider instanceof appeng.parts.AEBasePart part && part.getLevel() != null) {
            ResourceKey<Level> key = part.getLevel().dimension();
            return key.location().toString();
        }
        return "";
    }

    private static long blockPosKey(PatternContainer provider) {
        if (provider instanceof BlockEntity be) {
            return be.getBlockPos().asLong();
        }
        if (provider instanceof appeng.parts.AEBasePart part) {
            return part.getBlockEntity().getBlockPos().asLong();
        }
        return BlockPos.ZERO.asLong();
    }

    private static int faceKey(PatternContainer provider) {
        if (provider instanceof appeng.parts.AEBasePart part) {
            Direction side = part.getSide();
            return side != null ? side.ordinal() : -1;
        }
        return -1;
    }

    private record SortKey(PatternContainer provider, String groupName, String dimension, long blockPos, int face,
                           String className) {
    }
}
