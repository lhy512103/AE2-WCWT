package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.recipe.WcwtUniversalTerminalCombineRecipe;
import com.lhy.wcwt.recipe.WirelessComprehensiveWorkTerminalRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, WcwtMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WirelessComprehensiveWorkTerminalRecipe>>
            WIRELESS_COMPREHENSIVE_WORK_TERMINAL = SERIALIZERS.register(
                    "wireless_comprehensive_work_terminal",
                    WirelessComprehensiveWorkTerminalRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WcwtUniversalTerminalCombineRecipe>>
            WCWT_UNIVERSAL_TERMINAL_COMBINE = SERIALIZERS.register(
                    "wcwt_universal_terminal_combine",
                    WcwtUniversalTerminalCombineRecipe.Serializer::new);

    private ModRecipes() {
    }
}