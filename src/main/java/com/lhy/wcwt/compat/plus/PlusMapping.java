package com.lhy.wcwt.compat.plus;

import appeng.helpers.patternprovider.PatternContainer;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.parts.encoding.EncodingMode;
import com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public final class PlusMapping {
    private PlusMapping() {
    }

    public static void captureRecipeSearchKey(@Nullable Object recipeBase, @Nullable Recipe<?> recipe,
                                              EncodingMode mode) {
        if (mode != EncodingMode.PROCESSING) {
            ExtendedAEPatternUploadUtil.presetCraftingProviderSearchKey();
            return;
        }
        String name = ExtendedAEPatternUploadUtil.mapRecipeTypeToSearchKey(recipe);
        if ((name == null || name.isBlank()) && recipeBase != null) {
            name = ExtendedAEPatternUploadUtil.deriveSearchKeyFromUnknownRecipe(recipeBase);
        }
        if (name != null && !name.isBlank()) {
            ExtendedAEPatternUploadUtil.setLastProviderSearchKey(name);
        }
    }

    public static void captureRecipeSearchKey(@Nullable Object rawRecipe) {
        Recipe<?> recipe = null;
        Object recipeBase = rawRecipe;
        if (rawRecipe instanceof RecipeHolder<?> holder) {
            recipe = holder.value();
            recipeBase = holder;
        } else if (rawRecipe instanceof Recipe<?> value) {
            recipe = value;
        }
        captureRecipeSearchKey(recipeBase, recipe,
                recipe != null && EncodingHelper.isSupportedCraftingRecipe(recipe)
                        ? EncodingMode.CRAFTING
                        : EncodingMode.PROCESSING);
    }

    @Nullable
    public static String consumeLastProviderSearchKey() {
        return ExtendedAEPatternUploadUtil.consumeLastProviderSearchKey();
    }

    public static String resolveProviderSearchKey(@Nullable String rawKey) {
        if (rawKey == null) {
            return "";
        }
        String resolved = ExtendedAEPatternUploadUtil.resolveProviderSearchKey(rawKey);
        return resolved == null ? "" : resolved.trim();
    }

    @Nullable
    public static String mapRecipeTypeToSearchKey(@Nullable Recipe<?> recipe) {
        return ExtendedAEPatternUploadUtil.mapRecipeTypeToSearchKey(recipe);
    }

    @Nullable
    public static String deriveSearchKeyFromUnknownRecipe(@Nullable Object recipeBase) {
        return ExtendedAEPatternUploadUtil.deriveSearchKeyFromUnknownRecipe(recipeBase);
    }

    public static boolean addOrUpdateRecipeTypeMapping(String key, String value) {
        return ExtendedAEPatternUploadUtil.addOrUpdateRecipeTypeMapping(key, value);
    }

    public static void loadRecipeTypeNames() {
        ExtendedAEPatternUploadUtil.loadRecipeTypeNames();
    }

    public static int removeMappingsByCnValue(String value) {
        return ExtendedAEPatternUploadUtil.removeMappingsByCnValue(value);
    }

    public static String getProviderDisplayName(@Nullable PatternContainer provider) {
        if (provider == null) {
            return "";
        }
        var nameComponent = ExtendedAEPatternUploadUtil.getProviderDisplayNameComponent(provider);
        String name = nameComponent == null ? null : nameComponent.getString();
        return name == null ? "" : name;
    }
}
