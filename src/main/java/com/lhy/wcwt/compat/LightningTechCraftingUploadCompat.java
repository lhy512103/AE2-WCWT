package com.lhy.wcwt.compat;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import com.moakiee.ae2lt.logic.tianshu.terminal.PatternEncodingDuplicateFilter;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternUploadRouting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Uses Lightning Tech's Tianshu upload APIs so WCWT crafting auto-upload
 * matches the Tianshu terminal's preferred Matter Warping Matrix target.
 */
public final class LightningTechCraftingUploadCompat {
    private static final String AE2_LIGHTNING_TECH = "ae2lt";

    private LightningTechCraftingUploadCompat() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(AE2_LIGHTNING_TECH);
    }

    public static boolean isCraftingPattern(ItemStack pattern, Level level) {
        return isAvailable()
                && TianshuPatternUploadRouting.classify(pattern, level)
                == TianshuPatternUploadRouting.Route.CRAFTING_ASSEMBLER;
    }

    public static boolean isTianshuCraftingArray(PatternContainer provider) {
        return isAvailable()
                && provider != null
                && TianshuPatternUploadRouting.isMatterWarpingMatrixGroup(provider.getTerminalGroup());
    }

    public static int findDuplicateSlot(PatternContainer provider, ItemStack pattern, Level level) {
        if (provider == null || pattern == null || pattern.isEmpty()) {
            return -1;
        }
        InternalInventory inventory = provider.getTerminalPatternInventory();
        if (inventory == null) {
            return -1;
        }
        PatternEncodingDuplicateFilter.CheckResult result =
                PatternEncodingDuplicateFilter.checkEquivalentPattern(inventory, pattern, level);
        return result.duplicate() ? result.matchedSlot() : -1;
    }

    public static boolean insertCraftingPattern(PatternContainer provider, ItemStack pattern, boolean simulate) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        InternalInventory inventory = provider.getTerminalPatternInventory();
        if (inventory == null) {
            return false;
        }
        ItemStack toInsert = pattern.copyWithCount(1);
        int slot = firstFreePatternSlot(inventory, toInsert);
        if (slot < 0) {
            return false;
        }
        return inventory.insertItem(slot, toInsert, simulate).isEmpty();
    }

    private static int firstFreePatternSlot(InternalInventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty() && inventory.isItemValid(slot, stack)) {
                return slot;
            }
        }
        return -1;
    }
}
