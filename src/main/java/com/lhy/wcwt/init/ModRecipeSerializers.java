package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.recipe.WcwtUniversalTerminalUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, WcwtMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WcwtUniversalTerminalUpgradeRecipe>>
            UNIVERSAL_TERMINAL_UPGRADE = RECIPE_SERIALIZERS.register("universal_terminal_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(WcwtUniversalTerminalUpgradeRecipe::new));
}
