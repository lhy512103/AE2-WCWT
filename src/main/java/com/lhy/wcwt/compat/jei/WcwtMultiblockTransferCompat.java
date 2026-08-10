package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 JEI 中“只有结构材料、没有实际产物”的多方块信息页编码为处理样板。
 * 类名与槽位形态共同限定范围，避免把普通教程页和真实加工配方误识别为结构页。
 */
final class WcwtMultiblockTransferCompat {
    private static final String GTLCORE_MOD_ID = "gtlcore";
    private static final String GTCEU_MULTIBLOCK_INFO_WRAPPER =
            "com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper";
    private static final String GTL_CONFIG_CLASS = "org.gtlcore.gtlcore.config.ConfigHolder";
    private static final Set<Class<?>> UNSUPPORTED_RECIPE_CLASSES = ConcurrentHashMap.newKeySet();
    private static volatile @Nullable String[] cachedGtlHatchFilters;

    private WcwtMultiblockTransferCompat() {
    }

    static boolean isSupportedRecipe(@Nullable Object recipe, IRecipeSlotsView recipeSlots) {
        if (recipe == null || UNSUPPORTED_RECIPE_CLASSES.contains(recipe.getClass())) {
            return false;
        }
        String className = recipe.getClass().getName().toLowerCase(Locale.ROOT);
        if (!className.contains("multiblock")) {
            return false;
        }
        boolean knownGtceuStructurePage = GTCEU_MULTIBLOCK_INFO_WRAPPER.equals(recipe.getClass().getName());
        return hasStructureInputs(recipeSlots)
                && (knownGtceuStructurePage || !hasActualOutputs(recipeSlots));
    }

    @Nullable
    static TransferData buildTransferData(WirelessComprehensiveWorkTerminalMenu menu,
                                          Object recipe,
                                          IRecipeSlotsView recipeSlots) {
        try {
            var priorityContext = WcwtRecipeTransferHandler.createPriorityContext(menu);
            String[] hatchFilters = getGtlHatchFilters();
            List<GenericStack> inputs = new ArrayList<>();
            for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                List<ItemStack> alternatives = WcwtRecipeTransferHandler.collectVisibleItemAlternatives(slot);
                if (alternatives.isEmpty() || isFilteredHatchSlot(alternatives, hatchFilters)) {
                    continue;
                }
                GenericStack input = WcwtRecipeTransferHandler.toPreferredGenericStack(priorityContext, slot, true);
                if (input != null) {
                    inputs.add(input);
                }
            }
            if (inputs.isEmpty()) {
                return null;
            }
            return new TransferData(inputs, List.of(createDraftOutput(recipe)));
        } catch (ReflectiveOperationException | LinkageError e) {
            UNSUPPORTED_RECIPE_CLASSES.add(recipe.getClass());
            WcwtMod.LOGGER.warn("WCWT failed to read multiblock structure metadata from {}",
                    recipe.getClass().getName(), e);
            return null;
        }
    }

    private static boolean hasStructureInputs(IRecipeSlotsView recipeSlots) {
        return recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .anyMatch(slot -> !WcwtRecipeTransferHandler.collectVisibleItemAlternatives(slot).isEmpty());
    }

    private static boolean hasActualOutputs(IRecipeSlotsView recipeSlots) {
        return recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                .anyMatch(slot -> slot.getAllIngredients().findAny().isPresent());
    }

    private static boolean isFilteredHatchSlot(List<ItemStack> alternatives, String[] filters) {
        if (filters.length == 0) {
            return false;
        }
        return alternatives.stream().allMatch(stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String itemId = id == null ? "" : id.toString();
            return Arrays.stream(filters)
                    .filter(filter -> filter != null && !filter.isBlank())
                    .anyMatch(itemId::contains);
        });
    }

    private static String[] getGtlHatchFilters() {
        String[] cached = cachedGtlHatchFilters;
        if (cached != null) {
            return cached;
        }
        if (!ModList.get().isLoaded(GTLCORE_MOD_ID)) {
            cachedGtlHatchFilters = new String[0];
            return cachedGtlHatchFilters;
        }
        try {
            Class<?> configClass = Class.forName(GTL_CONFIG_CLASS);
            Object instance = getField(configClass, "INSTANCE").get(null);
            Object value = getField(configClass, "filterHatch").get(instance);
            if (value instanceof String[] filters) {
                cachedGtlHatchFilters = filters.clone();
            } else if (value instanceof List<?> list) {
                cachedGtlHatchFilters = list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toArray(String[]::new);
            } else {
                cachedGtlHatchFilters = new String[0];
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            cachedGtlHatchFilters = new String[0];
            WcwtMod.LOGGER.warn("WCWT could not read optional GTLCore hatch filters; continuing without them", e);
        }
        return cachedGtlHatchFilters;
    }

    private static GenericStack createDraftOutput(Object recipe) throws ReflectiveOperationException {
        Component name = resolveStructureName(recipe);
        ItemStack draft = new ItemStack(Items.ENCHANTED_BOOK);
        draft.setHoverName(name.copy().withStyle(Style.EMPTY.withColor(0xFC5AFC)));
        return GenericStack.fromItemStack(draft);
    }

    private static Component resolveStructureName(Object recipe) throws ReflectiveOperationException {
        Object definition = tryReadField(recipe, "definition");
        ResourceLocation definitionId = tryReadId(definition);
        if (definitionId != null) {
            return Component.translatable(definitionId.toLanguageKey("block"));
        }

        ResourceLocation recipeId = tryReadId(recipe);
        if (recipeId != null) {
            return Component.translatable(recipeId.toLanguageKey("block"));
        }

        Component component = tryReadComponent(recipe, "getDisplayName", "getTitle", "getName");
        if (component != null) {
            return component;
        }

        String fallback = recipe.getClass().getSimpleName()
                .replaceAll("(?i)multiblock|wrapper|recipe|info|page", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
        return Component.literal(fallback.isEmpty() ? "Multiblock Structure" : fallback);
    }

    @Nullable
    private static ResourceLocation tryReadId(@Nullable Object target) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        for (String methodName : List.of("getId", "getRegistryName")) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof ResourceLocation id) {
                    return id;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        Object fieldValue = tryReadField(target, "id");
        return fieldValue instanceof ResourceLocation id ? id : null;
    }

    @Nullable
    private static Component tryReadComponent(Object target, String... methodNames)
            throws ReflectiveOperationException {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Component component) {
                    return component;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Object tryReadField(Object target, String name) throws IllegalAccessException {
        try {
            return getField(target.getClass(), name).get(target);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static Field getField(Class<?> owner, String name) throws NoSuchFieldException {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException ignored) {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }

    record TransferData(List<GenericStack> inputs, List<GenericStack> outputs) {
    }
}