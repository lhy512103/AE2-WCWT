package com.lhy.wcwt.compat;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.config.WcwtClientConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GtceuRecipeTransferExclusions {
    private static final String GT_RECIPE_CLASS = "com.gregtechceu.gtceu.api.recipe.GTRecipe";
    private static final String GT_JEI_WRAPPER_CLASS = "com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeWrapper";
    private static final String GT_EMI_RECIPE_CLASS = "com.gregtechceu.gtceu.integration.emi.recipe.GTEmiRecipe";

    private GtceuRecipeTransferExclusions() {
    }

    public static List<@Nullable GenericStack> removeNonConsumableInputs(
            @Nullable Object recipeLike,
            List<@Nullable GenericStack> inputs) {
        if (!WcwtClientConfig.filterGtceuNonConsumablePatternInputs()) {
            return inputs;
        }
        Object gtRecipe = unwrapGtRecipe(recipeLike);
        if (gtRecipe == null || inputs.isEmpty()) {
            return inputs;
        }

        List<ItemMatcher> nonConsumableInputs = collectNonConsumableItemInputs(gtRecipe);
        if (nonConsumableInputs.isEmpty()) {
            return inputs;
        }

        List<@Nullable GenericStack> filtered = new ArrayList<>(inputs.size());
        for (GenericStack input : inputs) {
            if (input != null && input.what() instanceof AEItemKey itemKey
                    && removeFirstMatching(nonConsumableInputs, itemKey.toStack())) {
                continue;
            }
            filtered.add(input);
        }
        return filtered;
    }

    @Nullable
    private static Object unwrapGtRecipe(@Nullable Object recipeLike) {
        if (recipeLike == null) {
            return null;
        }
        if (isClassNamed(recipeLike, GT_RECIPE_CLASS)) {
            return recipeLike;
        }
        if (isClassNamed(recipeLike, GT_JEI_WRAPPER_CLASS) || isClassNamed(recipeLike, GT_EMI_RECIPE_CLASS)) {
            return readField(recipeLike, "recipe");
        }
        return null;
    }

    private static List<ItemMatcher> collectNonConsumableItemInputs(Object gtRecipe) {
        Object inputs = readField(gtRecipe, "inputs");
        if (!(inputs instanceof Map<?, ?> inputMap)) {
            return List.of();
        }

        List<ItemMatcher> matchers = new ArrayList<>();
        for (Object value : inputMap.values()) {
            if (!(value instanceof Iterable<?> contents)) {
                continue;
            }
            for (Object content : contents) {
                if (!isNonConsumableContent(content)) {
                    continue;
                }
                Object ingredient = readField(content, "content");
                ItemMatcher matcher = createMatcher(ingredient);
                if (matcher != null) {
                    matchers.add(matcher);
                }
            }
        }
        return matchers;
    }

    private static boolean isNonConsumableContent(@Nullable Object content) {
        Object chance = readField(content, "chance");
        return chance instanceof Number number && number.floatValue() == 0.0f;
    }

    @Nullable
    private static ItemMatcher createMatcher(@Nullable Object content) {
        if (content instanceof Ingredient ingredient) {
            return ingredient::test;
        }
        if (content instanceof ItemStack stack && !stack.isEmpty()) {
            ItemStack copy = stack.copy();
            return candidate -> ItemStack.isSameItem(copy, candidate)
                    && Objects.equals(copy.getTag(), candidate.getTag());
        }
        return null;
    }

    private static boolean removeFirstMatching(List<ItemMatcher> matchers, ItemStack stack) {
        Iterator<ItemMatcher> iterator = matchers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().matches(stack)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private static boolean isClassNamed(Object instance, String className) {
        return className.equals(instance.getClass().getName());
    }

    @Nullable
    private static Object readField(@Nullable Object instance, String fieldName) {
        if (instance == null) {
            return null;
        }
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface ItemMatcher {
        boolean matches(ItemStack stack);
    }
}
