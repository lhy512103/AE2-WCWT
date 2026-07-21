package com.lhy.wcwt.pull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;
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
        List<ItemStack> orderedAlts = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks);
        Ingredient wide = ingredientFromItemStacks(orderedAlts);
        boolean exactOnly = WcwtStackMatching.requiresExactItemKeyMatch(orderedAlts);
        List<AEItemKey> candidates = buildCandidateKeys(storage, filter, orderedAlts, wide, exactOnly);

        Map<AEItemKey, Integer> extractedByKey = new LinkedHashMap<>();
        int remaining = Math.max(0, amount);
        for (AEItemKey candidate : candidates) {
            if (remaining <= 0) {
                break;
            }
            long extracted = StorageHelper.poweredExtraction(energy, storage, candidate, remaining, actionSource);
            if (extracted <= 0) {
                continue;
            }
            int extractedAmount = (int) Math.min(extracted, remaining);
            extractedByKey.merge(candidate, extractedAmount, Integer::sum);
            remaining -= extractedAmount;
        }
        return extractedByKey.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue()))
                .toList();
    }

    private static List<AEItemKey> buildCandidateKeys(MEStorage storage, @Nullable IPartitionList filter,
            List<ItemStack> orderedAlts, @Nullable Ingredient wide, boolean exactOnly) {
        LinkedHashSet<AEItemKey> candidates = new LinkedHashSet<>();
        orderedAlts.stream()
                .map(AEItemKey::of)
                .filter(Objects::nonNull)
                .filter(key -> filter == null || filter.isListed(key))
                .forEach(candidates::add);
        if (exactOnly) {
            return List.copyOf(candidates);
        }

        List<AvailableCandidate> available = new ArrayList<>();
        for (var entry : storage.getAvailableStacks()) {
            if (entry.getLongValue() > 0 && entry.getKey() instanceof AEItemKey itemKey
                    && (filter == null || filter.isListed(itemKey))) {
                available.add(new AvailableCandidate(itemKey, entry.getLongValue()));
            }
        }
        available.sort((left, right) -> Long.compare(right.amount(), left.amount()));
        if (wide != null) {
            available.stream()
                    .map(AvailableCandidate::key)
                    .filter(key -> key.matches(wide))
                    .forEach(candidates::add);
        }
        Set<Item> fallbackItems = new HashSet<>();
        for (ItemStack alt : orderedAlts) {
            if (!alt.isEmpty()) {
                fallbackItems.add(alt.getItem());
            }
        }
        available.stream()
                .map(AvailableCandidate::key)
                .filter(key -> fallbackItems.contains(key.getItem()))
                .forEach(candidates::add);
        return List.copyOf(candidates);
    }

    public static boolean reserveOneUnit(Map<AEItemKey, Long> remaining, List<ItemStack> alternativeStacks,
            @Nullable Ingredient wideIngredient) {
        return reserveAmount(remaining, alternativeStacks, wideIngredient, 1) == 1;
    }

    public static int reserveAmount(Map<AEItemKey, Long> remaining, List<ItemStack> alternativeStacks,
            @Nullable Ingredient wideIngredient, int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
        }
        List<ItemStack> orderedAlts = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks);
        boolean exactOnly = WcwtStackMatching.requiresExactItemKeyMatch(orderedAlts);
        LinkedHashSet<AEItemKey> candidates = new LinkedHashSet<>();
        orderedAlts.stream()
                .map(AEItemKey::of)
                .filter(Objects::nonNull)
                .forEach(candidates::add);

        if (!exactOnly && wideIngredient != null) {
            for (var e : remaining.entrySet()) {
                AEItemKey key = e.getKey();
                if (e.getValue() > 0 && key.matches(wideIngredient)) {
                    candidates.add(key);
                }
            }
        }
        if (!exactOnly) {
            Set<Item> fallbackItems = new HashSet<>();
            for (ItemStack alt : orderedAlts) {
                if (!alt.isEmpty()) {
                    fallbackItems.add(alt.getItem());
                }
            }
            for (var e : remaining.entrySet()) {
                AEItemKey key = e.getKey();
                if (e.getValue() > 0 && fallbackItems.contains(key.getItem())) {
                    candidates.add(key);
                }
            }
        }

        int reserved = 0;
        for (AEItemKey candidate : candidates) {
            long available = remaining.getOrDefault(candidate, 0L);
            int needed = requestedAmount - reserved;
            if (available <= 0 || needed <= 0) {
                continue;
            }
            int amount = (int) Math.min(available, needed);
            remaining.put(candidate, available - amount);
            reserved += amount;
            if (reserved >= requestedAmount) {
                break;
            }
        }
        return reserved;
    }

    private record AvailableCandidate(AEItemKey key, long amount) {
    }
}
