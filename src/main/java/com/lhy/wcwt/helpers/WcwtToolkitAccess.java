package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.integration.modules.curios.CuriosIntegration;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
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
    public static InternalInventory findInventory(Player player) {
        var host = existingHost(player);
        if (host != null) {
            return hasToolkitCard(host.getItemStack())
                    ? host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT) : null;
        }
        ItemStack terminal = findTerminal(player);
        return !hasToolkitCard(terminal) ? null
                : WirelessComprehensiveWorkTerminalMenuHost.createToolkitInventory(player, terminal);
    }

    @Nullable
    public static InternalInventory findMemoryInventory(Player player) {
        var host = existingHost(player);
        if (host != null) {
            return hasToolkitCard(host.getItemStack())
                    ? host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT_MEMORY) : null;
        }
        ItemStack terminal = findTerminal(player);
        return !hasToolkitCard(terminal) ? null
                : WirelessComprehensiveWorkTerminalMenuHost.createToolkitMemoryInventory(player, terminal);
    }

    @Nullable
    private static WirelessComprehensiveWorkTerminalMenuHost existingHost(Player player) {
        return player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                ? menu.getMenuHost() : null;
    }

    @Nullable
    public static ItemStack findTerminal(Player player) {
        var curios = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (curios != null) {
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem) {
                    return stack;
                }
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
