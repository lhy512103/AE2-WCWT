package com.lhy.wcwt.util;

import com.lhy.wcwt.init.ModComponents;
import net.minecraft.nbt.CompoundTag;
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
        CompoundTag tag = getOrCreateRootTag(stack);
        if (normalizedSearchText != null) {
            tag.putString(ModComponents.PATTERN_UPLOAD_DATA, normalizedSearchText);
        } else {
            tag.remove(ModComponents.PATTERN_UPLOAD_DATA);
            cleanupRootTag(stack, tag);
        }
    }

    @Nullable
    public static String getProviderSearchText(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = getRootTag(stack);
        return tag == null ? null : normalize(tag.getString(ModComponents.PATTERN_UPLOAD_DATA));
    }

    public static ItemStack copyWithoutUploadData(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        CompoundTag tag = getRootTag(copy);
        if (tag != null) {
            tag.remove(ModComponents.PATTERN_UPLOAD_DATA);
            cleanupRootTag(copy, tag);
        }
        return copy;
    }

    public static boolean isSamePatternIgnoringUploadData(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }
        ItemStack left = copyWithoutUploadData(first);
        ItemStack right = copyWithoutUploadData(second);
        return ItemStack.isSameItem(left, right) && java.util.Objects.equals(left.getTag(), right.getTag());
    }

    @Nullable
    private static String normalize(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        CompoundTag stackTag = stack.getOrCreateTag();
        CompoundTag root = stackTag.getCompound(ModComponents.ROOT_TAG);
        stackTag.put(ModComponents.ROOT_TAG, root);
        return root;
    }

    @Nullable
    private static CompoundTag getRootTag(ItemStack stack) {
        CompoundTag stackTag = stack.getTag();
        if (stackTag == null || !stackTag.contains(ModComponents.ROOT_TAG, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        return stackTag.getCompound(ModComponents.ROOT_TAG);
    }

    private static void cleanupRootTag(ItemStack stack, CompoundTag rootTag) {
        if (rootTag.isEmpty()) {
            CompoundTag stackTag = stack.getTag();
            if (stackTag != null) {
                stackTag.remove(ModComponents.ROOT_TAG);
                if (stackTag.isEmpty()) {
                    stack.setTag(null);
                }
            }
        }
    }
}
