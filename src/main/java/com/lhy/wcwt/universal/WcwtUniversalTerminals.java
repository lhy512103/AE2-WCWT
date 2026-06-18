package com.lhy.wcwt.universal;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuHostLocator;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtEmbeddedTerminalLocator;
import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import appeng.util.inv.AppEngInternalInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WcwtUniversalTerminals {
    public static final int MAX_EMBEDDED_TERMINALS = 16;

    private WcwtUniversalTerminals() {
    }

    public static boolean isWcwt(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get());
    }

    public static boolean isMergeableTerminal(ItemStack stack) {
        if (stack.isEmpty() || isWcwt(stack) || AE2wtlibAPI.isUniversalTerminal(stack)) {
            return false;
        }
        return (stack.getItem() instanceof WirelessTerminalItem || stack.getItem() instanceof ItemWT)
                && WcwtItemIds.MERGEABLE_TERMINAL_SET.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static List<ItemStack> getInstalledTerminals(ItemStack wcwt) {
        if (!isWcwt(wcwt)) {
            return List.of();
        }
        var inv = readTerminalInventory(wcwt);
        List<ItemStack> terminals = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                terminals.add(stack.copy());
            }
        }
        return terminals;
    }

    public static int getInstalledTerminalCount(ItemStack wcwt) {
        if (!isWcwt(wcwt)) {
            return 0;
        }
        var inv = readTerminalInventory(wcwt);
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static boolean isUniversal(ItemStack wcwt) {
        return getInstalledTerminalCount(wcwt) > 0;
    }

    public static int getCurrentTerminalIndex(ItemStack wcwt) {
        if (!isWcwt(wcwt)) {
            return -1;
        }
        int index = wcwt.getOrDefault(ModComponents.CURRENT_UNIVERSAL_TERMINAL.get(), -1);
        if (index < 0 || index >= getInstalledTerminalCount(wcwt)) {
            return -1;
        }
        return index;
    }

    public static void setCurrentTerminalIndex(ItemStack wcwt, int index) {
        if (!isWcwt(wcwt)) {
            return;
        }
        int normalized = index >= 0 && index < getInstalledTerminalCount(wcwt) ? index : -1;
        wcwt.set(ModComponents.CURRENT_UNIVERSAL_TERMINAL.get(), normalized);
    }

    public static ItemStack getInstalledTerminal(ItemStack wcwt, int compactIndex) {
        if (!isWcwt(wcwt) || compactIndex < 0) {
            return ItemStack.EMPTY;
        }
        var inv = readTerminalInventory(wcwt);
        int seen = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (seen == compactIndex) {
                return stack.copy();
            }
            seen++;
        }
        return ItemStack.EMPTY;
    }

    public static boolean setInstalledTerminal(ItemStack wcwt, int compactIndex, ItemStack terminal) {
        if (!isWcwt(wcwt) || compactIndex < 0) {
            return false;
        }
        var inv = readTerminalInventory(wcwt);
        int seen = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (seen == compactIndex) {
                ItemStack copy = terminal.copy();
                copy.setCount(Math.min(1, copy.getCount()));
                inv.setItemDirect(i, copy);
                writeTerminalInventory(wcwt, inv);
                return true;
            }
            seen++;
        }
        return false;
    }

    public static boolean addTerminal(ItemStack wcwt, ItemStack terminal) {
        if (!isWcwt(wcwt) || !isMergeableTerminal(terminal)) {
            return false;
        }
        var inv = readTerminalInventory(wcwt);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack existing = inv.getStackInSlot(i);
            if (!existing.isEmpty() && existing.is(terminal.getItem())) {
                return false;
            }
        }
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                continue;
            }
            ItemStack copy = terminal.copy();
            copy.setCount(1);
            inv.setItemDirect(i, copy);
            writeTerminalInventory(wcwt, inv);
            return true;
        }
        return false;
    }

    public static boolean canAddTerminal(ItemStack wcwt, ItemStack terminal) {
        if (!isWcwt(wcwt) || !isMergeableTerminal(terminal)) {
            return false;
        }
        var inv = readTerminalInventory(wcwt);
        boolean hasEmpty = false;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack existing = inv.getStackInSlot(i);
            if (existing.isEmpty()) {
                hasEmpty = true;
            } else if (existing.is(terminal.getItem())) {
                return false;
            }
        }
        return hasEmpty;
    }

    public static List<ItemStack> splitInstalledTerminals(ItemStack wcwt) {
        if (!isWcwt(wcwt)) {
            return List.of();
        }
        var inv = readTerminalInventory(wcwt);
        List<ItemStack> terminals = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                terminals.add(stack.copy());
            }
        }
        if (terminals.isEmpty()) {
            return List.of();
        }

        double oldPower = wcwt.getItem() instanceof IAEItemPowerStorage powered
                ? powered.getAECurrentPower(wcwt)
                : 0;
        writeTerminalInventory(wcwt, new AppEngInternalInventory(MAX_EMBEDDED_TERMINALS));
        setCurrentTerminalIndex(wcwt, -1);

        double remainingPower = oldPower;
        if (wcwt.getItem() instanceof IAEItemPowerStorage powered) {
            remainingPower = Math.max(0, oldPower - powered.getAECurrentPower(wcwt));
        }
        for (ItemStack terminal : terminals) {
            remainingPower = prepareSplitTerminal(wcwt, terminal, remainingPower);
        }
        return terminals;
    }

    public static Optional<SwitchTarget> getNextSwitchTarget(Player player, MenuHostLocator currentLocator) {
        if (currentLocator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            ItemMenuHostLocator parentLocator = embeddedLocator.parentLocator();
            ItemStack parent = parentLocator.locateItem(player);
            int count = getInstalledTerminalCount(parent);
            if (count <= 0) {
                return Optional.empty();
            }
            int nextIndex = embeddedLocator.terminalIndex() + 1;
            if (nextIndex >= count) {
                return Optional.of(new SwitchTarget(parentLocator, -1, parent.copy()));
            }
            return Optional.of(new SwitchTarget(parentLocator, nextIndex, getInstalledTerminal(parent, nextIndex)));
        }
        if (currentLocator instanceof ItemMenuHostLocator itemLocator) {
            ItemStack parent = itemLocator.locateItem(player);
            if (!isWcwt(parent) || getInstalledTerminalCount(parent) <= 0) {
                return Optional.empty();
            }
            return Optional.of(new SwitchTarget(itemLocator, 0, getInstalledTerminal(parent, 0)));
        }
        return Optional.empty();
    }

    public static Optional<SwitchTarget> getPreviousSwitchTarget(Player player, MenuHostLocator currentLocator) {
        if (currentLocator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            ItemMenuHostLocator parentLocator = embeddedLocator.parentLocator();
            ItemStack parent = parentLocator.locateItem(player);
            int count = getInstalledTerminalCount(parent);
            if (count <= 0) {
                return Optional.empty();
            }
            int previousIndex = embeddedLocator.terminalIndex() - 1;
            if (previousIndex < 0) {
                return Optional.of(new SwitchTarget(parentLocator, -1, parent.copy()));
            }
            return Optional.of(new SwitchTarget(parentLocator, previousIndex, getInstalledTerminal(parent, previousIndex)));
        }
        if (currentLocator instanceof ItemMenuHostLocator itemLocator) {
            ItemStack parent = itemLocator.locateItem(player);
            int count = getInstalledTerminalCount(parent);
            if (!isWcwt(parent) || count <= 0) {
                return Optional.empty();
            }
            int lastIndex = count - 1;
            return Optional.of(new SwitchTarget(itemLocator, lastIndex, getInstalledTerminal(parent, lastIndex)));
        }
        return Optional.empty();
    }

    public static Optional<SwitchTarget> getBaseSwitchTarget(Player player, MenuHostLocator currentLocator) {
        if (currentLocator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            ItemMenuHostLocator parentLocator = embeddedLocator.parentLocator();
            ItemStack parent = parentLocator.locateItem(player);
            if (isWcwt(parent) && getInstalledTerminalCount(parent) > 0) {
                return Optional.of(new SwitchTarget(parentLocator, -1, parent.copy()));
            }
        }
        if (currentLocator instanceof ItemMenuHostLocator itemLocator) {
            ItemStack parent = itemLocator.locateItem(player);
            if (isWcwt(parent) && getInstalledTerminalCount(parent) > 0) {
                return Optional.of(new SwitchTarget(itemLocator, -1, parent.copy()));
            }
        }
        return Optional.empty();
    }

    public static @Nullable WTDefinition definitionFor(ItemStack stack) {
        if (stack.isEmpty() || isWcwt(stack)) {
            return null;
        }
        return WTDefinition.ofOrNull(stack);
    }

    public static boolean switchTo(ServerPlayer player, SwitchTarget target) {
        ItemStack parent = target.parentLocator().locateItem(player);
        if (!isWcwt(parent)) {
            return false;
        }
        setCurrentTerminalIndex(parent, target.terminalIndex());
        if (target.isBaseTerminal()) {
            return ((WirelessComprehensiveWorkTerminalItem) parent.getItem())
                    .openFromInventory(player, target.parentLocator());
        }

        var embeddedLocator = new WcwtEmbeddedTerminalLocator(target.parentLocator(), target.terminalIndex());
        ItemStack terminal = embeddedLocator.locateItem(player);
        if (terminal.getItem() instanceof ItemWT itemWT) {
            return itemWT.tryOpen(player, embeddedLocator, true);
        }
        if (terminal.getItem() instanceof WirelessTerminalItem wirelessTerminalItem) {
            return wirelessTerminalItem.openFromInventory(player, embeddedLocator);
        }
        return false;
    }

    public static @Nullable ItemMenuHostLocator getParentLocator(MenuHostLocator locator) {
        if (locator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            return embeddedLocator.parentLocator();
        }
        if (locator instanceof ItemMenuHostLocator itemLocator) {
            return itemLocator;
        }
        return null;
    }

    private static AppEngInternalInventory readTerminalInventory(ItemStack wcwt) {
        var inv = new AppEngInternalInventory(MAX_EMBEDDED_TERMINALS);
        inv.fromItemContainerContents(wcwt.getOrDefault(
                ModComponents.UNIVERSAL_TERMINALS.get(), ItemContainerContents.EMPTY));
        return inv;
    }

    private static void writeTerminalInventory(ItemStack wcwt, AppEngInternalInventory inv) {
        wcwt.set(ModComponents.UNIVERSAL_TERMINALS.get(), inv.toItemContainerContents());
        if (wcwt.getItem() instanceof WirelessComprehensiveWorkTerminalItem item) {
            item.updatePowerMultiplier(wcwt);
        }
    }

    private static double prepareSplitTerminal(ItemStack parent, ItemStack terminal, double availablePower) {
        terminal.setCount(1);
        copyRuntimeComponent(parent, terminal, AEComponents.WIRELESS_LINK_TARGET);

        if (sameRuntimeComponent(parent, terminal, AEComponents.ENERGY_CAPACITY)) {
            terminal.remove(AEComponents.ENERGY_CAPACITY);
        }
        terminal.remove(AEComponents.STORED_ENERGY);

        if (availablePower <= 0 || !(terminal.getItem() instanceof IAEItemPowerStorage powered)) {
            return availablePower;
        }
        double leftover = powered.injectAEPower(terminal, availablePower, Actionable.MODULATE);
        return Math.max(0, leftover);
    }

    private static <T> void copyRuntimeComponent(ItemStack source, ItemStack target, net.minecraft.core.component.DataComponentType<T> component) {
        T value = source.get(component);
        if (value == null) {
            target.remove(component);
        } else {
            target.set(component, value);
        }
    }

    private static <T> boolean sameRuntimeComponent(ItemStack a, ItemStack b,
            net.minecraft.core.component.DataComponentType<T> component) {
        return Objects.equals(a.get(component), b.get(component));
    }

    public record SwitchTarget(ItemMenuHostLocator parentLocator, int terminalIndex, ItemStack displayStack) {
        public boolean isBaseTerminal() {
            return terminalIndex < 0;
        }
    }
}
