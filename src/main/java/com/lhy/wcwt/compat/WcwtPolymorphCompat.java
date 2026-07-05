package com.lhy.wcwt.compat;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Optional;

public final class WcwtPolymorphCompat {
    private static final String MOD_ID = "polymorph";

    private WcwtPolymorphCompat() {
    }

    public static Optional<CraftingRecipe> getCraftingRecipe(
            AbstractContainerMenu menu, CraftingContainer input, Level level, Player player) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        }
        return Impl.getPlayerRecipe(menu, RecipeType.CRAFTING, input, level, player, List.of());
    }

    public static Optional<SmithingRecipe> getSmithingRecipe(
            AbstractContainerMenu menu, SmithingRecipeInput input, Level level, Player player) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
        }
        List<SmithingRecipe> recipes = level.getRecipeManager().getRecipesFor(RecipeType.SMITHING, input, level);
        return Impl.getPlayerRecipe(menu, RecipeType.SMITHING, input, level, player, recipes);
    }

    public static ResourceLocation getSelectedRecipeId(Player player) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        return Impl.getSelectedRecipeId(player);
    }

    private static final class Impl {
        private Impl() {
        }

        private static <C extends Container, T extends Recipe<C>> Optional<T> getPlayerRecipe(
                AbstractContainerMenu menu, RecipeType<T> type, C input, Level level, Player player, List<T> recipes) {
            var recipeData = PolymorphApi.common().getRecipeData(player);
            if (recipeData.isEmpty()) {
                return level.getRecipeManager().getRecipeFor(type, input, level);
            }
            var data = recipeData.get();
            data.setContainerMenu(menu);
            return data.getRecipe(type, input, level, recipes);
        }

        private static ResourceLocation getSelectedRecipeId(Player player) {
            return PolymorphApi.common().getRecipeData(player)
                    .flatMap(data -> data.getSelectedRecipe().map(Recipe::getId))
                    .orElse(null);
        }
    }
}
