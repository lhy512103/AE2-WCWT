package com.lhy.wcwt.api;

/**
 * 合成锁定宿主接口
 * 用于锁定JEI配方拉取到合成网格而非样板编码网格
 */
public interface ICraftingLockHost {
    /**
     * 检查合成网格是否被锁定
     * @return true表示锁定到合成网格，false表示拉取到样板编码网格
     */
    boolean isCraftingGridLocked();
    
    /**
     * 设置合成网格锁定状态
     * @param locked 是否锁定
     */
    void setCraftingGridLocked(boolean locked);
    
    /**
     * 切换合成网格锁定状态
     */
    default void toggleCraftingGridLock() {
        setCraftingGridLocked(!isCraftingGridLocked());
    }
}
