package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WcwtJeiBookmarkKeys {
    private static @Nullable IJeiRuntime jeiRuntime;

    private WcwtJeiBookmarkKeys() {
    }

    static void setRuntime(@Nullable IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public static List<AEKey> getBookmarkKeys() {
        List<AEKey> merged = new ArrayList<>();
        addUniqueKeys(merged, loadJeiBookmarkKeys());
        addUniqueKeys(merged, loadEmiFavoriteKeys());
        return List.copyOf(merged);
    }

    private static List<AEKey> loadJeiBookmarkKeys() {
        IJeiRuntime runtime = jeiRuntime;
        if (runtime == null) {
            return List.of();
        }

        IBookmarkOverlay overlay = runtime.getBookmarkOverlay();
        if (overlay == null) {
            return List.of();
        }

        try {
            Field bookmarkListField = overlay.getClass().getDeclaredField("bookmarkList");
            bookmarkListField.setAccessible(true);
            Object bookmarkList = bookmarkListField.get(overlay);

            Method getElementsMethod = bookmarkList.getClass().getMethod("getElements");
            List<?> elements = (List<?>) getElementsMethod.invoke(bookmarkList);

            List<AEKey> bookmarks = new ArrayList<>(elements.size());
            for (Object element : elements) {
                Method getTypedIngredientMethod = element.getClass().getMethod("getTypedIngredient");
                Object typedIngredient = getTypedIngredientMethod.invoke(element);
                if (!(typedIngredient instanceof ITypedIngredient<?> ingredient)) {
                    continue;
                }

                GenericStack stack = WcwtRecipeTransferHandler.toGenericStackForBookmark(ingredient);
                if (stack != null && stack.what() != null) {
                    bookmarks.add(stack.what());
                }
            }
            return List.copyOf(bookmarks);
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
                    if (emiStack == null) {
                        continue;
                    }

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

    public static @Nullable GenericStack chooseBookmarkedStack(IRecipeSlotView slotView) {
        return chooseBookmarkedStack(slotView, getBookmarkPriorities());
    }

    public static @Nullable GenericStack chooseBookmarkedStack(IRecipeSlotView slotView, Map<AEKey, Integer> priorities) {
        if (priorities.isEmpty()) {
            return null;
        }

        GenericStack best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (var ingredient : slotView.getAllIngredients().toList()) {
            GenericStack stack = WcwtRecipeTransferHandler.toGenericStackForBookmark(ingredient);
            if (stack == null || stack.what() == null) {
                continue;
            }
            Integer priority = priorities.get(stack.what());
            if (priority != null && priority < bestPriority) {
                best = stack;
                bestPriority = priority;
            }
        }
        return best;
    }

    public static ItemStack chooseBookmarkedItem(Ingredient ingredient, List<ItemStack> visibleAlternatives) {
        return chooseBookmarkedItem(ingredient, visibleAlternatives, getBookmarkPriorities());
    }

    public static ItemStack chooseBookmarkedItem(Ingredient ingredient,
                                                 List<ItemStack> visibleAlternatives,
                                                 Map<AEKey, Integer> priorities) {
        if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (priorities.isEmpty()) {
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

    private static void addUniqueKeys(List<AEKey> target, List<AEKey> source) {
        LinkedHashSet<AEKey> seen = new LinkedHashSet<>(target);
        for (AEKey key : source) {
            if (key != null && seen.add(key)) {
                target.add(key);
            }
        }
    }

    private static @Nullable GenericStack convertEmiStackToGenericStack(Object emiStack) {
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
                return GenericStack.fromFluidStack(new net.neoforged.neoforge.fluids.FluidStack(fluid, (int) Math.max(1L, amount)));
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
