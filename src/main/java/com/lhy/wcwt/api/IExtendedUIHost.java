package com.lhy.wcwt.api;

/**
 * 扩展UI宿主接口
 * 用于管理右侧扩展UI面板的状态
 */
public interface IExtendedUIHost {
    /**
     * 扩展UI类型
     */
    enum ExtendedUIType {
        NONE,               // 无扩展UI
        ADVANCED_CODING,    // 高级编码
        COSMETIC_ARMOR,     // 装饰装甲
        CURIOS,            // 饰品
        TOOL_SLOTS_BOX,    // 卡槽箱（AE 升级卡 3x3）
        TOOLKIT,           // 工具包
        RESONATING_LIGHTNING_PATTERN_CODING // 谐振过载编码器
    }
    
    /**
     * 获取当前打开的扩展UI类型
     */
    ExtendedUIType getCurrentExtendedUI();
    
    /**
     * 设置当前打开的扩展UI类型
     */
    void setCurrentExtendedUI(ExtendedUIType type);
    
    /**
     * 关闭当前扩展UI
     */
    default void closeExtendedUI() {
        setCurrentExtendedUI(ExtendedUIType.NONE);
    }
}
