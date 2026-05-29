package com.lhy.wcwt.menu;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

public class WcwtSlotSemantics {
    public static final SlotSemantic AE2WTLIB_HELMET = getAe2wtlibSemantic("HELMET");
    public static final SlotSemantic AE2WTLIB_CHESTPLATE = getAe2wtlibSemantic("CHESTPLATE");
    public static final SlotSemantic AE2WTLIB_LEGGINGS = getAe2wtlibSemantic("LEGGINGS");
    public static final SlotSemantic AE2WTLIB_BOOTS = getAe2wtlibSemantic("BOOTS");
    public static final SlotSemantic AE2WTLIB_OFFHAND = getAe2wtlibSemantic("OFFHAND");
    
    // 装饰盔甲槽位
    public static final SlotSemantic DECORATIVE_HELMET = SlotSemantics.register("DECORATIVE_HELMET", true);
    public static final SlotSemantic DECORATIVE_ARMOR = SlotSemantics.register("DECORATIVE_ARMOR", true);
    public static final SlotSemantic DECORATIVE_SHIN_GUARDS = SlotSemantics.register("DECORATIVE_SHIN_GUARDS", true);
    public static final SlotSemantic DECORATIVE_BOOTS = SlotSemantics.register("DECORATIVE_BOOTS", true);
    
    // 饰品槽位
    public static final SlotSemantic AE_CURIOS = SlotSemantics.register("AE_CURIOS", true);
    public static final SlotSemantic WCWT_TOOLKIT = SlotSemantics.register("WCWT_TOOLKIT", true);
    
    // 高级编码槽位
    public static final SlotSemantic COPY_PATTERN = SlotSemantics.register("COPY_PATTERN", false);
    public static final SlotSemantic REPLACE_INPUT = SlotSemantics.register("REPLACE_INPUT", false);
    public static final SlotSemantic REPLACE_OUTPUT = SlotSemantics.register("REPLACE_OUTPUT", false);
    public static final SlotSemantic WCWT_PATTERN_CACHE = SlotSemantics.register("WCWT_PATTERN_CACHE", false);
    public static final SlotSemantic WCWT_PATTERN_CRAFTING_GRID = SlotSemantics.register("WCWT_PATTERN_CRAFTING_GRID", false);
    public static final SlotSemantic WCWT_PATTERN_PROCESSING_INPUTS = SlotSemantics.register("WCWT_PATTERN_PROCESSING_INPUTS", false);
    public static final SlotSemantic WCWT_PATTERN_PROCESSING_OUTPUTS = SlotSemantics.register("WCWT_PATTERN_PROCESSING_OUTPUTS", false);
    public static final SlotSemantic WCWT_PATTERN_SMITHING_TEMPLATE = SlotSemantics.register("WCWT_PATTERN_SMITHING_TEMPLATE", false);
    public static final SlotSemantic WCWT_PATTERN_SMITHING_BASE = SlotSemantics.register("WCWT_PATTERN_SMITHING_BASE", false);
    public static final SlotSemantic WCWT_PATTERN_SMITHING_ADDITION = SlotSemantics.register("WCWT_PATTERN_SMITHING_ADDITION", false);
    public static final SlotSemantic WCWT_PATTERN_STONECUTTING_INPUT = SlotSemantics.register("WCWT_PATTERN_STONECUTTING_INPUT", false);
    public static final SlotSemantic WCWT_PATTERN_PREVIEW = SlotSemantics.register("WCWT_PATTERN_PREVIEW", false);
    public static final SlotSemantic WCWT_MANUAL_SMITHING_TEMPLATE = SlotSemantics.register("WCWT_MANUAL_SMITHING_TEMPLATE", false);
    public static final SlotSemantic WCWT_MANUAL_SMITHING_BASE = SlotSemantics.register("WCWT_MANUAL_SMITHING_BASE", false);
    public static final SlotSemantic WCWT_MANUAL_SMITHING_ADDITION = SlotSemantics.register("WCWT_MANUAL_SMITHING_ADDITION", false);
    public static final SlotSemantic WCWT_MANUAL_SMITHING_RESULT = SlotSemantics.register("WCWT_MANUAL_SMITHING_RESULT", false);
    public static final SlotSemantic WCWT_MANUAL_ANVIL_LEFT = SlotSemantics.register("WCWT_MANUAL_ANVIL_LEFT", false);
    public static final SlotSemantic WCWT_MANUAL_ANVIL_RIGHT = SlotSemantics.register("WCWT_MANUAL_ANVIL_RIGHT", false);
    public static final SlotSemantic WCWT_MANUAL_ANVIL_RESULT = SlotSemantics.register("WCWT_MANUAL_ANVIL_RESULT", false);

    // 元件工作台 - 存储元件槽位（单槽）
    public static final SlotSemantic WCWT_STORAGE_CELL = SlotSemantics.register("WCWT_STORAGE_CELL", false);
    public static final SlotSemantic WCWT_RESONATING_STORAGE = SlotSemantics.register("WCWT_RESONATING_STORAGE", false);

    // 元件工作台 - 元件升级卡槽位（最多 8 格，受元件实际可用升级槽数量动态显示）
    public static final SlotSemantic WCWT_CELL_UPGRADE = SlotSemantics.register("WCWT_CELL_UPGRADE", false);

    public static void init() {
        // 触发类加载以注册槽位语义
    }

    private static SlotSemantic getAe2wtlibSemantic(String id) {
        try {
            Class.forName("de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("AE2WTLib slot semantics are unavailable", e);
        }
        return SlotSemantics.getOrThrow("AE2WTLIB_" + id);
    }
}
