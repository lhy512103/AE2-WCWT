package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public final class WcwtToolkitHotbarState {
    public enum Bar {
        CENTER,
        LEFT,
        RIGHT
    }

    private static final Map<Player, Selection> SELECTIONS = new WeakHashMap<>();
    private static final Map<Player, ItemStack[]> CLIENT_SNAPSHOTS = new WeakHashMap<>();
    private static final Map<Player, ItemStack[]> CLIENT_MEMORY_SNAPSHOTS = new WeakHashMap<>();
    private static final Map<Player, Boolean> CLIENT_TOOLKIT_CARD = new WeakHashMap<>();

    private WcwtToolkitHotbarState() {
    }

    public static Bar getBar(Player player) {
        Selection selection = SELECTIONS.get(player);
        return selection == null ? Bar.CENTER : selection.bar;
    }

    public static int getSlot(Player player) {
        Selection selection = SELECTIONS.get(player);
        return selection == null ? player.getInventory().selected : selection.slot;
    }

    public static boolean isToolkitSelected(Player player) {
        return hasToolkitCard(player) && getBar(player) != Bar.CENTER;
    }

    public static boolean hasToolkitCard(Player player) {
        if (player.level().isClientSide()) {
            return Boolean.TRUE.equals(CLIENT_TOOLKIT_CARD.get(player));
        }
        return WcwtToolkitAccess.hasToolkitCard(player);
    }

    public static void setSelection(Player player, Bar bar, int slot) {
        if (bar == Bar.CENTER || !hasToolkitCard(player)) {
            SELECTIONS.remove(player);
            return;
        }
        int normalizedSlot = Math.max(0, Math.min(WcwtToolkitAccess.HOTBAR_SIZE - 1, slot));
        SELECTIONS.put(player, new Selection(bar, normalizedSlot, WcwtToolkitAccess.findInventory(player)));
    }

    public static void clear(Player player) {
        SELECTIONS.remove(player);
        CLIENT_SNAPSHOTS.remove(player);
        CLIENT_MEMORY_SNAPSHOTS.remove(player);
        CLIENT_TOOLKIT_CARD.remove(player);
    }

    public static int toolkitIndex(Player player) {
        Selection selection = SELECTIONS.get(player);
        if (selection == null || selection.bar == Bar.CENTER) {
            return -1;
        }
        return selection.bar == Bar.LEFT ? selection.slot : WcwtToolkitAccess.HOTBAR_SIZE + selection.slot;
    }

    public static InternalInventory getToolkitInventory(Player player) {
        Selection selection = SELECTIONS.get(player);
        if (selection != null && selection.inventory != null) {
            return selection.inventory;
        }
        return WcwtToolkitAccess.findInventory(player);
    }

    public static ItemStack getSelectedToolkit(Player player) {
        int index = toolkitIndex(player);
        if (index < 0) {
            return ItemStack.EMPTY;
        }
        if (player.level().isClientSide()) {
            ItemStack[] snapshot = getClientSnapshot(player);
            return snapshot[index];
        }
        Selection selection = SELECTIONS.get(player);
        InternalInventory inventory = selection == null ? null : selection.inventory;
        return inventory == null || index >= inventory.size() ? ItemStack.EMPTY : inventory.getStackInSlot(index);
    }

    public static void setSelectedToolkit(Player player, ItemStack stack) {
        int index = toolkitIndex(player);
        if (index < 0) {
            return;
        }
        if (player.level().isClientSide()) {
            getClientSnapshot(player)[index] = stack;
            return;
        }
        Selection selection = SELECTIONS.get(player);
        if (selection != null && selection.inventory != null && index < selection.inventory.size()) {
            selection.inventory.setItemDirect(index, stack);
        }
    }

    public static boolean persistSelectedToolkit(Player player) {
        int index = toolkitIndex(player);
        Selection selection = SELECTIONS.get(player);
        if (index < 0 || selection == null || selection.inventory == null || index >= selection.inventory.size()) {
            return false;
        }
        selection.inventory.setItemDirect(index, selection.inventory.getStackInSlot(index));
        return true;
    }

    public static boolean insertPickup(ItemEntity entity, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !hasToolkitCard(player)) {
            return false;
        }
        ItemStack entityStack = entity.getItem();
        if (!ToolkitItemRules.isBaseToolkitCandidate(entityStack)) {
            return false;
        }
        InternalInventory inventory = getToolkitInventory(player);
        InternalInventory memory = WcwtToolkitAccess.findMemoryInventory(player);
        if (inventory == null) {
            return false;
        }
        int selected = toolkitIndex(player);
        if (tryInsertPickup(serverPlayer, entity, inventory, selected, memory, true)
                || tryInsertHotbarPickup(serverPlayer, entity, inventory, memory, selected, true)) {
            return true;
        }
        return isToolkitSelected(player)
                && (tryInsertPickup(serverPlayer, entity, inventory, selected, memory, false)
                || tryInsertHotbarPickup(serverPlayer, entity, inventory, memory, selected, false));
    }

    private static boolean tryInsertHotbarPickup(ServerPlayer player, ItemEntity entity, InternalInventory inventory,
                                                 InternalInventory memory, int selected, boolean rememberedOnly) {
        for (int slot = 0; slot < WcwtToolkitAccess.HOTBAR_SLOTS; slot++) {
            if (slot != selected && tryInsertPickup(player, entity, inventory, slot, memory, rememberedOnly)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryInsertPickup(ServerPlayer player, ItemEntity entity, InternalInventory inventory,
                                           int slot, InternalInventory memory, boolean rememberedOnly) {
        ItemStack entityStack = entity.getItem();
        if (slot < 0 || slot >= inventory.size() || !inventory.getStackInSlot(slot).isEmpty()
                || !ToolkitItemRules.mayPlace(slot, entityStack)) {
            return false;
        }
        ItemStack remembered = memory != null && slot < memory.size() ? memory.getStackInSlot(slot) : ItemStack.EMPTY;
        if (rememberedOnly ? remembered.isEmpty() || !remembered.is(entityStack.getItem())
                : !remembered.isEmpty() && !remembered.is(entityStack.getItem())) {
            return false;
        }
        inventory.setItemDirect(slot, entityStack.copyWithCount(1));
        entityStack.shrink(1);
        player.awardStat(Stats.ITEM_PICKED_UP.get(entityStack.getItem()), 1);
        player.onItemPickup(entity);
        return true;
    }

    public static ItemStack[] getClientSnapshot(Player player) {
        ItemStack[] snapshot = CLIENT_SNAPSHOTS.get(player);
        if (snapshot != null) {
            return snapshot;
        }
        snapshot = new ItemStack[WcwtToolkitAccess.HOTBAR_SLOTS];
        java.util.Arrays.fill(snapshot, ItemStack.EMPTY);
        InternalInventory inventory = WcwtToolkitAccess.findInventory(player);
        if (inventory != null) {
            for (int i = 0; i < snapshot.length && i < inventory.size(); i++) {
                snapshot[i] = inventory.getStackInSlot(i).copy();
            }
        }
        CLIENT_SNAPSHOTS.put(player, snapshot);
        return snapshot;
    }

    public static void setClientSnapshot(Player player, java.util.List<ItemStack> stacks) {
        CLIENT_SNAPSHOTS.put(player, copyHotbar(stacks));
    }

    public static void setClientToolkitCard(Player player, boolean installed) {
        CLIENT_TOOLKIT_CARD.put(player, installed);
        if (!installed) {
            SELECTIONS.remove(player);
        }
    }

    public static ItemStack[] getClientMemorySnapshot(Player player) {
        ItemStack[] snapshot = CLIENT_MEMORY_SNAPSHOTS.get(player);
        if (snapshot != null) {
            return snapshot;
        }
        snapshot = new ItemStack[WcwtToolkitAccess.HOTBAR_SLOTS];
        java.util.Arrays.fill(snapshot, ItemStack.EMPTY);
        InternalInventory memory = WcwtToolkitAccess.findMemoryInventory(player);
        if (memory != null) {
            for (int i = 0; i < snapshot.length && i < memory.size(); i++) {
                snapshot[i] = memory.getStackInSlot(i).copy();
            }
        }
        CLIENT_MEMORY_SNAPSHOTS.put(player, snapshot);
        return snapshot;
    }

    public static void setClientMemorySnapshot(Player player, java.util.List<ItemStack> stacks) {
        CLIENT_MEMORY_SNAPSHOTS.put(player, copyHotbar(stacks));
    }

    private static ItemStack[] copyHotbar(java.util.List<ItemStack> stacks) {
        ItemStack[] snapshot = new ItemStack[WcwtToolkitAccess.HOTBAR_SLOTS];
        java.util.Arrays.fill(snapshot, ItemStack.EMPTY);
        for (int i = 0; i < snapshot.length && i < stacks.size(); i++) {
            snapshot[i] = stacks.get(i).copy();
        }
        return snapshot;
    }

    private static final class Selection {
        private final Bar bar;
        private final int slot;
        private final InternalInventory inventory;

        private Selection(Bar bar, int slot, InternalInventory inventory) {
            this.bar = bar;
            this.slot = slot;
            this.inventory = inventory;
        }
    }
}
