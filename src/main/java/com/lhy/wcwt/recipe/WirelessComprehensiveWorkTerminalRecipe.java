package com.lhy.wcwt.recipe;

import appeng.api.config.Actionable;
import appeng.api.upgrades.IUpgradeableItem;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.init.ModRecipes;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.mojang.serialization.MapCodec;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WirelessComprehensiveWorkTerminalRecipe extends CustomRecipe {
    public static final WirelessComprehensiveWorkTerminalRecipe INSTANCE =
            new WirelessComprehensiveWorkTerminalRecipe();
    public static final RecipeSerializer<WirelessComprehensiveWorkTerminalRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.unit(INSTANCE));

    private static final List<Identifier> REQUIRED_TERMINALS = List.of(
            Identifier.fromNamespaceAndPath("ae2wtlib", "wireless_pattern_encoding_terminal"),
            Identifier.fromNamespaceAndPath("ae2", "wireless_crafting_terminal"),
            Identifier.fromNamespaceAndPath("ae2wtlib", "wireless_pattern_access_terminal"),
            Identifier.fromNamespaceAndPath("ae2", "wireless_terminal"));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != REQUIRED_TERMINALS.size()) {
            return false;
        }

        Set<Identifier> found = new HashSet<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!REQUIRED_TERMINALS.contains(itemId) || !found.add(itemId)) {
                return false;
            }
        }
        return found.size() == REQUIRED_TERMINALS.size();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = new ItemStack(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get());
        WirelessComprehensiveWorkTerminalItem resultItem =
                (WirelessComprehensiveWorkTerminalItem) result.getItem();
        List<ItemStack> leftovers = mergeUpgrades(input, result);

        resultItem.updatePowerMultiplier(result);
        mergeTerminalState(input, result, resultItem, leftovers);

        if (!leftovers.isEmpty()) {
            result.set(ModComponents.CRAFTING_UPGRADE_LEFTOVERS.get(), ItemContainerContents.fromItems(leftovers));
        }
        return result;
    }

    private static void mergeTerminalState(CraftingInput input, ItemStack result,
            WirelessComprehensiveWorkTerminalItem resultItem, List<ItemStack> leftovers) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack terminal = input.getItem(i);
            if (terminal.isEmpty() || !(terminal.getItem() instanceof ItemWT terminalItem)) {
                continue;
            }

            resultItem.injectAEPower(
                    result,
                    terminalItem.getAECurrentPower(terminal),
                    Actionable.MODULATE);

            WcwtTerminalMergeSupport.applyTerminalComponents(result, terminal, leftovers);
        }
    }

    private static List<ItemStack> mergeUpgrades(CraftingInput input, ItemStack result) {
        WirelessComprehensiveWorkTerminalItem resultItem =
                (WirelessComprehensiveWorkTerminalItem) result.getItem();
        var targetUpgrades = resultItem.getUpgrades(result);
        List<ItemStack> leftovers = new ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            ItemStack terminal = input.getItem(i);
            if (terminal.isEmpty() || !(terminal.getItem() instanceof IUpgradeableItem upgradeable)) {
                continue;
            }
            for (ItemStack upgrade : upgradeable.getUpgrades(terminal)) {
                if (!upgrade.isEmpty()) {
                    ItemStack remaining = targetUpgrades.addItems(upgrade.copy());
                    if (!remaining.isEmpty()) {
                        leftovers.add(remaining);
                    }
                }
            }
        }
        return leftovers;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }
}
