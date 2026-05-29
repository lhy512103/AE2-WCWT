package com.lhy.wcwt.api;

import appeng.api.inventories.InternalInventory;
import net.minecraft.world.item.ItemStack;

/**
 * 样板缓存宿主接口
 * 用于管理样板缓存区（36个槽位）
 */
public interface IPatternCachingHost {
    /**
     * 获取样板缓存区库存
     * @return 36个槽位的库存
     */
    InternalInventory getPatternCacheInventory();
    
    /**
     * 获取当前选中的样板索引（用于高级编码）
     * @return 选中的槽位索引，-1表示未选中
     */
    int getSelectedPatternIndex();
    
    /**
     * 设置选中的样板索引
     * @param index 槽位索引
     */
    void setSelectedPatternIndex(int index);
    
    /**
     * 获取选中的样板
     * @return 选中的样板物品，如果未选中则返回空
     */
    default ItemStack getSelectedPattern() {
        int index = getSelectedPatternIndex();
        if (index >= 0 && index < getPatternCacheInventory().size()) {
            return getPatternCacheInventory().getStackInSlot(index);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * 清除选中状态
     */
    default void clearSelection() {
        setSelectedPatternIndex(-1);
    }
}
