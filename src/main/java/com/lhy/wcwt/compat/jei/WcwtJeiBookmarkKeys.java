package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IBookmarkOverlay;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

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

        var bookmarkList = WcwtReflect.findDeclaredField(overlay.getClass(), "bookmarkList")
                .flatMap(field -> WcwtReflect.readField(overlay, field))
                .orElse(null);
        if (bookmarkList == null) {
            return List.of();
        }
        var elements = WcwtReflect.findMethod(bookmarkList.getClass(), "getElements")
                .flatMap(method -> WcwtReflect.invoke(bookmarkList, method))
                .filter(List.class::isInstance)
                .map(value -> (List<?>) value)
                .orElse(List.of());
        List<AEKey> bookmarks = new ArrayList<>(elements.size());
        for (Object element : elements) {
            var typedIngredient = WcwtReflect.findMethod(element.getClass(), "getTypedIngredient")
                    .flatMap(method -> WcwtReflect.invoke(element, method))
                    .orElse(null);
            if (!(typedIngredient instanceof ITypedIngredient<?> ingredient)) {
                continue;
            }
            GenericStack stack = WcwtRecipeTransferHandler.toGenericStackForBookmark(ingredient);
            if (stack != null && stack.what() != null) {
                bookmarks.add(stack.what());
            }
        }
        return List.copyOf(bookmarks);
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
            return GenericStack.fromFluidStack(new net.neoforged.neoforge.fluids.FluidStack(
                    fluid, (int) Math.max(1L, amount)));
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
