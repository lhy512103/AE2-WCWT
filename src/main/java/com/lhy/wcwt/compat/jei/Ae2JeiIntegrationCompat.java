package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.GenericStack;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Ae2JeiIntegrationCompat {
    private static final String MOD_ID = "ae2jeiintegration";

    private Ae2JeiIntegrationCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    @Nullable
    public static GenericStack convert(@Nullable ITypedIngredient<?> ingredient) {
        if (!available() || ingredient == null) {
            return null;
        }
        Object raw = ingredient.getIngredient();
        if (raw instanceof ItemStack || raw instanceof FluidStack) {
            return null;
        }
        return Impl.convert(ingredient);
    }

    private static final class Impl {
        private static final Map<IIngredientType<?>, Object> CONVERTERS = new ConcurrentHashMap<>();
        private static final Object MISSING = new Object();

        private Impl() {
        }

        @Nullable
        static GenericStack convert(ITypedIngredient<?> ingredient) {
            try {
                return convertTyped(ingredient);
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Nullable
        private static <T> GenericStack convertTyped(ITypedIngredient<T> ingredient) {
            var converter = converterFor(ingredient.getType());
            return converter == null ? null : converter.getStackFromIngredient(ingredient.getIngredient());
        }

        @Nullable
        @SuppressWarnings("unchecked")
        private static <T> tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter<T> converterFor(
                IIngredientType<T> type) {
            var cached = CONVERTERS.get(type);
            if (cached == MISSING) {
                return null;
            }
            if (cached instanceof tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter<?> converter) {
                return (tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter<T>) converter;
            }
            var converter = tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters.getConverter(type);
            CONVERTERS.put(type, converter == null ? MISSING : converter);
            return converter;
        }
    }
}
