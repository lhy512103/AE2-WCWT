package com.lhy.wcwt.menu.locator;

import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.locator.MenuLocators;
import appeng.api.ids.AEComponents;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class WcwtEmbeddedTerminalLocator implements ItemMenuHostLocator {
    private final ItemMenuHostLocator parentLocator;
    private final int terminalIndex;
    private ItemStack cachedTerminal = ItemStack.EMPTY;

    public WcwtEmbeddedTerminalLocator(ItemMenuHostLocator parentLocator, int terminalIndex) {
        this.parentLocator = parentLocator;
        this.terminalIndex = terminalIndex;
    }

    public ItemMenuHostLocator parentLocator() {
        return parentLocator;
    }

    public int terminalIndex() {
        return terminalIndex;
    }

    @Override
    public ItemStack locateItem(Player player) {
        ItemStack parent = parentLocator.locateItem(player);
        if (!WcwtUniversalTerminals.isWcwt(parent)) {
            cachedTerminal = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        if (!cachedTerminal.isEmpty()) {
            syncRuntimeStateToParent(parent, cachedTerminal);
            WcwtUniversalTerminals.setInstalledTerminal(parent, terminalIndex, persistentTerminalCopy(cachedTerminal));
            syncRuntimeStateFromParent(parent, cachedTerminal);
            return cachedTerminal;
        }
        cachedTerminal = WcwtUniversalTerminals.getInstalledTerminal(parent, terminalIndex);
        syncRuntimeStateFromParent(parent, cachedTerminal);
        return cachedTerminal;
    }

    public void flush(Player player) {
        if (cachedTerminal.isEmpty()) {
            return;
        }
        ItemStack parent = parentLocator.locateItem(player);
        if (WcwtUniversalTerminals.isWcwt(parent)) {
            syncRuntimeStateToParent(parent, cachedTerminal);
            WcwtUniversalTerminals.setInstalledTerminal(parent, terminalIndex, persistentTerminalCopy(cachedTerminal));
        }
    }

    private static void syncRuntimeStateFromParent(ItemStack parent, ItemStack terminal) {
        if (terminal.isEmpty()) {
            return;
        }
        copyComponent(parent, terminal, AEComponents.WIRELESS_LINK_TARGET);
        copyComponent(parent, terminal, AEComponents.STORED_ENERGY);
        copyComponent(parent, terminal, AEComponents.ENERGY_CAPACITY);
    }

    private static void syncRuntimeStateToParent(ItemStack parent, ItemStack terminal) {
        // Some embedded terminals recompute their own default capacity while opening.
        // Treat the WCWT parent as the source of truth in that case, otherwise a 1.6M child
        // can clamp and overwrite a higher-capacity universal WCWT stack.
        if (!sameComponent(parent, terminal, AEComponents.ENERGY_CAPACITY)) {
            return;
        }
        copyComponent(terminal, parent, AEComponents.STORED_ENERGY);
    }

    private static ItemStack persistentTerminalCopy(ItemStack terminal) {
        ItemStack copy = terminal.copy();
        copy.remove(AEComponents.WIRELESS_LINK_TARGET);
        copy.remove(AEComponents.STORED_ENERGY);
        copy.remove(AEComponents.ENERGY_CAPACITY);
        return copy;
    }

    private static <T> void copyComponent(ItemStack source, ItemStack target, DataComponentType<T> component) {
        T value = source.get(component);
        if (value == null) {
            target.remove(component);
        } else {
            target.set(component, value);
        }
    }

    private static <T> boolean sameComponent(ItemStack a, ItemStack b, DataComponentType<T> component) {
        return Objects.equals(a.get(component), b.get(component));
    }

    @Override
    public @Nullable BlockHitResult hitResult() {
        return parentLocator.hitResult();
    }

    @Override
    public @Nullable Integer getPlayerInventorySlot() {
        return parentLocator.getPlayerInventorySlot();
    }

    public static void writeToPacket(WcwtEmbeddedTerminalLocator locator, FriendlyByteBuf buf) {
        MenuLocators.writeToPacket(buf, locator.parentLocator);
        buf.writeVarInt(locator.terminalIndex);
    }

    public static WcwtEmbeddedTerminalLocator readFromPacket(FriendlyByteBuf buf) {
        MenuHostLocator parent = MenuLocators.readFromPacket(buf);
        if (!(parent instanceof ItemMenuHostLocator itemLocator)) {
            throw new IllegalArgumentException("WCWT embedded terminal parent locator is not an item locator: " + parent);
        }
        return new WcwtEmbeddedTerminalLocator(itemLocator, buf.readVarInt());
    }

    @Override
    public String toString() {
        return "wcwt embedded terminal " + terminalIndex + " in " + parentLocator;
    }
}
