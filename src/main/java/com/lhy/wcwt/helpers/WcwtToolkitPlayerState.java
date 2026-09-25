package com.lhy.wcwt.helpers;

import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Toolkit hotbar state of a single player object.
 *
 * <p>Lives in a non-persistent attachment, so the logical client and the integrated server each own
 * their copy and never share mutable state across threads. A respawned or dimension-changed player
 * starts fresh and is refilled from player data (server) or a full sync (client).
 */
public final class WcwtToolkitPlayerState {
    WcwtToolkitHotbarState.Bar page = WcwtToolkitHotbarState.Bar.CENTER;
    int pageRevision;
    boolean hasCard;

    @Nullable
    AppEngInternalInventory items;
    @Nullable
    AppEngInternalInventory memory;

    @Nullable
    ItemStack[] lastSentItems;
    @Nullable
    ItemStack[] lastSentMemory;
    boolean lastSentHasCard;
    int lastSentPageRevision = -1;

    long budgetTick = Long.MIN_VALUE;
    int budgetUsed;

    final ItemStack[] clientItems = emptyHotbar();
    final ItemStack[] clientMemory = emptyHotbar();

    static ItemStack[] emptyHotbar() {
        ItemStack[] stacks = new ItemStack[WcwtToolkitAccess.HOTBAR_SLOTS];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }
}
