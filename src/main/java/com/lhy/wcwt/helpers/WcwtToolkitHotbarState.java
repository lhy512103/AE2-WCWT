package com.lhy.wcwt.helpers;

import appeng.api.inventories.InternalInventory;
import com.lhy.wcwt.init.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Toolkit extra hotbars: left toolkit 0-8, the vanilla hotbar, right toolkit 9-17.
 *
 * <p>{@code Inventory.selected} is always the slot inside the current page and the vanilla hotbar
 * contents never move. Selecting a side page only changes which toolkit cell
 * {@code Inventory.getSelected} reports while {@link WcwtToolkitHand#isOverrideActive} holds.
 */
public final class WcwtToolkitHotbarState {
    public enum Bar {
        CENTER,
        LEFT,
        RIGHT
    }

    private static final int PACKETS_PER_TICK = 8;

    private WcwtToolkitHotbarState() {
    }

    public static WcwtToolkitPlayerState state(Player player) {
        return player.getData(ModAttachments.TOOLKIT_STATE);
    }

    public static boolean hasToolkitCard(Player player) {
        return state(player).hasCard;
    }

    public static Bar getBar(Player player) {
        var state = state(player);
        return state.hasCard ? state.page : Bar.CENTER;
    }

    public static int getSlot(Player player) {
        return Math.floorMod(player.getInventory().selected, WcwtToolkitAccess.HOTBAR_SIZE);
    }

    public static boolean isToolkitSelected(Player player) {
        return getBar(player) != Bar.CENTER;
    }

    public static int toolkitIndex(Player player) {
        return toolkitIndex(getBar(player), getSlot(player));
    }

    public static int toolkitIndex(Bar bar, int slot) {
        if (bar == Bar.CENTER) {
            return -1;
        }
        int normalized = Math.floorMod(slot, WcwtToolkitAccess.HOTBAR_SIZE);
        return bar == Bar.LEFT ? normalized : WcwtToolkitAccess.HOTBAR_SIZE + normalized;
    }

    /** A selection made on this side; on the server the new page is pushed to the client. */
    public static void setSelection(Player player, Bar bar, int slot) {
        applySelection(player, bar, slot, !player.level().isClientSide());
    }

    /** A selection the client already made and shows; not echoed back. */
    public static void applyClientSelection(ServerPlayer player, Bar bar, int slot) {
        Bar before = getBar(player);
        int slotBefore = player.getInventory().selected;
        applySelection(player, bar, slot, false);
        if ((before != getBar(player) || slotBefore != player.getInventory().selected)
                && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            player.stopUsingItem();
        }
    }

    private static void applySelection(Player player, Bar bar, int slot, boolean notifyClient) {
        var state = state(player);
        player.getInventory().selected = Math.floorMod(slot, WcwtToolkitAccess.HOTBAR_SIZE);
        Bar target = bar == Bar.CENTER || !state.hasCard ? Bar.CENTER : bar;
        if (state.page != target) {
            state.page = target;
            if (notifyClient) {
                state.pageRevision++;
            }
        }
    }

    public static void setHasCard(Player player, boolean hasCard) {
        var state = state(player);
        state.hasCard = hasCard;
        if (!hasCard && state.page != Bar.CENTER) {
            state.page = Bar.CENTER;
            state.pageRevision++;
        }
    }

    public static boolean isValidToolkitIndex(int index) {
        return index >= 0 && index < WcwtToolkitAccess.HOTBAR_SLOTS;
    }

    public static ItemStack stackAt(Player player, int index) {
        if (!isValidToolkitIndex(index)) {
            return ItemStack.EMPTY;
        }
        if (player.level().isClientSide()) {
            return state(player).clientItems[index];
        }
        InternalInventory inventory = WcwtToolkitStore.items(player, null);
        return inventory == null || index >= inventory.size() ? ItemStack.EMPTY : inventory.getStackInSlot(index);
    }

    public static ItemStack memoryAt(Player player, int index) {
        if (!isValidToolkitIndex(index)) {
            return ItemStack.EMPTY;
        }
        if (player.level().isClientSide()) {
            return state(player).clientMemory[index];
        }
        InternalInventory memory = WcwtToolkitStore.memory(player, null);
        return memory == null || index >= memory.size() ? ItemStack.EMPTY : memory.getStackInSlot(index);
    }

    public static ItemStack getSelectedToolkit(Player player) {
        return stackAt(player, toolkitIndex(player));
    }

    /**
     * Writes the selected cell. Vanilla sometimes puts a stack the toolkit cannot hold into the main
     * hand, such as the empty bucket after pouring or the armor displaced by equipping; the cell is
     * emptied first so no second copy lingers, and the stack goes back to the player.
     */
    public static void setSelectedToolkit(Player player, ItemStack stack) {
        int index = toolkitIndex(player);
        if (!isValidToolkitIndex(index)) {
            return;
        }
        if (!stack.isEmpty() && !ToolkitItemRules.isBaseToolkitCandidate(stack)) {
            writeCell(player, index, ItemStack.EMPTY);
            if (!player.level().isClientSide()) {
                ItemStack remainder = stack.copy();
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }
            return;
        }
        writeCell(player, index, stack);
    }

    private static void writeCell(Player player, int index, ItemStack stack) {
        if (player.level().isClientSide()) {
            state(player).clientItems[index] = stack;
            return;
        }
        InternalInventory inventory = WcwtToolkitStore.items(player, null);
        if (inventory != null && index < inventory.size()) {
            inventory.setItemDirect(index, stack);
        }
    }

    /** Drops the selected extra-bar cell without touching the vanilla hotbar. */
    public static void dropSelected(ServerPlayer player, boolean all) {
        if (!isToolkitSelected(player)) {
            return;
        }
        ItemStack selected = getSelectedToolkit(player);
        if (selected.isEmpty() || !selected.onDroppedByPlayer(player)) {
            return;
        }
        ItemStack dropped = selected.copyWithCount(all ? selected.getCount() : 1);
        selected.shrink(dropped.getCount());
        setSelectedToolkit(player, selected.isEmpty() ? ItemStack.EMPTY : selected);
        player.drop(dropped, false, true);
    }

    /** Hand-paced actions; a burst inside one tick is never legitimate. */
    public static boolean tryAcquirePacketBudget(ServerPlayer player) {
        var state = state(player);
        long tick = player.level().getGameTime();
        if (state.budgetTick != tick) {
            state.budgetTick = tick;
            state.budgetUsed = 0;
        }
        return state.budgetUsed++ < PACKETS_PER_TICK;
    }

    public static boolean insertPickup(ItemEntity entity, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !hasToolkitCard(player)) {
            return false;
        }
        ItemStack entityStack = entity.getItem();
        if (!ToolkitItemRules.isBaseToolkitCandidate(entityStack)) {
            return false;
        }
        InternalInventory inventory = WcwtToolkitStore.items(player, null);
        InternalInventory memory = WcwtToolkitStore.memory(player, null);
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

    public static void applyClientSync(Player player, boolean hasCard, List<ItemStack> items, List<ItemStack> memory,
                                       int page) {
        var state = state(player);
        state.hasCard = hasCard;
        copyInto(state.clientItems, items);
        copyInto(state.clientMemory, memory);
        if (page >= 0 && page < Bar.values().length) {
            state.page = Bar.values()[page];
        }
        if (!hasCard) {
            state.page = Bar.CENTER;
        }
    }

    private static void copyInto(ItemStack[] target, List<ItemStack> source) {
        for (int i = 0; i < target.length; i++) {
            target[i] = i < source.size() ? source.get(i).copy() : ItemStack.EMPTY;
        }
    }
}
