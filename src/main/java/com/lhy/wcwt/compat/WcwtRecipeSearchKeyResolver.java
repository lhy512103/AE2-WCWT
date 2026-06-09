package com.lhy.wcwt.compat;

import appeng.parts.encoding.EncodingMode;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WcwtRecipeSearchKeyResolver {
    private static final String EAEP_RECIPE_TYPE_NAME_CONFIG =
            "com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig";

    private WcwtRecipeSearchKeyResolver() {
    }

    public static void updateEaepProviderSearchKey(Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode) {
        if (!ModList.get().isLoaded("extendedae_plus")) {
            return;
        }
        if (mode != EncodingMode.PROCESSING) {
            invokeStatic("presetCraftingProviderSearchKey");
            return;
        }

        String name = resolveProcessingSearchKey(recipeBase, recipe);
        if (name != null && !name.isBlank()) {
            invokeStatic("setLastProcessingName", new Class<?>[] { String.class }, name);
        }
    }

    @Nullable
    public static String resolveProcessingSearchKey(@Nullable Object recipeBase, @Nullable Recipe<?> recipe) {
        String name = recipe != null ? mapRecipeTypeToSearchKey(recipe) : null;
        if (name != null && !name.isBlank()) {
            return name;
        }

        Object gtRecipe = unwrapGtceuRecipe(recipeBase);
        name = mapGTCEuRecipeToSearchKey(gtRecipe);
        if (name != null && !name.isBlank()) {
            return name;
        }

        return recipeBase != null ? deriveSearchKeyFromUnknownRecipe(recipeBase) : null;
    }

    @Nullable
    public static String mapRecipeTypeToSearchKey(Recipe<?> recipe) {
        Object result = invokeStatic("mapRecipeTypeToSearchKey",
                new Class<?>[] { Recipe.class }, recipe);
        return result instanceof String text ? text : null;
    }

    @Nullable
    public static String deriveSearchKeyFromUnknownRecipe(Object recipeBase) {
        Object result = invokeStatic("deriveSearchKeyFromUnknownRecipe",
                new Class<?>[] { Object.class }, recipeBase);
        return result instanceof String text ? text : null;
    }

    @Nullable
    private static String mapGTCEuRecipeToSearchKey(@Nullable Object recipeBase) {
        if (recipeBase == null) {
            return null;
        }
        Object result = invokeStatic("mapGTCEuRecipeToSearchKey",
                new Class<?>[] { Object.class }, recipeBase);
        return result instanceof String text ? text : null;
    }

    @Nullable
    private static Object unwrapGtceuRecipe(@Nullable Object recipeBase) {
        if (recipeBase == null) {
            return null;
        }
        String className = recipeBase.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if (!className.contains("gtceu") && !className.contains("gregtech")) {
            return null;
        }

        String direct = mapGTCEuRecipeToSearchKey(recipeBase);
        if (direct != null && !direct.isBlank()) {
            return recipeBase;
        }

        Field field = findField(recipeBase.getClass(), "recipe");
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            Object value = field.get(recipeBase);
            return value != recipeBase ? value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Nullable
    private static Object invokeStatic(String methodName) {
        return invokeStatic(methodName, new Class<?>[0]);
    }

    @Nullable
    private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> configClass = Class.forName(EAEP_RECIPE_TYPE_NAME_CONFIG);
            Method method = configClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
