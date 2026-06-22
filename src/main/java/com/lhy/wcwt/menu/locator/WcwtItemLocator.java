package com.lhy.wcwt.menu.locator;

import appeng.menu.locator.MenuLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface WcwtItemLocator extends MenuLocator {
    ItemStack locateItem(Player player);

    default boolean storeItem(Player player, ItemStack stack) {
        return false;
    }

    default Integer inventorySlot() {
        return null;
    }
}
