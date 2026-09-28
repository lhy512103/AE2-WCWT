package com.lhy.wcwt.compat.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class CraftingInput implements Container {
    private final int width;
    private final int height;
    private final NonNullList<ItemStack> items;

    private CraftingInput(int width, int height, List<ItemStack> items) {
        this.width = width;
        this.height = height;
        this.items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int i = 0; i < this.items.size() && i < items.size(); i++) {
            this.items.set(i, items.get(i));
        }
    }

    public static CraftingInput of(int width, int height, List<ItemStack> items) {
        return new CraftingInput(width, height, items);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = existing.split(amount);
        if (existing.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
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
        items.clear();
    }
}
