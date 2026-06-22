package com.lhy.wcwt.menu.locator;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.helpers.WirelessCraftingTerminalMenuHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.ISubMenu;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class WcwtEmbeddedTerminalLocator implements WcwtItemLocator {
    private final WcwtItemLocator parentLocator;
    private final int terminalIndex;
    private ItemStack cachedTerminal = ItemStack.EMPTY;

    public WcwtEmbeddedTerminalLocator(WcwtItemLocator parentLocator, int terminalIndex) {
        this.parentLocator = parentLocator;
        this.terminalIndex = terminalIndex;
    }

    public WcwtItemLocator parentLocator() {
        return parentLocator;
    }

    public int terminalIndex() {
        return terminalIndex;
    }

    @Override
    @Nullable
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack terminal = locateItem(player);
        if (terminal.isEmpty()) {
            return null;
        }

        ItemMenuHost host = null;
        if (terminal.getItem() instanceof IUniversalWirelessTerminalItem universal) {
            host = universal.getMenuHost(player, this, terminal);
        } else if (terminal.getItem() instanceof WirelessCraftingTerminalItem) {
            host = new EmbeddedCraftingHost(player, this, terminal);
        } else if (terminal.getItem() instanceof WirelessTerminalItem) {
            host = new EmbeddedWirelessHost(player, this, terminal);
        }
        if (hostInterface.isInstance(host)) {
            return hostInterface.cast(host);
        }
        if (hostInterface.isInstance(terminal)) {
            return hostInterface.cast(terminal);
        }
        return null;
    }

    @Override
    public ItemStack locateItem(Player player) {
        ItemStack parent = parentLocator.locateItem(player);
        if (!WcwtUniversalTerminals.isWcwt(parent)) {
            cachedTerminal = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        if (!cachedTerminal.isEmpty()) {
            WcwtUniversalTerminals.syncRuntimeStateToParent(parent, cachedTerminal);
            WcwtUniversalTerminals.setInstalledTerminal(parent, terminalIndex,
                    WcwtUniversalTerminals.persistentTerminalCopy(cachedTerminal));
            WcwtUniversalTerminals.syncRuntimeStateFromParent(parent, cachedTerminal);
            parentLocator.storeItem(player, parent);
            return cachedTerminal;
        }
        cachedTerminal = WcwtUniversalTerminals.getInstalledTerminal(parent, terminalIndex);
        WcwtUniversalTerminals.syncRuntimeStateFromParent(parent, cachedTerminal);
        return cachedTerminal;
    }

    @Override
    public boolean storeItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack parent = parentLocator.locateItem(player);
        if (!WcwtUniversalTerminals.isWcwt(parent)) {
            return false;
        }
        cachedTerminal = stack;
        WcwtUniversalTerminals.syncRuntimeStateToParent(parent, cachedTerminal);
        boolean result = WcwtUniversalTerminals.setInstalledTerminal(parent, terminalIndex,
                WcwtUniversalTerminals.persistentTerminalCopy(cachedTerminal));
        parentLocator.storeItem(player, parent);
        return result;
    }

    public void flush(Player player) {
        if (!cachedTerminal.isEmpty()) {
            storeItem(player, cachedTerminal);
        }
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        MenuLocators.writeToPacket(buf, parentLocator);
        buf.writeVarInt(terminalIndex);
    }

    public static WcwtEmbeddedTerminalLocator readFromPacket(FriendlyByteBuf buf) {
        MenuLocator parent = MenuLocators.readFromPacket(buf);
        if (!(parent instanceof WcwtItemLocator itemLocator)) {
            throw new IllegalArgumentException("WCWT embedded terminal parent locator is not a WCWT item locator: "
                    + parent);
        }
        return new WcwtEmbeddedTerminalLocator(itemLocator, buf.readVarInt());
    }

    @Override
    public String toString() {
        return "wcwt embedded terminal " + terminalIndex + " in " + parentLocator;
    }

    private static final class EmbeddedWirelessHost extends WirelessTerminalMenuHost {
        private final WcwtEmbeddedTerminalLocator locator;

        private EmbeddedWirelessHost(Player player, WcwtEmbeddedTerminalLocator locator, ItemStack stack) {
            super(player, locator.inventorySlot(), stack, (p, subMenu) -> returnToParent(p, locator, subMenu));
            this.locator = locator;
        }

        @Override
        public boolean onBroadcastChanges(AbstractContainerMenu menu) {
            boolean result = super.onBroadcastChanges(menu);
            locator.storeItem(getPlayer(), getItemStack());
            return result;
        }
    }

    private static final class EmbeddedCraftingHost extends WirelessCraftingTerminalMenuHost {
        private final WcwtEmbeddedTerminalLocator locator;

        private EmbeddedCraftingHost(Player player, WcwtEmbeddedTerminalLocator locator, ItemStack stack) {
            super(player, locator.inventorySlot(), stack, (p, subMenu) -> returnToParent(p, locator, subMenu));
            this.locator = locator;
        }

        @Override
        public boolean onBroadcastChanges(AbstractContainerMenu menu) {
            boolean result = super.onBroadcastChanges(menu);
            locator.storeItem(getPlayer(), getItemStack());
            return result;
        }
    }

    private static void returnToParent(Player player, WcwtEmbeddedTerminalLocator locator, ISubMenu subMenu) {
        locator.flush(player);
        ItemStack parent = locator.parentLocator.locateItem(player);
        if (parent.getItem() instanceof com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem terminal) {
            terminal.openFromLocator(player, locator.parentLocator, true);
        }
    }
}
