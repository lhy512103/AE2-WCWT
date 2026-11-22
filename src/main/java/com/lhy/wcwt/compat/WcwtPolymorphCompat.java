package com.lhy.wcwt.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Polymorph compatibility wrapper. Polymorph is not yet available on Minecraft 26.1.2,
 * so lookups delegate directly to the server recipe manager.
 */
public final class WcwtPolymorphCompat {
    private WcwtPolymorphCompat() {
    }

    public static Optional<RecipeHolder<CraftingRecipe>> getCraftingRecipe(
            AbstractContainerMenu menu, CraftingInput input, Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
        }
        return Optional.empty();
    }

    public static Optional<RecipeHolder<SmithingRecipe>> getSmithingRecipe(
            AbstractContainerMenu menu, SmithingRecipeInput input, Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, serverLevel);
        }
        return Optional.empty();
    }
}
