package com.lhy.wcwt.compat.jei;

import appeng.core.localization.ItemModText;
import appeng.integration.modules.jeirei.CraftingHelper;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import com.lhy.wcwt.network.ManualWorkspaceModePacket;
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
import net.minecraft.world.item.crafting.RecipeType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WcwtCraftingRecipeTransferHandler
        implements IRecipeTransferHandler<WirelessComprehensiveWorkTerminalMenu, CraftingRecipe> {

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
    public mezz.jei.api.recipe.RecipeType<CraftingRecipe> getRecipeType() {
        return mezz.jei.api.recipe.RecipeType.create(
                "minecraft",
                "crafting",
                CraftingRecipe.class);
    }

    @Override
    public IRecipeTransferError transferRecipe(WirelessComprehensiveWorkTerminalMenu menu,
                                               CraftingRecipe recipe,
                                               IRecipeSlotsView recipeSlots,
                                               Player player,
                                               boolean maxTransfer,
                                               boolean doTransfer) {
        if (!WcwtClientConfig.enableRecipePullTransfer()) {
            return null;
        }
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
                WcwtRecipeTransferHandler.updateEaepProviderSearchKey(recipe, recipe, EncodingMode.CRAFTING);
                ModNetworking.sendToServer(new JeiCraftingTransferPacket(
                        WcwtRecipeTransferHandler.collectCraftingLikeInputs(
                                menu, null, recipe, recipeSlots, EncodingMode.CRAFTING),
                        java.util.List.of(),
                        false,
                        EncodingMode.CRAFTING));
            } else {
                var craftableSlots = WcwtRecipeTransferHandler.findCraftableEncodingSlots(menu, recipeSlots, 9);
                return new WcwtEncodingRecipeTransferError(craftableSlots);
            }
            return null;
        }

        if (doTransfer) {
            forceManualCraftingWorkspace(menu);
        }

        return WcwtPullRecipeTransfer.transfer(menu, recipe, recipeSlots, player, maxTransfer, doTransfer,
                transferHelper);
    }

    private static void forceManualCraftingWorkspace(WirelessComprehensiveWorkTerminalMenu menu) {
        if (menu.getManualWorkspaceMode() == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING) {
            return;
        }
        menu.setManualWorkspaceMode(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING);
        ModNetworking.sendToServer(new ManualWorkspaceModePacket(
                WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING.ordinal()));
    }

    private static Map<Integer, net.minecraft.world.item.crafting.Ingredient> createCraftingSlotMap(CraftingRecipe recipe) {
        var ingredients = appeng.util.CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
        Map<Integer, net.minecraft.world.item.crafting.Ingredient> result = new LinkedHashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            var ingredient = ingredients.get(i);
            if (!ingredient.isEmpty()) {
                result.put(i, ingredient);
            }
        }
        return result;
    }
}
