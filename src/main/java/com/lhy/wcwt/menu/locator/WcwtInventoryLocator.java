package com.lhy.wcwt.menu.locator;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record WcwtInventoryLocator(int slot, @Nullable BlockPos blockPos) implements WcwtItemLocator {
    public WcwtInventoryLocator(int slot) {
        this(slot, null);
    }

    @Override
    @Nullable
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack stack = locateItem(player);
        if (!stack.isEmpty() && stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal) {
            ItemMenuHost menuHost = terminal.getMenuHost(player, slot, stack, blockPos);
            if (hostInterface.isInstance(menuHost)) {
                return hostInterface.cast(menuHost);
            }
            WcwtMod.LOGGER.warn("Item in inventory slot {} did not create compatible WCWT menu {}", slot,
                    hostInterface);
        }
        if (hostInterface.isInstance(stack)) {
            return hostInterface.cast(stack);
        }
        return null;
    }

    @Override
    public ItemStack locateItem(Player player) {
        return slot >= 0 && slot < player.getInventory().getContainerSize()
                ? player.getInventory().getItem(slot)
                : ItemStack.EMPTY;
    }

    @Override
    public boolean storeItem(Player player, ItemStack stack) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return false;
        }
        player.getInventory().setItem(slot, stack);
        return true;
    }

    @Override
    public Integer inventorySlot() {
        return slot;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeBoolean(blockPos != null);
        if (blockPos != null) {
            buf.writeBlockPos(blockPos);
        }
    }

    public static WcwtInventoryLocator readFromPacket(FriendlyByteBuf buf) {
        int slot = buf.readInt();
        BlockPos blockPos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new WcwtInventoryLocator(slot, blockPos);
    }

    @Override
    public String toString() {
        return "wcwt inventory slot " + slot;
    }
}
