package com.lhy.wcwt.pull;

import java.util.ArrayList;
import java.util.HashMap;
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
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.util.prioritylist.IPartitionList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/** 自 ae2utility MeIngredientExtraction 迁入。 */
public final class WcwtMeIngredientExtraction {

    private WcwtMeIngredientExtraction() {
    }

    /**
     * The "wide" ingredient is built with {@link Ingredient#of(ItemStack...)}, which matches by item only.
     * The candidate searches below rely on that: a key whose item is not among the alternatives can never
     * match it, so they only look at keys of those items instead of testing every key in the network.
     */
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

    /**
     * What the network held when a pull started, indexed by item. One snapshot serves every ingredient of
     * the request; aggregating the whole network again for each ingredient was the dominant cost on big
     * networks. Amounts are only used to order candidates, so a snapshot that is a few extractions stale is
     * harmless: a key that ran out simply extracts nothing.
     */
    public static final class AvailableItems {
        private final Map<Item, List<AEItemKey>> keysByItem = new HashMap<>();
        private final Object2LongOpenHashMap<AEItemKey> amounts = new Object2LongOpenHashMap<>();

        private AvailableItems() {
            amounts.defaultReturnValue(0L);
        }

        public static AvailableItems of(KeyCounter available, @Nullable IPartitionList filter) {
            var result = new AvailableItems();
            for (var entry : available) {
                if (entry.getLongValue() <= 0 || !(entry.getKey() instanceof AEItemKey itemKey)) {
                    continue;
                }
                if (filter != null && !filter.isListed(itemKey)) {
                    continue;
                }
                result.keysByItem.computeIfAbsent(itemKey.getItem(), ignored -> new ArrayList<>()).add(itemKey);
                result.amounts.put(itemKey, entry.getLongValue());
            }
            return result;
        }

        public List<AEItemKey> keysFor(Item item) {
            return keysByItem.getOrDefault(item, List.of());
        }

        public long amount(AEItemKey key) {
            return amounts.getLong(key);
        }
    }

    public static List<ItemStack> extractAlternatives(MEStorage storage, IEnergySource energy, IActionSource actionSource,
            @Nullable IPartitionList filter, List<ItemStack> alternativeStacks, int amount) {
        return extractAlternatives(storage, AvailableItems.of(storage.getAvailableStacks(), filter), energy, actionSource,
                filter, alternativeStacks, amount);
    }

    public static List<ItemStack> extractAlternatives(MEStorage storage, AvailableItems available, IEnergySource energy,
            IActionSource actionSource, @Nullable IPartitionList filter, List<ItemStack> alternativeStacks, int amount) {
        List<ItemStack> orderedAlts = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks);
        Ingredient wide = ingredientFromItemStacks(orderedAlts);
        boolean exactOnly = WcwtStackMatching.requiresExactItemKeyMatch(orderedAlts);
        List<AEItemKey> candidates = buildCandidateKeys(available, filter, orderedAlts, wide, exactOnly);

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

    /**
     * Exact alternatives first, then every network key of the alternatives' items by descending amount, keys
     * the wide ingredient accepts ahead of the rest.
     */
    private static List<AEItemKey> buildCandidateKeys(AvailableItems available, @Nullable IPartitionList filter,
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

        Set<Item> alternativeItems = new HashSet<>();
        List<AvailableCandidate> matched = new ArrayList<>();
        for (ItemStack alt : orderedAlts) {
            if (alt.isEmpty() || !alternativeItems.add(alt.getItem())) {
                continue;
            }
            for (AEItemKey key : available.keysFor(alt.getItem())) {
                matched.add(new AvailableCandidate(key, available.amount(key)));
            }
        }
        matched.sort((left, right) -> Long.compare(right.amount(), left.amount()));
        if (wide != null) {
            for (AvailableCandidate candidate : matched) {
                if (candidate.key().matches(wide)) {
                    candidates.add(candidate.key());
                }
            }
        }
        for (AvailableCandidate candidate : matched) {
            candidates.add(candidate.key());
        }
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
        return reserveFromCandidates(remaining, reservationCandidates(remaining, alternativeStacks, wideIngredient),
                requestedAmount);
    }

    /**
     * The keys of {@code remaining} an ingredient may draw from, in the order a reservation tries them: exact
     * alternatives, then keys the wide ingredient accepts, then other keys of the same items. Computed once per
     * ingredient and reused across reservation rounds; the map is scanned a single time.
     */
    public static List<AEItemKey> reservationCandidates(Map<AEItemKey, Long> remaining, List<ItemStack> alternativeStacks,
            @Nullable Ingredient wideIngredient) {
        List<ItemStack> orderedAlts = WcwtIngredientPriorities.deduplicateItemAlternatives(alternativeStacks);
        boolean exactOnly = WcwtStackMatching.requiresExactItemKeyMatch(orderedAlts);
        LinkedHashSet<AEItemKey> candidates = new LinkedHashSet<>();
        orderedAlts.stream()
                .map(AEItemKey::of)
                .filter(Objects::nonNull)
                .forEach(candidates::add);
        if (exactOnly) {
            return List.copyOf(candidates);
        }

        Set<Item> alternativeItems = new HashSet<>();
        for (ItemStack alt : orderedAlts) {
            if (!alt.isEmpty()) {
                alternativeItems.add(alt.getItem());
            }
        }
        List<AEItemKey> wideMatches = new ArrayList<>();
        List<AEItemKey> itemMatches = new ArrayList<>();
        for (var e : remaining.entrySet()) {
            AEItemKey key = e.getKey();
            if (e.getValue() <= 0 || !alternativeItems.contains(key.getItem())) {
                continue;
            }
            if (wideIngredient != null && key.matches(wideIngredient)) {
                wideMatches.add(key);
            } else {
                itemMatches.add(key);
            }
        }
        candidates.addAll(wideMatches);
        candidates.addAll(itemMatches);
        return List.copyOf(candidates);
    }

    /** Reserves up to {@code requestedAmount} from {@code remaining}, trying the candidates in order. */
    public static int reserveFromCandidates(Map<AEItemKey, Long> remaining, List<AEItemKey> candidates,
            int requestedAmount) {
        if (requestedAmount <= 0) {
            return 0;
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
