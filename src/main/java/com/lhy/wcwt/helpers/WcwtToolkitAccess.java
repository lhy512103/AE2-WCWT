package com.lhy.wcwt.helpers;

import appeng.api.upgrades.IUpgradeableItem;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.menu.locator.WcwtToolkitItemLocator;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import de.mari_023.ae2wtlib.api.terminal.WUTHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class WcwtToolkitAccess {
    public static final int HOTBAR_SIZE = 9;
    public static final int HOTBAR_SLOTS = HOTBAR_SIZE * 2;

    private WcwtToolkitAccess() {
    }

    public static boolean hasToolkitCard(Player player) {
        // The terminal being opened and the terminal supplying the extra hotbars can be different.
        var terminal = WTDefinition.of("wcwt");
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (WUTHandler.hasTerminal(stack, terminal) && hasToolkitCard(stack)) {
                return true;
            }
        }
        return findHotbarTerminal(player, WcwtToolkitAccess::hasToolkitCard) != null
                || player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                && menu.getMenuHost() != null && hasToolkitCard(menu.getMenuHost().getItemStack());
    }

    public static boolean hasToolkitCard(ItemStack terminal) {
        return !terminal.isEmpty()
                && terminal.getItem() instanceof IUpgradeableItem upgradeable
                && upgradeable.getUpgrades(terminal).isInstalled(ModItems.TOOLKIT_CARD.get());
    }

    @Nullable
    public static WcwtToolkitItemLocator findHotbarTerminal(Player player, Predicate<ItemStack> predicate) {
        return findHotbarTerminal(player, WTDefinition.of("wcwt"), predicate);
    }

    @Nullable
    public static WcwtToolkitItemLocator findHotbarTerminal(Player player, WTDefinition terminal,
            Predicate<ItemStack> predicate) {
        boolean clientSide = player.level().isClientSide();
        var items = clientSide ? null : WcwtToolkitStore.existingItems(player);
        if (!clientSide && items == null) {
            return null;
        }
        int size = items == null ? HOTBAR_SLOTS : Math.min(HOTBAR_SLOTS, items.size());
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = items == null ? WcwtToolkitHotbarState.stackAt(player, slot) : items.getStackInSlot(slot);
            if (WUTHandler.hasTerminal(stack, terminal) && predicate.test(stack)) {
                return new WcwtToolkitItemLocator(slot, null);
            }
        }
        return null;
    }

    public static boolean openHotbarTerminal(ServerPlayer player, Predicate<ItemStack> predicate) {
        if (!WcwtToolkitHotbarState.hasToolkitCard(player)) {
            return false;
        }
        var locator = findHotbarTerminal(player, predicate);
        if (locator == null) {
            return false;
        }
        var terminal = WTDefinition.of("wcwt");
        var stack = locator.locateItem(player);
        WcwtToolkitSync.ensureLayoutSynced(player);
        if (stack.getItem() instanceof ItemWUT && WTDefinition.ofOrNull(stack) != terminal) {
            WUTHandler.setCurrentTerminal(player, locator, stack, terminal);
        }
        return terminal.containerOpener().tryOpen(player, locator, false);
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
