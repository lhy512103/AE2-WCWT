package com.lhy.wcwt.compat.jei;

import com.lhy.wcwt.WcwtMod;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Ae2JeiIntegrationCompat {
    private static volatile boolean broken;
    private static final String MOD_ID = "ae2jeiintegration";

    private Ae2JeiIntegrationCompat() {
    }

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID);
    }

    @Nullable
    public static GenericStack convert(@Nullable ITypedIngredient<?> ingredient) {
        if (ingredient == null) {
            return null;
        }
        Object raw = ingredient.getIngredient();
        if (raw instanceof ItemStack || raw instanceof FluidStack) {
            return null;
        }
        if (!available() || broken) {
            return null;
        }
        return Impl.convert(ingredient);
    }

    private static final class Impl {
        private static final Map<IIngredientType<?>, Object> CONVERTERS = new ConcurrentHashMap<>();
        private static final Object MISSING = new Object();
        private static volatile Method getConverterMethod;
        private static volatile Method getStackMethod;
        private static volatile boolean initialized;

        private Impl() {
        }

        private static void ensureInit() {
            if (initialized) {
                return;
            }
            initialized = true;
            try {
                Class<?> convertersClass = Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters");
                Class<?> converterClass = Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter");
                getConverterMethod = convertersClass.getMethod("getConverter", IIngredientType.class);
                getStackMethod = converterClass.getMethod("getStackFromIngredient", Object.class);
            } catch (Throwable e) {
                broken = true;
                WcwtMod.LOGGER.warn("WCWT: AE2 JEI Integration ingredient conversion disabled, its API changed", e);
            }
        }

        @Nullable
        static GenericStack convert(ITypedIngredient<?> ingredient) {
            ensureInit();
            if (broken || getConverterMethod == null || getStackMethod == null) {
                return null;
            }
            try {
                IIngredientType<?> type = ingredient.getType();
                Object cached = CONVERTERS.get(type);
                if (cached == MISSING) {
                    return null;
                }
                if (cached == null) {
                    cached = getConverterMethod.invoke(null, type);
                    CONVERTERS.put(type, cached == null ? MISSING : cached);
                    if (cached == null) {
                        return null;
                    }
                }
                Object stack = getStackMethod.invoke(cached, ingredient.getIngredient());
                return stack instanceof GenericStack gs ? gs : null;
            } catch (LinkageError e) {
                broken = true;
                WcwtMod.LOGGER.warn("WCWT: AE2 JEI Integration ingredient conversion disabled, its API changed", e);
                return null;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
