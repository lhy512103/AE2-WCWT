package com.lhy.wcwt.helpers;

import appeng.api.crafting.PatternDetailsHelper;
import com.lhy.wcwt.config.WcwtServerConfig;
import net.minecraft.world.item.ItemStack;

/**
 * 工具包槽位校验：不可堆叠且不含已编码样板。
 */
public final class ToolkitItemRules {
    private ToolkitItemRules() {
    }

    public static boolean isBaseToolkitCandidate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getMaxStackSize() != 1) {
            return false;
        }
        return !PatternDetailsHelper.isEncodedPattern(stack);
    }

    public static boolean mayPlace(int toolkitInventoryIndex, ItemStack stack) {
        return stack.isEmpty() || isBaseToolkitCandidate(stack);
    }

    /** Shift + 快捷：从玩家储物格搬进工具包的筛选（与基底规则一致）。 */
    public static boolean isEligibleForToolkitQuickFill(ItemStack stack) {
        return isBaseToolkitCandidate(stack);
    }

    public static int[] insertionIndexOrder(ItemStack stack) {
        int totalSlots = WcwtServerConfig.toolkitSlotCount();
        int[] out = new int[totalSlots];
        for (int i = 0; i < totalSlots; i++) {
            out[i] = i;
        }
        return out;
    }
}
