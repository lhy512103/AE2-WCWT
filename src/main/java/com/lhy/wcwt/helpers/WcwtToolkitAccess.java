package com.lhy.wcwt.helpers;

import appeng.api.upgrades.IUpgradeableItem;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Toolkit extra hotbars: two 9-slot pages of the toolkit shown next to the vanilla hotbar.
 * Cells 0-8 are the left page, cells 9-17 are the right page.
 */
public final class WcwtToolkitAccess {
    public static final int HOTBAR_SIZE = 9;
    public static final int HOTBAR_SLOTS = HOTBAR_SIZE * 2;

    private WcwtToolkitAccess() {
    }

    public static boolean hasToolkitCard(Player player) {
        var host = existingHost(player);
        if (host != null && hasToolkitCard(host.getItemStack())) {
            return true;
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
     * Prefers a terminal with the toolkit card, then a WCWT terminal, then any universal terminal, so
     * a worn universal terminal without the card does not hide a carded terminal elsewhere.
     */
    public static ItemStack findTerminal(Player player) {
        ItemStack wcwt = ItemStack.EMPTY;
        ItemStack any = ItemStack.EMPTY;
        List<ItemStack> candidates = new ArrayList<>();
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            candidates.add(curio.handler().getStackInSlot(curio.slotIndex()));
        }
        candidates.addAll(player.getInventory().items);
        candidates.addAll(player.getInventory().offhand);
        for (ItemStack stack : candidates) {
            if (!isTerminalItem(stack)) {
                continue;
            }
            if (hasToolkitCard(stack)) {
                return stack;
            }
            if (wcwt.isEmpty() && stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem) {
                wcwt = stack;
            }
            if (any.isEmpty()) {
                any = stack;
            }
        }
        return wcwt.isEmpty() ? any : wcwt;
    }

    private static boolean isTerminalItem(ItemStack stack) {
        return stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem
                || stack.getItem() instanceof ItemWUT;
    }
}
