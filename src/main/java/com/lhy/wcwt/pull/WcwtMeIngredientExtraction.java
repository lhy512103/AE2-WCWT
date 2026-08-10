package com.lhy.wcwt.pull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.util.prioritylist.IPartitionList;

/** 自 ae2utility MeIngredientExtraction 迁入。 */
public final class WcwtMeIngredientExtraction {

    private WcwtMeIngredientExtraction() {
    }

    @Nullable
    public static Ingredient ingredientFromItemStacks(List<ItemStack> alternativeStacks) {
        ItemStack[] stacks = alternativeStacks.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .map(ItemStack::copy)
                .toArray(ItemStack[]::new);
        if (stacks.length == 0) {
            return null;
        }
        return Ingredient.of(stacks);
    }

    public static List<ItemStack> extractAlternatives(MEStorage storage, IEnergySource energy, IActionSource actionSource,
            @Nullable IPartitionList filter, List<ItemStack> alternativeStacks, int amount) {
        List<AEItemKey> preferredKeys = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks).stream()
                .map(AEItemKey::of)
                .filter(Objects::nonNull)
                .filter(k -> filter == null || filter.isListed(k))
                .distinct()
                .toList();
        Ingredient wide = ingredientFromItemStacks(alternativeStacks);
        List<ItemStack> orderedAlts = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks);
        boolean exactOnly = WcwtStackMatching.requiresExactItemKeyMatch(orderedAlts);

        Map<AEItemKey, Integer> extractedByKey = new LinkedHashMap<>();
        int remaining = Math.max(0, amount);
        for (AEItemKey candidate : preferredKeys) {
            int extracted = extractAmount(storage, energy, actionSource, candidate, remaining);
            if (extracted > 0) {
                extractedByKey.merge(candidate, extracted, Integer::sum);
                remaining -= extracted;
            }
            if (remaining <= 0) {
                break;
            }
        }
        if (remaining > 0 && !exactOnly && wide != null) {
            for (var entry : storage.getAvailableStacks()) {
                if (entry.getLongValue() <= 0 || !(entry.getKey() instanceof AEItemKey itemKey)) {
                    continue;
                }
                if (filter != null && !filter.isListed(itemKey)) {
                    continue;
                }
                if (!itemKey.matches(wide)) {
                    continue;
                }
                int extracted = extractAmount(storage, energy, actionSource, itemKey, remaining);
                if (extracted > 0) {
                    extractedByKey.merge(itemKey, extracted, Integer::sum);
                    remaining -= extracted;
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
        if (remaining > 0 && !exactOnly) {
            for (ItemStack alt : orderedAlts) {
                if (alt.isEmpty()) {
                    continue;
                }
                var wantedItem = alt.getItem();
                for (var entry : storage.getAvailableStacks()) {
                    if (entry.getLongValue() <= 0 || !(entry.getKey() instanceof AEItemKey itemKey)) {
                        continue;
                    }
                    if (filter != null && !filter.isListed(itemKey)) {
                        continue;
                    }
                    if (itemKey.getItem() != wantedItem) {
                        continue;
                    }
                    int extracted = extractAmount(storage, energy, actionSource, itemKey, remaining);
                    if (extracted > 0) {
                        extractedByKey.merge(itemKey, extracted, Integer::sum);
                        remaining -= extracted;
                    }
                    if (remaining <= 0) {
                        break;
                    }
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return extractedByKey.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue()))
                .toList();
    }

    private static int extractAmount(MEStorage storage, IEnergySource energy, IActionSource actionSource,
            AEItemKey key, int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }
        long extracted = StorageHelper.poweredExtraction(energy, storage, key, requestedAmount, actionSource);
        return (int) Math.min(Math.max(extracted, 0), requestedAmount);
    }

    public static long reserveAmount(Map<AEItemKey, Long> remaining, List<ItemStack> alternativeStacks,
            @Nullable Ingredient wideIngredient, long requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }
        List<AEItemKey> preferredKeys = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks).stream()
                .map(AEItemKey::of)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<ItemStack> orderedAlts = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks);
        boolean exactOnly = WcwtStackMatching.requiresExactItemKeyMatch(orderedAlts);
        long amountLeft = requestedAmount;

        for (AEItemKey candidate : preferredKeys) {
            amountLeft -= reserveFromKey(remaining, candidate, amountLeft);
            if (amountLeft <= 0) {
                return requestedAmount;
            }
        }
        if (exactOnly) {
            return requestedAmount - amountLeft;
        }
        if (wideIngredient != null) {
            for (var entry : remaining.entrySet()) {
                AEItemKey key = entry.getKey();
                if (entry.getValue() <= 0 || !key.matches(wideIngredient)) {
                    continue;
                }
                amountLeft -= reserveFromKey(remaining, key, amountLeft);
                if (amountLeft <= 0) {
                    return requestedAmount;
                }
            }
        }
        for (ItemStack alt : orderedAlts) {
            if (alt.isEmpty()) {
                continue;
            }
            var wantedItem = alt.getItem();
            for (var entry : remaining.entrySet()) {
                AEItemKey key = entry.getKey();
                if (entry.getValue() <= 0 || key.getItem() != wantedItem) {
                    continue;
                }
                amountLeft -= reserveFromKey(remaining, key, amountLeft);
                if (amountLeft <= 0) {
                    return requestedAmount;
                }
            }
        }
        return requestedAmount - amountLeft;
    }

    private static long reserveFromKey(Map<AEItemKey, Long> remaining, AEItemKey key, long requestedAmount) {
        long available = remaining.getOrDefault(key, 0L);
        long reserved = Math.min(Math.max(available, 0), requestedAmount);
        if (reserved > 0) {
            remaining.put(key, available - reserved);
        }
        return reserved;
    }
}
