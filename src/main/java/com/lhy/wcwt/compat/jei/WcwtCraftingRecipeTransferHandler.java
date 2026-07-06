package com.lhy.wcwt.compat.jei;

import appeng.core.localization.ItemModText;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WcwtCraftingRecipeTransferHandler
        implements IRecipeTransferHandler<WirelessComprehensiveWorkTerminalMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper transferHelper;

    public WcwtCraftingRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }

    @Override
    public Class<WirelessComprehensiveWorkTerminalMenu> getContainerClass() {
        return WirelessComprehensiveWorkTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<WirelessComprehensiveWorkTerminalMenu>> getMenuType() {
        return Optional.of(ModMenus.WCWT_MENU.get());
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(WirelessComprehensiveWorkTerminalMenu menu,
                                               RecipeHolder<CraftingRecipe> recipeHolder,
                                               IRecipeSlotsView recipeSlots,
                                               Player player,
                                               boolean maxTransfer,
                                               boolean doTransfer) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return null;
        }
        var recipe = recipeHolder.value();
        if (recipe.getType() != RecipeType.CRAFTING) {
            return transferHelper.createInternalError();
        }

        if (recipe.getIngredients().isEmpty()) {
            return transferHelper.createUserErrorWithTooltip(ItemModText.INCOMPATIBLE_RECIPE.text());
        }

        if (!recipe.canCraftInDimensions(3, 3)) {
            return transferHelper.createUserErrorWithTooltip(ItemModText.RECIPE_TOO_LARGE.text());
        }

        boolean craftingToManualGrid = menu.getMenuHost() != null && menu.getMenuHost().isCraftingGridLocked();
        if (!craftingToManualGrid) {
            if (doTransfer) {
                WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, EncodingMode.CRAFTING);
                WcwtRecipeTransferHandler.updateEaepProviderSearchKey(recipeHolder, recipe, EncodingMode.CRAFTING);
                PacketDistributor.sendToServer(new JeiCraftingTransferPacket(
                        WcwtRecipeTransferHandler.collectCraftingLikeInputs(
                                menu, recipeHolder, recipe, recipeSlots, EncodingMode.CRAFTING),
                        java.util.List.of(),
                        false,
                        EncodingMode.CRAFTING));
            } else {
                var craftableSlots = WcwtRecipeTransferHandler.findCraftableEncodingSlots(menu, recipeSlots, 9);
                return new WcwtEncodingRecipeTransferError(craftableSlots);
            }
            return null;
        }

        boolean craftMissing = Screen.hasControlDown();
        var slotToIngredientMap = transferHelper.getGuiSlotIndexToIngredientMap(recipeHolder);
        CraftingTermMenu.MissingIngredientSlots missingSlots = menu.findMissingIngredients(slotToIngredientMap);

        if (missingSlots.missingSlots().size() == slotToIngredientMap.size()) {
            var inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            var missingSlotViews = missingSlots.missingSlots().stream()
                    .map(idx -> idx < inputSlots.size() ? inputSlots.get(idx) : null)
                    .filter(Objects::nonNull)
                    .toList();
            return transferHelper.createUserErrorForMissingSlots(ItemModText.NO_ITEMS.text(), missingSlotViews);
        }

        // Match ae2jeiintegration UseCraftingRecipeTransfer: preview red / blue slot overlays before click.
        if (!doTransfer && missingSlots.totalSize() != 0) {
            List<IRecipeSlotView> gridSlots =
                    recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream().limit(9).toList();
            return new WcwtPartialRecipeTransferError(missingSlots, gridSlots);
        }

        if (doTransfer) {
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, EncodingMode.CRAFTING);
            // Use WcwtPullRecipeTransfer for all locked-grid cases, including CRAFTING mode.
            // CraftingHelper.performTransfer uses AE2's generic item matching (most abundant first,
            // no NBT preference), which can pull wrong NBT variants. WcwtPullRecipeTransfer
            // respects NBT-specific alternatives from JEI slot views, preserving exact item matching.
            return WcwtPullRecipeTransfer.transfer(menu, recipeHolder, recipeSlots, player, maxTransfer, true,
                    transferHelper, false);
        }

        return null;
    }
}
