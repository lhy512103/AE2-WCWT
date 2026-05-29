package com.lhy.wcwt.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class WcwtRestockState {
    private static boolean enabled;
    private static Map<Item, Long> availableItems = Map.of();

    private WcwtRestockState() {
    }

    public static void update(boolean enabled, Map<Item, Long> items) {
        WcwtRestockState.enabled = enabled;
        WcwtRestockState.availableItems = new HashMap<>(items);
    }

    public static boolean shouldRender(ItemStack stack) {
        return enabled && !stack.isEmpty() && availableItems.containsKey(stack.getItem())
                && getAccessibleAmount(stack) > stack.getCount();
    }

    public static long getAccessibleAmount(ItemStack stack) {
        return stack.getCount() + availableItems.getOrDefault(stack.getItem(), 0L);
    }
}
