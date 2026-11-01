package com.lhy.wcwt.pull;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import com.google.common.base.Suppliers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Resolves partial component predicates without treating their display stacks as exact item keys. */
public final class WcwtComponentIngredientEncoding {
    private WcwtComponentIngredientEncoding() {
    }

    public static boolean isPartialComponentIngredient(Ingredient ingredient) {
        return ingredient.getCustomIngredient() instanceof DataComponentIngredient components && !components.isStrict();
    }

    public static Supplier<List<ItemStack>> recipeResults(@Nullable Level level) {
        return Suppliers.memoize(() -> level == null ? List.of()
                : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                        .filter(holder -> holder.value().getClass() == ShapedRecipe.class
                                || holder.value().getClass() == ShapelessRecipe.class)
                        .sorted(Comparator.comparing(holder -> holder.id().toString()))
                        .map(holder -> holder.value().getResultItem(level.registryAccess()))
                        .toList());
    }

    public static ItemStack resolve(WcwtIngredientPriorities.PriorityContext context, Ingredient ingredient,
                                   List<ItemStack> visibleAlternatives, Supplier<List<ItemStack>> recipeResults) {
        if (!isPartialComponentIngredient(ingredient)) {
            return WcwtIngredientPriorities.chooseBestItemForEncoding(context, ingredient, visibleAlternatives);
        }

        // Explicit user choices still win, including keys with components absent from the display sample.
        ItemStack favorite = preferred(context.favoritePriorities(), ingredient);
        if (!favorite.isEmpty()) {
            return favorite;
        }
        ItemStack bookmark = preferred(context.bookmarkPriorities(), ingredient);
        if (!bookmark.isEmpty()) {
            return bookmark;
        }
        ItemStack available = context.ingredientPriorities().entrySet().stream()
                .filter(entry -> entry.getKey() instanceof AEItemKey item && item.matches(ingredient))
                .max(Map.Entry.comparingByValue())
                .map(entry -> ((AEItemKey) entry.getKey()).toStack())
                .orElse(ItemStack.EMPTY);
        if (!available.isEmpty()) {
            return available;
        }

        // getItems() is only a predicate example: a previous stage can produce additional components.
        for (ItemStack result : recipeResults.get()) {
            if (!result.isEmpty() && ingredient.test(result)) {
                return result.copyWithCount(1);
            }
        }
        for (ItemStack visible : visibleAlternatives) {
            if (visible != null && !visible.isEmpty() && ingredient.test(visible)) {
                return visible.copyWithCount(1);
            }
        }
        ItemStack[] examples = ingredient.getItems();
        return examples.length == 0 ? ItemStack.EMPTY : examples[0].copyWithCount(1);
    }

    private static ItemStack preferred(@Nullable Map<AEKey, Integer> priorities, Ingredient ingredient) {
        if (priorities == null || priorities.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return priorities.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof AEItemKey item && item.matches(ingredient))
                .min(Map.Entry.comparingByValue())
                .map(entry -> ((AEItemKey) entry.getKey()).toStack())
                .orElse(ItemStack.EMPTY);
    }
}
