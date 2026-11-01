package com.lhy.wcwt.helpers;

import appeng.api.upgrades.IUpgradeableItem;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.WUTHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class WcwtToolkitAccess {
    public static final int HOTBAR_SIZE = 9;
    public static final int HOTBAR_SLOTS = HOTBAR_SIZE * 2;

    private WcwtToolkitAccess() {
    }

    public static boolean hasToolkitCard(Player player) {
        var host = existingHost(player);
        if (host != null) {
            return hasToolkitCard(host.getItemStack());
        }
        return hasToolkitCard(findTerminal(player));
    }

    public static boolean hasToolkitCard(ItemStack terminal) {
        return !terminal.isEmpty()
                && terminal.getItem() instanceof IUpgradeableItem upgradeable
                && upgradeable.getUpgrades(terminal).isInstalled(ModItems.TOOLKIT_CARD.get());
    }

    @Nullable
    private static WirelessComprehensiveWorkTerminalMenuHost existingHost(Player player) {
        return player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                ? menu.getMenuHost() : null;
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
