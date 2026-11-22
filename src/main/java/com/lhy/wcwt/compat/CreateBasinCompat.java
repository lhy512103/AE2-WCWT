package com.lhy.wcwt.compat;

import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/** Compatibility facade for Create basin recipes (Create is not yet available on Minecraft 26.1.2). */
public final class CreateBasinCompat {
    private CreateBasinCompat() {
    }

    public static boolean available() {
        return false;
    }

    @Nullable
    public static List<@Nullable GenericStack> collectInputs(@Nullable Recipe<?> recipe,
                                                             Function<Ingredient, @Nullable GenericStack> itemMapper,
                                                             Function<SizedFluidIngredient, @Nullable GenericStack> fluidMapper) {
        return null;
    }

    @Nullable
    public static List<@Nullable GenericStack> collectOutputs(@Nullable Recipe<?> recipe) {
        return null;
    }
}
