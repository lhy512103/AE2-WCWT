package com.lhy.wcwt.compat;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Shared bookmark/favorite priority lookup for recipe-viewer transfers.
 *
 * <p>This class intentionally has no hard JEI or EMI API references. The EMI
 * handler is also loaded when JEI is absent, so touching JEI API types here
 * would make EMI-only recipe transfer fail during class loading.
 */
public final class WcwtRecipeViewerBookmarkKeys {
    private WcwtRecipeViewerBookmarkKeys() {
    }

    public static List<AEKey> getBookmarkKeys() {
        List<AEKey> merged = new ArrayList<>();
        addUniqueKeys(merged, loadJeiBookmarkKeys());
        addUniqueKeys(merged, loadEmiFavoriteKeys());
        return List.copyOf(merged);
    }

    public static Map<AEKey, Integer> getBookmarkPriorities() {
        List<AEKey> keys = getBookmarkKeys();
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
        if (ingredient == null || ingredient.isEmpty() || priorities.isEmpty()) {
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

    @SuppressWarnings("unchecked")
    private static List<AEKey> loadJeiBookmarkKeys() {
        if (!ModList.get().isLoaded("jei")) {
            return List.of();
        }
        try {
            Class<?> helperClass = Class.forName("com.lhy.wcwt.compat.jei.WcwtJeiBookmarkKeys");
            Method method = helperClass.getMethod("getBookmarkKeys");
            Object result = method.invoke(null);
            if (!(result instanceof List<?> rawKeys)) {
                return List.of();
            }

            List<AEKey> keys = new ArrayList<>(rawKeys.size());
            for (Object key : rawKeys) {
                if (key instanceof AEKey aeKey) {
                    keys.add(aeKey);
                }
            }
            return List.copyOf(keys);
        } catch (Throwable ignored) {
            return List.of();
        }
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
        if (source == null) {
            return;
        }
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

    private static void addUniqueKeys(List<AEKey> target, List<AEKey> source) {
        LinkedHashSet<AEKey> seen = new LinkedHashSet<>(target);
        for (AEKey key : source) {
            if (key != null && seen.add(key)) {
                target.add(key);
            }
        }
    }

    private static @Nullable GenericStack convertEmiStackToGenericStack(@Nullable Object emiStack) {
        if (emiStack == null) {
            return null;
        }
        try {
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            if (!emiStackClass.isInstance(emiStack)) {
                return null;
            }

            Method getKey = emiStackClass.getMethod("getKey");
            Object key = getKey.invoke(emiStack);
            if (key instanceof Fluid fluid && fluid != Fluids.EMPTY) {
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
