package com.lhy.wcwt.compat.emi;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.compat.AppliedMekanisticsCompat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EmiStackCompat {
    private static final String MOD_ID = "emi";

    private EmiStackCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    @Nullable
    public static GenericStack convert(@Nullable Object raw) {
        if (!available() || raw == null) {
            return null;
        }
        return Impl.convert(raw);
    }

    public static List<GenericStack> stacksFromIngredient(@Nullable Object ingredient) {
        if (!available() || ingredient == null) {
            return List.of();
        }
        return Impl.stacksFromIngredient(ingredient);
    }

    private static final class Impl {
        private Impl() {
        }

        @Nullable
        static GenericStack convert(Object raw) {
            try {
                if (!(raw instanceof dev.emi.emi.api.stack.EmiStack stack) || stack.isEmpty()) {
                    return null;
                }
                Object key = stack.getKey();
                if (key instanceof Fluid fluid && fluid != Fluids.EMPTY) {
                    return GenericStack.fromFluidStack(new FluidStack(fluid, (int) Math.max(1L, stack.getAmount())));
                }
                ItemStack item = stack.getItemStack();
                if (!item.isEmpty()) {
                    return GenericStack.fromItemStack(item.copyWithCount(1));
                }
                return AppliedMekanisticsCompat.fromChemicalKey(key, stack.getAmount());
            } catch (Throwable ignored) {
                return null;
            }
        }

        static List<GenericStack> stacksFromIngredient(Object ingredient) {
            try {
                if (!(ingredient instanceof dev.emi.emi.api.stack.EmiIngredient emiIngredient)) {
                    return List.of();
                }
                List<GenericStack> stacks = new ArrayList<>();
                for (var emiStack : emiIngredient.getEmiStacks()) {
                    GenericStack stack = convert(emiStack);
                    if (stack != null && stack.what() != null) {
                        stacks.add(stack);
                    }
                }
                return stacks;
            } catch (Throwable ignored) {
                return List.of();
            }
        }
    }
}
