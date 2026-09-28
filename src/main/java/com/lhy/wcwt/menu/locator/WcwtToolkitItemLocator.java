package com.lhy.wcwt.menu.locator;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.InternalInventory;
import appeng.menu.locator.MenuLocator;
import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.helpers.WcwtToolkitStore;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

/**
 * Locates an item menu on a toolkit extra-bar cell.
 *
 * <p>With a side page selected the main hand is a toolkit cell, which AE2's
 * {@code MenuLocators.forHand} cannot find in the player inventory and throws on. Wrenches, network
 * tools, wireless terminals and other item menus opened from the extra bar resolve here instead.
 */
public record WcwtToolkitItemLocator(int toolkitIndex, @Nullable BlockPos blockPos) implements WcwtItemLocator {

    @Nullable
    public static MenuLocator forHand(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !WcwtToolkitHand.isOverrideActive(player)) {
            return null;
        }
        int index = WcwtToolkitHotbarState.toolkitIndex(player);
        return WcwtToolkitHotbarState.isValidToolkitIndex(index) ? new WcwtToolkitItemLocator(index, null) : null;
    }

    @Nullable
    public static MenuLocator forUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(forHand(player, context.getHand()) instanceof WcwtToolkitItemLocator toolkit)) {
            return null;
        }
        return new WcwtToolkitItemLocator(toolkit.toolkitIndex(), context.getClickedPos());
    }

    @Override
    @Nullable
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack stack = locateItem(player);
        if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal) {
            // Bound to this locator so returning from a submenu reopens the toolkit cell, not the vanilla slot.
            ItemMenuHost menuHost = terminal.getMenuHost(player, this, stack);
            return hostInterface.isInstance(menuHost) ? hostInterface.cast(menuHost) : null;
        }
        if (!stack.isEmpty() && stack.getItem() instanceof IMenuItem guiItem) {
            ItemMenuHost menuHost = guiItem.getMenuHost(player, player.getInventory().selected, stack, blockPos);
            if (hostInterface.isInstance(menuHost)) {
                return hostInterface.cast(menuHost);
            }
        }
        if (hostInterface.isInstance(stack)) {
            return hostInterface.cast(stack);
        }
        return null;
    }

    @Override
    public ItemStack locateItem(Player player) {
        return WcwtToolkitHotbarState.stackAt(player, toolkitIndex);
    }

    @Override
    public boolean storeItem(Player player, ItemStack stack) {
        if (!WcwtToolkitHotbarState.isValidToolkitIndex(toolkitIndex) || player.level().isClientSide()) {
            return false;
        }
        InternalInventory inventory = WcwtToolkitStore.items(player, null);
        if (inventory == null || toolkitIndex >= inventory.size()) {
            return false;
        }
        inventory.setItemDirect(toolkitIndex, stack);
        return true;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeVarInt(toolkitIndex);
        buf.writeBoolean(blockPos != null);
        if (blockPos != null) {
            buf.writeBlockPos(blockPos);
        }
    }

    public static WcwtToolkitItemLocator readFromPacket(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        BlockPos blockPos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new WcwtToolkitItemLocator(index, blockPos);
    }

    @Override
    public String toString() {
        return "wcwt toolkit cell " + toolkitIndex;
    }
}
