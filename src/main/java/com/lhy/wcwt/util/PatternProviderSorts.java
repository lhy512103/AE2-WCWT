package com.lhy.wcwt.util;

import appeng.helpers.patternprovider.PatternContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;

public final class PatternProviderSorts {
    private PatternProviderSorts() {
    }

    public static final Comparator<PatternContainer> STABLE = Comparator
            .comparing((PatternContainer provider) -> provider.getTerminalGroup().name().getString().toLowerCase())
            .thenComparing(PatternProviderSorts::dimensionKey)
            .thenComparingLong(PatternProviderSorts::blockPosKey)
            .thenComparingInt(PatternProviderSorts::faceKey)
            .thenComparing(provider -> provider.getClass().getName());

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
}
