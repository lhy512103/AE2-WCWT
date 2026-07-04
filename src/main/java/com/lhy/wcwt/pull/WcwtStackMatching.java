package com.lhy.wcwt.pull;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.MEStorageMenu;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class WcwtStackMatching {
    private WcwtStackMatching() {
    }

    public static boolean hasSpecificData(ItemStack stack) {
        return WcwtPullIngredientOrdering.componentSpecificityRank(stack) > 0;
    }

    public static boolean requiresExactItemKeyMatch(List<ItemStack> alternatives) {
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty() && hasSpecificData(alternative)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesAnyAlternative(ItemStack stack, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (ItemStack alternative : alternatives) {
            if (sameItemAndComponents(stack, alternative)) {
                return true;
            }
        }
        if (requiresExactItemKeyMatch(alternatives)) {
            return false;
        }
        if (wideIngredient != null && wideIngredient.test(stack)) {
            return true;
        }
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty() && ItemStack.isSameItem(stack, alternative)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesItemKey(AEItemKey itemKey, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        if (itemKey == null) {
            return false;
        }
        for (ItemStack alternative : alternatives) {
            AEItemKey alternativeKey = AEItemKey.of(alternative);
            if (alternativeKey != null && alternativeKey.equals(itemKey)) {
                return true;
            }
        }
        if (requiresExactItemKeyMatch(alternatives)) {
            return false;
        }
        if (wideIngredient != null && itemKey.matches(wideIngredient)) {
            return true;
        }
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty() && itemKey.getItem() == alternative.getItem()) {
                return true;
            }
        }
        return false;
    }

    public static boolean reserveClientRepoStoredIngredient(MEStorageMenu menu, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient, Object2IntOpenHashMap<AEItemKey> reservedAmounts) {
        var clientRepo = menu.getClientRepo();
        if (clientRepo == null) {
            return false;
        }
        for (var entry : clientRepo.getAllEntries()) {
            if (entry.getStoredAmount() <= 0 || !(entry.getWhat() instanceof AEItemKey itemKey)) {
                continue;
            }
            if (!matchesItemKey(itemKey, alternatives, wideIngredient)) {
                continue;
            }

            long available = entry.getStoredAmount() - reservedAmounts.getInt(itemKey);
            if (available <= 0) {
                continue;
            }
            reservedAmounts.addTo(itemKey, 1);
            return true;
        }
        return false;
    }

    public static boolean hasClientRepoCraftableIngredient(MEStorageMenu menu, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        var clientRepo = menu.getClientRepo();
        if (clientRepo == null) {
            return false;
        }
        for (var entry : clientRepo.getAllEntries()) {
            if (!entry.isCraftable() || !(entry.getWhat() instanceof AEItemKey itemKey)) {
                continue;
            }
            if (matchesItemKey(itemKey, alternatives, wideIngredient)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sameItemAndComponents(ItemStack first, ItemStack second) {
        return first != null
                && second != null
                && !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.isSameItemSameComponents(first, second);
    }
}
