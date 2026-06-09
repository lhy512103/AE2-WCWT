package com.lhy.wcwt.helpers;

import org.jetbrains.annotations.Nullable;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
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
    public static NetworkToolMenuHost findToolkitNetworkToolHost(Player player) {
        NetworkToolMenuHost inventoryHost = findInInventory(player);
        return inventoryHost != null ? inventoryHost : findInCurios(player);
    }

    @Nullable
    private static NetworkToolMenuHost findInInventory(Player player) {
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
    private static NetworkToolMenuHost findInCurios(Player player) {
        var curios = CuriosBridge.getEquippedSlots(player);
        for (int curioIndex = 0; curioIndex < curios.size(); curioIndex++) {
            var curio = curios.get(curioIndex);
            ItemStack terminalStack = curio.handler().getStackInSlot(curio.slotIndex());
            var host = createHostFromTerminal(player, terminalStack,
                    new WcwtToolkitNetworkToolLocator(WcwtToolkitNetworkToolLocator.SourceKind.CURIOS, curioIndex, 0));
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost createHostFromTerminal(Player player, ItemStack terminalStack,
            WcwtToolkitNetworkToolLocator baseLocator) {
        if (!(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem)) {
            return null;
        }

        @Nullable Integer inventorySlot = baseLocator.sourceKind() == WcwtToolkitNetworkToolLocator.SourceKind.INVENTORY
                ? baseLocator.sourceSlot()
                : null;
        var toolkit = WirelessComprehensiveWorkTerminalMenuHost.createToolkitInventory(player, terminalStack);
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
                    player,
                    locator.getPlayerInventorySlot(),
                    terminalStack,
                    stack,
                    null,
                    toolkit,
                    toolkitSlot);
        }
        return null;
    }
}
