package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.IClientRepo;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.parts.encoding.EncodingMode;
import appeng.util.CraftingRecipeUtil;
import com.lhy.wcwt.compat.GtceuRecipeTransferExclusions;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.compat.WcwtRecipeSearchKeyResolver;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.client.WcwtFavorites;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import com.lhy.wcwt.pull.WcwtStackMatching;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.library.plugins.jei.info.IngredientInfoRecipe;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WcwtRecipeTransferHandler
        implements IUniversalRecipeTransferHandler<WirelessComprehensiveWorkTerminalMenu> {
    private static final String GTCEU_MULTIBLOCK_INFO_WRAPPER_CLASS =
            "com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper";

    private final IRecipeTransferHandlerHelper transferHelper;

    public WcwtRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }

    private static boolean isCraftingGridLocked(WirelessComprehensiveWorkTerminalMenu menu) {
        return menu.getMenuHost() != null && menu.getMenuHost().isCraftingGridLocked();
    }

    @Override
    public Class<? extends WirelessComprehensiveWorkTerminalMenu> getContainerClass() {
        return WirelessComprehensiveWorkTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<WirelessComprehensiveWorkTerminalMenu>> getMenuType() {
        return Optional.of(ModMenus.WCWT_MENU.get());
    }

    @Override
    public IRecipeTransferError transferRecipe(WirelessComprehensiveWorkTerminalMenu menu, Object recipe,
                                               IRecipeSlotsView recipeSlots, Player player,
                                               boolean maxTransfer, boolean doTransfer) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return null;
        }
        if (shouldSkipTransferAnalysis(recipe)) {
            return null;
        }

        RecipeHolder<?> recipeHolder = recipe instanceof RecipeHolder<?> holder ? holder : null;
        Recipe<?> minecraftRecipe = recipeHolder != null ? recipeHolder.value()
                : recipe instanceof Recipe<?> directRecipe ? directRecipe
                : null;
        EncodingMode mode = getTransferMode(minecraftRecipe, recipeSlots);

        if (isCraftingGridLocked(menu)) {
            if (doTransfer) {
                WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            }
            return WcwtPullRecipeTransfer.transfer(menu, recipe, recipeSlots, player, maxTransfer, doTransfer,
                    transferHelper, mode != EncodingMode.CRAFTING);
        }

        boolean encodingRecipe = mode != EncodingMode.PROCESSING;

        if (!doTransfer) {
            int inputHighlightLimit = mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE;
            var craftableSlots = findCraftableEncodingSlots(menu, recipeSlots, inputHighlightLimit);
            return new WcwtEncodingRecipeTransferError(craftableSlots);
        }

        List<@Nullable GenericStack> inputs = encodingRecipe
                ? collectCraftingLikeInputs(menu, recipeHolder, minecraftRecipe, recipeSlots, mode)
                : collectProcessingInputs(menu, minecraftRecipe, recipeSlots);
        List<@Nullable GenericStack> outputs = encodingRecipe ? List.of()
                : collectProcessingOutputs(minecraftRecipe, recipeSlots);
        if (mode == EncodingMode.PROCESSING) {
            inputs = GtceuRecipeTransferExclusions.removeNonConsumableInputs(recipe, inputs);
            inputs = compactProcessingIngredientOrder(inputs);
            outputs = compactProcessingIngredientOrder(outputs);
        }
        if (inputs.stream().allMatch(Objects::isNull) && outputs.stream().allMatch(Objects::isNull)) {
            return null;
        }

        if (doTransfer) {
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            updateEaepProviderSearchKey(recipe, minecraftRecipe, mode);
            ModNetworking.sendToServer(new JeiCraftingTransferPacket(inputs, outputs, false, mode));
        }
        return null;
    }

    /**
     * JEI encoding preview: follow AE2 original behavior and only highlight inputs whose candidates are craftable.
     */
    public static List<IRecipeSlotView> findCraftableEncodingSlots(WirelessComprehensiveWorkTerminalMenu menu,
                                                                   IRecipeSlotsView slotsView,
                                                                   int maxInputSlots) {
        var repo = menu.getClientRepo();
        if (repo == null) {
            return List.of();
        }
        var stream = slotsView.getSlotViews(RecipeIngredientRole.INPUT).stream();
        if (maxInputSlots < Integer.MAX_VALUE) {
            stream = stream.limit(maxInputSlots);
        }
        return stream
                .filter(slotView -> hasCraftableEncodingCandidate(repo, slotView))
                .toList();
    }

    private static boolean hasCraftableEncodingCandidate(IClientRepo repo, IRecipeSlotView slotView) {
        List<ItemStack> candidates = collectVisibleItemAlternatives(slotView);
        if (candidates.isEmpty()) {
            return false;
        }

        if (WcwtStackMatching.requiresExactItemKeyMatch(candidates)) {
            List<ItemStack> exactCandidates = narrowSpecificAlternativesToDisplayed(candidates);
            return repo.getAllEntries().stream()
                    .filter(entry -> entry.isCraftable() && entry.getWhat() instanceof AEItemKey itemKey
                            && containsEquivalentKey(exactCandidates, itemKey))
                    .findAny()
                    .isPresent();
        }

        Ingredient ingredient = Ingredient.of(candidates.stream());
        for (var entry : repo.getByIngredient(ingredient)) {
            if (entry.isCraftable()) {
                return true;
            }
        }
        return false;
    }

    public static void updateEaepProviderSearchKey(Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode) {
        WcwtRecipeSearchKeyResolver.updateEaepProviderSearchKey(recipeBase, recipe, mode);
    }

    private static EncodingMode getTransferMode(@Nullable Recipe<?> recipe, IRecipeSlotsView slotsView) {
        if (recipe != null && EncodingHelper.isSupportedCraftingRecipe(recipe)) {
            if (recipe.getType() == RecipeType.STONECUTTING) {
                return EncodingMode.STONECUTTING;
            }
            if (recipe.getType() == RecipeType.SMITHING) {
                return EncodingMode.SMITHING_TABLE;
            }
            return EncodingMode.CRAFTING;
        }

        return EncodingMode.PROCESSING;
    }

    static List<@Nullable GenericStack> collectCraftingLikeInputs(WirelessComprehensiveWorkTerminalMenu menu,
                                                                  @Nullable RecipeHolder<?> recipeHolder,
                                                                  @Nullable Recipe<?> recipe,
                                                                  IRecipeSlotsView recipeSlots,
                                                                  EncodingMode mode) {
        var priorityContext = createPriorityContext(menu);
        if (recipe != null) {
            List<IRecipeSlotView> inputSlots = mode == EncodingMode.CRAFTING
                    ? recipeSlots.getSlotViews().stream().limit(9).toList()
                    : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream().toList();
            List<List<ItemStack>> visibleRecipeAlternatives = collectVisibleItemAlternativesBySlot(inputSlots);
            if (mode == EncodingMode.CRAFTING) {
                var ingredients = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
                List<@Nullable GenericStack> resolved = new ArrayList<>(ingredients.size());
                for (int slot = 0; slot < ingredients.size(); slot++) {
                    List<ItemStack> visibleAlternatives = slot < visibleRecipeAlternatives.size()
                            ? visibleRecipeAlternatives.get(slot)
                            : List.of();
                    resolved.add(toBestGenericStack(priorityContext, ingredients.get(slot), visibleAlternatives));
                }
                return resolved;
            }
            var ingredients = CraftingRecipeUtil.getIngredients(recipe);
            List<@Nullable GenericStack> resolved = new ArrayList<>(ingredients.size());
            for (int slot = 0; slot < ingredients.size(); slot++) {
                List<ItemStack> visibleAlternatives = slot < visibleRecipeAlternatives.size()
                        ? visibleRecipeAlternatives.get(slot)
                        : List.of();
                resolved.add(toBestGenericStack(priorityContext, ingredients.get(slot), visibleAlternatives));
            }
            return resolved;
        }

        return collectStacksPreservingSlots(priorityContext, recipeSlots, RecipeIngredientRole.INPUT,
                mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE, false);
    }

    /**
     * JEI 处理配方往往在输入/输出槽之间留空位以对齐布局；拉取到 WCWT 编码区时应保持材料先后顺序，
     * 去掉中间的 {@code null}，从样板网格左上角起连续排列（与原版样板终端常见体验一致）。
     */
    private static List<@Nullable GenericStack> compactProcessingIngredientOrder(List<@Nullable GenericStack> sparse) {
        List<@Nullable GenericStack> dense = new ArrayList<>();
        for (GenericStack stack : sparse) {
            if (stack != null) {
                dense.add(stack);
            }
        }
        return dense;
    }

    private static List<@Nullable GenericStack> collectStacksPreservingSlots(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                                             IRecipeSlotsView recipeSlots,
                                                                             RecipeIngredientRole role,
                                                                             int limit,
                                                                             boolean preserveItemAmounts) {
        return recipeSlots.getSlotViews(role).stream()
                .limit(limit)
                .map(slotView -> toPreferredGenericStack(priorityContext, slotView, preserveItemAmounts))
                .toList();
    }

    private static List<@Nullable GenericStack> collectProcessingInputs(WirelessComprehensiveWorkTerminalMenu menu,
                                                                        @Nullable Recipe<?> recipe,
                                                                        IRecipeSlotsView recipeSlots) {
        var priorityContext = createPriorityContext(menu);
        if (isCreateBasinRecipe(recipe)) {
            List<@Nullable GenericStack> resolved = new ArrayList<>();
            for (GenericStack stack : collectItemStacksPreservingSlots(priorityContext,
                    recipeSlots, RecipeIngredientRole.INPUT)) {
                resolved.add(stack);
            }
            for (SizedFluidIngredient fluidIngredient : getCreateFluidIngredients(recipe)) {
                resolved.add(toGenericStack(fluidIngredient));
            }
            return resolved;
        }
        return collectStacksPreservingSlots(priorityContext, recipeSlots, RecipeIngredientRole.INPUT,
                Integer.MAX_VALUE, true);
    }

    private static List<@Nullable GenericStack> collectProcessingOutputs(@Nullable Recipe<?> recipe,
                                                                         IRecipeSlotsView recipeSlots) {
        if (isCreateBasinRecipe(recipe)) {
            List<@Nullable GenericStack> resolved = new ArrayList<>();
            for (ItemStack stack : getCreateRollableResultStacks(recipe)) {
                resolved.add(GenericStack.fromItemStack(stack.copy()));
            }
            for (FluidStack fluidStack : getCreateFluidResults(recipe)) {
                if (!fluidStack.isEmpty()) {
                    resolved.add(GenericStack.fromFluidStack(fluidStack.copy()));
                }
            }
            return resolved;
        }
        return collectStacksPreservingSlots(WcwtIngredientPriorities.PriorityContext.EMPTY,
                recipeSlots, RecipeIngredientRole.OUTPUT, Integer.MAX_VALUE, true);
    }

    @Nullable
    public static GenericStack toBestGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                  Ingredient ingredient,
                                                  List<ItemStack> visibleAlternatives) {
        if (ingredient.isEmpty()) {
            return null;
        }

        ItemStack best = WcwtIngredientPriorities.chooseBestItemForEncoding(
                priorityContext, ingredient, visibleAlternatives);
        return best.isEmpty() ? null : GenericStack.fromItemStack(best.copyWithCount(1));
    }

    private static List<List<ItemStack>> collectVisibleItemAlternativesBySlot(List<IRecipeSlotView> inputSlots) {
        return inputSlots.stream()
                .map(WcwtRecipeTransferHandler::collectVisibleItemAlternatives)
                .toList();
    }

    private static List<ItemStack> collectVisibleItemAlternatives(IRecipeSlotView slotView) {
        List<ItemStack> alternatives = new ArrayList<>();
        slotView.getDisplayedItemStack()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .ifPresent(alternatives::add);
        slotView.getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .forEach(alternatives::add);
        return WcwtIngredientPriorities.deduplicateItemAlternatives(alternatives);
    }

    private static List<ItemStack> narrowSpecificAlternativesToDisplayed(List<ItemStack> alternatives) {
        for (ItemStack alternative : alternatives) {
            if (WcwtStackMatching.hasSpecificData(alternative)) {
                return List.of(alternative.copy());
            }
        }
        return List.of();
    }

    private static boolean containsEquivalentKey(List<ItemStack> stacks, AEItemKey key) {
        for (ItemStack stack : stacks) {
            AEItemKey stackKey = AEItemKey.of(stack);
            if (stackKey != null && stackKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static List<GenericStack> collectItemStacksPreservingSlots(
            WcwtIngredientPriorities.PriorityContext priorityContext,
            IRecipeSlotsView recipeSlots,
            RecipeIngredientRole role) {
        return collectStacksPreservingSlots(priorityContext, recipeSlots, role, Integer.MAX_VALUE, true).stream()
                .filter(Objects::nonNull)
                .filter(stack -> stack.what() instanceof AEItemKey)
                .toList();
    }

    private static GenericStack toPreferredGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                        IRecipeSlotView slotView,
                                                        boolean preserveItemAmounts) {
        List<ItemStack> itemCandidates = collectVisibleItemAlternatives(slotView);
        if (!itemCandidates.isEmpty()) {
            if (WcwtStackMatching.requiresExactItemKeyMatch(itemCandidates)) {
                ItemStack selected = itemCandidates.get(0);
                GenericStack stack = GenericStack.fromItemStack(preserveItemAmounts
                        ? selected.copy()
                        : selected.copyWithCount(1));
                if (stack != null) {
                    return stack;
                }
            }

            List<GenericStack> itemStacks = itemCandidates.stream()
                    .map(stack -> GenericStack.fromItemStack(preserveItemAmounts
                            ? stack.copy()
                            : stack.copyWithCount(1)))
                    .filter(Objects::nonNull)
                    .toList();
            GenericStack selected = WcwtIngredientPriorities.chooseBestGenericStack(priorityContext, itemStacks);
            if (selected != null) {
                return preserveItemAmounts
                        ? withDisplayedItemCount(slotView, selected)
                        : new GenericStack(selected.what(), 1);
            }
        }

        List<GenericStack> fallbackCandidates = slotView.getAllIngredients()
                .map(ingredient -> toGenericStack(ingredient, preserveItemAmounts))
                .filter(Objects::nonNull)
                .toList();
        if (fallbackCandidates.isEmpty()) {
            return null;
        }

        GenericStack best = WcwtIngredientPriorities.chooseBestGenericStack(priorityContext, fallbackCandidates);
        return best != null ? best : fallbackCandidates.get(0);
    }

    private static GenericStack withDisplayedItemCount(IRecipeSlotView slotView, @Nullable GenericStack selected) {
        if (selected == null || !(selected.what() instanceof AEItemKey itemKey)) {
            return selected;
        }
        for (ItemStack displayed : collectVisibleItemAlternatives(slotView)) {
            AEItemKey displayedKey = AEItemKey.of(displayed);
            if (displayedKey != null && itemKey.equals(displayedKey)) {
                GenericStack stack = GenericStack.fromItemStack(displayed.copy());
                return stack != null ? stack : selected;
            }
        }
        return selected;
    }

    private static WcwtIngredientPriorities.PriorityContext createPriorityContext(WirelessComprehensiveWorkTerminalMenu menu) {
        Map<appeng.api.stacks.AEKey, Integer> bookmarkPriorities = WcwtClientConfig.preferJeiBookmarksForPatternEncoding()
                ? WcwtJeiBookmarkKeys.getBookmarkPriorities()
                : Map.of();
        Map<appeng.api.stacks.AEKey, Integer> favoritePriorities = WcwtClientConfig.preferFavoritesForPatternEncoding()
                ? WcwtFavorites.getFavoritePriorities()
                : Map.of();
        return WcwtIngredientPriorities.createContext(menu, bookmarkPriorities, favoritePriorities);
    }

    private static boolean shouldSkipTransferAnalysis(Object recipe) {
        return recipe instanceof ITagInfoRecipe
                || recipe instanceof IngredientInfoRecipe
                || isClassNamed(recipe, GTCEU_MULTIBLOCK_INFO_WRAPPER_CLASS);
    }

    private static boolean isClassNamed(@Nullable Object instance, String className) {
        return instance != null && className.equals(instance.getClass().getName());
    }

    @Nullable
    private static GenericStack toGenericStack(Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return null;
        }
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0 || items[0].isEmpty()) {
            return null;
        }
        return GenericStack.fromItemStack(items[0].copyWithCount(1));
    }

    @Nullable
    private static GenericStack toGenericStack(SizedFluidIngredient ingredient) {
        for (FluidStack fluidStack : ingredient.getFluids()) {
            if (!fluidStack.isEmpty()) {
                FluidStack copy = fluidStack.copy();
                copy.setAmount(Math.max(1, ingredient.amount()));
                return GenericStack.fromFluidStack(copy);
            }
        }
        return null;
    }

    private static boolean isCreateBasinRecipe(@Nullable Recipe<?> recipe) {
        return recipe != null && "com.simibubi.create.content.processing.basin.BasinRecipe".equals(recipe.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static List<SizedFluidIngredient> getCreateFluidIngredients(Recipe<?> recipe) {
        try {
            Object result = recipe.getClass().getMethod("getFluidIngredients").invoke(recipe);
            return result instanceof List<?> list ? (List<SizedFluidIngredient>) list : List.of();
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
    }

    private static List<ItemStack> getCreateRollableResultStacks(Recipe<?> recipe) {
        try {
            Object result = recipe.getClass().getMethod("getRollableResults").invoke(recipe);
            if (!(result instanceof List<?> list)) {
                return List.of();
            }
            List<ItemStack> stacks = new ArrayList<>();
            for (Object entry : list) {
                Object stack = entry.getClass().getMethod("getStack").invoke(entry);
                if (stack instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                    stacks.add(itemStack);
                }
            }
            return stacks;
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<FluidStack> getCreateFluidResults(Recipe<?> recipe) {
        try {
            Object result = recipe.getClass().getMethod("getFluidResults").invoke(recipe);
            return result instanceof List<?> list ? (List<FluidStack>) list : List.of();
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
    }

    @Nullable
    static GenericStack toGenericStackForBookmark(@Nullable ITypedIngredient<?> ingredient) {
        return toGenericStack(ingredient, false);
    }

    @Nullable
    private static GenericStack toGenericStack(@Nullable ITypedIngredient<?> ingredient) {
        return toGenericStack(ingredient, false);
    }

    @Nullable
    private static GenericStack toGenericStack(@Nullable ITypedIngredient<?> ingredient, boolean preserveItemAmounts) {
        if (ingredient == null) {
            return null;
        }
        GenericStack converted = convertWithAe2JeiIntegration(ingredient);
        if (converted != null) {
            return normalizeItemAmount(converted, preserveItemAmounts);
        }

        Object raw = ingredient.getIngredient();
        if (raw instanceof ItemStack stack && !stack.isEmpty()) {
            return GenericStack.fromItemStack(preserveItemAmounts ? stack.copy() : stack.copyWithCount(1));
        }
        if (raw instanceof FluidStack fluid && !fluid.isEmpty()) {
            return GenericStack.fromFluidStack(fluid.copy());
        }
        return convertMekanismChemical(raw);
    }

    private static GenericStack normalizeItemAmount(GenericStack stack, boolean preserveItemAmounts) {
        if (!preserveItemAmounts && stack.what() instanceof AEItemKey) {
            return new GenericStack(stack.what(), 1);
        }
        return stack;
    }

    @Nullable
    private static GenericStack convertWithAe2JeiIntegration(ITypedIngredient<?> ingredient) {
        try {
            Class<?> convertersClass =
                    Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters");
            Method getConverter = convertersClass.getMethod("getConverter", IIngredientType.class);
            Object converter = getConverter.invoke(null, ingredient.getType());
            if (converter == null) {
                return null;
            }
            Method getStackFromIngredient = converter.getClass().getMethod("getStackFromIngredient", Object.class);
            Object converted = getStackFromIngredient.invoke(converter, ingredient.getIngredient());
            return converted instanceof GenericStack stack ? stack : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Nullable
    private static GenericStack convertMekanismChemical(Object raw) {
        try {
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            if (!chemicalStackClass.isInstance(raw)) {
                return null;
            }
            Class<?> keyClass = Class.forName("me.ramidzkh.mekae2.ae2.MekanismKey");
            Method of = keyClass.getMethod("of", chemicalStackClass);
            Object key = of.invoke(null, raw);
            if (!(key instanceof appeng.api.stacks.AEKey aeKey)) {
                return null;
            }
            Method getAmount = chemicalStackClass.getMethod("getAmount");
            long amount = ((Number) getAmount.invoke(raw)).longValue();
            return new GenericStack(aeKey, Math.max(1, amount));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
