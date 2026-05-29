package net.minecraft.world.item.crafting;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class SmithingRecipeInput implements Container {
    private final ItemStack[] items = new ItemStack[3];

    public SmithingRecipeInput(ItemStack template, ItemStack base, ItemStack addition) {
        this.items[0] = template;
        this.items[1] = base;
        this.items[2] = addition;
    }

    @Override
    public int getContainerSize() {
        return items.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.length ? items[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.length || items[slot].isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = items[slot].split(amount);
        if (items[slot].isEmpty()) {
            items[slot] = ItemStack.EMPTY;
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.length) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = items[slot];
        items[slot] = ItemStack.EMPTY;
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.length) {
            items[slot] = stack;
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
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }
}
