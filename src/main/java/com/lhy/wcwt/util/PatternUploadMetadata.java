package com.lhy.wcwt.util;

import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.compat.ExtendedAePlusPatternMetadata;
import com.lhy.wcwt.network.WcwtPacketLimits;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PatternUploadMetadata {
    private PatternUploadMetadata() {
    }

    public static void write(ItemStack stack, @Nullable String providerSearchText) {
        if (stack.isEmpty()) {
            return;
        }

        String normalizedSearchText = normalize(providerSearchText);
        if (normalizedSearchText != null) {
            stack.set(ModComponents.PATTERN_UPLOAD_DATA.get(), normalizedSearchText);
        } else {
            stack.remove(ModComponents.PATTERN_UPLOAD_DATA.get());
        }
    }

    @Nullable
    public static String getProviderSearchText(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return normalize(stack.get(ModComponents.PATTERN_UPLOAD_DATA.get()));
    }

    public static ItemStack copyWithoutUploadData(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.remove(ModComponents.PATTERN_UPLOAD_DATA.get());
        return ExtendedAePlusPatternMetadata.copyWithoutEncoder(copy);
    }

    public static boolean isSamePatternIgnoringUploadData(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(copyWithoutUploadData(first), copyWithoutUploadData(second));
    }

    @Nullable
    private static String normalize(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        // Stored in the pattern's components, so it must stay short regardless of where it came from.
        if (trimmed.length() > WcwtPacketLimits.MAX_SEARCH_TEXT_LENGTH) {
            trimmed = trimmed.substring(0, WcwtPacketLimits.MAX_SEARCH_TEXT_LENGTH);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
