package com.lhy.wcwt.init;

import com.lhy.wcwt.WcwtMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = 
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WcwtMod.MOD_ID);
    
    /**
     * AE2WTLIB装备槽库存 (头盔, 胸甲, 护腿, 靴子, 副手)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> AE2WTLIB_ARMOR_INV = 
            register("ae2wtlib_armor_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));
    
    /**
     * 装饰性装备槽库存 (头盔, 胸甲, 护腿, 靴子)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> DECORATIVE_ARMOR_INV = 
            register("decorative_armor_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));
    
    /**
     * Curios槽位库存
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> CURIOS_INV = 
            register("curios_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));
    
    /**
     * 高级样板编码槽库存 (复制样板, 替换输入, 替换输出)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> ADVANCED_PATTERN_INV = 
            register("advanced_pattern_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));
    
    /**
     * 样板缓存区库存 (36个槽位)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> PATTERN_CACHE_INV = 
            register("pattern_cache_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 工具包库存
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> TOOLKIT_INV =
            register("toolkit_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 工具包记忆槽位过滤模板。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> TOOLKIT_MEMORY =
            register("toolkit_memory", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * AE2 原版显示元件槽库存 (5个槽位)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> VIEW_CELL_INV =
            register("view_cell_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 元件工作台 - 存储元件槽位 (1个槽位)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> STORAGE_CELL_INV =
            register("storage_cell_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 谐振样板缓存区库存 (21个槽位，3行×7列)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> RESONATING_PATTERN_CACHE_INV =
            register("resonating_pattern_cache_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 手动锻造台工作区库存 (3个槽位)。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> MANUAL_SMITHING_INV =
            register("manual_smithing_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 手动铁砧工作区库存 (2个槽位)。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> MANUAL_ANVIL_INV =
            register("manual_anvil_inv", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 无线通用综合工作终端内嵌的其它无线终端。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> UNIVERSAL_TERMINALS =
            register("universal_terminals", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /**
     * 无线综合非通用终端当前打开的子终端。-1 = 综合工作终端，其它值 = 内嵌终端下标。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CURRENT_UNIVERSAL_TERMINAL =
            register("current_universal_terminal", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * JEI 拉取目标锁定状态：false = 样板编码区，true = 手动合成 3x3。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CRAFTING_GRID_LOCKED =
            register("crafting_grid_locked", builder -> builder
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * 左上手动工作区当前模式。0 = 工作台，1 = 锻造台，2 = 铁砧。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MANUAL_WORKSPACE_MODE =
            register("manual_workspace_mode", builder -> builder
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT));

    /**
     * 铁砧工作区当前重命名文本。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MANUAL_ANVIL_NAME =
            register("manual_anvil_name", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /**
     * 手动合成区物品替换开关。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MANUAL_CRAFTING_ITEM_SUBSTITUTION =
            register("manual_crafting_item_substitution", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * 手动合成区流体替换开关。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MANUAL_CRAFTING_FLUID_SUBSTITUTION =
            register("manual_crafting_fluid_substitution", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * 样板管理区“启用上传样板功能”开关。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PATTERN_MANAGEMENT_UPLOAD_ENABLED =
            register("pattern_management_upload_enabled", builder -> builder
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * 样板管理区显示模式。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PATTERN_MANAGEMENT_DISPLAY_MODE =
            register("pattern_management_display_mode", builder -> builder
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT));

    /**
     * 样板管理区是否显示样板槽。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PATTERN_MANAGEMENT_SHOW_SLOTS =
            register("pattern_management_show_slots", builder -> builder
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * 样板管理区搜索模式。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PATTERN_MANAGEMENT_SEARCH_MODE =
            register("pattern_management_search_mode", builder -> builder
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT));

    /**
     * 编码样板的 WCWT 上传元数据。
     * 用于在样板离开当前编码上下文后，仍能恢复其自动上传目标信息。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PATTERN_UPLOAD_DATA =
            register("pattern_upload_data", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, Consumer<DataComponentType.Builder<T>> customizer) {
        return DATA_COMPONENTS.register(name, () -> {
            var builder = DataComponentType.<T>builder();
            customizer.accept(builder);
            return builder.build();
        });
    }
}
