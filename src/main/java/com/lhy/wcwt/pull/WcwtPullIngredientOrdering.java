package com.lhy.wcwt.pull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

/** 自 ae2utility PullIngredientOrdering 迁入：JEI 多选一物品时优先匹配数据组件更具体的堆叠。 */
public final class WcwtPullIngredientOrdering {

    private WcwtPullIngredientOrdering() {
    }

    public static List<ItemStack> preferSpecificComponentsFirst(List<ItemStack> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) {
            return alternatives == null ? List.of() : List.copyOf(alternatives);
        }
        if (alternatives.size() == 1) {
            return List.copyOf(alternatives);
        }
        List<ItemStack> copy = new ArrayList<>(alternatives.size());
        copy.addAll(alternatives);
        copy.sort(ITEM_STACK_COMPARATOR);
        return copy;
    }

    public static List<AEItemKey> preferSpecificItemKeysFirst(List<AEItemKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return keys == null ? List.of() : List.copyOf(keys);
        }
        if (keys.size() == 1) {
            return List.copyOf(keys);
        }
        List<AEItemKey> copy = new ArrayList<>(keys);
        copy.sort(AE_ITEM_KEY_COMPARATOR);
        return copy;
    }

    public static int genericStackItemSpecificityRank(GenericStack stack) {
        if (stack == null || stack.what() == null) {
            return 0;
        }
        if (!(stack.what() instanceof AEItemKey itemKey)) {
            return 0;
        }
        return itemKeyComponentSpecificityRank(itemKey);
    }

    public static int itemKeyComponentSpecificityRank(AEItemKey key) {
        if (key == null) {
            return 0;
        }
        return componentSpecificityRank(key.toStack().copyWithCount(1));
    }

    public static int componentSpecificityRank(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack plain = new ItemStack(stack.getItem(), 1);
        return WcwtStackMatching.sameItemAndTag(stack, plain) ? 0 : 1;
    }

    private static final Comparator<ItemStack> ITEM_STACK_COMPARATOR = Comparator
            .comparingInt(WcwtPullIngredientOrdering::componentSpecificityRank)
            .reversed()
            .thenComparing(ItemStack::getDescriptionId)
            .thenComparing(stack -> String.valueOf(stack.getTag()));

    private static final Comparator<AEItemKey> AE_ITEM_KEY_COMPARATOR = Comparator
            .comparingInt(WcwtPullIngredientOrdering::itemKeyComponentSpecificityRank)
            .reversed()
            .thenComparing(k -> k.toStack().getDescriptionId())
            .thenComparing(k -> String.valueOf(k.toStack().getTag()));
}
