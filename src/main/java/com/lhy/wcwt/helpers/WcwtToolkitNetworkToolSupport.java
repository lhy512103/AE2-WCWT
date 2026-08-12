package com.lhy.wcwt.helpers;

import org.jetbrains.annotations.Nullable;

import appeng.core.definitions.AEItems;
import appeng.integration.modules.curios.CuriosIntegration;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 为 AE 原版的网络工具工具箱逻辑补充 WCWT 工具包来源。
 */
public final class WcwtToolkitNetworkToolSupport {
    private WcwtToolkitNetworkToolSupport() {
    }

    @Nullable
    public static NetworkToolMenuHost<?> findCardBackedNetworkToolHost(Player player) {
        var fromCurios = findCardBackedInCurios(player);
        if (fromCurios != null) {
            return fromCurios;
        }
        return findCardBackedInInventory(player);
    }

    @Nullable
    public static NetworkToolMenuHost<?> findToolkitNetworkToolHost(Player player) {
        var fromCurios = findInCurios(player);
        if (fromCurios != null) {
            return fromCurios;
        }
        return findInInventory(player);
    }

    @Nullable
    private static NetworkToolMenuHost<?> findCardBackedInInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var host = createCardBackedHost(player, inventory.getItem(slot),
                    new WcwtToolkitNetworkToolLocator(
                            WcwtToolkitNetworkToolLocator.SourceKind.INVENTORY, slot, -1));
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost<?> findCardBackedInCurios(Player player) {
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap == null) {
            return null;
        }
        for (int slot = 0; slot < cap.getSlots(); slot++) {
            var host = createCardBackedHost(player, cap.getStackInSlot(slot),
                    new WcwtToolkitNetworkToolLocator(
                            WcwtToolkitNetworkToolLocator.SourceKind.CURIOS, slot, -1));
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost<?> createCardBackedHost(Player player, ItemStack terminalStack,
            WcwtToolkitNetworkToolLocator locator) {
        if (!(terminalStack.getItem() instanceof ItemWT terminalItem)
                || !terminalItem.getUpgrades(terminalStack).isInstalled(ModItems.TOOL_SLOTS_BOX_CARD.get())) {
            return null;
        }
        return new WcwtSimulatedNetworkToolMenuHost(
                (NetworkToolItem) AEItems.NETWORK_TOOL.asItem(), player, locator, terminalStack);
    }

    @Nullable
    private static NetworkToolMenuHost<?> findInInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack terminalStack = inventory.getItem(slot);
            var host = createHostFromTerminal(player, terminalStack,
                    new WcwtToolkitNetworkToolLocator(WcwtToolkitNetworkToolLocator.SourceKind.INVENTORY, slot, 0));
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost<?> findInCurios(Player player) {
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap == null) {
            return null;
        }
        for (int slot = 0; slot < cap.getSlots(); slot++) {
            ItemStack terminalStack = cap.getStackInSlot(slot);
            var host = createHostFromTerminal(player, terminalStack,
                    new WcwtToolkitNetworkToolLocator(WcwtToolkitNetworkToolLocator.SourceKind.CURIOS, slot, 0));
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost<?> createHostFromTerminal(Player player, ItemStack terminalStack,
            WcwtToolkitNetworkToolLocator baseLocator) {
        if (!(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)) {
            return null;
        }

        var terminalLocator = switch (baseLocator.sourceKind()) {
            case INVENTORY -> MenuLocators.forInventorySlot(baseLocator.sourceSlot());
            case CURIOS -> MenuLocators.forCurioSlot(baseLocator.sourceSlot());
        };
        var terminalHost = terminalItem.getMenuHost(player, terminalLocator, null);
        if (terminalHost == null) {
            return null;
        }

        var toolkit = terminalHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
        if (toolkit == null) {
            return null;
        }

        for (int toolkitSlot = 0; toolkitSlot < toolkit.size(); toolkitSlot++) {
            ItemStack stack = toolkit.getStackInSlot(toolkitSlot);
            if (!(stack.getItem() instanceof NetworkToolItem networkToolItem)) {
                continue;
            }
            var locator = new WcwtToolkitNetworkToolLocator(baseLocator.sourceKind(), baseLocator.sourceSlot(), toolkitSlot);
            return new WcwtToolkitNetworkToolMenuHost(
                    networkToolItem,
                    player,
                    locator,
                    null,
                    terminalHost,
                    toolkitSlot);
        }
        return null;
    }
}
