package com.lhy.wcwt.helpers;

import org.jetbrains.annotations.Nullable;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
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
        var fromCurios = findInCurios(player);
        if (fromCurios != null) {
            return fromCurios;
        }
        return findInInventory(player);
    }

    @Nullable
    private static NetworkToolMenuHost findInInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack terminalStack = inventory.getItem(slot);
            var host = createHostFromTerminal(player, terminalStack, slot);
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost findInCurios(Player player) {
        for (var curio : CuriosBridge.getVisibleSlots(player)) {
            ItemStack terminalStack = curio.handler().getStackInSlot(curio.slotIndex());
            var host = createHostFromTerminal(player, terminalStack, null);
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost createHostFromTerminal(Player player, ItemStack terminalStack,
            @Nullable Integer inventorySlot) {
        if (!(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)) {
            return null;
        }

        var terminalHost = terminalItem.getMenuHost(player, inventorySlot != null ? inventorySlot : -1, terminalStack, null);
        if (!(terminalHost instanceof WirelessComprehensiveWorkTerminalMenuHost wcwtHost)) {
            return null;
        }

        var toolkit = wcwtHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
        if (toolkit == null) {
            return null;
        }

        for (int toolkitSlot = 0; toolkitSlot < toolkit.size(); toolkitSlot++) {
            ItemStack stack = toolkit.getStackInSlot(toolkitSlot);
            if (!(stack.getItem() instanceof NetworkToolItem networkToolItem)) {
                continue;
            }
            return new WcwtToolkitNetworkToolMenuHost(
                    player,
                    inventorySlot,
                    stack,
                    null,
                    wcwtHost,
                    toolkitSlot);
        }
        return null;
    }
}
