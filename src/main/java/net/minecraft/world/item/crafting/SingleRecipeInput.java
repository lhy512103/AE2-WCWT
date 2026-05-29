package net.minecraft.world.item.crafting;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class SingleRecipeInput implements Container {
    private ItemStack item;

    public SingleRecipeInput(ItemStack item) {
        this.item = item;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? item : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = item.split(amount);
        if (item.isEmpty()) {
            item = ItemStack.EMPTY;
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = item;
        item = ItemStack.EMPTY;
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            item = stack;
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        item = ItemStack.EMPTY;
    }
}
