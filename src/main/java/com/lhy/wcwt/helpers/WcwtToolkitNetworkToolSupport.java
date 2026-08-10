package com.lhy.wcwt.helpers;

import org.jetbrains.annotations.Nullable;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtToolkitNetworkToolLocator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 为 AE 原版网络工具逻辑提供 WCWT 卡槽包和工具包来源。
 */
public final class WcwtToolkitNetworkToolSupport {
    private static final boolean DEBUG_TOOLKIT =
            Boolean.getBoolean("wcwt.debug.toolkit") || Boolean.getBoolean("wcwt.debug.magnet");

    private WcwtToolkitNetworkToolSupport() {
    }

    /**
     * 卡槽包卡是终端内统一的网络工具库存；安装后优先于玩家携带的实体网络工具。
     */
    @Nullable
    public static NetworkToolMenuHost findCardBackedNetworkToolHost(Player player) {
        NetworkToolMenuHost inventoryHost = findInInventory(player, true);
        if (inventoryHost != null) {
            debug(player, "found card-backed network tool in inventory WCWT");
            return inventoryHost;
        }

        NetworkToolMenuHost curiosHost = findInCurios(player, true);
        if (curiosHost != null) {
            debug(player, "found card-backed network tool in Curios WCWT");
            return curiosHost;
        }
        return null;
    }

    /**
     * 未安装卡槽包卡且 AE2 没找到玩家携带的网络工具时，仅从已启用的 WCWT 工具包中寻找实体网络工具。
     */
    @Nullable
    public static NetworkToolMenuHost findToolkitNetworkToolHost(Player player) {
        NetworkToolMenuHost inventoryHost = findInInventory(player, false);
        if (inventoryHost != null) {
            debug(player, "found physical network tool in inventory WCWT toolkit");
            return inventoryHost;
        }

        NetworkToolMenuHost curiosHost = findInCurios(player, false);
        if (curiosHost != null) {
            debug(player, "found physical network tool in Curios WCWT toolkit");
            return curiosHost;
        }

        debug(player, "no WCWT network tool host found");
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost findInInventory(Player player, boolean cardBacked) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack terminalStack = inventory.getItem(slot);
            var locator = new WcwtToolkitNetworkToolLocator(
                    WcwtToolkitNetworkToolLocator.SourceKind.INVENTORY, slot, cardBacked ? -1 : 0);
            var host = cardBacked
                    ? createCardBackedHost(player, terminalStack, locator)
                    : createToolkitHost(player, terminalStack, locator);
            if (host != null) {
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost findInCurios(Player player, boolean cardBacked) {
        var curios = CuriosBridge.getEquippedSlots(player);
        for (int curioIndex = 0; curioIndex < curios.size(); curioIndex++) {
            var curio = curios.get(curioIndex);
            ItemStack terminalStack = curio.handler().getStackInSlot(curio.slotIndex());
            var locator = new WcwtToolkitNetworkToolLocator(
                    WcwtToolkitNetworkToolLocator.SourceKind.CURIOS, curioIndex, cardBacked ? -1 : 0);
            var host = cardBacked
                    ? createCardBackedHost(player, terminalStack, locator)
                    : createToolkitHost(player, terminalStack, locator);
            if (host != null) {
                debug(player, "Curios WCWT network tool: identifier={}, slot={}, equippedIndex={}, cardBacked={}",
                        curio.identifier(), curio.slotIndex(), curioIndex, cardBacked);
                return host;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkToolMenuHost createCardBackedHost(Player player, ItemStack terminalStack,
            WcwtToolkitNetworkToolLocator locator) {
        if (!(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)
                || !terminalItem.getUpgrades(terminalStack).isInstalled(ModItems.TOOL_SLOTS_BOX_CARD.get())) {
            return null;
        }
        return new WcwtSimulatedNetworkToolMenuHost(
                player,
                locator.getPlayerInventorySlot(),
                terminalStack,
                locator);
    }

    @Nullable
    private static NetworkToolMenuHost createToolkitHost(Player player, ItemStack terminalStack,
            WcwtToolkitNetworkToolLocator baseLocator) {
        if (!(terminalStack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)
                || !terminalItem.getUpgrades(terminalStack).isInstalled(ModItems.TOOLKIT_CARD.get())) {
            return null;
        }

        var toolkit = WirelessComprehensiveWorkTerminalMenuHost.createToolkitInventory(player, terminalStack);
        if (toolkit == null) {
            debug(player, "WCWT toolkit unavailable for {}", baseLocator);
            return null;
        }

        for (int toolkitSlot = 0; toolkitSlot < toolkit.size(); toolkitSlot++) {
            ItemStack stack = toolkit.getStackInSlot(toolkitSlot);
            if (!(stack.getItem() instanceof NetworkToolItem)) {
                continue;
            }
            var locator = new WcwtToolkitNetworkToolLocator(
                    baseLocator.sourceKind(), baseLocator.sourceSlot(), toolkitSlot);
            debug(player, "WCWT toolkit network tool candidate: source={}, toolkitSlot={}, stack={}",
                    baseLocator, toolkitSlot, stack);
            return new WcwtToolkitNetworkToolMenuHost(
                    player,
                    locator.getPlayerInventorySlot(),
                    terminalStack,
                    stack,
                    null,
                    toolkit,
                    toolkitSlot,
                    locator);
        }
        return null;
    }

    private static void debug(Player player, String message, Object... args) {
        if (!DEBUG_TOOLKIT) {
            return;
        }
        Object[] withPlayer = new Object[args.length + 1];
        withPlayer[0] = player == null ? "<null>" : player.getScoreboardName();
        System.arraycopy(args, 0, withPlayer, 1, args.length);
        WcwtMod.LOGGER.info("WCWT toolkit debug: player={}, " + message, withPlayer);
    }
}