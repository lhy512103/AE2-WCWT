package com.lhy.wcwt.pull;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WcwtIngredientPriorities {
    private static final Comparator<GridInventoryEntry> ENTRY_COMPARATOR = Comparator
            .comparing(GridInventoryEntry::isCraftable)
            .thenComparing(WcwtIngredientPriorities::isUndamaged)
            .thenComparing(GridInventoryEntry::getStoredAmount);

    private WcwtIngredientPriorities() {
    }

    public record PriorityContext(Map<AEKey, Integer> ingredientPriorities,
                                  Map<AEKey, Integer> bookmarkPriorities,
                                  Map<AEKey, Integer> favoritePriorities) {
        public static final PriorityContext EMPTY = new PriorityContext(Map.of(), Map.of(), Map.of());

        public boolean hasBookmarkPriorities() {
            return bookmarkPriorities != null && !bookmarkPriorities.isEmpty();
        }

        public boolean hasFavoritePriorities() {
            return favoritePriorities != null && !favoritePriorities.isEmpty();
        }
    }

    public static Map<AEKey, Integer> getIngredientPriorities(@Nullable MEStorageMenu menu) {
        if (menu == null || menu.getClientRepo() == null) {
            return Map.of();
        }

        var orderedEntries = menu.getClientRepo().getAllEntries().stream()
                .sorted(ENTRY_COMPARATOR)
                .map(GridInventoryEntry::getWhat)
                .toList();

        var result = new HashMap<AEKey, Integer>(orderedEntries.size());
        for (int i = 0; i < orderedEntries.size(); i++) {
            var key = orderedEntries.get(i);
            if (key != null) {
                result.put(key, i);
            }
        }

        for (var item : menu.getPlayerInventory().items) {
            var key = AEItemKey.of(item);
            if (key != null) {
                result.putIfAbsent(key, -1);
            }
        }

        return result;
    }

    public static PriorityContext createContext(@Nullable MEStorageMenu menu, Map<AEKey, Integer> bookmarkPriorities) {
        return createContext(menu, bookmarkPriorities, Map.of());
    }

    public static PriorityContext createContext(@Nullable MEStorageMenu menu, Map<AEKey, Integer> bookmarkPriorities,
                                                Map<AEKey, Integer> favoritePriorities) {
        return new PriorityContext(getIngredientPriorities(menu),
                bookmarkPriorities == null || bookmarkPriorities.isEmpty() ? Map.of() : Map.copyOf(bookmarkPriorities),
                favoritePriorities == null || favoritePriorities.isEmpty() ? Map.of() : Map.copyOf(favoritePriorities));
    }

    public static List<ItemStack> deduplicateItemAlternatives(List<ItemStack> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) {
            return List.of();
        }

        List<ItemStack> deduplicated = new ArrayList<>(alternatives.size());
        for (var alternative : alternatives) {
            if (alternative == null || alternative.isEmpty() || containsEquivalentStack(deduplicated, alternative)) {
                continue;
            }
            deduplicated.add(alternative.copy());
        }
        return deduplicated;
    }

    public static List<ItemStack> sortItemAlternatives(@Nullable MEStorageMenu menu, List<ItemStack> alternatives) {
        return sortItemAlternatives(createContext(menu, Map.of()), alternatives);
    }

    public static List<ItemStack> sortItemAlternatives(PriorityContext context, List<ItemStack> alternatives) {
        List<ItemStack> sorted = new ArrayList<>(deduplicateItemAlternatives(alternatives));
        if (sorted.size() <= 1) {
            return sorted;
        }

        sorted.sort(Comparator
                .comparingInt((ItemStack stack) -> getPriority(context.ingredientPriorities(), stack)).reversed()
                .thenComparing(Comparator.comparingInt(WcwtPullIngredientOrdering::componentSpecificityRank).reversed())
                .thenComparing(ItemStack::getDescriptionId)
                .thenComparing(stack -> String.valueOf(stack.getTag())));
        return sorted;
    }

    @Nullable
    public static GenericStack chooseBestGenericStack(@Nullable MEStorageMenu menu, List<GenericStack> candidates) {
        return chooseBestGenericStack(createContext(menu, Map.of()), candidates);
    }

    @Nullable
    public static GenericStack chooseBestGenericStack(PriorityContext context, List<GenericStack> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        GenericStack bookmarked = chooseBookmarkedGenericStack(context, candidates);
        if (bookmarked != null) {
            return bookmarked;
        }
        GenericStack favorited = chooseFavoritedGenericStack(context, candidates);
        if (favorited != null) {
            return favorited;
        }

        return candidates.stream()
                .max(Comparator
                        .comparingInt((GenericStack stack) -> getPriority(context.ingredientPriorities(), stack.what()))
                        .thenComparingInt(WcwtPullIngredientOrdering::genericStackItemSpecificityRank))
                .orElse(null);
    }

    public static ItemStack chooseBestItem(@Nullable MEStorageMenu menu,
                                           Ingredient ingredient,
                                           List<ItemStack> visibleAlternatives) {
        return chooseBestItem(createContext(menu, Map.of()), ingredient, visibleAlternatives);
    }

    public static ItemStack chooseBestItem(PriorityContext context,
                                           Ingredient ingredient,
                                           List<ItemStack> visibleAlternatives) {
        return chooseBestItemForPull(context, ingredient, visibleAlternatives);
    }

    public static ItemStack chooseBestItemForEncoding(PriorityContext context,
                                                      Ingredient ingredient) {
        return chooseBestItemForEncoding(context, ingredient, List.of());
    }

    public static ItemStack chooseBestItemForEncoding(PriorityContext context,
                                                      Ingredient ingredient,
                                                      List<ItemStack> visibleAlternatives) {
        if (ingredient == null || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> exactAlternatives = collectExactAlternatives(ingredient, visibleAlternatives);
        ItemStack exactVisible = chooseFirstSpecificVisibleAlternative(ingredient, visibleAlternatives);
        if (!exactVisible.isEmpty()) {
            return exactVisible;
        }

        ItemStack bookmarked = exactAlternatives.isEmpty()
                ? chooseBookmarkedItem(context, ingredient, visibleAlternatives)
                : choosePrioritizedExactItem(exactAlternatives, context.bookmarkPriorities());
        if (!bookmarked.isEmpty()) {
            return bookmarked;
        }
        ItemStack favorited = exactAlternatives.isEmpty()
                ? chooseFavoritedItem(context, ingredient, visibleAlternatives)
                : choosePrioritizedExactItem(exactAlternatives, context.favoritePriorities());
        if (!favorited.isEmpty()) {
            return favorited;
        }
        if (!exactAlternatives.isEmpty()) {
            ItemStack exactNetworkIngredient = findBestExactNetworkIngredient(context, exactAlternatives);
            if (!exactNetworkIngredient.isEmpty()) {
                return exactNetworkIngredient;
            }
            return exactAlternatives.get(0).copy();
        }

        ItemStack bestNetworkIngredient = findBestNetworkIngredient(context, ingredient);
        if (!bestNetworkIngredient.isEmpty()) {
            return bestNetworkIngredient;
        }

        ItemStack[] items = ingredient.getItems();
        if (visibleAlternatives != null && !visibleAlternatives.isEmpty()) {
            ItemStack bestVisible = chooseMostSpecificItem(items,
                    stack -> containsEquivalentStack(visibleAlternatives, stack));
            if (!bestVisible.isEmpty()) {
                return bestVisible;
            }
        }

        ItemStack bestAny = chooseMostSpecificItem(items, stack -> true);
        if (!bestAny.isEmpty()) {
            return bestAny;
        }
        return items.length > 0 ? items[0].copy() : ItemStack.EMPTY;
    }

    private static GenericStack chooseBookmarkedGenericStack(PriorityContext context, List<GenericStack> candidates) {
        if (!context.hasBookmarkPriorities()) {
            return null;
        }
        GenericStack best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (GenericStack candidate : candidates) {
            if (candidate == null || candidate.what() == null) {
                continue;
            }
            Integer priority = context.bookmarkPriorities().get(candidate.what());
            if (priority != null && priority < bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    private static GenericStack chooseFavoritedGenericStack(PriorityContext context, List<GenericStack> candidates) {
        if (!context.hasFavoritePriorities()) {
            return null;
        }
        GenericStack best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (GenericStack candidate : candidates) {
            if (candidate == null || candidate.what() == null) {
                continue;
            }
            Integer priority = context.favoritePriorities().get(candidate.what());
            if (priority != null && priority < bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    private static ItemStack chooseBookmarkedItem(PriorityContext context,
                                                  Ingredient ingredient,
                                                  List<ItemStack> visibleAlternatives) {
        if (!context.hasBookmarkPriorities()) {
            return ItemStack.EMPTY;
        }
        return choosePrioritizedItem(ingredient, visibleAlternatives, context.bookmarkPriorities());
    }

    private static ItemStack chooseFavoritedItem(PriorityContext context,
                                                 Ingredient ingredient,
                                                 List<ItemStack> visibleAlternatives) {
        if (!context.hasFavoritePriorities()) {
            return ItemStack.EMPTY;
        }
        return choosePrioritizedItem(ingredient, visibleAlternatives, context.favoritePriorities());
    }

    private static ItemStack choosePrioritizedItem(Ingredient ingredient,
                                                  List<ItemStack> visibleAlternatives,
                                                  Map<AEKey, Integer> priorities) {
        ItemStack best = ItemStack.EMPTY;
        int bestPriority = Integer.MAX_VALUE;
        List<ItemStack> candidates = new ArrayList<>(deduplicateItemAlternatives(visibleAlternatives));
        for (ItemStack item : ingredient.getItems()) {
            if (item != null && !item.isEmpty() && !containsEquivalentStack(candidates, item)) {
                candidates.add(item.copy());
            }
        }

        for (ItemStack stack : candidates) {
            if (stack == null || stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }
            var key = AEItemKey.of(stack);
            Integer priority = key != null ? priorities.get(key) : null;
            if (priority != null && priority < bestPriority) {
                best = stack.copy();
                bestPriority = priority;
            }
        }
        return best;
    }

    private static ItemStack chooseMostSpecificItem(ItemStack[] items,
                                                    java.util.function.Predicate<ItemStack> visiblePredicate) {
        ItemStack best = ItemStack.EMPTY;
        int bestSpecificity = Integer.MIN_VALUE;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty() || !visiblePredicate.test(stack)) {
                continue;
            }
            int specificity = WcwtPullIngredientOrdering.componentSpecificityRank(stack);
            if (best.isEmpty() || specificity > bestSpecificity) {
                best = stack.copy();
                bestSpecificity = specificity;
            }
        }
        return best;
    }

    public static ItemStack chooseBestItemForPull(PriorityContext context,
                                                  Ingredient ingredient,
                                                  List<ItemStack> visibleAlternatives) {
        ItemStack exactVisible = chooseFirstSpecificVisibleAlternative(ingredient, visibleAlternatives);
        if (!exactVisible.isEmpty()) {
            return exactVisible;
        }
        List<ItemStack> exactAlternatives = collectExactAlternatives(ingredient, visibleAlternatives);
        if (!exactAlternatives.isEmpty()) {
            ItemStack exactNetworkIngredient = findBestExactNetworkIngredient(context, exactAlternatives);
            if (!exactNetworkIngredient.isEmpty()) {
                return exactNetworkIngredient;
            }
            return exactAlternatives.get(0).copy();
        }

        ItemStack bestNetworkIngredient = findBestNetworkIngredient(context, ingredient);
        if (!bestNetworkIngredient.isEmpty()) {
            return bestNetworkIngredient;
        }

        for (var visibleAlternative : sortItemAlternatives(context, visibleAlternatives)) {
            if (ingredient.test(visibleAlternative)) {
                return visibleAlternative.copy();
            }
        }

        ItemStack[] items = ingredient.getItems();
        return items.length > 0 ? items[0].copy() : ItemStack.EMPTY;
    }

    private static ItemStack findBestNetworkIngredient(@Nullable MEStorageMenu menu, Ingredient ingredient) {
        return findBestNetworkIngredient(createContext(menu, Map.of()), ingredient);
    }

    private static ItemStack findBestNetworkIngredient(PriorityContext context, Ingredient ingredient) {
        return context.ingredientPriorities().entrySet().stream()
                .filter(entry -> entry.getKey() instanceof AEItemKey itemKey && itemKey.matches(ingredient))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .filter(AEItemKey.class::isInstance)
                .map(AEItemKey.class::cast)
                .map(AEItemKey::toStack)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack findBestExactNetworkIngredient(PriorityContext context, List<ItemStack> exactAlternatives) {
        AEItemKey best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (ItemStack alternative : exactAlternatives) {
            AEItemKey key = AEItemKey.of(alternative);
            if (key == null) {
                continue;
            }
            int priority = context.ingredientPriorities().getOrDefault(key, Integer.MIN_VALUE);
            if (priority > bestPriority) {
                best = key;
                bestPriority = priority;
            }
        }
        return best != null && bestPriority != Integer.MIN_VALUE ? best.toStack() : ItemStack.EMPTY;
    }

    private static ItemStack chooseFirstSpecificVisibleAlternative(Ingredient ingredient,
                                                                  List<ItemStack> visibleAlternatives) {
        if (!WcwtStackMatching.requiresExactItemKeyMatch(visibleAlternatives)) {
            return ItemStack.EMPTY;
        }
        for (var visibleAlternative : deduplicateItemAlternatives(visibleAlternatives)) {
            if (WcwtStackMatching.hasSpecificData(visibleAlternative)
                    && ingredient.test(visibleAlternative)) {
                return visibleAlternative.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> collectExactAlternatives(Ingredient ingredient,
                                                           List<ItemStack> visibleAlternatives) {
        List<ItemStack> exactAlternatives = new ArrayList<>();
        for (ItemStack visibleAlternative : deduplicateItemAlternatives(visibleAlternatives)) {
            if (WcwtStackMatching.hasSpecificData(visibleAlternative)
                    && ingredient.test(visibleAlternative)
                    && !containsEquivalentStack(exactAlternatives, visibleAlternative)) {
                exactAlternatives.add(visibleAlternative.copy());
            }
        }
        for (ItemStack stack : ingredient.getItems()) {
            if (WcwtStackMatching.hasSpecificData(stack)
                    && !containsEquivalentStack(exactAlternatives, stack)) {
                exactAlternatives.add(stack.copy());
            }
        }
        return exactAlternatives;
    }

    private static ItemStack choosePrioritizedExactItem(List<ItemStack> exactAlternatives,
                                                       Map<AEKey, Integer> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack best = ItemStack.EMPTY;
        int bestPriority = Integer.MAX_VALUE;
        for (ItemStack stack : exactAlternatives) {
            AEItemKey key = AEItemKey.of(stack);
            Integer priority = key != null ? priorities.get(key) : null;
            if (priority != null && priority < bestPriority) {
                best = stack.copy();
                bestPriority = priority;
            }
        }
        return best;
    }

    private static boolean isUndamaged(GridInventoryEntry entry) {
        return !(entry.getWhat() instanceof AEItemKey itemKey) || !itemKey.isDamaged();
    }

    private static int getPriority(Map<AEKey, Integer> priorities, ItemStack stack) {
        return getPriority(priorities, AEItemKey.of(stack));
    }

    private static int getPriority(Map<AEKey, Integer> priorities, @Nullable AEKey key) {
        return key == null ? Integer.MIN_VALUE : priorities.getOrDefault(key, Integer.MIN_VALUE);
    }

    private static boolean containsEquivalentStack(List<ItemStack> stacks, ItemStack candidate) {
        for (var existing : stacks) {
            if (ItemStack.isSameItem(existing, candidate)
                    && java.util.Objects.equals(existing.getTag(), candidate.getTag())) {
                return true;
            }
        }
        return false;
    }
}
