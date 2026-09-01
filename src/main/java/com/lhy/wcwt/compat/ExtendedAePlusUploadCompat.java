package com.lhy.wcwt.compat;

import appeng.helpers.patternprovider.PatternContainer;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.plus.PlusMapping;
import com.lhy.wcwt.compat.plus.PlusPresence;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

public final class ExtendedAePlusUploadCompat {
    private ExtendedAePlusUploadCompat() {
    }

    public static void captureRecipeSearchKey(@Nullable Object recipeBase, @Nullable Recipe<?> recipe,
                                              EncodingMode mode) {
        if (PlusPresence.available()) {
            PlusMapping.captureRecipeSearchKey(recipeBase, recipe, mode);
        }
    }

    @Nullable
    public static String consumeLastProviderSearchKey() {
        return PlusPresence.available() ? PlusMapping.consumeLastProviderSearchKey() : null;
    }

    public static String resolveProviderSearchKey(@Nullable String rawKey) {
        if (!PlusPresence.available()) {
            return rawKey == null ? "" : rawKey.trim();
        }
        return PlusMapping.resolveProviderSearchKey(rawKey);
    }

    @Nullable
    public static String mapRecipeTypeToSearchKey(@Nullable Recipe<?> recipe) {
        return PlusPresence.available() ? PlusMapping.mapRecipeTypeToSearchKey(recipe) : null;
    }

    @Nullable
    public static String deriveSearchKeyFromUnknownRecipe(@Nullable Object recipeBase) {
        return PlusPresence.available() ? PlusMapping.deriveSearchKeyFromUnknownRecipe(recipeBase) : null;
    }

    public static boolean addOrUpdateRecipeTypeMapping(String key, String value) {
        return PlusPresence.available() && PlusMapping.addOrUpdateRecipeTypeMapping(key, value);
    }

    public static void loadRecipeTypeNames() {
        if (PlusPresence.available()) {
            PlusMapping.loadRecipeTypeNames();
        }
    }

    public static int removeMappingsByCnValue(String value) {
        return PlusPresence.available() ? PlusMapping.removeMappingsByCnValue(value) : 0;
    }

    @Nullable
    public static String getProviderDisplayName(@Nullable PatternContainer provider) {
        if (!PlusPresence.available()) {
            return null;
        }
        String name = PlusMapping.getProviderDisplayName(provider);
        return name.isBlank() ? null : name;
    }
}
