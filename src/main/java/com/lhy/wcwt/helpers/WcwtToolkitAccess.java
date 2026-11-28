package com.lhy.wcwt.helpers;

import appeng.api.upgrades.IUpgradeableItem;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.WUTHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WcwtToolkitAccess {
    public static final int HOTBAR_SIZE = 9;
    public static final int HOTBAR_SLOTS = HOTBAR_SIZE * 2;

    private WcwtToolkitAccess() {
    }

    public static boolean hasToolkitCard(Player player) {
        // The terminal being opened and the terminal supplying the extra hotbars can be different.
        if (hasCarriedToolkitTerminal(player, -1)) {
            return true;
        }
        return player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                && menu.getMenuHost() != null && hasToolkitCard(menu.getMenuHost().getItemStack());
    }

    public static boolean hasToolkitCard(ItemStack terminal) {
        return !terminal.isEmpty()
                && terminal.getItem() instanceof IUpgradeableItem upgradeable
                && upgradeable.getUpgrades(terminal).isInstalled(ModItems.TOOLKIT_CARD.get());
    }

    public static boolean canReplaceOffhand(Player player, ItemStack replacement) {
        var terminal = WTDefinition.of("wcwt");
        return !isToolkitTerminal(player.getOffhandItem(), terminal)
                || isToolkitTerminal(replacement, terminal)
                || hasCarriedToolkitTerminal(player, Inventory.SLOT_OFFHAND);
    }

    private static boolean hasCarriedToolkitTerminal(Player player, int excludedSlot) {
        var terminal = WTDefinition.of("wcwt");
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot != excludedSlot && isToolkitTerminal(inventory.getItem(slot), terminal)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isToolkitTerminal(ItemStack stack, WTDefinition terminal) {
        return WUTHandler.hasTerminal(stack, terminal) && hasToolkitCard(stack);
    }

    /**
     * Delegates to ae2wtlib's own lookup, which scans all player inventory slots including the
     * offhand.
     */
    public static ItemStack findTerminal(Player player) {
        var locator = WUTHandler.findTerminal(player, WTDefinition.of("wcwt"));
        return locator == null ? ItemStack.EMPTY : locator.locateItem(player);
    }
}
