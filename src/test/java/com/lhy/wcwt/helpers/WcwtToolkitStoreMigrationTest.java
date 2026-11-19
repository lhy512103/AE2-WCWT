package com.lhy.wcwt.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.util.inv.AppEngInternalInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WcwtToolkitStoreMigrationTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        Bootstrap.bootStrap();
    }

    @Test
    void legacyItemsKeepTheirSlotWhenItIsFree() {
        var inventory = new AppEngInternalInventory(3);
        List<ItemStack> overflow = new ArrayList<>();

        WcwtToolkitStore.mergeLegacyToolkit(
                List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_PICKAXE)), inventory, overflow::add);

        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertTrue(inventory.getStackInSlot(1).is(Items.IRON_PICKAXE));
        assertTrue(overflow.isEmpty());
    }

    @Test
    void occupiedSlotMovesLegacyItemToFirstFreeSlot() {
        var inventory = new AppEngInternalInventory(3);
        inventory.setItemDirect(0, new ItemStack(Items.SHEARS));
        List<ItemStack> overflow = new ArrayList<>();

        WcwtToolkitStore.mergeLegacyToolkit(List.of(new ItemStack(Items.IRON_AXE)), inventory, overflow::add);

        assertTrue(inventory.getStackInSlot(0).is(Items.SHEARS));
        assertTrue(inventory.getStackInSlot(1).is(Items.IRON_AXE));
        assertTrue(overflow.isEmpty());
    }

    @Test
    void itemsThatDoNotFitAreHandedBackInsteadOfDeleted() {
        var inventory = new AppEngInternalInventory(1);
        inventory.setItemDirect(0, new ItemStack(Items.SHEARS));
        List<ItemStack> overflow = new ArrayList<>();

        WcwtToolkitStore.mergeLegacyToolkit(
                List.of(new ItemStack(Items.IRON_AXE), ItemStack.EMPTY, new ItemStack(Items.IRON_SHOVEL)),
                inventory, overflow::add);

        assertTrue(inventory.getStackInSlot(0).is(Items.SHEARS));
        assertEquals(2, overflow.size());
        assertTrue(overflow.get(0).is(Items.IRON_AXE));
        assertTrue(overflow.get(1).is(Items.IRON_SHOVEL));
    }
}
