package com.lhy.wcwt.compat;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public final class AdvancedAePatternCompat {
    private static final String MOD_ID = "advanced_ae";

    private AdvancedAePatternCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    @Nullable
    public static ItemStack encodeProcessingPattern(ItemStack encodedSlotPattern, List<GenericStack> inputs,
                                                    List<GenericStack> outputs, Level level) {
        if (!available()) {
            return null;
        }
        return Impl.encodeProcessingPattern(encodedSlotPattern, inputs, outputs, level);
    }

    @Nullable
    public static AdvView view(ItemStack stack, Level level, boolean allowVanillaProcessing) {
        if (!available()) {
            return null;
        }
        return Impl.view(stack, level, allowVanillaProcessing);
    }

    public static ItemStack encode(List<GenericStack> inputs, List<GenericStack> outputs,
                                   HashMap<AEKey, Direction> dirMap) {
        if (!available()) {
            return ItemStack.EMPTY;
        }
        return Impl.encode(inputs, outputs, dirMap);
    }

    public record AdvView(List<GenericStack> inputs, List<GenericStack> outputs,
                          HashMap<AEKey, Direction> dirMap) {
    }

    private static final class Impl {
        private Impl() {
        }

        @Nullable
        static ItemStack encodeProcessingPattern(ItemStack encodedSlotPattern, List<GenericStack> inputs,
                                                 List<GenericStack> outputs, Level level) {
            AdvView view = view(encodedSlotPattern, level, false);
            if (view == null) {
                return null;
            }
            var newDirMap = new HashMap<AEKey, Direction>();
            for (GenericStack input : inputs) {
                if (input == null || !view.dirMap().containsKey(input.what())) {
                    continue;
                }
                newDirMap.put(input.what(), view.dirMap().get(input.what()));
            }
            if (newDirMap.isEmpty()) {
                return null;
            }
            return encode(inputs, outputs, newDirMap);
        }

        @Nullable
        static AdvView view(ItemStack stack, Level level, boolean allowVanillaProcessing) {
            if (stack == null || stack.isEmpty() || level == null) {
                return null;
            }
            var details = PatternDetailsHelper.decodePattern(stack, level);
            if (details instanceof net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern pattern) {
                return new AdvView(pattern.getSparseInputs(), pattern.getSparseOutputs(),
                        new HashMap<>(pattern.getDirectionMap()));
            }
            if (allowVanillaProcessing && details instanceof AEProcessingPattern process) {
                var dirMap = new HashMap<AEKey, Direction>();
                for (GenericStack input : process.getSparseInputs()) {
                    if (input != null) {
                        dirMap.putIfAbsent(input.what(), null);
                    }
                }
                return new AdvView(process.getSparseInputs(), process.getSparseOutputs(), dirMap);
            }
            return null;
        }

        static ItemStack encode(List<GenericStack> inputs, List<GenericStack> outputs,
                                HashMap<AEKey, Direction> dirMap) {
            return net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder
                    .encodeProcessingPattern(inputs, outputs, dirMap);
        }
    }
}
