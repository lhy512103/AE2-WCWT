package com.lhy.wcwt.helpers;

import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The player's toolkit and its remembered slots.
 *
 * <p>On the server there is exactly one live inventory pair per player object, shared by every
 * terminal menu, the extra hotbars, pickup and the network-tool locators, so no two copies can
 * diverge. Contents persist in {@code PlayerPersisted/wcwt/shared_toolkit}, which survives death.
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

    /** A detached client inventory for a terminal menu's slots, filled by menu slot sync. */
    public static AppEngInternalInventory clientInventory(boolean memory) {
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
            }

            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
            }

            @Override
            public boolean isClientSide() {
                return true;
            }
        }, configuredSize());
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
        withLoadSuppressed(() -> {
            var shared = playerData(player).getCompound(PLAYER_TOOLKIT_DATA_TAG);
            if (shared.isEmpty()) {
                inventory.readFromNBT(getOrCreateRootTag(terminal), WirelessComprehensiveWorkTerminalMenuHost.LEGACY_TOOLKIT_INV_KEY);
                debug("migrated item-bound toolkit into player data for player={}", player);
            } else if (shared.contains(ITEMS_TAG, Tag.TAG_LIST)) {
                readSlots(player, shared, inventory, true);
            } else {
                inventory.readFromNBT(shared, WirelessComprehensiveWorkTerminalMenuHost.LEGACY_TOOLKIT_INV_KEY);
                debug("upgraded legacy shared toolkit codec data for player={}", player);
            }
        });
        // Keep item NBT clean for old saves — the item mirror is deprecated.
        if (!terminal.isEmpty()) {
            var root = terminal.getTag();
            if (root != null && root.contains(WirelessComprehensiveWorkTerminalMenuHost.LEGACY_TOOLKIT_INV_KEY, Tag.TAG_LIST)) {
                root.remove(WirelessComprehensiveWorkTerminalMenuHost.LEGACY_TOOLKIT_INV_KEY);
            }
        }
        saveItems(player, inventory);
        return inventory;
    }

    private static AppEngInternalInventory loadMemory(Player player, ItemStack terminal) {
        var inventory = newServerInventory(player, true);
        withLoadSuppressed(() -> {
            var memoryTag = playerData(player).getCompound(PLAYER_TOOLKIT_DATA_TAG).getCompound(MEMORY_TAG);
            if (!memoryTag.isEmpty()) {
                readSlots(player, memoryTag, inventory, false);
            } else {
                inventory.readFromNBT(getOrCreateRootTag(terminal), WirelessComprehensiveWorkTerminalMenuHost.LEGACY_TOOLKIT_MEMORY_INV_KEY);
            }
        });
        saveMemory(player, inventory);
        return inventory;
    }

    private static AppEngInternalInventory newServerInventory(Player player, boolean memory) {
        final AppEngInternalInventory[] inventoryRef = new AppEngInternalInventory[1];
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
                if (LOADING.get()) {
                    return;
                }
                if (memory) {
                    saveMemory(player, inventoryRef[0]);
                } else {
                    saveItems(player, inventoryRef[0]);
                }
            }

            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
            }

            @Override
            public boolean isClientSide() {
                return false;
            }
        }, configuredSize());
        inventoryRef[0] = inventory;
        if (!memory) {
            inventory.setFilter(new ToolkitItemFilter());
        }
        return inventory;
    }

    private static void saveItems(Player player, InternalInventory inventory) {
        var data = playerData(player);
        var existing = data.getCompound(PLAYER_TOOLKIT_DATA_TAG);
        var serialized = serializeSlots(inventory);
        if (existing.contains(MEMORY_TAG, Tag.TAG_COMPOUND)) {
            serialized.put(MEMORY_TAG, existing.getCompound(MEMORY_TAG));
        }
        data.put(PLAYER_TOOLKIT_DATA_TAG, serialized);
    }

    private static void saveMemory(Player player, InternalInventory memory) {
        var data = playerData(player);
        var shared = data.getCompound(PLAYER_TOOLKIT_DATA_TAG);
        shared.put(MEMORY_TAG, serializeSlots(memory));
        data.put(PLAYER_TOOLKIT_DATA_TAG, shared);
    }

    private static CompoundTag serializeSlots(InternalInventory inventory) {
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
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            entry.put(STACK_TAG, stackTag);
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
            ItemStack stack = ItemStack.of(entry.getCompound(STACK_TAG));
            if (slot < inventory.size()) {
                inventory.setItemDirect(slot, stack);
            } else if (returnOverflow && !stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }
        return stack.getTag();
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
