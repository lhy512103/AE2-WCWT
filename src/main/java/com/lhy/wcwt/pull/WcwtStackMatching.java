package com.lhy.wcwt.pull;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.MEStorageMenu;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class WcwtStackMatching {
    private WcwtStackMatching() {
    }

    public static final class UniqueStacks {
        private final List<ItemStack> stacks = new ArrayList<>();
        private final LongOpenHashSet hashes = new LongOpenHashSet();

        public boolean add(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            long hash = ItemStack.hashItemAndComponents(stack);
            if (!hashes.add(hash)) {
                for (ItemStack existing : stacks) {
                    if (ItemStack.isSameItemSameComponents(existing, stack)) {
                        return false;
                    }
                }
            }
            stacks.add(stack);
            return true;
        }

        public List<ItemStack> list() {
            return stacks;
        }
    }

    public static final class ClientRepoIndex {
        private final Object2LongOpenHashMap<AEItemKey> stored = new Object2LongOpenHashMap<>();
        private final ObjectOpenHashSet<AEItemKey> craftable = new ObjectOpenHashSet<>();
        private final ObjectOpenHashSet<Item> storedItems = new ObjectOpenHashSet<>();
        private final ObjectOpenHashSet<Item> craftableItems = new ObjectOpenHashSet<>();

        private ClientRepoIndex() {
            stored.defaultReturnValue(0L);
        }

        public static ClientRepoIndex of(MEStorageMenu menu) {
            ClientRepoIndex index = new ClientRepoIndex();
            var clientRepo = menu.getClientRepo();
            if (clientRepo == null) {
                return index;
            }
            for (var entry : clientRepo.getAllEntries()) {
                if (!(entry.getWhat() instanceof AEItemKey itemKey)) {
                    continue;
                }
                if (entry.getStoredAmount() > 0) {
                    index.stored.addTo(itemKey, entry.getStoredAmount());
                    index.storedItems.add(itemKey.getItem());
                }
                if (entry.isCraftable()) {
                    index.craftable.add(itemKey);
                    index.craftableItems.add(itemKey.getItem());
                }
            }
            return index;
        }
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
        return reserveClientRepoStoredIngredient(menu, alternatives, wideIngredient, reservedAmounts, 1) == 1;
    }

    public static int reserveClientRepoStoredIngredient(MEStorageMenu menu, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient, Object2IntOpenHashMap<AEItemKey> reservedAmounts,
            int requestedAmount) {
        return reserveFromIndex(ClientRepoIndex.of(menu), alternatives, wideIngredient, reservedAmounts, requestedAmount);
    }

    public static int reserveFromIndex(ClientRepoIndex index, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient, Object2IntOpenHashMap<AEItemKey> reservedAmounts,
            int requestedAmount) {
        if (index == null || requestedAmount <= 0) {
            return 0;
        }
        int reserved = 0;
        for (ItemStack alternative : alternatives) {
            AEItemKey key = AEItemKey.of(alternative);
            if (key == null) {
                continue;
            }
            reserved += takeStored(index, key, reservedAmounts, requestedAmount - reserved);
            if (reserved >= requestedAmount) {
                return reserved;
            }
        }
        if (requiresExactItemKeyMatch(alternatives)) {
            return reserved;
        }
        if (!mayMatchStored(index, alternatives, wideIngredient)) {
            return reserved;
        }
        for (var entry : index.stored.object2LongEntrySet()) {
            AEItemKey itemKey = entry.getKey();
            if (!matchesItemKey(itemKey, alternatives, wideIngredient)) {
                continue;
            }
            reserved += takeStored(index, itemKey, reservedAmounts, requestedAmount - reserved);
            if (reserved >= requestedAmount) {
                return reserved;
            }
        }
        return reserved;
    }

    public static boolean hasClientRepoCraftableIngredient(MEStorageMenu menu, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        return hasCraftableFromIndex(ClientRepoIndex.of(menu), alternatives, wideIngredient);
    }

    public static boolean hasCraftableFromIndex(ClientRepoIndex index, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        if (index == null) {
            return false;
        }
        for (ItemStack alternative : alternatives) {
            AEItemKey key = AEItemKey.of(alternative);
            if (key != null && index.craftable.contains(key)) {
                return true;
            }
        }
        if (requiresExactItemKeyMatch(alternatives)) {
            return false;
        }
        if (!mayMatchCraftable(index, alternatives, wideIngredient)) {
            return false;
        }
        for (AEItemKey itemKey : index.craftable) {
            if (matchesItemKey(itemKey, alternatives, wideIngredient)) {
                return true;
            }
        }
        return false;
    }

    private static int takeStored(ClientRepoIndex index, AEItemKey itemKey,
            Object2IntOpenHashMap<AEItemKey> reservedAmounts, int remaining) {
        if (remaining <= 0) {
            return 0;
        }
        long available = index.stored.getLong(itemKey) - reservedAmounts.getInt(itemKey);
        if (available <= 0) {
            return 0;
        }
        int amount = (int) Math.min(available, remaining);
        reservedAmounts.addTo(itemKey, amount);
        return amount;
    }

    private static boolean mayMatchStored(ClientRepoIndex index, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        if (wideIngredient != null && !wideIngredient.isEmpty()) {
            return true;
        }
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty() && index.storedItems.contains(alternative.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static boolean mayMatchCraftable(ClientRepoIndex index, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        if (wideIngredient != null && !wideIngredient.isEmpty()) {
            return true;
        }
        for (ItemStack alternative : alternatives) {
            if (alternative != null && !alternative.isEmpty() && index.craftableItems.contains(alternative.getItem())) {
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
