package com.lhy.wcwt.helpers;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.core.definitions.AEItems;
import appeng.items.tools.MemoryCardItem;
import appeng.items.tools.NetworkToolItem;
import appeng.items.tools.quartz.QuartzCuttingKnifeItem;
import appeng.items.tools.quartz.QuartzWrenchItem;
import com.lhy.wcwt.config.WcwtServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import org.jetbrains.annotations.Nullable;

/**
 * 工具包槽位校验：基底规则 + 前 11 格的固定归类。
 */
public final class ToolkitItemRules {
    /** 前两行 6 列减去最后一格：剑、镐、斧、锹、锄头、扳手、切割刀、网络工具、内存卡、MEK 配置卡、MEK 配置器 */
    public static final int DEDICATED_SLOT_COUNT = 11;
    /** AE2 网络工具固定槽在第 8 列（逻辑下标从 0 计为 7）。 */
    public static final int NETWORK_TOOL_DEDICATED_INDEX = 7;

    private ToolkitItemRules() {
    }

    /**
     * 可放入任意「自由」工具包格的物品：不可堆叠，且不含已编码样板。
     */
    public static boolean isBaseToolkitCandidate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getMaxStackSize() != 1) {
            return false;
        }
        return !PatternDetailsHelper.isEncodedPattern(stack);
    }

    /**
     * 指定背包逻辑槽序号是否可以放入。
     */
    public static boolean mayPlace(int toolkitInventoryIndex, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (!isBaseToolkitCandidate(stack)) {
            return false;
        }
        if (toolkitInventoryIndex < DEDICATED_SLOT_COUNT) {
            return matchesDedicatedSlot(toolkitInventoryIndex, stack);
        }
        return true;
    }

    /** Shift + 快捷：从玩家储物格搬进工具包的筛选（与基底规则一致）。 */
    public static boolean isEligibleForToolkitQuickFill(ItemStack stack) {
        return isBaseToolkitCandidate(stack);
    }

    /** 是否为可放入专用网络工具格的 AE 网络工具物品。 */
    public static boolean isAeNetworkToolkitItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof NetworkToolItem || AEItems.NETWORK_TOOL.isSameAs(stack));
    }

    /**
     * 优先尝试的逻辑槽序号：匹配的专用槽（若有）先于通用区。
     */
    public static int[] insertionIndexOrder(ItemStack stack) {
        int totalSlots = Math.max(DEDICATED_SLOT_COUNT, WcwtServerConfig.toolkitSlotCount());
        int pref = preferredDedicatedSlot(stack);
        int generalSlots = totalSlots - DEDICATED_SLOT_COUNT;
        if (pref < 0) {
            int[] out = new int[generalSlots];
            int k = 0;
            for (int i = DEDICATED_SLOT_COUNT; i < totalSlots; i++) {
                out[k++] = i;
            }
            return out;
        }
        int[] out = new int[generalSlots + 1];
        out[0] = pref;
        int k = 1;
        for (int i = DEDICATED_SLOT_COUNT; i < totalSlots; i++) {
            out[k++] = i;
        }
        return out;
    }

    /**
     * 与该物品匹配的「第一个」专用槽序号，用于快捷放入顺序；若没有则返回 -1。
     */
    public static int preferredDedicatedSlot(ItemStack stack) {
        for (int i = 0; i < DEDICATED_SLOT_COUNT; i++) {
            if (matchesDedicatedSlot(i, stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 前 11 格空槽悬停提示翻译键；非专用格返回 null。
     */
    @Nullable
    public static String dedicatedSlotTooltipKey(int toolkitInventoryIndex) {
        if (toolkitInventoryIndex < 0 || toolkitInventoryIndex >= DEDICATED_SLOT_COUNT) {
            return null;
        }
        return switch (toolkitInventoryIndex) {
            case 0 -> "gui.wcwt.toolkit.slot.sword";
            case 1 -> "gui.wcwt.toolkit.slot.pickaxe";
            case 2 -> "gui.wcwt.toolkit.slot.axe";
            case 3 -> "gui.wcwt.toolkit.slot.shovel";
            case 4 -> "gui.wcwt.toolkit.slot.hoe";
            case 5 -> "gui.wcwt.toolkit.slot.wrench";
            case 6 -> "gui.wcwt.toolkit.slot.cutting_knife";
            case 7 -> "gui.wcwt.toolkit.slot.network_tool";
            case 8 -> "gui.wcwt.toolkit.slot.memory_card";
            case 9 -> "gui.wcwt.toolkit.slot.mek_configuration_card";
            case 10 -> "gui.wcwt.toolkit.slot.mek_configurator";
            default -> null;
        };
    }

    private static boolean matchesDedicatedSlot(int index, ItemStack stack) {
        return switch (index) {
            case 0 -> isSwordCategory(stack);
            case 1 -> stack.getItem() instanceof PickaxeItem;
            case 2 -> stack.getItem() instanceof AxeItem;
            case 3 -> stack.getItem() instanceof ShovelItem;
            case 4 -> stack.getItem() instanceof HoeItem;
            case 5 -> isWrenchCategory(stack);
            case 6 -> stack.getItem() instanceof QuartzCuttingKnifeItem
                    || AEItems.CERTUS_QUARTZ_KNIFE.isSameAs(stack);
            case 7 -> isAeNetworkToolkitItem(stack);
            case 8 -> stack.getItem() instanceof MemoryCardItem || AEItems.MEMORY_CARD.isSameAs(stack);
            case 9 -> isMekanismConfigurationCard(stack);
            case 10 -> isMekanismConfigurator(stack);
            default -> false;
        };
    }

    /** 剑格：原版与其它模组中以 {@link SwordItem} 实现的近战武器为主，三叉戟归入同一格便于收纳。 */
    private static boolean isSwordCategory(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof TridentItem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return "minecraft".equals(id.getNamespace()) && ("trident".equals(id.getPath()) || id.getPath().contains("sword"));
    }

    private static boolean isWrenchCategory(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof QuartzWrenchItem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if ("ae2".equals(id.getNamespace()) && id.getPath().contains("wrench")) {
            return true;
        }
        String p = id.getPath();
        return p.endsWith("_wrench") || p.contains("wrench");
    }

    private static boolean isMekanismConfigurationCard(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "mekanism".equals(id.getNamespace()) && id.getPath().contains("configuration_card");
    }

    private static boolean isMekanismConfigurator(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"mekanism".equals(id.getNamespace())) {
            return false;
        }
        String p = id.getPath();
        return (p.contains("configurator") && !p.contains("configuration_card"));
    }
}
