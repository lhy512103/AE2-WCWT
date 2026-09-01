package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Encodes JEI multiblock information pages that have structure inputs but no real output. */
final class WcwtMultiblockTransferCompat {
    private static final String GTLCORE_MOD_ID = "gtlcore";
    private static final String GTCEU_MULTIBLOCK_INFO_WRAPPER =
            "com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper";
    private static final String GTL_CONFIG_CLASS = "org.gtlcore.gtlcore.config.ConfigHolder";
    private static final Set<Class<?>> UNSUPPORTED_RECIPE_CLASSES = ConcurrentHashMap.newKeySet();
    private static volatile @Nullable String[] cachedGtlHatchFilters;

    private WcwtMultiblockTransferCompat() {
    }

    static boolean isSupportedRecipe(@Nullable Object recipe, IRecipeSlotsView slots) {
        if (recipe == null || UNSUPPORTED_RECIPE_CLASSES.contains(recipe.getClass())) {
            return false;
        }
        String className = recipe.getClass().getName().toLowerCase(Locale.ROOT);
        if (!className.contains("multiblock")) {
            return false;
        }
        return hasStructureInputs(slots)
                && (GTCEU_MULTIBLOCK_INFO_WRAPPER.equals(recipe.getClass().getName()) || !hasActualOutputs(slots));
    }

    @Nullable
    static TransferData buildTransferData(WirelessComprehensiveWorkTerminalMenu menu, Object recipe,
                                           IRecipeSlotsView slots) {
        try {
            var priorityContext = WcwtRecipeTransferHandler.createPriorityContext(menu);
            String[] filters = getGtlHatchFilters();
            List<GenericStack> inputs = new ArrayList<>();
            for (IRecipeSlotView slot : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
                List<ItemStack> alternatives = slot.getItemStacks().filter(stack -> !stack.isEmpty())
                        .map(ItemStack::copy).toList();
                if (alternatives.isEmpty() || isFilteredHatchSlot(alternatives, filters)) {
                    continue;
                }
                GenericStack input = WcwtRecipeTransferHandler.toPreferredGenericStack(priorityContext, slot, true);
                if (input != null) {
                    inputs.add(input);
                }
            }
            return inputs.isEmpty() ? null : new TransferData(inputs, List.of(createDraftOutput(recipe)));
        } catch (RuntimeException e) {
            UNSUPPORTED_RECIPE_CLASSES.add(recipe.getClass());
            WcwtMod.LOGGER.warn("WCWT failed to read multiblock structure metadata from {}", recipe.getClass().getName(), e);
            return null;
        }
    }

    private static boolean hasStructureInputs(IRecipeSlotsView slots) {
        return slots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .anyMatch(slot -> slot.getItemStacks().anyMatch(stack -> !stack.isEmpty()));
    }

    private static boolean hasActualOutputs(IRecipeSlotsView slots) {
        return slots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                .anyMatch(slot -> slot.getAllIngredients().findAny().isPresent());
    }

    private static boolean isFilteredHatchSlot(List<ItemStack> alternatives, String[] filters) {
        return filters.length > 0 && alternatives.stream().allMatch(stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String itemId = id == null ? "" : id.toString();
            return Arrays.stream(filters).filter(filter -> filter != null && !filter.isBlank())
                    .anyMatch(itemId::contains);
        });
    }

    private static String[] getGtlHatchFilters() {
        if (cachedGtlHatchFilters != null) {
            return cachedGtlHatchFilters;
        }
        if (!ModList.get().isLoaded(GTLCORE_MOD_ID)) {
            return cachedGtlHatchFilters = new String[0];
        }
        var instance = WcwtReflect.readStaticField(GTLCORE_MOD_ID, GTL_CONFIG_CLASS, "INSTANCE").orElse(null);
        Object value = instance == null ? null : readMember(instance, "filterHatch");
        if (value instanceof String[] filters) {
            cachedGtlHatchFilters = filters.clone();
        } else if (value instanceof List<?> list) {
            cachedGtlHatchFilters = list.stream().filter(String.class::isInstance)
                    .map(String.class::cast).toArray(String[]::new);
        } else {
            cachedGtlHatchFilters = new String[0];
        }
        return cachedGtlHatchFilters;
    }

    private static GenericStack createDraftOutput(Object recipe) {
        ItemStack draft = new ItemStack(Items.ENCHANTED_BOOK);
        draft.set(DataComponents.CUSTOM_NAME, Component.literal(resolveStructureName(recipe).getString())
                .withStyle(Style.EMPTY.withColor(0xFC5AFC)));
        return GenericStack.fromItemStack(draft);
    }

    private static Component resolveStructureName(Object recipe) {
        ResourceLocation id = tryReadId(readMember(recipe, "definition"));
        if (id == null) {
            id = tryReadId(recipe);
        }
        if (id != null) {
            return Component.translatable(id.toLanguageKey("block"));
        }
        Component component = tryReadComponent(recipe, "getDisplayName", "getTitle", "getName");
        if (component != null) {
            return component;
        }
        String fallback = recipe.getClass().getSimpleName().replaceAll("(?i)multiblock|wrapper|recipe|info|page", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2").trim();
        return Component.literal(fallback.isEmpty() ? "Multiblock Structure" : fallback);
    }

    @Nullable
    private static ResourceLocation tryReadId(@Nullable Object target) {
        if (target == null) {
            return null;
        }
        for (String name : List.of("getId", "getRegistryName")) {
            Object value = WcwtReflect.findMethod(target.getClass(), name)
                    .flatMap(method -> WcwtReflect.invoke(target, method))
                    .orElse(null);
            if (value instanceof ResourceLocation id) {
                return id;
            }
        }
        Object value = readMember(target, "id");
        return value instanceof ResourceLocation id ? id : null;
    }

    @Nullable
    private static Component tryReadComponent(Object target, String... names) {
        for (String name : names) {
            Object value = WcwtReflect.findMethod(target.getClass(), name)
                    .flatMap(method -> WcwtReflect.invoke(target, method))
                    .orElse(null);
            if (value instanceof Component component) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private static Object readMember(Object target, String name) {
        if (target == null) {
            return null;
        }
        return WcwtReflect.findField(target.getClass(), name)
                .or(() -> WcwtReflect.findDeclaredField(target.getClass(), name))
                .flatMap(field -> WcwtReflect.readField(target, field))
                .orElse(null);
    }

    record TransferData(List<GenericStack> inputs, List<GenericStack> outputs) {
    }
}
