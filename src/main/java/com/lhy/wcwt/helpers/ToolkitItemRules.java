package com.lhy.wcwt.helpers;

import appeng.api.crafting.PatternDetailsHelper;
import com.lhy.wcwt.config.WcwtServerConfig;
import net.minecraft.world.item.ItemStack;

/** Common validation for every toolkit slot. */
public final class ToolkitItemRules {
    private ToolkitItemRules() {
    }

    /** Toolkit entries are single, usable items and never encoded patterns. */
    public static boolean isBaseToolkitCandidate(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getMaxStackSize() == 1
                && !PatternDetailsHelper.isEncodedPattern(stack);
    }

    public static boolean mayPlace(int toolkitInventoryIndex, ItemStack stack) {
        return stack.isEmpty() || isBaseToolkitCandidate(stack);
    }

    public static boolean isEligibleForToolkitQuickFill(ItemStack stack) {
        return isBaseToolkitCandidate(stack);
    }

    /** Returns every slot in visual order. */
    public static int[] insertionIndexOrder(ItemStack ignored) {
        int totalSlots = Math.max(WcwtServerConfig.MIN_TOOLKIT_SLOTS, WcwtServerConfig.toolkitSlotCount());
        int[] result = new int[totalSlots];
        for (int i = 0; i < totalSlots; i++) {
            result[i] = i;
        }
        return result;
    }
}
