package com.lhy.wcwt.universal;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.menu.locator.WcwtEmbeddedTerminalLocator;
import com.lhy.wcwt.menu.locator.WcwtInventoryLocator;
import com.lhy.wcwt.menu.locator.WcwtItemLocator;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WcwtUniversalTerminals {
    public static final int MAX_EMBEDDED_TERMINALS = 16;
    private static final String TERMINALS_TAG = "universal_terminals";
    private static final String CURRENT_TERMINAL_TAG = "current_universal_terminal";
    private static final String ACCESS_POINT_TAG = "accessPoint";
    private static final String CURRENT_POWER_TAG = "internalCurrentPower";
    private static final String MAX_POWER_TAG = "internalMaxPower";

    private WcwtUniversalTerminals() {
    }

    public static boolean isWcwt(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get());
    }

    public static boolean isMergeableTerminal(ItemStack stack) {
        if (stack.isEmpty() || isWcwt(stack)) {
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
        CompoundTag root = getRootTag(wcwt);
        int index = root != null && root.contains(CURRENT_TERMINAL_TAG, Tag.TAG_INT)
                ? root.getInt(CURRENT_TERMINAL_TAG)
                : -1;
        return index >= 0 && index < getInstalledTerminalCount(wcwt) ? index : -1;
    }

    public static void setCurrentTerminalIndex(ItemStack wcwt, int index) {
        if (!isWcwt(wcwt)) {
            return;
        }
        int normalized = index >= 0 && index < getInstalledTerminalCount(wcwt) ? index : -1;
        getOrCreateRootTag(wcwt).putInt(CURRENT_TERMINAL_TAG, normalized);
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
            ItemStack copy = persistentTerminalCopy(terminal);
            copy.setCount(1);
            inv.setItemDirect(i, copy);
            writeTerminalInventory(wcwt, inv);
            return true;
        }
        return false;
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
        double oldPower = wcwt.getItem() instanceof IAEItemPowerStorage powered ? powered.getAECurrentPower(wcwt) : 0;
        writeTerminalInventory(wcwt, new AppEngInternalInventory(MAX_EMBEDDED_TERMINALS));
        setCurrentTerminalIndex(wcwt, -1);
        double power = oldPower;
        if (wcwt.getItem() instanceof IAEItemPowerStorage powered) {
            power = Math.max(0, oldPower - powered.getAECurrentPower(wcwt));
        }
        for (ItemStack terminal : terminals) {
            power = prepareSplitTerminal(wcwt, terminal, power);
        }
        return terminals;
    }

    public static Optional<SwitchTarget> getNextSwitchTarget(Player player, WcwtItemLocator locator) {
        if (locator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            WcwtItemLocator parentLocator = embeddedLocator.parentLocator();
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
        ItemStack parent = locator.locateItem(player);
        if (!isWcwt(parent) || getInstalledTerminalCount(parent) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new SwitchTarget(locator, 0, getInstalledTerminal(parent, 0)));
    }

    public static Optional<SwitchTarget> getPreviousSwitchTarget(Player player, WcwtItemLocator locator) {
        if (locator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            WcwtItemLocator parentLocator = embeddedLocator.parentLocator();
            ItemStack parent = parentLocator.locateItem(player);
            int count = getInstalledTerminalCount(parent);
            if (count <= 0) {
                return Optional.empty();
            }
            int previousIndex = embeddedLocator.terminalIndex() - 1;
            if (previousIndex < 0) {
                return Optional.of(new SwitchTarget(parentLocator, -1, parent.copy()));
            }
            return Optional.of(new SwitchTarget(parentLocator, previousIndex,
                    getInstalledTerminal(parent, previousIndex)));
        }
        ItemStack parent = locator.locateItem(player);
        int count = getInstalledTerminalCount(parent);
        if (!isWcwt(parent) || count <= 0) {
            return Optional.empty();
        }
        int lastIndex = count - 1;
        return Optional.of(new SwitchTarget(locator, lastIndex, getInstalledTerminal(parent, lastIndex)));
    }

    public static Optional<SwitchTarget> getBaseSwitchTarget(Player player, WcwtItemLocator locator) {
        WcwtItemLocator parentLocator = locator instanceof WcwtEmbeddedTerminalLocator embeddedLocator
                ? embeddedLocator.parentLocator()
                : locator;
        ItemStack parent = parentLocator.locateItem(player);
        if (isWcwt(parent) && getInstalledTerminalCount(parent) > 0) {
            return Optional.of(new SwitchTarget(parentLocator, -1, parent.copy()));
        }
        return Optional.empty();
    }

    public static boolean switchTo(ServerPlayer player, SwitchTarget target) {
        ItemStack parent = target.parentLocator().locateItem(player);
        if (!isWcwt(parent)) {
            return false;
        }
        setCurrentTerminalIndex(parent, target.terminalIndex());
        target.parentLocator().storeItem(player, parent);
        if (target.isBaseTerminal()) {
            return ((WirelessComprehensiveWorkTerminalItem) parent.getItem())
                    .openFromLocator(player, target.parentLocator(), true);
        }
        var embeddedLocator = new WcwtEmbeddedTerminalLocator(target.parentLocator(), target.terminalIndex());
        return openEmbedded(player, embeddedLocator, true);
    }

    public static boolean openEmbedded(Player player, WcwtEmbeddedTerminalLocator locator, boolean returningFromSubmenu) {
        ItemStack terminal = locator.locateItem(player);
        if (terminal.isEmpty()) {
            return false;
        }
        if (terminal.getItem() instanceof IUniversalWirelessTerminalItem universal) {
            return universal.tryOpen(player, locator, terminal, returningFromSubmenu);
        }
        if (terminal.getItem() instanceof WirelessTerminalItem wirelessTerminal) {
            return MenuOpener.open(wirelessTerminal.getMenuType(), player, locator, returningFromSubmenu);
        }
        return false;
    }

    public static @Nullable WcwtItemLocator parentLocatorOf(MenuLocator locator) {
        if (locator instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
            return embeddedLocator.parentLocator();
        }
        if (locator instanceof WcwtItemLocator itemLocator) {
            return itemLocator;
        }
        return null;
    }

    public static @Nullable WcwtItemLocator currentLocatorOf(AEBaseMenu menu) {
        WcwtItemLocator locator = currentLocatorOf(menu.getLocator());
        if (locator != null) {
            return locator;
        }
        if (menu instanceof WirelessComprehensiveWorkTerminalMenu wcwtMenu) {
            locator = inventoryLocatorOf(wcwtMenu.getMenuHost());
            if (locator != null) {
                return locator;
            }
        }
        Object target = menu.getTarget();
        if (target instanceof WirelessComprehensiveWorkTerminalMenuHost host) {
            return inventoryLocatorOf(host);
        }
        if (target instanceof ItemMenuHost host) {
            Integer slot = host.getSlot();
            if (slot != null && WcwtUniversalTerminals.isWcwt(host.getItemStack())) {
                return new WcwtInventoryLocator(slot);
            }
        }
        return null;
    }

    private static @Nullable WcwtItemLocator currentLocatorOf(MenuLocator locator) {
        if (locator instanceof WcwtItemLocator itemLocator) {
            return itemLocator;
        }
        return null;
    }

    private static @Nullable WcwtItemLocator inventoryLocatorOf(@Nullable WirelessComprehensiveWorkTerminalMenuHost host) {
        if (host == null) {
            return null;
        }
        Integer slot = host.getSlot();
        if (slot != null && isWcwt(host.getItemStack())) {
            return new WcwtInventoryLocator(slot);
        }
        return null;
    }

    public static AppEngInternalInventory readTerminalInventory(ItemStack wcwt) {
        var inv = new AppEngInternalInventory(MAX_EMBEDDED_TERMINALS);
        CompoundTag root = getRootTag(wcwt);
        if (root != null) {
            inv.readFromNBT(root, TERMINALS_TAG);
        }
        return inv;
    }

    private static void writeTerminalInventory(ItemStack wcwt, AppEngInternalInventory inv) {
        inv.writeToNBT(getOrCreateRootTag(wcwt), TERMINALS_TAG);
        if (wcwt.getItem() instanceof WirelessComprehensiveWorkTerminalItem item) {
            item.updatePowerMultiplier(wcwt);
        }
    }

    public static void syncRuntimeStateFromParent(ItemStack parent, ItemStack terminal) {
        copyTag(parent, terminal, ACCESS_POINT_TAG);
        copyDoubleTag(parent, terminal, CURRENT_POWER_TAG);
        copyDoubleTag(parent, terminal, MAX_POWER_TAG);
    }

    public static void syncRuntimeStateToParent(ItemStack parent, ItemStack terminal) {
        CompoundTag parentTag = parent.getTag();
        CompoundTag terminalTag = terminal.getTag();
        boolean sameCapacity = (parentTag == null ? 0 : parentTag.getDouble(MAX_POWER_TAG))
                == (terminalTag == null ? 0 : terminalTag.getDouble(MAX_POWER_TAG));
        if (sameCapacity) {
            copyDoubleTag(terminal, parent, CURRENT_POWER_TAG);
        }
    }

    public static ItemStack persistentTerminalCopy(ItemStack terminal) {
        ItemStack copy = terminal.copy();
        copy.removeTagKey(ACCESS_POINT_TAG);
        copy.removeTagKey(CURRENT_POWER_TAG);
        copy.removeTagKey(MAX_POWER_TAG);
        return copy;
    }

    private static double prepareSplitTerminal(ItemStack parent, ItemStack terminal, double availablePower) {
        terminal.setCount(1);
        copyTag(parent, terminal, ACCESS_POINT_TAG);
        terminal.removeTagKey(MAX_POWER_TAG);
        terminal.removeTagKey(CURRENT_POWER_TAG);
        if (availablePower <= 0 || !(terminal.getItem() instanceof IAEItemPowerStorage powered)) {
            return availablePower;
        }
        double leftover = powered.injectAEPower(terminal, availablePower, Actionable.MODULATE);
        return Math.max(0, leftover);
    }

    private static void copyTag(ItemStack source, ItemStack target, String key) {
        CompoundTag sourceTag = source.getTag();
        if (sourceTag != null && sourceTag.contains(key)) {
            target.getOrCreateTag().put(key, sourceTag.get(key).copy());
        } else {
            target.removeTagKey(key);
        }
    }

    private static void copyDoubleTag(ItemStack source, ItemStack target, String key) {
        CompoundTag sourceTag = source.getTag();
        if (sourceTag != null && sourceTag.contains(key, Tag.TAG_DOUBLE)) {
            target.getOrCreateTag().putDouble(key, sourceTag.getDouble(key));
        } else {
            target.removeTagKey(key);
        }
    }

    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag root = tag.getCompound(ModComponents.ROOT_TAG);
        tag.put(ModComponents.ROOT_TAG, root);
        return root;
    }

    private static @Nullable CompoundTag getRootTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ModComponents.ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(ModComponents.ROOT_TAG);
    }

    public record SwitchTarget(WcwtItemLocator parentLocator, int terminalIndex, ItemStack displayStack) {
        public boolean isBaseTerminal() {
            return terminalIndex < 0;
        }
    }
}
