package com.lhy.wcwt.compat.plus;

import appeng.parts.encoding.EncodingMode;
import com.extendedae_plus.compat.JeiRuntimeCompat;
import com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

/** Resolves recipe labels on the client, where JEI/EMI use the player's language. */
public final class PlusRecipeMappingClient {
    private PlusRecipeMappingClient() {
    }

    public static void captureJei(@Nullable Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode) {
        if (mode == EncodingMode.PROCESSING) {
            String key = JeiRuntimeCompat.getRecipeCategorySearchKey(recipeBase);
            if (key != null && !key.isBlank()) {
                ExtendedAEPatternUploadUtil.setLastProviderSearchKey(key);
                return;
            }
        }
        PlusMapping.captureRecipeSearchKey(recipeBase, recipe, mode);
    }

    public static void captureCategory(@Nullable Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode,
                                        @Nullable ResourceLocation categoryId, @Nullable String categoryTitle) {
        if (mode == EncodingMode.PROCESSING && captureLocalizedCategory(categoryId, categoryTitle)) {
            return;
        }
        PlusMapping.captureRecipeSearchKey(recipeBase, recipe, mode);
    }

    static boolean captureLocalizedCategory(@Nullable ResourceLocation categoryId, @Nullable String categoryTitle) {
        if (categoryId == null) {
            return false;
        }
        // EAEP keeps explicit names and aliases ahead of the localized category title.
        String key = ExtendedAEPatternUploadUtil.resolveRecipeTypeSearchKey(categoryId, categoryTitle);
        if (key == null || key.isBlank()) {
            return false;
        }
        ExtendedAEPatternUploadUtil.setLastProviderSearchKey(key);
        return true;
    }
}
