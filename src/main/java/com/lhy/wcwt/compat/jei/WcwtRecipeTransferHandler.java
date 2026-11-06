package com.lhy.wcwt.compat.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.parts.encoding.EncodingMode;
import appeng.util.CraftingRecipeUtil;
import com.lhy.wcwt.compat.reflect.WcwtReflect;
import com.lhy.wcwt.compat.AppliedMekanisticsCompat;
import com.lhy.wcwt.compat.CreateBasinCompat;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.compat.WcwtRecipeTransferCommon;
import com.lhy.wcwt.client.WcwtFavorites;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import com.lhy.wcwt.pull.WcwtComponentIngredientEncoding;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class WcwtRecipeTransferHandler
        implements IUniversalRecipeTransferHandler<WirelessComprehensiveWorkTerminalMenu> {
    private static Set<AEKey> cachedCraftableKeys = Set.of();
    private static long cachedCraftableKeysTick = Long.MIN_VALUE;
    private static int cachedCraftableKeysRepoId;

    private final IRecipeTransferHandlerHelper transferHelper;

    public WcwtRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }

    @Override
    public Class<? extends WirelessComprehensiveWorkTerminalMenu> getContainerClass() {
        return WirelessComprehensiveWorkTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<WirelessComprehensiveWorkTerminalMenu>> getMenuType() {
        return Optional.of(ModMenus.WCWT_MENU_TYPE);
    }

    @Override
    public IRecipeTransferError transferRecipe(WirelessComprehensiveWorkTerminalMenu menu, Object recipe,
                                               IRecipeSlotsView recipeSlots, Player player,
                                               boolean maxTransfer, boolean doTransfer) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return null;
        }
        if (com.lhy.wcwt.compat.UselessModCompat.isRecipe(recipe)) {
            var logic = menu.getMenuHost().getLogic();
            if (!doTransfer) {
                // JEI previews every visible recipe each tick. The preview only needs the slot counts, which
                // never change, so the full transfer (it sorts the whole client repo) is built once per recipe.
                int[] slotCounts = uselessRecipeSlotCounts(recipe, menu);
                if (slotCounts == null) return transferHelper.createInternalError();
                if (slotCounts[0] > logic.getEncodedInputInv().size()
                        || slotCounts[1] > logic.getEncodedOutputInv().size()) {
                    return transferHelper.createUserErrorWithTooltip(net.minecraft.network.chat.Component
                            .translatable("gui.useless_mod.omniversal_pattern.too_many_slots"));
                }
                return new WcwtEncodingRecipeTransferError(
                        findCraftableEncodingSlots(menu, recipeSlots, Integer.MAX_VALUE));
            }
            var transfer = com.lhy.wcwt.compat.UselessModCompat.transfer(recipe, menu);
            if (transfer == null) return transferHelper.createInternalError();
            if (transfer.inputs().size() > logic.getEncodedInputInv().size()
                    || transfer.outputs().size() > logic.getEncodedOutputInv().size()) {
                return transferHelper.createUserErrorWithTooltip(net.minecraft.network.chat.Component
                        .translatable("gui.useless_mod.omniversal_pattern.too_many_slots"));
            }
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, EncodingMode.PROCESSING);
            PacketDistributor.sendToServer(new com.lhy.wcwt.network.OmniversalPatternTransferPacket(
                    menu.containerId, transfer.selection(), new JeiCraftingTransferPacket(
                            transfer.inputs(), transfer.outputs(), false, EncodingMode.PROCESSING)));
            return null;
        }
        if (WcwtMultiblockTransferCompat.isSupportedRecipe(recipe, recipeSlots)) {
            if (!doTransfer) {
                // Building the transfer data sorts the whole client repo; the preview does not need it.
                return new WcwtEncodingRecipeTransferError(
                        findCraftableEncodingSlots(menu, recipeSlots, Integer.MAX_VALUE));
            }
            var transfer = WcwtMultiblockTransferCompat.buildTransferData(menu, recipe, recipeSlots);
            if (transfer == null) return transferHelper.createInternalError();
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, EncodingMode.PROCESSING);
            PacketDistributor.sendToServer(new JeiCraftingTransferPacket(
                    transfer.inputs(), transfer.outputs(), false, EncodingMode.PROCESSING));
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
            inputs = compactProcessingIngredientOrder(inputs);
            outputs = compactProcessingIngredientOrder(outputs);
        }
        if (inputs.stream().allMatch(Objects::isNull) && outputs.stream().allMatch(Objects::isNull)) {
            return null;
        }

        if (doTransfer) {
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, mode);
            updateEaepProviderSearchKey(recipe, minecraftRecipe, mode);
            PacketDistributor.sendToServer(new JeiCraftingTransferPacket(inputs, outputs, false, mode));
        }
        return null;
    }

    /**
     * JEI encoding preview: follow AE2 original behavior and only highlight inputs whose candidates are craftable.
     */
    public static List<IRecipeSlotView> findCraftableEncodingSlots(WirelessComprehensiveWorkTerminalMenu menu,
                                                                   IRecipeSlotsView slotsView,
                                                                   int maxInputSlots) {
        var craftableKeys = craftableKeysForTick(menu);
        if (craftableKeys.isEmpty()) {
            return List.of();
        }

        var stream = slotsView.getSlotViews(RecipeIngredientRole.INPUT).stream();
        if (maxInputSlots < Integer.MAX_VALUE) {
            stream = stream.limit(maxInputSlots);
        }
        return stream
                .filter(slotView -> slotView.getAllIngredients().anyMatch(ingredient -> {
                    AEKey key = toPreviewKey(ingredient);
                    return key != null && craftableKeys.contains(key);
                }))
                .toList();
    }

    private static final Map<Object, int[]> USELESS_RECIPE_SLOT_COUNTS = new WeakHashMap<>();
    private static final int[] USELESS_RECIPE_INVALID = new int[0];

    /** Input and output slot counts of a Useless Mod alloy furnace recipe; {@code null} when it cannot transfer. */
    @Nullable
    private static int[] uselessRecipeSlotCounts(Object recipe, WirelessComprehensiveWorkTerminalMenu menu) {
        int[] counts = USELESS_RECIPE_SLOT_COUNTS.get(recipe);
        if (counts == null) {
            var transfer = com.lhy.wcwt.compat.UselessModCompat.transfer(recipe, menu);
            counts = transfer == null ? USELESS_RECIPE_INVALID
                    : new int[] {transfer.inputs().size(), transfer.outputs().size()};
            USELESS_RECIPE_SLOT_COUNTS.put(recipe, counts);
        }
        return counts.length == 0 ? null : counts;
    }

    private static Set<AEKey> craftableKeysForTick(WirelessComprehensiveWorkTerminalMenu menu) {
        var repo = menu.getClientRepo();
        if (repo == null) {
            return Set.of();
        }
        long tick = currentClientTick();
        int repoId = System.identityHashCode(repo);
        if (cachedCraftableKeysTick == tick && cachedCraftableKeysRepoId == repoId) {
            return cachedCraftableKeys;
        }
        Set<AEKey> keys = new HashSet<>();
        for (GridInventoryEntry entry : repo.getAllEntries()) {
            if (entry.getWhat() != null && entry.isCraftable()) {
                keys.add(entry.getWhat());
            }
        }
        cachedCraftableKeys = keys.isEmpty() ? Set.of() : keys;
        cachedCraftableKeysTick = tick;
        cachedCraftableKeysRepoId = repoId;
        return cachedCraftableKeys;
    }

    private static long currentClientTick() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    @Nullable
    private static AEKey toPreviewKey(@Nullable ITypedIngredient<?> ingredient) {
        if (ingredient == null) {
            return null;
        }
        Object raw = ingredient.getIngredient();
        if (raw instanceof ItemStack stack && !stack.isEmpty()) {
            return AEItemKey.of(stack);
        }
        if (raw instanceof FluidStack fluid && !fluid.isEmpty()) {
            return AEFluidKey.of(fluid);
        }
        GenericStack converted = Ae2JeiIntegrationCompat.convert(ingredient);
        if (converted != null) {
            return converted.what();
        }
        GenericStack chemical = AppliedMekanisticsCompat.fromChemicalStack(raw);
        return chemical == null ? null : chemical.what();
    }

    public static void updateEaepProviderSearchKey(Object recipeBase, @Nullable Recipe<?> recipe, EncodingMode mode) {
        WcwtRecipeTransferCommon.updateEaepProviderSearchKey(recipeBase, recipe, mode);
    }

    static EncodingMode getTransferMode(@Nullable Object recipeObject, IRecipeSlotsView slotsView) {
        Recipe<?> recipe = recipeObject instanceof RecipeHolder<?> holder ? holder.value()
                : recipeObject instanceof Recipe<?> directRecipe ? directRecipe
                : null;
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
        var recipeResults = WcwtComponentIngredientEncoding.recipeResults(menu.getPlayer().level());
        if (recipe != null && recipeHolder != null) {
            List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                    .limit(mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE)
                    .toList();
            if (mode == EncodingMode.CRAFTING) {
                var ingredients = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
                List<@Nullable GenericStack> resolved = new ArrayList<>(ingredients.size());
                for (int slot = 0; slot < ingredients.size(); slot++) {
                    List<ItemStack> slotAlternatives = collectPerSlotAlternatives(inputSlots, slot);
                    resolved.add(toBestGenericStack(priorityContext, ingredients.get(slot), slotAlternatives, recipeResults));
                }
                return resolved;
            }
            var ingredients = CraftingRecipeUtil.getIngredients(recipe);
            List<@Nullable GenericStack> resolved = new ArrayList<>(ingredients.size());
            for (int slot = 0; slot < ingredients.size(); slot++) {
                // For non-crafting modes (smithing/stonecutting), JEI slots may not map 1:1
                // to recipe ingredients. Pass empty alternatives to avoid cross-slot contamination.
                resolved.add(toBestGenericStack(priorityContext, ingredients.get(slot), List.of()));
            }
            return resolved;
        }

        return collectStacksPreservingSlots(priorityContext, recipeSlots, RecipeIngredientRole.INPUT,
                mode == EncodingMode.CRAFTING ? 9 : Integer.MAX_VALUE);
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
                                                                             int limit) {
        return collectStacksPreservingSlots(priorityContext, recipeSlots, role, limit, false);
    }

    private static List<@Nullable GenericStack> collectStacksPreservingSlots(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                                             IRecipeSlotsView recipeSlots,
                                                                             RecipeIngredientRole role,
                                                                             int limit,
                                                                             boolean preserveDisplayedItemCounts) {
        return recipeSlots.getSlotViews(role).stream()
                .limit(limit)
                .map(slotView -> toPreferredGenericStack(priorityContext, slotView, preserveDisplayedItemCounts))
                .toList();
    }

    private static List<@Nullable GenericStack> collectProcessingInputs(WirelessComprehensiveWorkTerminalMenu menu,
                                                                        @Nullable Recipe<?> recipe,
                                                                        IRecipeSlotsView recipeSlots) {
        var priorityContext = createPriorityContext(menu);
        List<@Nullable GenericStack> fromDisplayedSlots = collectStacksPreservingSlots(
                priorityContext, recipeSlots, RecipeIngredientRole.INPUT, Integer.MAX_VALUE, true);
        if (containsAnyStack(fromDisplayedSlots)) {
            return fromDisplayedSlots;
        }

        // Create's BasinRecipe may only be referenced once Create is loaded; see CreateBasinCompat.
        List<@Nullable GenericStack> basinInputs = CreateBasinCompat.collectInputs(recipe,
                ingredient -> toBestGenericStack(priorityContext, ingredient, List.of()),
                fluidIngredient -> toGenericStack(fluidIngredient));
        if (basinInputs != null) {
            return basinInputs;
        }
        return fromDisplayedSlots;
    }

    private static List<@Nullable GenericStack> collectProcessingOutputs(@Nullable Recipe<?> recipe,
                                                                         IRecipeSlotsView recipeSlots) {
        List<@Nullable GenericStack> fromDisplayedSlots = collectStacksPreservingSlots(
                WcwtIngredientPriorities.PriorityContext.EMPTY, recipeSlots, RecipeIngredientRole.OUTPUT, Integer.MAX_VALUE,
                true);
        if (containsAnyStack(fromDisplayedSlots)) {
            return fromDisplayedSlots;
        }

        List<@Nullable GenericStack> basinOutputs = CreateBasinCompat.collectOutputs(recipe);
        if (basinOutputs != null) {
            return basinOutputs;
        }
        return fromDisplayedSlots;
    }

    private static boolean containsAnyStack(List<@Nullable GenericStack> stacks) {
        return stacks.stream().anyMatch(Objects::nonNull);
    }

    @Nullable
    public static GenericStack toBestGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                  Ingredient ingredient,
                                                  List<ItemStack> visibleAlternatives) {
        return toBestGenericStack(priorityContext, ingredient, visibleAlternatives,
                WcwtComponentIngredientEncoding.recipeResults(Minecraft.getInstance().level));
    }

    @Nullable
    private static GenericStack toBestGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                   Ingredient ingredient,
                                                   List<ItemStack> visibleAlternatives,
                                                   java.util.function.Supplier<List<ItemStack>> recipeResults) {
        if (ingredient.isEmpty()) {
            return null;
        }

        if (WcwtComponentIngredientEncoding.isPartialComponentIngredient(ingredient)) {
            ItemStack selected = WcwtComponentIngredientEncoding.resolve(priorityContext, ingredient,
                    visibleAlternatives, recipeResults);
            return selected.isEmpty() ? null : GenericStack.fromItemStack(selected.copyWithCount(1));
        }

        if (priorityContext.hasFavoritePriorities()) {
            ItemStack favorited = WcwtRecipeTransferCommon.chooseFavoritedItem(
                    ingredient, visibleAlternatives, priorityContext.favoritePriorities());
            if (!favorited.isEmpty()) {
                return GenericStack.fromItemStack(favorited.copyWithCount(1));
            }
        }

        if (WcwtClientConfig.preferJeiBookmarksForPatternEncoding() && priorityContext.hasBookmarkPriorities()) {
            ItemStack bookmarked = WcwtJeiBookmarkKeys.chooseBookmarkedItem(
                    ingredient, visibleAlternatives, priorityContext.bookmarkPriorities());
            if (!bookmarked.isEmpty()) {
                return GenericStack.fromItemStack(bookmarked.copyWithCount(1));
            }
        }
        ItemStack best = WcwtIngredientPriorities.chooseBestItemForEncoding(priorityContext, ingredient, visibleAlternatives);
        return best.isEmpty() ? null : GenericStack.fromItemStack(best.copyWithCount(1));
    }

    /**
     * Collect visible item alternatives for a single JEI input slot.
     * Returns only the items visible in this specific slot, preventing
     * cross-slot NBT contamination when resolving recipe ingredients.
     */
    private static List<ItemStack> collectPerSlotAlternatives(List<IRecipeSlotView> inputSlots, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
            return List.of();
        }
        return inputSlots.get(slotIndex).getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    @Nullable
    static GenericStack toPreferredGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                        IRecipeSlotView slotView) {
        return toPreferredGenericStack(priorityContext, slotView, false);
    }

    @Nullable
    static GenericStack toPreferredGenericStack(WcwtIngredientPriorities.PriorityContext priorityContext,
                                                        IRecipeSlotView slotView,
                                                        boolean preserveDisplayedItemCounts) {
        if (priorityContext.hasFavoritePriorities()) {
            GenericStack favoritedItem = WcwtRecipeTransferCommon.chooseFavoritedItemStack(
                    slotView.getItemStacks().toList(), priorityContext.favoritePriorities(),
                    preserveDisplayedItemCounts);
            if (favoritedItem != null) {
                return favoritedItem;
            }
        }

        if (WcwtClientConfig.preferJeiBookmarksForPatternEncoding() && priorityContext.hasBookmarkPriorities()) {
            GenericStack bookmarked = WcwtJeiBookmarkKeys.chooseBookmarkedStack(
                    slotView, priorityContext.bookmarkPriorities());
            if (bookmarked != null) {
                return preserveDisplayedItemCounts ? withDisplayedItemCount(slotView, bookmarked) : bookmarked;
            }
        }

        List<ItemStack> itemCandidates = slotView.getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();

        if (!itemCandidates.isEmpty()) {
            List<GenericStack> itemStacks = itemCandidates.stream()
                    .map(GenericStack::fromItemStack)
                    .filter(Objects::nonNull)
                    .toList();
            GenericStack selected = WcwtIngredientPriorities.chooseBestGenericStackForEncoding(priorityContext,
                    itemStacks);
            if (selected != null) {
                return preserveDisplayedItemCounts
                        ? withDisplayedItemCount(slotView, selected)
                        : new GenericStack(selected.what(), 1);
            }
        }

        List<GenericStack> candidates = slotView.getAllIngredients()
                .map(WcwtRecipeTransferHandler::toGenericStack)
                .filter(Objects::nonNull)
                .toList();
        GenericStack selected = WcwtIngredientPriorities.chooseBestGenericStackForEncoding(priorityContext, candidates);
        return preserveDisplayedItemCounts ? withDisplayedItemCount(slotView, selected) : selected;
    }

    private static GenericStack withDisplayedItemCount(IRecipeSlotView slotView, @Nullable GenericStack selected) {
        if (selected == null || !(selected.what() instanceof AEItemKey itemKey)) {
            return selected;
        }
        for (ItemStack displayed : slotView.getItemStacks().toList()) {
            if (!displayed.isEmpty() && itemKey.matches(displayed)) {
                GenericStack stack = GenericStack.fromItemStack(displayed.copy());
                return stack != null ? stack : selected;
            }
        }
        return selected;
    }

    static WcwtIngredientPriorities.PriorityContext createPriorityContext(WirelessComprehensiveWorkTerminalMenu menu) {
        Map<appeng.api.stacks.AEKey, Integer> bookmarkPriorities = WcwtClientConfig.preferJeiBookmarksForPatternEncoding()
                ? WcwtJeiBookmarkKeys.getBookmarkPriorities()
                : Map.of();
        return WcwtIngredientPriorities.createContext(menu, bookmarkPriorities, getWcwtFavoritePriorities());
    }

    private static Map<appeng.api.stacks.AEKey, Integer> getWcwtFavoritePriorities() {
        if (!WcwtClientConfig.preferWcwtFavoritesForRecipeTransfer()) {
            return Map.of();
        }
        var favorites = WcwtFavorites.getFavoritedKeys();
        if (favorites.isEmpty()) {
            return Map.of();
        }
        Map<appeng.api.stacks.AEKey, Integer> priorities = new java.util.LinkedHashMap<>(favorites.size());
        int index = 0;
        for (appeng.api.stacks.AEKey key : favorites) {
            if (key != null) {
                priorities.putIfAbsent(key, index);
            }
            index++;
        }
        return priorities;
    }

    /** JEI's own info pages are library internals, not API; matched by name so a JEI that moves them cannot break every preview. */
    private static final String[] JEI_INFO_RECIPE_CLASSES = {
            "mezz.jei.library.plugins.jei.tags.ITagInfoRecipe",
            "mezz.jei.library.plugins.jei.info.IngredientInfoRecipe"};

    public static boolean shouldSkipTransferAnalysis(Object recipe) {
        for (String className : JEI_INFO_RECIPE_CLASSES) {
            if (WcwtReflect.findClass("jei", className).map(type -> type.isInstance(recipe)).orElse(false)) {
                return true;
            }
        }
        return false;
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
    public static GenericStack toGenericStack(SizedFluidIngredient ingredient) {
        for (FluidStack fluidStack : ingredient.getFluids()) {
            if (!fluidStack.isEmpty()) {
                FluidStack copy = fluidStack.copy();
                copy.setAmount(Math.max(1, ingredient.amount()));
                return GenericStack.fromFluidStack(copy);
            }
        }
        return null;
    }

    @Nullable
    static GenericStack toGenericStackForBookmark(@Nullable ITypedIngredient<?> ingredient) {
        return toGenericStack(ingredient);
    }

    @Nullable
    /**
     * Converts any JEI ingredient type that WCWT can transfer into an AE2 stack.
     * Package-private so the advanced coding ghost handler can use the exact same
     * integration and fallback conversions as recipe transfer.
     */
    static GenericStack toGenericStack(@Nullable ITypedIngredient<?> ingredient) {
        if (ingredient == null) {
            return null;
        }
        GenericStack converted = Ae2JeiIntegrationCompat.convert(ingredient);
        if (converted != null) {
            return converted;
        }

        Object raw = ingredient.getIngredient();
        if (raw instanceof ItemStack stack && !stack.isEmpty()) {
            return GenericStack.fromItemStack(stack.copyWithCount(1));
        }
        if (raw instanceof FluidStack fluid && !fluid.isEmpty()) {
            return GenericStack.fromFluidStack(fluid.copy());
        }
        return AppliedMekanisticsCompat.fromChemicalStack(raw);
    }
}
