package com.lhy.wcwt.compat;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        if (mode != EncodingMode.PROCESSING) {
            ExtendedAePlusUploadCompat.presetCraftingProviderSearchKey();
            return;
        }

        String name = ExtendedAePlusUploadCompat.mapRecipeTypeToSearchKey(recipe);
        if ((name == null || name.isBlank()) && recipeBase != null) {
            name = ExtendedAePlusUploadCompat.deriveSearchKeyFromUnknownRecipe(recipeBase);
        }
        if (name != null && !name.isBlank()) {
            ExtendedAePlusUploadCompat.setLastProviderSearchKey(name);
        }
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
        try {
            Class<?> favoritesClass = Class.forName("dev.emi.emi.runtime.EmiFavorites");
            Field favoritesField = favoritesClass.getDeclaredField("favorites");
            Object rawFavorites = favoritesField.get(null);
            if (!(rawFavorites instanceof List<?> favorites)) {
                return List.of();
            }

            List<AEKey> keys = new ArrayList<>(favorites.size());
            for (Object favorite : favorites) {
                if (favorite == null) {
                    continue;
                }

                Method getStack = favorite.getClass().getMethod("getStack");
                Object ingredient = getStack.invoke(favorite);
                if (ingredient == null) {
                    continue;
                }

                Method getEmiStacks = ingredient.getClass().getMethod("getEmiStacks");
                Object rawEmiStacks = getEmiStacks.invoke(ingredient);
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
        } catch (Throwable ignored) {
            return List.of();
        }
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
        try {
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            if (!emiStackClass.isInstance(emiStack)) {
                return null;
            }

            Method getKey = emiStackClass.getMethod("getKey");
            Object key = getKey.invoke(emiStack);
            if (key instanceof net.minecraft.world.level.material.Fluid fluid
                    && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                Method getAmount = emiStackClass.getMethod("getAmount");
                long amount = ((Number) getAmount.invoke(emiStack)).longValue();
                return GenericStack.fromFluidStack(new FluidStack(fluid, (int) Math.max(1L, amount)));
            }

            Method getItemStack = emiStackClass.getMethod("getItemStack");
            Object rawItemStack = getItemStack.invoke(emiStack);
            if (rawItemStack instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                return GenericStack.fromItemStack(itemStack.copyWithCount(1));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
