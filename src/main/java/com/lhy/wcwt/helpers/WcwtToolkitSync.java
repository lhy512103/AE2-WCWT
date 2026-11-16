package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import appeng.items.tools.NetworkToolItem;
import com.lhy.wcwt.network.WcwtToolkitHotbarSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** Pushes the extra-bar cells, remembered items, card state and server-side page changes to the client. */
public final class WcwtToolkitSync {
    private WcwtToolkitSync() {
    }

    /** Server tick: refresh whether the toolkit card is carried, then send whatever changed. */
    public static void tick(ServerPlayer player) {
        WcwtToolkitHotbarState.setHasCard(player, WcwtToolkitAccess.hasToolkitCard(player));
        send(player, false, false);
    }

    public static void sendNow(ServerPlayer player) {
        send(player, true, false);
    }

    /** Everything including the page, for a client that has just (re)created its player. */
    public static void sendFull(ServerPlayer player) {
        WcwtToolkitHotbarState.setHasCard(player, WcwtToolkitAccess.hasToolkitCard(player));
        send(player, true, true);
    }

    /**
     * Called while the server builds a menu whose slot layout depends on the toolkit: the WCWT terminal
     * (toolkit size) or an AE2 toolbox backed by a toolkit network tool. A menu is constructed before its
     * open-screen packet is sent, so syncing here makes the client lay out the same slots.
     */
    public static void ensureLayoutSynced(ServerPlayer player) {
        send(player, false, false);
    }

    /** First toolkit slot holding a network tool, or -1. */
    public static int findNetworkToolSlot(@Nullable InternalInventory toolkit) {
        if (toolkit == null) {
            return -1;
        }
        for (int slot = 0; slot < toolkit.size(); slot++) {
            if (toolkit.getStackInSlot(slot).getItem() instanceof NetworkToolItem) {
                return slot;
            }
        }
        return -1;
    }

    private static void send(ServerPlayer player, boolean force, boolean includePage) {
        var state = WcwtToolkitHotbarState.state(player);
        // Only a toolkit that is already loaded can back a toolbox; loading it here would migrate every player.
        int networkToolSlot = findNetworkToolSlot(state.items);
        int toolkitSize = state.items != null ? state.items.size() : WcwtToolkitStore.configuredSize();
        boolean hasCard = state.hasCard;
        InternalInventory items = hasCard ? WcwtToolkitStore.items(player, null) : null;
        InternalInventory memory = hasCard ? WcwtToolkitStore.memory(player, null) : null;
        boolean pageChanged = includePage || state.lastSentPageRevision != state.pageRevision;
        boolean itemsChanged = !matchesLive(state.lastSentItems, items);
        boolean changed = pageChanged
                || itemsChanged
                || !matchesLive(state.lastSentMemory, memory)
                || state.lastSentHasCard != hasCard
                || state.lastSentNetworkToolSlot != networkToolSlot
                || state.lastSentToolkitSize != toolkitSize;
        if (!force && !changed) {
            return;
        }
        if (hasCard && itemsChanged && state.lastSentItems != null) {
            // A tool used from the extra bar changes in place without notifying the inventory.
            WcwtToolkitStore.persist(player);
        }
        state.lastSentItems = snapshot(items);
        state.lastSentMemory = snapshot(memory);
        state.lastSentHasCard = hasCard;
        state.lastSentPageRevision = state.pageRevision;
        state.lastSentNetworkToolSlot = networkToolSlot;
        state.lastSentToolkitSize = toolkitSize;
        PacketDistributor.sendToPlayer(player, new WcwtToolkitHotbarSyncPacket(
                Arrays.asList(state.lastSentItems), Arrays.asList(state.lastSentMemory), hasCard,
                pageChanged ? state.page.ordinal() : WcwtToolkitHotbarSyncPacket.PAGE_UNCHANGED,
                networkToolSlot, toolkitSize));
    }

    private static ItemStack[] snapshot(@Nullable InternalInventory inventory) {
        ItemStack[] stacks = WcwtToolkitPlayerState.emptyHotbar();
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = live(inventory, i).copy();
        }
        return stacks;
    }

    private static boolean matchesLive(@Nullable ItemStack[] sent, @Nullable InternalInventory inventory) {
        if (sent == null) {
            return false;
        }
        for (int i = 0; i < sent.length; i++) {
            if (!ItemStack.matches(sent[i], live(inventory, i))) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack live(@Nullable InternalInventory inventory, int index) {
        return inventory != null && index < inventory.size() ? inventory.getStackInSlot(index) : ItemStack.EMPTY;
    }
}
