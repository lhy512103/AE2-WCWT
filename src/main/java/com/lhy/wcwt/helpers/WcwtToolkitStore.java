package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/**
 * The player's toolkit and its remembered slots.
 *
 * <p>On the server there is exactly one live inventory pair per player object, shared by every
 * terminal menu, the extra hotbars, pickup and the network-tool locators, so no two copies can
 * diverge. Contents persist in {@code PlayerPersisted/wcwt/shared_toolkit}, which survives death.
 * Terminals no longer carry a mirror of the toolkit; old mirrors are removed when first seen.
 */
public final class WcwtToolkitStore {
    private static final boolean DEBUG = Boolean.getBoolean("wcwt.debug.toolkit");
    private static final String PLAYER_PERSISTED_TAG = "PlayerPersisted";
    private static final String WCWT_PLAYER_DATA_TAG = WcwtMod.MOD_ID;
    private static final String PLAYER_TOOLKIT_DATA_TAG = "shared_toolkit";
    private static final String SIZE_TAG = "size";
    private static final String ITEMS_TAG = "items";
    private static final String MEMORY_TAG = "memory";
    private static final String SLOT_TAG = "slot";
    private static final String STACK_TAG = "stack";
    private static final ThreadLocal<Boolean> LOADING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WcwtToolkitStore() {
    }

    public static int configuredSize() {
        return Math.max(WcwtServerConfig.MIN_TOOLKIT_SLOTS, WcwtServerConfig.toolkitSlotCount());
    }

    /** Live server toolkit; {@code null} on the client. */
    @Nullable
    public static AppEngInternalInventory items(Player player, @Nullable ItemStack terminalHint) {
        if (player.level().isClientSide()) {
            return null;
        }
        var state = WcwtToolkitHotbarState.state(player);
        if (state.items == null || needsResize(player, state.items)) {
            state.items = loadItems(player, resolveTerminal(player, terminalHint));
        }
        return state.items;
    }

    @Nullable
    public static AppEngInternalInventory memory(Player player, @Nullable ItemStack terminalHint) {
        if (player.level().isClientSide()) {
            return null;
        }
        var state = WcwtToolkitHotbarState.state(player);
        if (state.memory == null || needsResize(player, state.memory)) {
            state.memory = loadMemory(player, resolveTerminal(player, terminalHint));
        }
        return state.memory;
    }

    /**
     * A plain inventory for a client-side terminal menu; menu slot sync fills it. It is sized like the
     * server toolkit, which the server syncs before opening the menu, because the client's copy of the
     * server config is only refreshed at login and {@code toolkitSlotCount} can change at runtime.
     */
    public static AppEngInternalInventory clientInventory(Player player, boolean memory) {
        int syncedSize = WcwtToolkitHotbarState.clientToolkitSize(player);
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
            }

            @Override
            public boolean isClientSide() {
                return true;
            }
        }, syncedSize > 0 ? syncedSize : configuredSize());
        if (!memory) {
            inventory.setFilter(new ToolkitItemFilter());
        }
        return inventory;
    }

    /** Writes the live inventories back, for changes made to a stack in place such as tool wear. */
    public static void persist(Player player) {
        var state = WcwtToolkitHotbarState.state(player);
        if (state.items != null) {
            saveItems(player, state.items);
        }
        if (state.memory != null) {
            saveMemory(player, state.memory);
        }
    }

    public static CompoundTag playerData(Player player) {
        var persisted = player.getPersistentData().getCompound(PLAYER_PERSISTED_TAG);
        player.getPersistentData().put(PLAYER_PERSISTED_TAG, persisted);
        var wcwtData = persisted.getCompound(WCWT_PLAYER_DATA_TAG);
        persisted.put(WCWT_PLAYER_DATA_TAG, wcwtData);
        return wcwtData;
    }

    /** A resize is deferred while a terminal menu is open, since its slots were laid out for the old size. */
    private static boolean needsResize(Player player, AppEngInternalInventory inventory) {
        return inventory.size() != configuredSize()
                && !(player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu);
    }

    private static ItemStack resolveTerminal(Player player, @Nullable ItemStack terminalHint) {
        if (terminalHint != null && !terminalHint.isEmpty()) {
            return terminalHint;
        }
        return WcwtToolkitAccess.findTerminal(player);
    }

    private static AppEngInternalInventory loadItems(Player player, ItemStack terminal) {
        var inventory = newServerInventory(player, false);
        var legacy = terminal.isEmpty() ? null : terminal.get(ModComponents.TOOLKIT_INV.get());
        withLoadSuppressed(() -> {
            var shared = playerData(player).getCompound(PLAYER_TOOLKIT_DATA_TAG);
            // Player data without an item list (for example only remembered slots) has not been migrated yet.
            boolean migrated = shared.contains(ITEMS_TAG, Tag.TAG_LIST);
            if (migrated) {
                readSlots(player, shared, inventory, true);
            }
            if (legacy != null) {
                mergeLegacyToolkit(legacy.stream().toList(), inventory, stack -> returnToPlayer(player, stack));
                debug("merged item-bound toolkit into player data for player={}", player);
            }
        });
        if (legacy != null) {
            terminal.remove(ModComponents.TOOLKIT_INV.get());
        }
        saveItems(player, inventory);
        return inventory;
    }

    /**
     * Folds a toolkit that older versions stored on a terminal into the shared toolkit. Each item keeps its
     * slot when that slot is free, otherwise takes the first free slot, and anything that does not fit goes
     * back to the player instead of being deleted with the component.
     */
    static void mergeLegacyToolkit(java.util.List<ItemStack> legacy, InternalInventory inventory,
                                   java.util.function.Consumer<ItemStack> overflow) {
        for (int slot = 0; slot < legacy.size(); slot++) {
            ItemStack stack = legacy.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int target = slot < inventory.size() && inventory.getStackInSlot(slot).isEmpty() ? slot : -1;
            if (target < 0) {
                target = firstFreeSlot(inventory);
            }
            if (target >= 0) {
                inventory.setItemDirect(target, stack.copy());
            } else {
                overflow.accept(stack.copy());
            }
        }
    }

    private static int firstFreeSlot(InternalInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static void returnToPlayer(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static AppEngInternalInventory loadMemory(Player player, ItemStack terminal) {
        var inventory = newServerInventory(player, true);
        withLoadSuppressed(() -> {
            var memoryTag = playerData(player).getCompound(PLAYER_TOOLKIT_DATA_TAG).getCompound(MEMORY_TAG);
            if (!memoryTag.isEmpty()) {
                readSlots(player, memoryTag, inventory, false);
            } else {
                inventory.fromItemContainerContents(
                        terminal.getOrDefault(ModComponents.TOOLKIT_MEMORY.get(), ItemContainerContents.EMPTY));
            }
        });
        if (!terminal.isEmpty()) {
            terminal.remove(ModComponents.TOOLKIT_MEMORY.get());
        }
        saveMemory(player, inventory);
        return inventory;
    }

    private static AppEngInternalInventory newServerInventory(Player player, boolean memory) {
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                if (LOADING.get()) {
                    return;
                }
                if (memory) {
                    saveMemory(player, inv);
                } else {
                    saveItems(player, inv);
                }
            }

            @Override
            public boolean isClientSide() {
                return false;
            }
        }, configuredSize());
        if (!memory) {
            inventory.setFilter(new ToolkitItemFilter());
        }
        return inventory;
    }

    private static void saveItems(Player player, InternalInventory inventory) {
        var data = playerData(player);
        var existing = data.getCompound(PLAYER_TOOLKIT_DATA_TAG);
        var serialized = serializeSlots(player, inventory);
        if (existing.contains(MEMORY_TAG, Tag.TAG_COMPOUND)) {
            serialized.put(MEMORY_TAG, existing.getCompound(MEMORY_TAG));
        }
        data.put(PLAYER_TOOLKIT_DATA_TAG, serialized);
    }

    private static void saveMemory(Player player, InternalInventory memory) {
        var data = playerData(player);
        var shared = data.getCompound(PLAYER_TOOLKIT_DATA_TAG);
        shared.put(MEMORY_TAG, serializeSlots(player, memory));
        data.put(PLAYER_TOOLKIT_DATA_TAG, shared);
    }

    private static CompoundTag serializeSlots(Player player, InternalInventory inventory) {
        CompoundTag serialized = new CompoundTag();
        serialized.putInt(SIZE_TAG, inventory.size());
        ListTag items = new ListTag();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(STACK_TAG, stack.saveOptional(player.registryAccess()));
            items.add(entry);
        }
        serialized.put(ITEMS_TAG, items);
        return serialized;
    }

    /**
     * @param returnOverflow hand items stored past the current size back to the player, so shrinking
     *                       {@code toolkitSlotCount} does not delete them
     */
    private static void readSlots(Player player, CompoundTag source, AppEngInternalInventory inventory,
                                  boolean returnOverflow) {
        ListTag items = source.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt(SLOT_TAG);
            if (slot < 0 || !entry.contains(STACK_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), entry.getCompound(STACK_TAG));
            if (slot < inventory.size()) {
                inventory.setItemDirect(slot, stack);
            } else if (returnOverflow && !stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static void withLoadSuppressed(Runnable action) {
        if (LOADING.get()) {
            action.run();
            return;
        }
        LOADING.set(true);
        try {
            action.run();
        } finally {
            LOADING.set(false);
        }
    }

    private static void debug(String message, Player player) {
        if (DEBUG) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: " + message, player.getScoreboardName());
        }
    }

    private static final class ToolkitItemFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return ToolkitItemRules.mayPlace(slot, stack);
        }
    }
}
