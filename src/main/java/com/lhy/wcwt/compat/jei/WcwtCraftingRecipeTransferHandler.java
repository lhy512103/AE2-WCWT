package com.lhy.wcwt.compat.jei;

import appeng.core.localization.ItemModText;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.WcwtManualWorkspaceRecipeSwitch;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

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
        return Optional.of(ModMenus.WCWT_MENU_TYPE);
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

        if (doTransfer) {
            WcwtManualWorkspaceRecipeSwitch.switchForTransfer(menu, EncodingMode.CRAFTING);
            WcwtRecipeTransferHandler.updateEaepProviderSearchKey(recipeHolder, recipe, EncodingMode.CRAFTING);
            PacketDistributor.sendToServer(new JeiCraftingTransferPacket(
                    WcwtRecipeTransferHandler.collectCraftingLikeInputs(
                            menu, recipeHolder, recipe, recipeSlots, EncodingMode.CRAFTING),
                    java.util.List.of(),
                    false,
                    EncodingMode.CRAFTING));
            return null;
        }
        var craftableSlots = WcwtRecipeTransferHandler.findCraftableEncodingSlots(menu, recipeSlots, 9);
        return new WcwtEncodingRecipeTransferError(craftableSlots);
    }
}
