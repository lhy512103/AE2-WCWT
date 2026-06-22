package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.recipe.WcwtUniversalTerminalUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, WcwtMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<WcwtUniversalTerminalUpgradeRecipe>>
            UNIVERSAL_TERMINAL_UPGRADE = RECIPE_SERIALIZERS.register("universal_terminal_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(WcwtUniversalTerminalUpgradeRecipe::new));

    private ModRecipeSerializers() {
    }
}
