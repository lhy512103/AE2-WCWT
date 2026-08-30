package com.lhy.wcwt.compat;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class WcwtRecipeTransferCommon {
    private WcwtRecipeTransferCommon() {
    }

    public static void updateEaepProviderSearchKey(Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode) {
        ExtendedAePlusUploadCompat.captureRecipeSearchKey(recipeBase, recipe, mode);
    }

    public static Map<AEKey, Integer> getEmiFavoritePriorities() {
        List<AEKey> keys = loadEmiFavoriteKeys();
        if (keys.isEmpty()) {
            return Map.of();
        }

        Map<AEKey, Integer> priorities = new LinkedHashMap<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            AEKey key = keys.get(i);
            if (key != null) {
                priorities.putIfAbsent(key, i);
            }
        }
        return priorities;
    }

    public static ItemStack chooseBookmarkedItem(Ingredient ingredient,
                                                 List<ItemStack> visibleAlternatives,
                                                 Map<AEKey, Integer> priorities) {
        if (ingredient.isEmpty() || priorities.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> allCandidates = new ArrayList<>();
        LinkedHashSet<AEKey> seen = new LinkedHashSet<>();
        addCandidates(allCandidates, seen, visibleAlternatives);
        addCandidates(allCandidates, seen, Arrays.asList(ingredient.getItems()));

        ItemStack best = ItemStack.EMPTY;
        int bestPriority = Integer.MAX_VALUE;
        for (ItemStack candidate : allCandidates) {
            if (candidate == null || candidate.isEmpty() || !ingredient.test(candidate)) {
                continue;
            }
            GenericStack stack = GenericStack.fromItemStack(candidate);
            if (stack == null || stack.what() == null) {
                continue;
            }
            Integer priority = priorities.get(stack.what());
            if (priority != null && priority < bestPriority) {
                best = candidate.copy();
                bestPriority = priority;
            }
        }
        return best;
    }

    public static ItemStack chooseFavoritedItem(Ingredient ingredient,
                                                List<ItemStack> visibleAlternatives,
                                                Map<AEKey, Integer> priorities) {
        return chooseBookmarkedItem(ingredient, visibleAlternatives, priorities);
    }

    @Nullable
    public static GenericStack chooseFavoritedStack(List<GenericStack> candidates,
                                                    Map<AEKey, Integer> priorities) {
        if (candidates == null || candidates.isEmpty() || priorities == null || priorities.isEmpty()) {
            return null;
        }

        GenericStack best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (GenericStack candidate : candidates) {
            if (candidate == null || candidate.what() == null) {
                continue;
            }
            Integer priority = priorities.get(candidate.what());
            if (priority != null && priority < bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    @Nullable
    public static GenericStack chooseFavoritedItemStack(Iterable<ItemStack> candidates,
                                                        Map<AEKey, Integer> priorities,
                                                        boolean preserveDisplayedItemCounts) {
        if (priorities == null || priorities.isEmpty()) {
            return null;
        }

        ItemStack best = ItemStack.EMPTY;
        int bestPriority = Integer.MAX_VALUE;
        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(candidate);
            Integer priority = key == null ? null : priorities.get(key);
            if (priority != null && priority < bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        if (best.isEmpty()) {
            return null;
        }
        return GenericStack.fromItemStack(preserveDisplayedItemCounts ? best.copy() : best.copyWithCount(1));
    }

    @Nullable
    public static GenericStack toBestGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                  Ingredient ingredient,
                                                  List<ItemStack> visibleAlternatives) {
        if (ingredient.isEmpty()) {
            return null;
        }

        if (WcwtClientConfig.preferJeiBookmarksForPatternEncoding() && priorityContext.hasBookmarkPriorities()) {
            ItemStack bookmarked = chooseBookmarkedItem(
                    ingredient, visibleAlternatives, priorityContext.bookmarkPriorities());
            if (!bookmarked.isEmpty()) {
                return GenericStack.fromItemStack(bookmarked.copyWithCount(1));
            }
        }

        ItemStack best = WcwtIngredientPriorities.chooseBestItemForEncoding(
                priorityContext, ingredient, visibleAlternatives);
        return best.isEmpty() ? null : GenericStack.fromItemStack(best.copyWithCount(1));
    }

    @Nullable
    public static GenericStack toGenericStack(SizedFluidIngredient ingredient) {
        for (FluidStack fluidStack : ingredient.getFluids()) {
            if (!fluidStack.isEmpty()) {
                FluidStack copy = fluidStack.copy();
                copy.setAmount(Math.max(1, ingredient.amount()));
                return GenericStack.fromFluidStack(copy);
            }
        }
        return null;
    }

    private static List<AEKey> loadEmiFavoriteKeys() {
        var rawFavorites = WcwtReflect.findClass("emi", "dev.emi.emi.runtime.EmiFavorites")
                .flatMap(owner -> WcwtReflect.findDeclaredField(owner, "favorites")
                        .flatMap(field -> WcwtReflect.readField(null, field)))
                .orElse(null);
        if (!(rawFavorites instanceof List<?> favorites)) {
            return List.of();
        }

        List<AEKey> keys = new ArrayList<>(favorites.size());
        for (Object favorite : favorites) {
            if (favorite == null) {
                continue;
            }
            var ingredient = WcwtReflect.findMethod(favorite.getClass(), "getStack")
                    .flatMap(method -> WcwtReflect.invoke(favorite, method))
                    .orElse(null);
            if (ingredient == null) {
                continue;
            }
            var rawEmiStacks = WcwtReflect.findMethod(ingredient.getClass(), "getEmiStacks")
                    .flatMap(method -> WcwtReflect.invoke(ingredient, method))
                    .orElse(null);
            if (!(rawEmiStacks instanceof List<?> emiStacks)) {
                continue;
            }
            for (Object emiStack : emiStacks) {
                GenericStack stack = convertEmiStackToGenericStack(emiStack);
                if (stack != null && stack.what() != null) {
                    keys.add(stack.what());
                }
            }
        }
        return List.copyOf(keys);
    }

    private static void addCandidates(List<ItemStack> target, LinkedHashSet<AEKey> seen, List<ItemStack> source) {
        for (ItemStack candidate : source) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            GenericStack stack = GenericStack.fromItemStack(candidate);
            if (stack == null || stack.what() == null || !seen.add(stack.what())) {
                continue;
            }
            target.add(candidate.copy());
        }
    }

    @Nullable
    private static GenericStack convertEmiStackToGenericStack(Object emiStack) {
        if (!WcwtReflect.isInstance("emi", "dev.emi.emi.api.stack.EmiStack", emiStack)) {
            return null;
        }
        var key = WcwtReflect.findMethod(emiStack.getClass(), "getKey")
                .flatMap(method -> WcwtReflect.invoke(emiStack, method))
                .orElse(null);
        if (key instanceof net.minecraft.world.level.material.Fluid fluid
                && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
            long amount = WcwtReflect.findMethod(emiStack.getClass(), "getAmount")
                    .flatMap(method -> WcwtReflect.invoke(emiStack, method))
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .map(Number::longValue)
                    .orElse(1L);
            return GenericStack.fromFluidStack(new FluidStack(fluid, (int) Math.max(1L, amount)));
        }
        var rawItemStack = WcwtReflect.findMethod(emiStack.getClass(), "getItemStack")
                .flatMap(method -> WcwtReflect.invoke(emiStack, method))
                .orElse(null);
        if (rawItemStack instanceof ItemStack itemStack && !itemStack.isEmpty()) {
            return GenericStack.fromItemStack(itemStack.copyWithCount(1));
        }
        return null;
    }
}
