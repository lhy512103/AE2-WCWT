package com.lhy.wcwt.recipe;

import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.init.ModRecipeSerializers;
import com.lhy.wcwt.universal.WcwtItemIds;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class WcwtUniversalTerminalUpgradeRecipe extends CustomRecipe {
    public WcwtUniversalTerminalUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        ItemStack wcwt = ItemStack.EMPTY;
        int terminals = 0;
        Set<net.minecraft.world.item.Item> seen = new HashSet<>();
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (WcwtUniversalTerminals.isWcwt(stack)) {
                if (!wcwt.isEmpty()) {
                    return false;
                }
                wcwt = stack;
            } else if (WcwtUniversalTerminals.isMergeableTerminal(stack)) {
                terminals++;
                if (!seen.add(stack.getItem())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (wcwt.isEmpty() || terminals == 0) {
            return false;
        }
        ItemStack result = wcwt.copy();
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (WcwtUniversalTerminals.isMergeableTerminal(stack)
                    && !WcwtUniversalTerminals.addTerminal(result, stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registryAccess) {
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (WcwtUniversalTerminals.isWcwt(stack)) {
                result = stack.copy();
                result.setCount(1);
                break;
            }
        }
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (WcwtUniversalTerminals.isMergeableTerminal(stack)) {
                WcwtUniversalTerminals.addTerminal(result, stack);
            }
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.UNIVERSAL_TERMINAL_UPGRADE.get();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get()));

        java.util.ArrayList<ItemStack> terminals = new java.util.ArrayList<>();
        for (ResourceLocation id : WcwtItemIds.MERGEABLE_TERMINALS) {
            var item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                terminals.add(new ItemStack(item));
            }
        }
        if (!terminals.isEmpty()) {
            ingredients.add(Ingredient.of(terminals.stream()));
        }
        return ingredients;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        ItemStack result = new ItemStack(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get());
        for (ResourceLocation id : WcwtItemIds.MERGEABLE_TERMINALS) {
            var item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                WcwtUniversalTerminals.addTerminal(result, new ItemStack(item));
            }
        }
        return result;
    }
}
