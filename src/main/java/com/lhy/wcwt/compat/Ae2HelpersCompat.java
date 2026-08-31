package com.lhy.wcwt.compat;

import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.CraftingRecipeUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import rearth.ae2helpers.client.AutoCraftingWatcher;

import java.util.HashMap;

public final class Ae2HelpersCompat {
    private static final String MOD_ID = "ae2helpers";

    private Ae2HelpersCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void watchMissingCraftingSlots(CraftingTermMenu menu, @Nullable Recipe<?> recipe) {
        if (!available() || recipe == null || !Screen.hasControlDown()) {
            return;
        }
        Impl.watch(menu, recipe);
    }

    private static final class Impl {
        private Impl() {
        }

        static void watch(CraftingTermMenu menu, Recipe<?> recipe) {
            if (!AutoCraftingWatcher.INSTANCE.isAutoInsertEnabled()) {
                return;
            }
            var ingredients = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
            var slotToIngredient = new HashMap<Integer, Ingredient>();
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i);
                if (!ingredient.isEmpty()) {
                    slotToIngredient.put(i, ingredient);
                }
            }
            if (slotToIngredient.isEmpty()) {
                return;
            }
            var missing = menu.findMissingIngredients(slotToIngredient);
            if (!missing.craftableSlots().isEmpty()) {
                AutoCraftingWatcher.INSTANCE.setPending(slotToIngredient, missing.craftableSlots());
            }
        }
    }
}
