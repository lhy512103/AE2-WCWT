package com.lhy.wcwt.compat;

import appeng.api.crafting.PatternDetailsHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class LightningTechOverloadCompat {
    private static final String AE2LT = "ae2lt";

    private LightningTechOverloadCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(AE2LT);
    }

    public static boolean isOverloadPattern(ItemStack stack) {
        return available() && Impl.isOverloadPattern(stack);
    }

    public static ItemStack convertToOverload(ItemStack sourcePattern, Level level,
                                              HolderLookup.Provider registries,
                                              int[] inputIdOnlySlots, int[] outputIdOnlySlots) {
        if (!available() || sourcePattern == null || sourcePattern.isEmpty() || level == null) {
            return ItemStack.EMPTY;
        }
        return Impl.convert(sourcePattern, level, registries, inputIdOnlySlots, outputIdOnlySlots);
    }

    public static Set<Integer> inputIdOnlySlots(ItemStack stack) {
        return available() ? Impl.idOnlySlots(stack, true) : Set.of();
    }

    public static Set<Integer> outputIdOnlySlots(ItemStack stack) {
        return available() ? Impl.idOnlySlots(stack, false) : Set.of();
    }

    private static final class Impl {
        private Impl() {
        }

        static boolean isOverloadPattern(ItemStack stack) {
            return stack != null && !stack.isEmpty()
                    && stack.getItem() instanceof com.moakiee.ae2lt.item.OverloadPatternItem;
        }

        static ItemStack convert(ItemStack sourcePattern, Level level, HolderLookup.Provider registries,
                                 int[] inputIdOnlySlots, int[] outputIdOnlySlots) {
            try {
                var resolver = new com.moakiee.ae2lt.overload.runtime.pattern.Ae2PlainPatternResolver(level);
                var service = new com.moakiee.ae2lt.overload.pattern.PatternConversionService();
                var editable = service.resolveEditableSource(sourcePattern, resolver, registries).orElse(null);
                if (editable == null) {
                    return ItemStack.EMPTY;
                }
                var details = PatternDetailsHelper.decodePattern(
                        editable.parsedPattern().sourcePattern().toItemStack(registries), level);
                if (details instanceof appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern) {
                    return ItemStack.EMPTY;
                }
                var existing = editable.encodedPattern();
                var builder = com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern.builder();
                for (var input : editable.parsedPattern().inputs()) {
                    builder.input(input.slotIndex(), contains(inputIdOnlySlots, input.slotIndex())
                            ? com.moakiee.ae2lt.overload.runtime.model.MatchMode.ID_ONLY
                            : existing.inputModeOrDefault(input.slotIndex()));
                }
                for (var output : editable.parsedPattern().outputs()) {
                    builder.output(output.slotIndex(), contains(outputIdOnlySlots, output.slotIndex())
                            ? com.moakiee.ae2lt.overload.runtime.model.MatchMode.ID_ONLY
                            : existing.outputModeOrDefault(output.slotIndex()));
                }
                var overloadItem = com.moakiee.ae2lt.registry.ModItems.OVERLOAD_PATTERN.get();
                if (!(overloadItem instanceof com.moakiee.ae2lt.item.OverloadPatternItem item)) {
                    return ItemStack.EMPTY;
                }
                return service.createOverloadPatternStack(item, editable.parsedPattern(), builder.build());
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        static Set<Integer> idOnlySlots(ItemStack stack, boolean inputs) {
            if (!isOverloadPattern(stack)) {
                return Set.of();
            }
            var encoded = ((com.moakiee.ae2lt.item.OverloadPatternItem) stack.getItem())
                    .readEncodedPattern(stack).orElse(null);
            if (encoded == null) {
                return Set.of();
            }
            Set<Integer> result = new HashSet<>();
            var slots = inputs ? encoded.inputSlots() : encoded.outputSlots();
            for (var slot : slots) {
                if (slot.matchMode() == com.moakiee.ae2lt.overload.runtime.model.MatchMode.ID_ONLY) {
                    result.add(slot.slotIndex());
                }
            }
            return result;
        }

        private static boolean contains(@Nullable int[] values, int slot) {
            if (values == null) {
                return false;
            }
            for (int value : values) {
                if (value == slot) {
                    return true;
                }
            }
            return false;
        }
    }
}
