package com.lhy.wcwt.compat;

import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Create basin recipes only expose their ingredients through Create's own classes. Resolving
 * {@code BasinRecipe} for an {@code instanceof} needs the class to exist, so every reference lives in
 * {@link Impl}, which is only touched once Create is known to be loaded.
 */
public final class CreateBasinCompat {
    private static final String CREATE = "create";

    private CreateBasinCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(CREATE);
    }

    /**
     * Inputs of a basin recipe, in recipe order, or {@code null} when Create is absent or the recipe is not
     * a basin recipe. Each ingredient is converted by the caller's own mappers so JEI and EMI keep their
     * respective priority rules.
     */
    @Nullable
    public static List<@Nullable GenericStack> collectInputs(@Nullable Recipe<?> recipe,
                                                             Function<Ingredient, @Nullable GenericStack> itemMapper,
                                                             Function<SizedFluidIngredient, @Nullable GenericStack> fluidMapper) {
        if (recipe == null || !available()) {
            return null;
        }
        try {
            return Impl.collectInputs(recipe, itemMapper, fluidMapper);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Outputs of a basin recipe, or {@code null} when Create is absent or the recipe is not a basin recipe. */
    @Nullable
    public static List<@Nullable GenericStack> collectOutputs(@Nullable Recipe<?> recipe) {
        if (recipe == null || !available()) {
            return null;
        }
        try {
            return Impl.collectOutputs(recipe);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class Impl {
        private Impl() {
        }

        @Nullable
        static List<@Nullable GenericStack> collectInputs(Recipe<?> recipe,
                                                          Function<Ingredient, @Nullable GenericStack> itemMapper,
                                                          Function<SizedFluidIngredient, @Nullable GenericStack> fluidMapper) {
            if (!(recipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basinRecipe)) {
                return null;
            }
            List<@Nullable GenericStack> inputs = new ArrayList<>();
            for (Ingredient ingredient : basinRecipe.getIngredients()) {
                inputs.add(itemMapper.apply(ingredient));
            }
            for (SizedFluidIngredient fluidIngredient : basinRecipe.getFluidIngredients()) {
                inputs.add(fluidMapper.apply(fluidIngredient));
            }
            return inputs;
        }

        @Nullable
        static List<@Nullable GenericStack> collectOutputs(Recipe<?> recipe) {
            if (!(recipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basinRecipe)) {
                return null;
            }
            List<@Nullable GenericStack> outputs = new ArrayList<>();
            for (var result : basinRecipe.getRollableResults()) {
                outputs.add(GenericStack.fromItemStack(result.getStack().copy()));
            }
            for (FluidStack fluidStack : basinRecipe.getFluidResults()) {
                if (!fluidStack.isEmpty()) {
                    outputs.add(GenericStack.fromFluidStack(fluidStack.copy()));
                }
            }
            return outputs;
        }
    }
}
