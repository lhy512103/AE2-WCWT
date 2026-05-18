package com.lhy.wcwt.menu;

import appeng.api.config.CopyMode;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.IOptionalSlotHost;
import appeng.menu.slot.OptionalRestrictedInputSlot;
import appeng.menu.slot.PatternTermSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.gui.widgets.PatternMultiplierButton;
import com.lhy.wcwt.compat.CosmeticArmorReworkedBridge;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.network.EncodePatternPacket;
import com.lhy.wcwt.network.PatternEncodingModePacket;
import com.lhy.wcwt.network.PatternEncodingOptionPacket;
import com.lhy.wcwt.network.PatternProviderFocusPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.TopActionPacket;
import com.lhy.wcwt.util.PatternProviderSorts;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import com.google.common.math.LongMath;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class WirelessComprehensiveWorkTerminalMenu extends CraftingTermMenu implements IOptionalSlotHost {
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");
    private static final long PERF_LOG_THRESHOLD_NS = 1_000_000L;
    private static final int PATTERN_PROVIDER_SYNC_INTERVAL_TICKS = 20;
    private static final long PATTERN_PROVIDER_SYNC_SUBSCRIPTION_TICKS = 100L;

    public static final String TYPE_ID = "wireless_comprehensive_work_terminal";
    public static final String TOP_ACTION = "topAction";
    private static final boolean DEBUG_ENCODE = Boolean.getBoolean("wcwt.debug.encode");
    private static final boolean DEBUG_ADVANCED = Boolean.getBoolean("wcwt.debug.advanced");

    /** 元件可装升级卡的最大格数（与 AE2 CellWorkbenchMenu 保持一致：8）。*/
    public static final int CELL_UPGRADE_SLOTS = 8;
    private static final int OPTIONAL_SLOT_GROUP_RESONATING_STORAGE_BASE = CELL_UPGRADE_SLOTS;
    private static final EquipmentSlot[] ARMOR_EQUIPMENT_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = {
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    };

    /**
     * 元件工作台复制模式，通过自定义 DataSlot 同步给客户端。
     * 客户端读取此字段来更新复制模式按钮图标。
     */
    public CopyMode cellCopyMode = CopyMode.CLEAR_ON_REMOVE;
    private int syncedExtendedUiOrdinal = IExtendedUIHost.ExtendedUIType.NONE.ordinal();
    private boolean syncedPatternManagementUploadEnabled = true;
    private int syncedPatternManagementDisplayMode = 1;
    private boolean syncedPatternManagementShowSlots = true;
    private int syncedPatternManagementSearchMode = 2;

    private final WirelessComprehensiveWorkTerminalMenuHost menuHost;
    private final PatternEncodingLogic patternEncodingLogic;
    private final FakeSlot[] patternCraftingSlots = new FakeSlot[9];
    private final FakeSlot[] processingInputSlots = new FakeSlot[appeng.crafting.pattern.AEProcessingPattern.MAX_INPUT_SLOTS];
    private final FakeSlot[] processingOutputSlots = new FakeSlot[appeng.crafting.pattern.AEProcessingPattern.MAX_OUTPUT_SLOTS];
    private FakeSlot smithingTableTemplateSlot;
    private FakeSlot smithingTableBaseSlot;
    private FakeSlot smithingTableAdditionSlot;
    private FakeSlot stonecuttingInputSlot;
    private PatternTermSlot patternPreviewSlot;
    private RestrictedInputSlot blankPatternSlot;
    private RestrictedInputSlot encodedPatternSlot;
    private int syncedPatternEncodingMode = EncodingMode.PROCESSING.ordinal();
    private final List<RecipeHolder<StonecutterRecipe>> stonecuttingRecipes = new ArrayList<>();

    /** 处理样板：合并相同输入材料（JEI 编码与手动编辑时行为对齐 AE2 EncodingHelper）。 */
    private boolean processingMaterialsMerge;

    /**
     * 防止合并写入触发 {@link #broadcastChanges()} 时递归合并。
     */
    private boolean wcwtMergeProcessingGuard;
    /**
     * 样板管理区不是实时容器，而是服务端构建的供应器快照。
     * 打开界面期间定期推送一次，避免只能重开终端才能看到外部变化。
     */
    private int patternProviderSyncCooldown;
    private long patternProviderSyncSubscriptionUntilTick;
    private long lastPatternProviderRequestTick = Long.MIN_VALUE;

    public WirelessComprehensiveWorkTerminalMenu(int id, Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) {
        super(com.lhy.wcwt.init.ModMenus.WCWT_MENU.get(), id, ip, host, false);
        this.menuHost = host;
        this.patternEncodingLogic = host.getLogic();
        // 注意：不要在这里 new ToolboxMenu(this)！
        // 父类 MEStorageMenu 的构造器已经 new ToolboxMenu(this) 并添加了 9 个 TOOLBOX 槽，
        // 若在此重复创建则共有 18 个槽，界面会显示"两重"效果。

        addPlayerEquipmentSlots(ip);
        addCosmeticArmorSlots(ip);
        
        addCuriosSlots(ip);

        addPatternEncodingSlots();
        tryFillBlankPatternFromNetwork();
        
        // 添加高级样板编码槽 (复制样板, 替换输入, 替换输出) - 3个槽位
        var advancedPatternInv = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_ADVANCED_PATTERN);
        if (advancedPatternInv != null) {
            addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, advancedPatternInv, 0), 
                    WcwtSlotSemantics.COPY_PATTERN);
        }

        // 添加元件工作台存储元件槽 (1个槽位)
        var storageCellInv = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_STORAGE_CELL);
        if (storageCellInv != null) {
            addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.WORKBENCH_CELL, storageCellInv, 0),
                    WcwtSlotSemantics.WCWT_STORAGE_CELL);
        }

        // 元件升级卡槽（最多 8 格，按元件实际可用升级数 isSlotEnabled 动态显示，参考 AE2 CellWorkbenchMenu.setupUpgrades）
        // 用 SupplierInternalInventory 包一层，保证元件换出/换入时返回最新的 IUpgradeInventory。
        var cellUpgradeSupplier = new appeng.util.inv.SupplierInternalInventory<>(host::getCellUpgrades);
        for (int i = 0; i < CELL_UPGRADE_SLOTS; i++) {
            addSlot(new OptionalRestrictedInputSlot(
                            RestrictedInputSlot.PlacableItemType.UPGRADES,
                            cellUpgradeSupplier, this, i, i, ip),
                    WcwtSlotSemantics.WCWT_CELL_UPGRADE);
        }

        // 注意：曾经在这里注册过 63 个 CellPartitionSlot（SlotSemantics.CONFIG），
        // 用来直接复用 AE2 CellWorkbench 的点击逻辑。但是这会让 AbstractContainerScreen.findSlot()
        // 把这 63 个 fake-slot 也纳入命中检测，叠加 widgets.hitTest() 后导致：
        //   - 玩家任意点击都能"碰到"一个 CONFIG 槽（即便位置在 -9999），从而吃掉点击。
        //   - 鼠标拿着任意物品在玩家背包/丢弃区域点击，都因为命中 fake-slot 而无法落下。
        // 因此现在改回 AdvancedCodingPanel 内部纯自绘 + CellConfigSetPacket 直接处理 ghost 标记，
        // 真正的过滤数据写到 menuHost.cellConfigInv，对元件 ItemStack 的同步由 host 端负责。
        
        // 添加样板缓存区 (36个槽位，界面每次显示2行×9列)
        var patternCacheInv = host.getPatternCacheInventory();
        if (patternCacheInv != null) {
            for (int i = 0; i < patternCacheInv.size(); i++) {
                var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                        patternCacheInv, i);
                addSlot(slot, WcwtSlotSemantics.WCWT_PATTERN_CACHE);
            }
        }

        var toolkitInv = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
        if (toolkitInv != null) {
            for (int i = 0; i < toolkitInv.size(); i++) {
                addSlot(new ToolkitSlot(toolkitInv, i), WcwtSlotSemantics.WCWT_TOOLKIT);
            }
        }

        var resonatingStorageInv = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_RESONATING_PATTERN_CACHE);
        if (resonatingStorageInv != null) {
            for (int i = 0; i < resonatingStorageInv.size(); i++) {
                addSlot(new OptionalRestrictedInputSlot(
                                RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                                resonatingStorageInv, this, i,
                                OPTIONAL_SLOT_GROUP_RESONATING_STORAGE_BASE + i, ip),
                        WcwtSlotSemantics.WCWT_RESONATING_STORAGE);
            }
        }

        // 添加缠绕态奇点槽（放在升级槽最前，ScrollingUpgradesPanel 约定第一槽为奇点槽）
        // 使用 RestrictedInputSlot.QE_SINGULARITY：
        //   1. 限制只能放入「量子缠绕态奇点」物品；
        //   2. 空槽时自动显示奇点占位图标（AppEngSlot 不会画占位图标）。
        // WTLib 自家 WCT/WAT/WET 也都是这么写的。
        var singularityInv = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_SINGULARITY);
        if (singularityInv != null) {
            addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                    singularityInv, 0), AE2wtlibSlotSemantics.SINGULARITY);
        }

        // 注意：UPGRADE 槽已经由父类 MEStorageMenu 在构造器里通过 setupUpgrades(host.getUpgrades()) 自动添加，
        // 不要在这里重复添加，否则会出现两组槽指向同一个 IUpgradeInventory，
        // 视觉上看就像"放第 N 格，第 N±size 格也同时出现"。
        
        // 同步复制模式到客户端（用 DataSlot 传输 ordinal）
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return cellCopyMode.ordinal(); }
            @Override public void set(int v) {
                cellCopyMode = (v >= 0 && v < CopyMode.values().length)
                        ? CopyMode.values()[v] : CopyMode.CLEAR_ON_REMOVE;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() {
                return patternEncodingLogic == null ? syncedPatternEncodingMode : patternEncodingLogic.getMode().ordinal();
            }

            @Override public void set(int v) {
                syncedPatternEncodingMode = isValidEncodingModeOrdinal(v) ? v : EncodingMode.PROCESSING.ordinal();
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return processingMaterialsMerge ? 1 : 0;
            }

            @Override
            public void set(int value) {
                processingMaterialsMerge = value != 0;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost == null ? syncedExtendedUiOrdinal : menuHost.getCurrentExtendedUI().ordinal();
            }

            @Override
            public void set(int value) {
                syncedExtendedUiOrdinal = isValidExtendedUiOrdinal(value)
                        ? value
                        : IExtendedUIHost.ExtendedUIType.NONE.ordinal();
                if (menuHost != null) {
                    menuHost.setCurrentExtendedUI(IExtendedUIHost.ExtendedUIType.values()[syncedExtendedUiOrdinal]);
                }
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost != null && menuHost.isPatternManagementUploadEnabled() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                syncedPatternManagementUploadEnabled = value != 0;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost != null ? menuHost.getPatternManagementDisplayMode() : syncedPatternManagementDisplayMode;
            }

            @Override
            public void set(int value) {
                syncedPatternManagementDisplayMode = value;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost != null && menuHost.isPatternManagementShowSlots() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                syncedPatternManagementShowSlots = value != 0;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost != null ? menuHost.getPatternManagementSearchMode() : syncedPatternManagementSearchMode;
            }

            @Override
            public void set(int value) {
                syncedPatternManagementSearchMode = value;
            }
        });
        registerClientAction(TOP_ACTION, TopActionPacket.Action.class, this::handleTopAction);

        // 创建玩家物品栏槽位
        this.createPlayerInventorySlots(ip);
    }

    private void addPlayerEquipmentSlots(Inventory inventory) {
        Player player = inventory.player;
        addSlot(new PlayerArmorSlot(inventory, 39, ARMOR_EQUIPMENT_SLOTS[0], player),
                WcwtSlotSemantics.AE2WTLIB_HELMET);
        addSlot(new PlayerArmorSlot(inventory, 38, ARMOR_EQUIPMENT_SLOTS[1], player),
                WcwtSlotSemantics.AE2WTLIB_CHESTPLATE);
        addSlot(new PlayerArmorSlot(inventory, 37, ARMOR_EQUIPMENT_SLOTS[2], player),
                WcwtSlotSemantics.AE2WTLIB_LEGGINGS);
        addSlot(new PlayerArmorSlot(inventory, 36, ARMOR_EQUIPMENT_SLOTS[3], player),
                WcwtSlotSemantics.AE2WTLIB_BOOTS);
        addSlot(new OffhandSlot(inventory, 40), WcwtSlotSemantics.AE2WTLIB_OFFHAND);
    }

    public static final class ToolkitSlot extends AppEngSlot {
        private final int toolkitIndex;

        ToolkitSlot(InternalInventory inventory, int toolkitIndex) {
            super(inventory, toolkitIndex);
            this.toolkitIndex = toolkitIndex;
            String tipKey = ToolkitItemRules.dedicatedSlotTooltipKey(toolkitIndex);
            if (tipKey != null) {
                setEmptyTooltip(() -> List.of(Component.translatable(tipKey)));
            }
        }

        /** 客户端命中检测／双击网络工具槽等用。 */
        public int toolkitLogicalIndex() {
            return toolkitIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ToolkitItemRules.mayPlace(toolkitIndex, stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    public IExtendedUIHost.ExtendedUIType getSyncedExtendedUIType() {
        if (menuHost != null) {
            return menuHost.getCurrentExtendedUI();
        }
        return isValidExtendedUiOrdinal(syncedExtendedUiOrdinal)
                ? IExtendedUIHost.ExtendedUIType.values()[syncedExtendedUiOrdinal]
                : IExtendedUIHost.ExtendedUIType.NONE;
    }

    public boolean isPatternManagementUploadEnabled() {
        return menuHost != null ? menuHost.isPatternManagementUploadEnabled() : syncedPatternManagementUploadEnabled;
    }

    public void setPatternManagementUploadEnabled(boolean enabled) {
        syncedPatternManagementUploadEnabled = enabled;
        if (menuHost != null) {
            menuHost.setPatternManagementUploadEnabled(enabled);
        }
        broadcastChanges();
    }

    public int getPatternManagementDisplayMode() {
        return menuHost != null ? menuHost.getPatternManagementDisplayMode() : syncedPatternManagementDisplayMode;
    }

    public void setPatternManagementDisplayMode(int mode) {
        syncedPatternManagementDisplayMode = mode;
        if (menuHost != null) {
            menuHost.setPatternManagementDisplayMode(mode);
        }
        broadcastChanges();
    }

    public boolean isPatternManagementShowSlots() {
        return menuHost != null ? menuHost.isPatternManagementShowSlots() : syncedPatternManagementShowSlots;
    }

    public void setPatternManagementShowSlots(boolean showSlots) {
        syncedPatternManagementShowSlots = showSlots;
        if (menuHost != null) {
            menuHost.setPatternManagementShowSlots(showSlots);
        }
        broadcastChanges();
    }

    public int getPatternManagementSearchMode() {
        return menuHost != null ? menuHost.getPatternManagementSearchMode() : syncedPatternManagementSearchMode;
    }

    public void setPatternManagementSearchMode(int mode) {
        syncedPatternManagementSearchMode = mode;
        if (menuHost != null) {
            menuHost.setPatternManagementSearchMode(mode);
        }
        broadcastChanges();
    }

    private static boolean isValidExtendedUiOrdinal(int value) {
        return value >= 0 && value < IExtendedUIHost.ExtendedUIType.values().length;
    }

    private void addCosmeticArmorSlots(Inventory inventory) {
        Container cosmeticArmorInv = CosmeticArmorReworkedBridge.getCosmeticArmorInventory(inventory.player);
        if (cosmeticArmorInv != null) {
            for (int i = 0; i < 4; i++) {
                CosmeticArmorReworkedBridge.syncSlotToClient(inventory.player, i);
            }
            addSlot(new CosmeticArmorSlot(cosmeticArmorInv, 3, ARMOR_EQUIPMENT_SLOTS[0], inventory.player),
                    WcwtSlotSemantics.DECORATIVE_HELMET);
            addSlot(new CosmeticArmorSlot(cosmeticArmorInv, 2, ARMOR_EQUIPMENT_SLOTS[1], inventory.player),
                    WcwtSlotSemantics.DECORATIVE_ARMOR);
            addSlot(new CosmeticArmorSlot(cosmeticArmorInv, 1, ARMOR_EQUIPMENT_SLOTS[2], inventory.player),
                    WcwtSlotSemantics.DECORATIVE_SHIN_GUARDS);
            addSlot(new CosmeticArmorSlot(cosmeticArmorInv, 0, ARMOR_EQUIPMENT_SLOTS[3], inventory.player),
                    WcwtSlotSemantics.DECORATIVE_BOOTS);
            return;
        }

        // 没有安装 Cosmetic Armor Reworked 时保留旧的终端内置库存，避免槽位语义缺失导致界面异常。
        var decorativeArmorInv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_DECORATIVE_ARMOR);
        if (decorativeArmorInv != null) {
            addSlot(new AppEngSlot(decorativeArmorInv, 0), WcwtSlotSemantics.DECORATIVE_HELMET);
            addSlot(new AppEngSlot(decorativeArmorInv, 1), WcwtSlotSemantics.DECORATIVE_ARMOR);
            addSlot(new AppEngSlot(decorativeArmorInv, 2), WcwtSlotSemantics.DECORATIVE_SHIN_GUARDS);
            addSlot(new AppEngSlot(decorativeArmorInv, 3), WcwtSlotSemantics.DECORATIVE_BOOTS);
        }
    }

    private void addCuriosSlots(Inventory inventory) {
        var curiosSlots = CuriosBridge.getVisibleSlots(inventory.player);
        if (!curiosSlots.isEmpty()) {
            for (var spec : curiosSlots) {
                addSlot(new WcwtCurioSlot(spec), WcwtSlotSemantics.AE_CURIOS);
            }
            return;
        }

        // 没有安装 Curios 时保留旧的终端内置单槽，防止已有存档中的该组件无法显示。
        var curiosInv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_CURIOS);
        if (curiosInv != null) {
            addSlot(new AppEngSlot(curiosInv, 0), WcwtSlotSemantics.AE_CURIOS);
        }
    }

    private static class PlayerArmorSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final Player player;

        PlayerArmorSlot(Container inventory, int slot, EquipmentSlot equipmentSlot, Player player) {
            super(inventory, slot, 0, 0);
            this.equipmentSlot = equipmentSlot;
            this.player = player;
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            player.onEquipItem(equipmentSlot, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            return (stack.isEmpty()
                    || player.isCreative()
                    || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
                    && super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[equipmentSlot.getIndex()]);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(equipmentSlot, player);
        }
    }

    private static class CosmeticArmorSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final Player player;

        CosmeticArmorSlot(Container inventory, int slot, EquipmentSlot equipmentSlot, Player player) {
            super(inventory, slot, 0, 0);
            this.equipmentSlot = equipmentSlot;
            this.player = player;
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[equipmentSlot.getIndex()]);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(equipmentSlot, player);
        }
    }

    private static class OffhandSlot extends Slot {
        OffhandSlot(Container inventory, int slot) {
            super(inventory, slot, 0, 0);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }
    }

    public static class WcwtCurioSlot extends SlotItemHandler {
        private final String identifier;
        private final boolean canToggleRendering;
        private boolean renderStatus;

        WcwtCurioSlot(CuriosBridge.CurioSlotSpec spec) {
            super(spec.handler(), spec.slotIndex(), 0, 0);
            this.identifier = spec.identifier();
            this.canToggleRendering = spec.canToggleRendering();
            this.renderStatus = spec.renderStatus();
            setBackground(InventoryMenu.BLOCK_ATLAS, spec.icon());
        }

        public String getIdentifier() {
            return identifier;
        }

        public boolean canToggleRendering() {
            return canToggleRendering;
        }

        public boolean getRenderStatus() {
            return renderStatus;
        }

        public void toggleRenderStatus() {
            renderStatus = !renderStatus;
        }
    }
    
    public WirelessComprehensiveWorkTerminalMenu(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host) {
        super(menuType, id, ip, host, true);
        this.menuHost = null;
        this.patternEncodingLogic = host instanceof appeng.helpers.IPatternTerminalMenuHost patternHost
                ? patternHost.getLogic()
                : null;
    }
    
    public WirelessComprehensiveWorkTerminalMenuHost getMenuHost() {
        return menuHost;
    }

    public EncodingMode getPatternEncodingMode() {
        if (isClientSide()) {
            return EncodingMode.values()[syncedPatternEncodingMode];
        }
        return patternEncodingLogic == null ? EncodingMode.PROCESSING : patternEncodingLogic.getMode();
    }

    public void setPatternEncodingMode(EncodingMode mode) {
        if (isClientSide()) {
            syncedPatternEncodingMode = mode.ordinal();
            PacketDistributor.sendToServer(new PatternEncodingModePacket(mode));
            return;
        }
        if (patternEncodingLogic != null) {
            patternEncodingLogic.setMode(mode);
            syncedPatternEncodingMode = mode.ordinal();
            updatePatternPreview(mode);
            broadcastChanges();
        }
    }

    public List<RecipeHolder<StonecutterRecipe>> getStonecuttingRecipes() {
        updateStonecuttingRecipes();
        return stonecuttingRecipes;
    }

    @Nullable
    public ResourceLocation getStonecuttingRecipeId() {
        return patternEncodingLogic == null ? null : patternEncodingLogic.getStonecuttingRecipeId();
    }

    public void setStonecuttingRecipeId(ResourceLocation id) {
        if (patternEncodingLogic != null) {
            patternEncodingLogic.setStonecuttingRecipeId(id);
            updatePatternPreview(EncodingMode.STONECUTTING);
            broadcastChanges();
        }
    }

    public void cycleProcessingOutput() {
        if (isClientSide() || patternEncodingLogic == null || getPatternEncodingMode() != EncodingMode.PROCESSING) {
            return;
        }

        var outputInv = patternEncodingLogic.getEncodedOutputInv();
        var newOutputs = new GenericStack[outputInv.size()];
        for (int i = 0; i < outputInv.size(); i++) {
            if (outputInv.getStack(i) == null) {
                continue;
            }

            // Match AE2: rotate only populated output slots and skip empty gaps.
            for (int j = 1; j < outputInv.size(); j++) {
                var next = outputInv.getStack((i + j) % outputInv.size());
                if (next != null) {
                    newOutputs[i] = next;
                    break;
                }
            }
        }

        for (int i = 0; i < newOutputs.length; i++) {
            outputInv.setStack(i, newOutputs[i]);
        }
        broadcastChanges();
    }

    public boolean canCycleProcessingOutputs() {
        if (patternEncodingLogic == null || getPatternEncodingMode() != EncodingMode.PROCESSING) {
            return false;
        }

        int outputs = 0;
        var outputInv = patternEncodingLogic.getEncodedOutputInv();
        for (int i = 0; i < outputInv.size(); i++) {
            if (outputInv.getStack(i) != null) {
                outputs++;
            }
        }
        return outputs > 1;
    }

    public void clearPatternEncoding() {
        if (patternEncodingLogic == null) {
            return;
        }

        patternEncodingLogic.getEncodedInputInv().clear();
        patternEncodingLogic.getEncodedOutputInv().clear();
        updatePatternPreview(getPatternEncodingMode());
        broadcastChanges();
    }

    public boolean isPatternSubstitute() {
        return patternEncodingLogic != null && patternEncodingLogic.isSubstitution();
    }

    public void setPatternSubstitute(boolean substitute) {
        if (patternEncodingLogic != null) {
            patternEncodingLogic.setSubstitution(substitute);
            broadcastChanges();
        }
    }

    public boolean isPatternFluidSubstitute() {
        return patternEncodingLogic != null && patternEncodingLogic.isFluidSubstitution();
    }

    public void setPatternFluidSubstitute(boolean substitute) {
        if (patternEncodingLogic != null) {
            patternEncodingLogic.setFluidSubstitution(substitute);
            broadcastChanges();
        }
    }

    public void handlePatternEncodingOption(int action, boolean value) {
        switch (action) {
            case PatternEncodingOptionPacket.ACTION_CLEAR -> clearPatternEncoding();
            case PatternEncodingOptionPacket.ACTION_SUBSTITUTE -> setPatternSubstitute(value);
            case PatternEncodingOptionPacket.ACTION_FLUID_SUBSTITUTE -> setPatternFluidSubstitute(value);
            case PatternEncodingOptionPacket.ACTION_PROCESSING_MERGE_MATERIALS -> {
                processingMaterialsMerge = value;
                if (value && patternEncodingLogic != null && getPatternEncodingMode() == EncodingMode.PROCESSING) {
                    wcwtMergeProcessingGuard = true;
                    try {
                        mergeProcessingPatternInputs();
                    } finally {
                        wcwtMergeProcessingGuard = false;
                    }
                }
            }
            default -> {
            }
        }
    }

    public boolean isProcessingMaterialsMerge() {
        return processingMaterialsMerge;
    }

    @Override
    public void broadcastChanges() {
        long totalStartNs = 0L;
        long preSuperNs = 0L;
        long superNs = 0L;
        long refreshNs = 0L;
        long mergeNs = 0L;
        long providerSyncNs = 0L;
        boolean didProviderSync = false;
        int cooldownBefore = patternProviderSyncCooldown;
        if (DEBUG_PERF && isServerSide()) {
            totalStartNs = System.nanoTime();
        }
        if (isServerSide() && menuHost != null) {
            long stageStartNs = DEBUG_PERF ? System.nanoTime() : 0L;
            menuHost.refreshMenuSyncState();
            if (DEBUG_PERF) {
                refreshNs = System.nanoTime() - stageStartNs;
            }
        }
        if (isServerSide() && !wcwtMergeProcessingGuard && processingMaterialsMerge
                && getPatternEncodingMode() == EncodingMode.PROCESSING && patternEncodingLogic != null) {
            wcwtMergeProcessingGuard = true;
            try {
                long stageStartNs = DEBUG_PERF ? System.nanoTime() : 0L;
                mergeProcessingPatternInputs();
                if (DEBUG_PERF) {
                    mergeNs = System.nanoTime() - stageStartNs;
                }
            } finally {
                wcwtMergeProcessingGuard = false;
            }
        }
        if (isServerSide() && getPlayer() instanceof ServerPlayer serverPlayer) {
            boolean shouldSyncProviders = shouldSyncPatternProviders(serverPlayer);
            if (shouldSyncProviders && patternProviderSyncCooldown <= 0) {
                long stageStartNs = DEBUG_PERF ? System.nanoTime() : 0L;
                PacketDistributor.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
                patternProviderSyncCooldown = PATTERN_PROVIDER_SYNC_INTERVAL_TICKS;
                didProviderSync = true;
                if (DEBUG_PERF) {
                    providerSyncNs = System.nanoTime() - stageStartNs;
                }
            } else if (shouldSyncProviders) {
                patternProviderSyncCooldown--;
            } else {
                patternProviderSyncCooldown = 0;
            }
        }
        if (DEBUG_PERF && isServerSide()) {
            preSuperNs = System.nanoTime() - totalStartNs;
        }
        long superStartNs = DEBUG_PERF && isServerSide() ? System.nanoTime() : 0L;
        super.broadcastChanges();
        if (DEBUG_PERF && isServerSide()) {
            superNs = System.nanoTime() - superStartNs;
        }
        if (DEBUG_PERF && isServerSide()) {
            long totalNs = System.nanoTime() - totalStartNs;
            if (totalNs >= PERF_LOG_THRESHOLD_NS
                    || preSuperNs >= PERF_LOG_THRESHOLD_NS
                    || superNs >= PERF_LOG_THRESHOLD_NS
                    || refreshNs >= PERF_LOG_THRESHOLD_NS
                    || mergeNs >= PERF_LOG_THRESHOLD_NS
                    || providerSyncNs >= PERF_LOG_THRESHOLD_NS
                    || didProviderSync) {
                String playerName = getPlayer() != null ? getPlayer().getScoreboardName() : "<unknown>";
                WcwtMod.LOGGER.info(
                        "WCWT perf: broadcastChanges player={}, totalMs={}, preSuperMs={}, superMs={}, refreshMs={}, mergeMs={}, providerSyncMs={}, providerSync={}, cooldownBefore={}, cooldownAfter={}, mode={}, processingMerge={}",
                        playerName,
                        formatPerfMs(totalNs),
                        formatPerfMs(preSuperNs),
                        formatPerfMs(superNs),
                        formatPerfMs(refreshNs),
                        formatPerfMs(mergeNs),
                        formatPerfMs(providerSyncNs),
                        didProviderSync,
                        cooldownBefore,
                        patternProviderSyncCooldown,
                        getPatternEncodingMode(),
                        processingMaterialsMerge);
            }
        }
    }

    private static String formatPerfMs(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    public void requestPatternProviderSyncSubscription() {
        if (!isServerSide() || !(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long gameTime = serverPlayer.serverLevel().getGameTime();
        if (gameTime - lastPatternProviderRequestTick < 5L) {
            patternProviderSyncSubscriptionUntilTick = Math.max(
                    patternProviderSyncSubscriptionUntilTick,
                    gameTime + PATTERN_PROVIDER_SYNC_SUBSCRIPTION_TICKS);
            return;
        }
        lastPatternProviderRequestTick = gameTime;
        patternProviderSyncSubscriptionUntilTick = Math.max(
                patternProviderSyncSubscriptionUntilTick,
                gameTime + PATTERN_PROVIDER_SYNC_SUBSCRIPTION_TICKS);
    }

    public boolean shouldServeImmediatePatternProviderRequest() {
        if (!isServerSide() || !(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        long gameTime = serverPlayer.serverLevel().getGameTime();
        return gameTime - lastPatternProviderRequestTick >= 5L;
    }

    private boolean shouldSyncPatternProviders(ServerPlayer serverPlayer) {
        return serverPlayer.serverLevel().getGameTime() <= patternProviderSyncSubscriptionUntilTick;
    }

    private static boolean isValidEncodingModeOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < EncodingMode.values().length;
    }

    private MatrixUploadResult uploadEncodedPatternToMatrix(ItemStack encodedPattern) {
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)
                || encodedPattern.isEmpty()
                || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return MatrixUploadResult.FAILURE;
        }

        var grid = getMenuGrid();
        if (grid == null) {
            return MatrixUploadResult.FAILURE;
        }

        try {
            EcoUploadDuplicateResult ecoDuplicate = findEcoDuplicatePattern(grid, encodedPattern);
            if (ecoDuplicate.duplicate()) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.eco_pattern_duplicate"));
                return MatrixUploadResult.uploaded(ecoDuplicate.providerId(), ecoDuplicate.slot());
            }
            if (NeoEcoUploadBridge.uploadPatternToEcoStorage(grid, encodedPattern.copy())) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.eco_pattern_uploaded"));
                return findEcoUploadResult(encodedPattern);
            }
            if (ExtendedAePlusUploadBridge.matrixContainsPattern(grid, encodedPattern)) {
                serverPlayer.sendSystemMessage(Component.translatable("extendedae_plus.message.matrix.duplicate"));
                return returnBlankPatternFromMatrixUpload(encodedPattern.getCount())
                        ? MatrixUploadResult.DUPLICATE_RETURNED
                        : MatrixUploadResult.DUPLICATE_ABORTED;
            }
            if (ExtendedAePlusUploadBridge.uploadPatternToMatrix(serverPlayer, encodedPattern.copy(), grid)) {
                return findMatrixUploadResult(encodedPattern);
            }
        } catch (Throwable ignored) {
        }
        return MatrixUploadResult.FAILURE;
    }

    private EcoUploadDuplicateResult findEcoDuplicatePattern(appeng.api.networking.IGrid grid, ItemStack encodedPattern) {
        var providers = listUploadProviders(false);
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            if (!NeoEcoUploadBridge.isEcoPatternProvider(provider)) {
                continue;
            }
            int duplicateSlot = findMatchingPatternSlot(provider, encodedPattern);
            if (duplicateSlot >= 0) {
                return new EcoUploadDuplicateResult(true, i + 1L, duplicateSlot);
            }
        }
        return EcoUploadDuplicateResult.NONE;
    }

    private MatrixUploadResult findEcoUploadResult(ItemStack encodedPattern) {
        var providers = listUploadProviders(false);
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            if (!NeoEcoUploadBridge.isEcoPatternProvider(provider)) {
                continue;
            }
            int insertedSlot = findMatchingPatternSlot(provider, encodedPattern);
            if (insertedSlot >= 0) {
                return MatrixUploadResult.uploaded(i + 1L, insertedSlot);
            }
        }
        return MatrixUploadResult.UPLOADED;
    }

    private MatrixUploadResult findMatrixUploadResult(ItemStack encodedPattern) {
        var providers = listUploadProviders(false);
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            int insertedSlot = findLastInsertedPatternSlot(provider, encodedPattern);
            if (insertedSlot >= 0) {
                return MatrixUploadResult.uploaded(i + 1L, insertedSlot);
            }
        }
        return MatrixUploadResult.UPLOADED;
    }

    private boolean returnBlankPatternFromMatrixUpload(int count) {
        if (count <= 0) {
            return true;
        }

        ItemStack remainingBlanks = AEItems.BLANK_PATTERN.stack(count);
        if (blankPatternSlot != null && blankPatternSlot.mayPlace(remainingBlanks)) {
            remainingBlanks = blankPatternSlot.safeInsert(remainingBlanks);
            if (remainingBlanks.isEmpty()) {
                return true;
            }
        }

        try {
            var blankKey = AEItemKey.of(AEItems.BLANK_PATTERN.asItem());
            long inserted = StorageHelper.poweredInsert(energySource, storage, blankKey,
                    remainingBlanks.getCount(), getActionSource());
            return inserted >= remainingBlanks.getCount();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Nullable
    private appeng.api.networking.IGrid getMenuGrid() {
        Object target = getTarget();
        if (target instanceof appeng.api.networking.security.IActionHost host
                && host.getActionableNode() != null) {
            return host.getActionableNode().getGrid();
        }
        return null;
    }

    private UploadAttemptResult uploadEncodedPatternToMatchingProvider(ItemStack encodedPattern, String searchText) {
        String query = searchText == null ? "" : searchText.trim();
        if (query.isEmpty() || encodedPattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return UploadAttemptResult.NO_TARGET;
        }

        var providers = listUploadProviders(false);
        var matchingTargets = new ArrayList<ProviderTarget>();
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            String providerName = getUploadProviderDisplayName(provider);
            if (providerNameMatches(providerName, query)) {
                matchingTargets.add(new ProviderTarget(i + 1L, provider, providerName));
            }
        }

        var matchingGroupNames = matchingTargets.stream()
                .map(ProviderTarget::providerName)
                .distinct()
                .toList();
        if (matchingGroupNames.size() != 1 || matchingTargets.isEmpty()) {
            return UploadAttemptResult.NO_TARGET;
        }

        String targetName = matchingGroupNames.get(0);
        var candidateTargets = matchingTargets.stream()
                .filter(target -> targetName.equals(target.providerName()))
                .toList();
        for (var target : candidateTargets) {
            if (insertEncodedPattern(target.provider(), encodedPattern)) {
                int insertedSlot = findLastInsertedPatternSlot(target.provider(), encodedPattern);
                return new UploadAttemptResult(true, true, targetName, target.providerId(), insertedSlot);
            }
        }
        return new UploadAttemptResult(false, true, targetName, candidateTargets.get(0).providerId(), -1);
    }

    private List<PatternContainer> listUploadProviders(boolean requireAvailableSlots) {
        var grid = getMenuGrid();
        if (grid == null) {
            return List.of();
        }

        var providers = new ArrayList<PatternContainer>();
        for (var machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
            for (var container : grid.getActiveMachines(containerClass)) {
                if (container != null
                        && container.isVisibleInTerminal()
                        && container.getTerminalPatternInventory() != null
                        && container.getTerminalPatternInventory().size() > 0
                        && container.getTerminalGroup() != null
                        && (!requireAvailableSlots || getAvailablePatternSlots(container) > 0)) {
                    providers.add(container);
                }
            }
        }
        providers.sort(PatternProviderSorts.STABLE);
        return providers;
    }

    private int findLastInsertedPatternSlot(PatternContainer provider, ItemStack encodedPattern) {
        InternalInventory inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return -1;
        }
        for (int i = inv.size() - 1; i >= 0; i--) {
            var stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, encodedPattern)) {
                return i;
            }
        }
        return -1;
    }

    private int findMatchingPatternSlot(PatternContainer provider, ItemStack encodedPattern) {
        InternalInventory inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return -1;
        }
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, encodedPattern)) {
                return i;
            }
        }
        return -1;
    }

    private record ProviderTarget(long providerId, PatternContainer provider, String providerName) {
    }

    private record EcoUploadDuplicateResult(boolean duplicate, long providerId, int slot) {
        private static final EcoUploadDuplicateResult NONE = new EcoUploadDuplicateResult(false, -1, -1);
    }

    private record MatrixUploadResult(MatrixUploadState state, long providerId, int slot) {
        private static final MatrixUploadResult UPLOADED = new MatrixUploadResult(MatrixUploadState.UPLOADED, -1, -1);
        private static final MatrixUploadResult DUPLICATE_RETURNED =
                new MatrixUploadResult(MatrixUploadState.DUPLICATE_RETURNED, -1, -1);
        private static final MatrixUploadResult DUPLICATE_ABORTED =
                new MatrixUploadResult(MatrixUploadState.DUPLICATE_ABORTED, -1, -1);
        private static final MatrixUploadResult FAILURE = new MatrixUploadResult(MatrixUploadState.FAILURE, -1, -1);

        private static MatrixUploadResult uploaded(long providerId, int slot) {
            return new MatrixUploadResult(MatrixUploadState.UPLOADED, providerId, slot);
        }
    }

    private enum MatrixUploadState {
        UPLOADED,
        DUPLICATE_RETURNED,
        DUPLICATE_ABORTED,
        FAILURE
    }

    private record UploadAttemptResult(boolean uploaded, boolean hadTarget, String providerName, long providerId, int slot) {
        private static final UploadAttemptResult NO_TARGET = new UploadAttemptResult(false, false, "", -1, -1);
    }

    private static boolean providerNameMatches(String providerName, String query) {
        return providerName != null && providerName.toLowerCase().contains(query.toLowerCase());
    }

    private static String getUploadProviderDisplayName(PatternContainer provider) {
        String bridgeName = ExtendedAePlusUploadBridge.getProviderDisplayName(provider);
        if (bridgeName != null && !bridgeName.isBlank()) {
            return bridgeName;
        }
        return provider.getTerminalGroup().name().getString();
    }

    private static int getAvailablePatternSlots(PatternContainer provider) {
        InternalInventory inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return 0;
        }
        int available = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                available++;
            }
        }
        return available;
    }

    private static boolean insertEncodedPattern(PatternContainer provider, ItemStack encodedPattern) {
        InternalInventory inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return false;
        }
        var filtered = new FilteredInternalInventory(inv, new EncodedPatternFilter());
        ItemStack toInsert = encodedPattern.copy();
        ItemStack remain = filtered.addItems(toInsert);
        return remain.getCount() < toInsert.getCount();
    }

    private void addPatternEncodingSlots() {
        if (patternEncodingLogic == null) {
            return;
        }

        var encodedInputs = patternEncodingLogic.getEncodedInputInv().createMenuWrapper();
        var encodedOutputs = patternEncodingLogic.getEncodedOutputInv().createMenuWrapper();

        for (int i = 0; i < patternCraftingSlots.length; i++) {
            addSlot(patternCraftingSlots[i] = new FakeSlot(encodedInputs, i), WcwtSlotSemantics.WCWT_PATTERN_CRAFTING_GRID);
            patternCraftingSlots[i].setHideAmount(true);
        }
        for (int i = 0; i < processingInputSlots.length; i++) {
            addSlot(processingInputSlots[i] = new FakeSlot(encodedInputs, i), WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_INPUTS);
        }
        for (int i = 0; i < processingOutputSlots.length; i++) {
            addSlot(processingOutputSlots[i] = new FakeSlot(encodedOutputs, i), WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_OUTPUTS);
        }
        processingOutputSlots[0].setIcon(appeng.client.gui.Icon.BACKGROUND_PRIMARY_OUTPUT);

        addSlot(stonecuttingInputSlot = new FakeSlot(encodedInputs, 0), WcwtSlotSemantics.WCWT_PATTERN_STONECUTTING_INPUT);
        stonecuttingInputSlot.setHideAmount(true);

        addSlot(smithingTableTemplateSlot = new FakeSlot(encodedInputs, 0), WcwtSlotSemantics.WCWT_PATTERN_SMITHING_TEMPLATE);
        smithingTableTemplateSlot.setHideAmount(true);
        addSlot(smithingTableBaseSlot = new FakeSlot(encodedInputs, 1), WcwtSlotSemantics.WCWT_PATTERN_SMITHING_BASE);
        smithingTableBaseSlot.setHideAmount(true);
        addSlot(smithingTableAdditionSlot = new FakeSlot(encodedInputs, 2), WcwtSlotSemantics.WCWT_PATTERN_SMITHING_ADDITION);
        smithingTableAdditionSlot.setHideAmount(true);

        patternPreviewSlot = new PatternTermSlot();
        patternPreviewSlot.setActive(false);
        addSlot(patternPreviewSlot, WcwtSlotSemantics.WCWT_PATTERN_PREVIEW);

        blankPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.BLANK_PATTERN,
                patternEncodingLogic.getBlankPatternInv(), 0);
        addSlot(blankPatternSlot, SlotSemantics.BLANK_PATTERN);

        encodedPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                patternEncodingLogic.getEncodedPatternInv(), 0);
        encodedPatternSlot.setStackLimit(1);
        encodedPatternSlot.setIcon(null);
        addSlot(encodedPatternSlot, SlotSemantics.ENCODED_PATTERN);
    }

    @Override
    public void setItem(int slotID, int stateId, ItemStack stack) {
        super.setItem(slotID, stateId, stack);
        updatePatternPreview(getPatternEncodingMode());
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        updatePatternPreview(getPatternEncodingMode());
    }

    public void encodePattern(EncodingMode mode) {
        encodePattern(mode, false, "", false);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText) {
        encodePattern(mode, uploadEnabled, providerSearchText, false);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              boolean fallbackToEditSlot) {
        if (isClientSide()) {
            logEncode("client clicked encode, mode={}", mode);
            PacketDistributor.sendToServer(new EncodePatternPacket(mode, uploadEnabled, providerSearchText,
                    fallbackToEditSlot));
            return;
        }

        tryFillBlankPatternFromNetwork();

        if (patternEncodingLogic == null || blankPatternSlot == null) {
            logEncode("missing patternEncodingLogic or blankPatternSlot, mode={}", mode);
            return;
        }

        logEncode("start mode={}, blank={}, inputs={}, outputs={}",
                mode, blankPatternSlot.getItem(), summarizeConfig(patternEncodingLogic.getEncodedInputInv()),
                summarizeConfig(patternEncodingLogic.getEncodedOutputInv()));

        // 不在此处调用 patternEncodingLogic.setMode(mode)：
        // setMode() 会触发 fixCraftingRecipes()，切换到 CRAFTING 模式时会清除
        // encodedInputInv 中所有非 AEItemKey 的物品（如流体），
        // 导致 PROCESSING 模式下已放置的流体输入丢失，使 PROCESSING 编码也失败。
        // 这里只需要根据传入的 mode 参数分发编码逻辑即可。
        ItemStack encodedPattern;
        try {
            encodedPattern = switch (mode) {
                case CRAFTING -> encodeCraftingPattern();
                case PROCESSING -> createProcessingPattern();
                case SMITHING_TABLE -> encodeSmithingTablePattern();
                case STONECUTTING -> encodeStonecuttingPattern();
            };
        } catch (Exception e) {
            if (DEBUG_ENCODE) {
                com.lhy.wcwt.WcwtMod.LOGGER.warn("WCWT encode debug: exception mode={}", mode, e);
            }
            return;
        }
        if (encodedPattern.isEmpty()) {
            logEncode("encoded pattern is empty, mode={}", mode);
            return;
        }

        if (encodedPatternSlot == null || blankPatternSlot == null) {
            logEncode("missing encoded or blank pattern slot, mode={}, encoded={}",
                    mode, encodedPattern);
            return;
        }

        ItemStack editSlotStack = encodedPatternSlot.getItem();
        boolean consumeEditPattern = PatternDetailsHelper.isEncodedPattern(editSlotStack);
        if (!editSlotStack.isEmpty() && !consumeEditPattern) {
            logEncode("encoded pattern slot contains invalid item, mode={}, existing={}",
                    mode, editSlotStack);
            return;
        }

        if (!consumeEditPattern && !hasBlankPatternForEncoding()) {
            logEncode("no consumable pattern source, mode={}, blank={}, edit={}",
                    mode, blankPatternSlot.getItem(), editSlotStack);
            return;
        }

        if (uploadEnabled && mode != EncodingMode.PROCESSING) {
            MatrixUploadResult matrixUploadResult = uploadEncodedPatternToMatrix(encodedPattern);
            if (matrixUploadResult.state() == MatrixUploadState.UPLOADED
                    || matrixUploadResult.state() == MatrixUploadState.DUPLICATE_RETURNED) {
                consumePatternForUpload(consumeEditPattern);
                patternEncodingLogic.setMode(mode);
                syncedPatternEncodingMode = mode.ordinal();
                updatePatternPreview(mode);
                tryFillBlankPatternFromNetwork();
                if (getPlayer() instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
                    if (matrixUploadResult.providerId() > 0 && matrixUploadResult.slot() >= 0) {
                        PacketDistributor.sendToPlayer(serverPlayer,
                                new PatternProviderFocusPacket(matrixUploadResult.providerId(), matrixUploadResult.slot()));
                    }
                }
                logEncode("matrix upload result={}, mode={}, encoded={}",
                        matrixUploadResult.state(), mode, encodedPattern);
                broadcastChanges();
                return;
            }
            if (matrixUploadResult.state() == MatrixUploadState.DUPLICATE_ABORTED) {
                logEncode("matrix duplicate detected but blank return failed, mode={}, encoded={}",
                        mode, encodedPattern);
                return;
            }
        }

        UploadAttemptResult uploadAttempt = UploadAttemptResult.NO_TARGET;
        if (uploadEnabled) {
            uploadAttempt = uploadEncodedPatternToMatchingProvider(encodedPattern, providerSearchText);
            if (uploadAttempt.uploaded()) {
                consumePatternForUpload(consumeEditPattern);
                patternEncodingLogic.setMode(mode);
                syncedPatternEncodingMode = mode.ordinal();
                updatePatternPreview(mode);
                tryFillBlankPatternFromNetwork();
                if (getPlayer() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "extendedae_plus.screen.upload.auto_upload_success", uploadAttempt.providerName()));
                    PacketDistributor.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
                    if (uploadAttempt.providerId() > 0 && uploadAttempt.slot() >= 0) {
                        PacketDistributor.sendToPlayer(serverPlayer,
                                new PatternProviderFocusPacket(uploadAttempt.providerId(), uploadAttempt.slot()));
                    }
                }
                logEncode("uploaded mode={}, search={}, provider={}, encoded={}",
                        mode, providerSearchText, uploadAttempt.providerName(), encodedPattern);
                broadcastChanges();
                return;
            }
        }

        boolean preferEditSlot = uploadEnabled && fallbackToEditSlot;
        if (uploadEnabled && uploadAttempt.hadTarget() && getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "extendedae_plus.screen.upload.auto_upload_failed", uploadAttempt.providerName()));
        }

        storeEncodedPatternLocally(mode, encodedPattern, consumeEditPattern, preferEditSlot,
                uploadAttempt.hadTarget() ? "upload_failed_local_fallback" : "local_encode");
    }

    private boolean hasBlankPatternForEncoding() {
        return AEItems.BLANK_PATTERN.is(blankPatternSlot.getItem());
    }

    private void consumeBlankPatternForEncoding() {
        var blankPattern = blankPatternSlot.getItem();
        blankPattern.shrink(1);
        if (blankPattern.getCount() <= 0) {
            blankPatternSlot.set(ItemStack.EMPTY);
        }
    }

    private void consumePatternForUpload(boolean consumeEditPattern) {
        if (consumeEditPattern) {
            encodedPatternSlot.set(ItemStack.EMPTY);
        } else {
            consumeBlankPatternForEncoding();
        }
    }

    private void storeEncodedPatternLocally(EncodingMode mode,
                                            ItemStack encodedPattern,
                                            boolean consumeEditPattern,
                                            boolean preferEditSlot,
                                            String reason) {
        boolean storedInCache = false;

        if (preferEditSlot) {
            if (!consumeEditPattern) {
                consumeBlankPatternForEncoding();
            }
            encodedPatternSlot.set(encodedPattern);
            patternEncodingLogic.setMode(mode);
            syncedPatternEncodingMode = mode.ordinal();
            updatePatternPreview(mode);
            tryFillBlankPatternFromNetwork();
            logEncode("success mode={}, targetSlot=encoded_edit_slot, reason={}, encoded={}",
                    mode, reason, encodedPattern);
            broadcastChanges();
            return;
        }

        Slot targetSlot = findEmptyPatternCacheSlot();
        if (targetSlot != null && !consumeEditPattern) {
            consumeBlankPatternForEncoding();
            targetSlot.set(encodedPattern);
            patternEncodingLogic.setMode(mode);
            syncedPatternEncodingMode = mode.ordinal();
            updatePatternPreview(mode);
            tryFillBlankPatternFromNetwork();
            logEncode("success mode={}, targetSlot=pattern_cache_{}, reason={}, encoded={}",
                    mode, targetSlot.index, reason, encodedPattern);
            storedInCache = true;
        }

        if (!storedInCache) {
            if (!consumeEditPattern) {
                consumeBlankPatternForEncoding();
            }
            encodedPatternSlot.set(encodedPattern);
            syncedPatternEncodingMode = patternEncodingLogic.getMode().ordinal();
            updatePatternPreview(getPatternEncodingMode());
            tryFillBlankPatternFromNetwork();
            logEncode("success mode={}, targetSlot=encoded_edit_slot, reason={}, encoded={}",
                    mode, reason, encodedPattern);
        }

        broadcastChanges();
    }

    private void tryFillBlankPatternFromNetwork() {
        if (isClientSide() || patternEncodingLogic == null || blankPatternSlot == null) {
            return;
        }

        try {
            InternalInventory blankInv = patternEncodingLogic.getBlankPatternInv();
            ItemStack current = blankInv.getStackInSlot(0);
            int space = Math.max(0, blankInv.getSlotLimit(0) - current.getCount());
            space = Math.min(space, AEItems.BLANK_PATTERN.stack().getMaxStackSize());
            if (space <= 0) {
                return;
            }

            AEItemKey blankKey = AEItemKey.of(AEItems.BLANK_PATTERN.asItem());
            long extracted = StorageHelper.poweredExtraction(energySource, storage, blankKey, space, getActionSource());
            if (extracted <= 0) {
                return;
            }

            int toInsert = (int) Math.min(extracted, space);
            ItemStack slotStack = blankPatternSlot.getItem();
            if (slotStack.isEmpty()) {
                blankPatternSlot.set(AEItems.BLANK_PATTERN.stack(toInsert));
            } else if (AEItems.BLANK_PATTERN.is(slotStack)) {
                slotStack.grow(toInsert);
                blankPatternSlot.set(slotStack);
            } else {
                StorageHelper.poweredInsert(energySource, storage, blankKey, extracted, getActionSource());
                return;
            }

            long leftover = extracted - toInsert;
            if (leftover > 0) {
                StorageHelper.poweredInsert(energySource, storage, blankKey, leftover, getActionSource());
            }
            broadcastChanges();
        } catch (Exception e) {
            com.lhy.wcwt.WcwtMod.LOGGER.debug("WCWT blank pattern auto-fill failed", e);
        }
    }

    public void handleTopAction(TopActionPacket.Action action) {
        if (isClientSide()) {
            sendClientAction(TOP_ACTION, action);
        }
        if (menuHost == null) {
            return;
        }
        switch (action) {
            case TOGGLE_PICKUP_MODE -> menuHost.toggleMagnetPickupMode();
            case TOGGLE_INSERT_MODE -> menuHost.toggleMagnetInsertMode();
            case COPY_UP -> menuHost.copyMagnetInsertToPickup();
            case COPY_DOWN -> menuHost.copyMagnetPickupToInsert();
            case SWITCH_FILTERS -> menuHost.switchMagnetFilters();
            case OPEN_MAGNET_MENU -> openWcwtSubMenu(ModMenus.WCWT_MAGNET_MENU.get());
            case OPEN_TRASH_MENU -> openWcwtSubMenu(ModMenus.WCWT_TRASH_MENU.get());
            default -> {
            }
        }
    }

    private void openWcwtSubMenu(MenuType<?> menuType) {
        if (isClientSide()) {
            return;
        }
        if (getLocator() != null && !MenuOpener.open(menuType, getPlayer(), getLocator())) {
            getPlayer().displayClientMessage(Component.translatable("gui.wcwt.top_action.unavailable"), true);
        }
    }

    public void openWcwtMagnetMenu() {
        if (isClientSide()) {
            PacketDistributor.sendToServer(new TopActionPacket(TopActionPacket.Action.OPEN_MAGNET_MENU));
            return;
        }
        openWcwtSubMenu(ModMenus.WCWT_MAGNET_MENU.get());
    }

    public void openWcwtTrashMenu() {
        if (isClientSide()) {
            PacketDistributor.sendToServer(new TopActionPacket(TopActionPacket.Action.OPEN_TRASH_MENU));
            return;
        }
        openWcwtSubMenu(ModMenus.WCWT_TRASH_MENU.get());
    }

    public void updatePatternPreview(EncodingMode mode) {
        if (patternPreviewSlot == null || patternEncodingLogic == null) {
            return;
        }

        ItemStack preview = switch (mode) {
            case CRAFTING -> getCraftingPatternPreview();
            case SMITHING_TABLE -> getSmithingPatternPreview();
            case STONECUTTING -> getStonecuttingPatternPreview();
            case PROCESSING -> ItemStack.EMPTY;
        };
        patternPreviewSlot.setResultItem(preview);
        patternPreviewSlot.setActive(mode != EncodingMode.PROCESSING);
    }

    private ItemStack getCraftingPatternPreview() {
        var ingredients = new ItemStack[9];
        for (int slot = 0; slot < ingredients.length; slot++) {
            GenericStack stack = patternEncodingLogic.getEncodedInputInv().getStack(slot);
            if (stack == null) {
                ingredients[slot] = ItemStack.EMPTY;
            } else if (stack.what() instanceof AEItemKey itemKey) {
                ingredients[slot] = itemKey.toStack(1);
            } else {
                return ItemStack.EMPTY;
            }
        }

        var input = CraftingInput.of(3, 3, java.util.Arrays.asList(ingredients));
        var level = getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElse(null);
        return recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input, level.registryAccess());
    }

    private ItemStack getSmithingPatternPreview() {
        if (!(patternEncodingLogic.getEncodedInputInv().getKey(0) instanceof AEItemKey template)
                || !(patternEncodingLogic.getEncodedInputInv().getKey(1) instanceof AEItemKey base)
                || !(patternEncodingLogic.getEncodedInputInv().getKey(2) instanceof AEItemKey addition)) {
            return ItemStack.EMPTY;
        }

        var input = new SmithingRecipeInput(template.toStack(), base.toStack(), addition.toStack());
        var level = getPlayer().level();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).orElse(null);
        return recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input, level.registryAccess());
    }

    private ItemStack getStonecuttingPatternPreview() {
        if (!(patternEncodingLogic.getEncodedInputInv().getKey(0) instanceof AEItemKey inputKey)) {
            return ItemStack.EMPTY;
        }

        updateStonecuttingRecipes();
        var selectedRecipeId = patternEncodingLogic.getStonecuttingRecipeId();
        if (selectedRecipeId == null && !stonecuttingRecipes.isEmpty()) {
            selectedRecipeId = stonecuttingRecipes.get(0).id();
            patternEncodingLogic.setStonecuttingRecipeId(selectedRecipeId);
        }

        var input = new SingleRecipeInput(inputKey.toStack());
        var level = getPlayer().level();
        var recipe = selectedRecipeId == null ? null : level.getRecipeManager()
                .getRecipeFor(RecipeType.STONECUTTING, input, level, selectedRecipeId)
                .orElse(null);
        return recipe == null ? ItemStack.EMPTY : recipe.value().getResultItem(level.registryAccess()).copy();
    }

    private ItemStack encodeCraftingPattern() {
        var ingredients = new ItemStack[9];
        boolean valid = false;
        for (int slot = 0; slot < ingredients.length; slot++) {
            GenericStack stack = patternEncodingLogic.getEncodedInputInv().getStack(slot);
            if (stack == null) {
                ingredients[slot] = ItemStack.EMPTY;
                continue;
            }
            if (!(stack.what() instanceof AEItemKey itemKey)) {
                logEncode("crafting slot {} is not an item key: {}", slot, stack);
                return ItemStack.EMPTY;
            }
            ingredients[slot] = itemKey.toStack(1);
            valid = true;
        }
        if (!valid) {
            logEncode("crafting has no valid inputs");
            return ItemStack.EMPTY;
        }

        var input = CraftingInput.of(3, 3, java.util.Arrays.asList(ingredients));
        var level = getPlayer().level();
        RecipeHolder<CraftingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElse(null);
        if (recipe == null) {
            logEncode("crafting recipe not found, ingredients={}",
                    java.util.Arrays.toString(ingredients));
            return ItemStack.EMPTY;
        }

        ItemStack result = recipe.value().assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            logEncode("crafting recipe assembled empty, recipe={}", recipe.id());
            return ItemStack.EMPTY;
        }
        logEncode("crafting recipe found id={}, result={}", recipe.id(), result);
        return PatternDetailsHelper.encodeCraftingPattern(recipe, ingredients, result,
                patternEncodingLogic.isSubstitution(), patternEncodingLogic.isFluidSubstitution());
    }

    private ItemStack encodeSmithingTablePattern() {
        if (!(patternEncodingLogic.getEncodedInputInv().getKey(0) instanceof AEItemKey template)
                || !(patternEncodingLogic.getEncodedInputInv().getKey(1) instanceof AEItemKey base)
                || !(patternEncodingLogic.getEncodedInputInv().getKey(2) instanceof AEItemKey addition)) {
            logEncode("smithing missing item inputs, inputs={}",
                    summarizeConfig(patternEncodingLogic.getEncodedInputInv()));
            return ItemStack.EMPTY;
        }

        var input = new SmithingRecipeInput(template.toStack(), base.toStack(), addition.toStack());
        var level = getPlayer().level();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).orElse(null);
        if (recipe == null) {
            logEncode("smithing recipe not found, template={}, base={}, addition={}",
                    template, base, addition);
            return ItemStack.EMPTY;
        }
        var output = AEItemKey.of(recipe.value().assemble(input, level.registryAccess()));
        logEncode("smithing recipe found id={}, output={}", recipe.id(), output);
        return PatternDetailsHelper.encodeSmithingTablePattern(recipe, template, base, addition, output,
                patternEncodingLogic.isSubstitution());
    }

    private ItemStack encodeStonecuttingPattern() {
        if (!(patternEncodingLogic.getEncodedInputInv().getKey(0) instanceof AEItemKey inputKey)) {
            logEncode("stonecutting missing item input, inputs={}",
                    summarizeConfig(patternEncodingLogic.getEncodedInputInv()));
            return ItemStack.EMPTY;
        }

        updateStonecuttingRecipes();
        var selectedRecipeId = patternEncodingLogic.getStonecuttingRecipeId();
        if (selectedRecipeId == null && !stonecuttingRecipes.isEmpty()) {
            selectedRecipeId = stonecuttingRecipes.get(0).id();
            patternEncodingLogic.setStonecuttingRecipeId(selectedRecipeId);
        }

        var input = new SingleRecipeInput(inputKey.toStack());
        var level = getPlayer().level();
        var recipe = selectedRecipeId == null ? null : level.getRecipeManager()
                .getRecipeFor(RecipeType.STONECUTTING, input, level, selectedRecipeId)
                .orElse(null);
        if (recipe == null) {
            logEncode("stonecutting recipe not found, input={}", inputKey);
            return ItemStack.EMPTY;
        }
        var output = AEItemKey.of(recipe.value().getResultItem(level.registryAccess()));
        logEncode("stonecutting recipe found id={}, output={}", recipe.id(), output);
        return PatternDetailsHelper.encodeStonecuttingPattern(recipe, inputKey, output,
                patternEncodingLogic.isSubstitution());
    }

    private ItemStack createProcessingPattern() {
        if (patternEncodingLogic == null || blankPatternSlot == null) {
            logEncode("processing missing patternEncodingLogic or blankPatternSlot");
            return ItemStack.EMPTY;
        }

        var inputs = new GenericStack[patternEncodingLogic.getEncodedInputInv().size()];
        boolean valid = false;
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = patternEncodingLogic.getEncodedInputInv().getStack(slot);
            if (inputs[slot] != null) {
                valid = true;
            }
        }
        if (!valid) {
            logEncode("processing has no inputs");
            return ItemStack.EMPTY;
        }

        var outputs = new GenericStack[patternEncodingLogic.getEncodedOutputInv().size()];
        valid = false;
        for (int slot = 0; slot < outputs.length; slot++) {
            outputs[slot] = patternEncodingLogic.getEncodedOutputInv().getStack(slot);
            if (outputs[slot] != null) {
                valid = true;
            }
        }
        if (!valid) {
            logEncode("processing has no outputs");
            return ItemStack.EMPTY;
        }
        if (outputs[0] == null) {
            logEncode("processing primary output is empty, outputs={}",
                    summarizeConfig(patternEncodingLogic.getEncodedOutputInv()));
            return ItemStack.EMPTY;
        }

        logEncode("processing encode inputs={}, outputs={}",
                java.util.Arrays.toString(inputs), java.util.Arrays.toString(outputs));
        return PatternDetailsHelper.encodeProcessingPattern(
                java.util.Arrays.stream(inputs).toList(),
                java.util.Arrays.stream(outputs).toList());
    }

    private static String summarizeConfig(appeng.util.ConfigInventory inventory) {
        StringBuilder result = new StringBuilder("[");
        boolean first = true;
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack == null) {
                continue;
            }
            if (!first) {
                result.append(", ");
            }
            first = false;
            result.append(i).append('=').append(stack);
        }
        return result.append(']').toString();
    }

    private static void logEncode(String message, Object... args) {
        if (DEBUG_ENCODE) {
            com.lhy.wcwt.WcwtMod.LOGGER.info("WCWT encode debug: " + message, args);
        }
    }

    private static void logAdvanced(String message, Object... args) {
        if (DEBUG_ADVANCED) {
            com.lhy.wcwt.WcwtMod.LOGGER.info("WCWT advanced debug: " + message, args);
        }
    }

    private void updateStonecuttingRecipes() {
        stonecuttingRecipes.clear();
        if (patternEncodingLogic == null
                || !(patternEncodingLogic.getEncodedInputInv().getKey(0) instanceof AEItemKey inputKey)) {
            return;
        }

        var level = getPlayer().level();
        var recipeInput = new SingleRecipeInput(inputKey.toStack());
        stonecuttingRecipes.addAll(level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, recipeInput, level));

        var selected = patternEncodingLogic.getStonecuttingRecipeId();
        if (selected != null && stonecuttingRecipes.stream().noneMatch(recipe -> recipe.id().equals(selected))) {
            patternEncodingLogic.setStonecuttingRecipeId(null);
        }
    }

    private boolean mergeProcessingPatternInputs() {
        if (patternEncodingLogic == null) {
            return false;
        }
        ConfigInventory inputInv = patternEncodingLogic.getEncodedInputInv();
        List<GenericStack> merged = mergeDenseProcessingInputsFromInventory(inputInv);
        if (slotsMatchMergedProcessingInputs(inputInv, merged)) {
            return false;
        }
        inputInv.beginBatch();
        try {
            for (int i = 0; i < inputInv.size(); i++) {
                inputInv.setStack(i, i < merged.size() ? merged.get(i) : null);
            }
        } finally {
            inputInv.endBatch();
        }
        return true;
    }

    private static boolean slotsMatchMergedProcessingInputs(ConfigInventory inputInv, List<GenericStack> merged) {
        for (int i = 0; i < inputInv.size(); i++) {
            GenericStack expected = i < merged.size() ? merged.get(i) : null;
            if (!Objects.equals(inputInv.getStack(i), expected)) {
                return false;
            }
        }
        return true;
    }

    private static List<GenericStack> mergeDenseProcessingInputsFromInventory(ConfigInventory inputInv) {
        List<GenericStack> merged = new ArrayList<>();
        for (int i = 0; i < inputInv.size(); i++) {
            GenericStack s = inputInv.getStack(i);
            if (s != null) {
                addOrMergeProcessingStacks(merged, s);
            }
        }
        return merged;
    }

    private static List<GenericStack> mergeDenseProcessingInputsFromSparseList(List<@Nullable GenericStack> sparse) {
        List<GenericStack> merged = new ArrayList<>();
        for (GenericStack s : sparse) {
            if (s != null) {
                addOrMergeProcessingStacks(merged, s);
            }
        }
        return merged;
    }

    /** 与 AE2 {@code EncodingHelper#addOrMerge} 相同逻辑。 */
    private static void addOrMergeProcessingStacks(List<GenericStack> stacks, GenericStack newStack) {
        for (int i = 0; i < stacks.size(); i++) {
            var existingStack = stacks.get(i);
            if (Objects.equals(existingStack.what(), newStack.what())) {
                long newAmount = LongMath.saturatedAdd(existingStack.amount(), newStack.amount());
                stacks.set(i, new GenericStack(newStack.what(), newAmount));
                long overflow = newStack.amount() - (newAmount - existingStack.amount());
                if (overflow > 0) {
                    stacks.add(new GenericStack(newStack.what(), overflow));
                }
                return;
            }
        }
        stacks.add(newStack);
    }

    public void transferJeiRecipe(List<@Nullable GenericStack> inputs, List<@Nullable GenericStack> outputs,
                                  boolean toCraftingGrid, EncodingMode mode) {
        if (inputs.stream().allMatch(java.util.Objects::isNull)
                && outputs.stream().allMatch(java.util.Objects::isNull)) {
            return;
        }
        if (toCraftingGrid) {
            if (mode != EncodingMode.CRAFTING) {
                return;
            }
            if (getPlayer() instanceof ServerPlayer sp) {
                var templates = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);
                for (int i = 0; i < Math.min(9, inputs.size()); i++) {
                    templates.set(i, toItemStack(inputs.get(i)));
                }
                new FillCraftingGridFromRecipePacket(null, templates, false).handleOnServer(sp);
            }
            return;
        }

        if (patternEncodingLogic == null) {
            return;
        }
        patternEncodingLogic.setMode(mode);
        var inputInv = patternEncodingLogic.getEncodedInputInv();
        List<GenericStack> mergedInputs = null;
        if (mode == EncodingMode.PROCESSING && processingMaterialsMerge) {
            mergedInputs = mergeDenseProcessingInputsFromSparseList(inputs);
        }
        for (int i = 0; i < inputInv.size(); i++) {
            GenericStack stack = mergedInputs != null
                    ? (i < mergedInputs.size() ? mergedInputs.get(i) : null)
                    : (i < inputs.size() ? inputs.get(i) : null);
            inputInv.setStack(i, stack);
        }
        var outputInv = patternEncodingLogic.getEncodedOutputInv();
        for (int i = 0; i < outputInv.size(); i++) {
            outputInv.setStack(i, i < outputs.size() ? outputs.get(i) : null);
        }
        broadcastChanges();
    }

    private static ItemStack toItemStack(@Nullable GenericStack stack) {
        if (stack != null && stack.what() instanceof AEItemKey itemKey) {
            return itemKey.toStack((int) Math.min(Math.max(1, stack.amount()), itemKey.getMaxStackSize()));
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private Slot findEmptyPatternCacheSlot() {
        for (Slot slot : getPatternCacheSlots()) {
            if (slot.getItem().isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        long startNs = DEBUG_PERF && isServerSide() ? System.nanoTime() : 0L;
        ItemStack result = wcwtQuickMoveStackUnchecked(player, slotIndex);
        if (DEBUG_PERF && isServerSide()) {
            long totalNs = System.nanoTime() - startNs;
            if (totalNs >= PERF_LOG_THRESHOLD_NS) {
                Slot sourceSlot = slotIndex >= 0 && slotIndex < slots.size() ? slots.get(slotIndex) : null;
                ItemStack sourceStack = sourceSlot != null ? sourceSlot.getItem() : ItemStack.EMPTY;
                WcwtMod.LOGGER.info(
                        "WCWT perf: quickMoveStack player={}, slotIndex={}, sourceEmpty={}, sourceItem={}, resultEmpty={}, totalMs={}",
                        player.getScoreboardName(),
                        slotIndex,
                        sourceStack.isEmpty(),
                        sourceStack.isEmpty() ? "<empty>" : sourceStack.getItem().toString(),
                        result.isEmpty(),
                        formatPerfMs(totalNs));
            }
        }
        return result;
    }

    private ItemStack wcwtQuickMoveStackUnchecked(Player player, int slotIndex) {
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            Slot sourceSlot = slots.get(slotIndex);
            if (sourceSlot.hasItem() && !isPlayerArmorSlot(sourceSlot)) {
                ItemStack sourceStack = sourceSlot.getItem();
                ItemStack movedCurioToPlayer = tryMoveCurioToPlayerInventoryFirst(player, sourceSlot, sourceStack);
                if (!movedCurioToPlayer.isEmpty()) {
                    return movedCurioToPlayer;
                }

                EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(sourceStack);
                if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    Slot targetSlot = getPlayerArmorSlot(equipmentSlot);
                    if (targetSlot != null && !targetSlot.hasItem() && targetSlot.mayPlace(sourceStack)) {
                        ItemStack original = sourceStack.copy();
                        if (moveItemStackTo(sourceStack, targetSlot.index, targetSlot.index + 1, false)) {
                            finishPriorityQuickMove(player, sourceSlot, sourceStack);
                            return original;
                        }
                    }
                }

                ItemStack movedToCurio = tryMoveStackToCurioSlot(player, sourceSlot, sourceStack);
                if (!movedToCurio.isEmpty()) {
                    return movedToCurio;
                }

                ItemStack toolkitMove = tryToolkitQuickMoveShortcuts(player, sourceSlot, sourceStack);
                if (!toolkitMove.isEmpty()) {
                    return toolkitMove;
                }
            }
        }
        return super.quickMoveStack(player, slotIndex);
    }

    private ItemStack tryMoveStackToCurioSlot(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty() || isCurioSlot(sourceSlot)) {
            return ItemStack.EMPTY;
        }
        if (menuHost.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.CURIOS) {
            return ItemStack.EMPTY;
        }

        for (var targetSlot : getSlots(WcwtSlotSemantics.AE_CURIOS)) {
            if (targetSlot == sourceSlot || targetSlot.hasItem() || !targetSlot.mayPlace(sourceStack)) {
                continue;
            }

            ItemStack original = sourceStack.copy();
            if (moveItemStackTo(sourceStack, targetSlot.index, targetSlot.index + 1, false)) {
                finishPriorityQuickMove(player, sourceSlot, sourceStack);
                return original;
            }
        }

        return ItemStack.EMPTY;
    }

    private void finishPriorityQuickMove(Player player, Slot sourceSlot, ItemStack remainingStack) {
        if (remainingStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, remainingStack);
    }

    @Nullable
    private Slot getPlayerArmorSlot(EquipmentSlot equipmentSlot) {
        var semantic = switch (equipmentSlot) {
            case HEAD -> WcwtSlotSemantics.AE2WTLIB_HELMET;
            case CHEST -> WcwtSlotSemantics.AE2WTLIB_CHESTPLATE;
            case LEGS -> WcwtSlotSemantics.AE2WTLIB_LEGGINGS;
            case FEET -> WcwtSlotSemantics.AE2WTLIB_BOOTS;
            default -> null;
        };
        if (semantic == null) {
            return null;
        }
        var armorSlots = getSlots(semantic);
        return armorSlots.isEmpty() ? null : armorSlots.get(0);
    }

    private boolean isPlayerArmorSlot(Slot slot) {
        return slot == getPlayerArmorSlot(EquipmentSlot.HEAD)
                || slot == getPlayerArmorSlot(EquipmentSlot.CHEST)
                || slot == getPlayerArmorSlot(EquipmentSlot.LEGS)
                || slot == getPlayerArmorSlot(EquipmentSlot.FEET);
    }

    private boolean isCurioSlot(Slot slot) {
        return getSlots(WcwtSlotSemantics.AE_CURIOS).contains(slot);
    }

    private ItemStack tryToolkitQuickMoveShortcuts(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }
        if (menuHost.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.TOOLKIT) {
            return ItemStack.EMPTY;
        }
        var toolkitSlotsOrdered = getSlots(WcwtSlotSemantics.WCWT_TOOLKIT);
        if (toolkitSlotsOrdered.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (toolkitSlotsOrdered.contains(sourceSlot)) {
            return tryQuickMoveToolkitToPlayerBulk(sourceSlot, sourceStack);
        }
        if (isPlayerHotbarOrStorageSemanticSlot(sourceSlot)) {
            if (ToolkitItemRules.isEligibleForToolkitQuickFill(sourceStack)) {
                return tryQuickMovePlayerIntoToolkitSlots(sourceSlot, sourceStack);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack tryMoveCurioToPlayerInventoryFirst(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty() || !isCurioSlot(sourceSlot)) {
            return ItemStack.EMPTY;
        }
        if (menuHost.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.CURIOS) {
            return ItemStack.EMPTY;
        }
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        if (playerInventoryStart < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceStack.copy();
        if (moveItemStackTo(sourceStack, playerInventoryStart, slots.size(), false)) {
            finishPriorityQuickMove(player, sourceSlot, sourceStack);
            return original;
        }
        return ItemStack.EMPTY;
    }

    private boolean isPlayerHotbarOrStorageSemanticSlot(Slot slot) {
        var semantic = getSlotSemantic(slot);
        return semantic == SlotSemantics.PLAYER_HOTBAR || semantic == SlotSemantics.PLAYER_INVENTORY;
    }

    private int getPlayerInventoryStartMenuIndex() {
        var inventory = getPlayerInventory();
        int compartment = inventory.items.size();
        int start = slots.size() - compartment;
        return start < 0 || start >= slots.size() ? -1 : start;
    }

    private ItemStack tryQuickMoveToolkitToPlayerBulk(Slot toolkitSlot, ItemStack toolkitStack) {
        if (toolkitStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int mainStorageStartMenu = getPlayerInventoryStartMenuIndex();
        if (mainStorageStartMenu < 0 || mainStorageStartMenu >= slots.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = toolkitStack.copy();
        if (moveItemStackTo(toolkitStack, mainStorageStartMenu, slots.size(), false)) {
            finishPriorityQuickMove(getPlayerInventory().player, toolkitSlot, toolkitStack);
            return original;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack tryQuickMovePlayerIntoToolkitSlots(Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var toolkitSlotsOrdered = getSlots(WcwtSlotSemantics.WCWT_TOOLKIT);
        Slot[] lookup = buildToolkitLogicalSlotLookup(toolkitSlotsOrdered);
        ItemStack original = sourceStack.copy();
        for (int logicalIndex : ToolkitItemRules.insertionIndexOrder(sourceStack)) {
            if (logicalIndex < 0 || logicalIndex >= lookup.length) {
                continue;
            }
            Slot targetSlot = lookup[logicalIndex];
            if (targetSlot == null || targetSlot.hasItem() || !targetSlot.mayPlace(sourceStack)) {
                continue;
            }
            int tgtMenu = slots.indexOf(targetSlot);
            if (tgtMenu < 0) {
                continue;
            }
            if (moveItemStackTo(sourceStack, tgtMenu, tgtMenu + 1, false)) {
                finishPriorityQuickMove(getPlayerInventory().player, sourceSlot, sourceStack);
                return original;
            }
        }
        return ItemStack.EMPTY;
    }

    private Slot[] buildToolkitLogicalSlotLookup(List<Slot> toolkitSlotsOrdered) {
        Slot[] lookup = new Slot[toolkitSlotsOrdered.size()];
        for (Slot slot : toolkitSlotsOrdered) {
            if (slot instanceof ToolkitSlot toolkitSlot) {
                int logical = toolkitSlot.toolkitLogicalIndex();
                if (logical >= 0 && logical < lookup.length) {
                    lookup[logical] = slot;
                }
            }
        }
        return lookup;
    }

    /**
     * 客户端可见的元件 ItemStack（COPY 自 WCWT_STORAGE_CELL 槽，由 vanilla broadcastChanges 同步）。
     * 用于客户端读取元件 config 数据（DataComponent 序列化在 stack 上）。
     */
    private ItemStack getCurrentCellStack() {
        var slots = getSlots(WcwtSlotSemantics.WCWT_STORAGE_CELL);
        return slots.isEmpty() ? ItemStack.EMPTY : slots.get(0).getItem();
    }

    /**
     * 元件编辑区当前可用槽位数量（双端可调用）。
     *  - 已放入元件：取 cwi.getConfigInventory(stack).size()（典型为 18 / 36 / 63）
     *  - 未放入 + KEEP_ON_REMOVE：返回 63（沿用最大容量做占位）
     *  - 其他：0
     */
    public int getCellConfigInventorySize() {
        var stack = getCurrentCellStack();
        if (!stack.isEmpty() && stack.getItem() instanceof ICellWorkbenchItem cwi) {
            var inv = cwi.getConfigInventory(stack);
            return inv == null ? 0 : inv.size();
        }
        return cellCopyMode == CopyMode.KEEP_ON_REMOVE ? 63 : 0;
    }

    /**
     * 读取元件编辑区第 idx 格的 GenericStack（双端可调用：直接解析当前 cell ItemStack 上的 DataComponent）。
     * 越界/空槽返回 null。
     */
    @Nullable
    public GenericStack getCellConfigStack(int idx) {
        var stack = getCurrentCellStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ICellWorkbenchItem cwi)) return null;
        var inv = cwi.getConfigInventory(stack);
        if (inv == null || idx < 0 || idx >= inv.size()) return null;
        return inv.getStack(idx);
    }

    public boolean isCellConfigKeyAllowed(int idx, @Nullable AEKey key) {
        if (key == null) {
            return true;
        }
        var stack = getCurrentCellStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ICellWorkbenchItem cwi)) {
            return false;
        }
        var inv = cwi.getConfigInventory(stack);
        return inv != null && idx >= 0 && idx < inv.size() && inv.isAllowedIn(idx, key);
    }

    /**
     * 元件编辑区第 idx 格当前是否可用（受元件容量与复制模式控制）。
     * AdvancedCodingPanel 通过这个方法决定 ghost 标记/点击是否生效。
     */
    public boolean isCellSlotEnabled(int idx) {
        if (menuHost == null) {
            return false;
        }
        var cwi = menuHost.getStorageCellWorkbenchItem();
        if (cwi != null && cellCopyMode == CopyMode.CLEAR_ON_REMOVE) {
            return idx < cwi.getConfigInventory(menuHost.getStorageCellItem()).size();
        }
        return cellCopyMode == CopyMode.KEEP_ON_REMOVE;
    }

    /**
     * IOptionalSlotHost 实现：
     * 1. 前 8 格用于元件升级卡槽，仅当元件实际支持对应升级数时启用。
     * 2. 后续一段索引用于谐振样板缓存区，仅在谐振过载编码器面板打开时启用。
     */
    @Override
    public boolean isSlotEnabled(int idx) {
        if (menuHost == null) {
            return false;
        }
        if (idx < CELL_UPGRADE_SLOTS) {
            var upgrades = menuHost.getCellUpgrades();
            return upgrades != null && idx < upgrades.size();
        }

        int resonatingIndex = idx - OPTIONAL_SLOT_GROUP_RESONATING_STORAGE_BASE;
        var resonatingStorage = getSlots(WcwtSlotSemantics.WCWT_RESONATING_STORAGE);
        return resonatingIndex >= 0
                && resonatingIndex < resonatingStorage.size()
                && menuHost.getCurrentExtendedUI() == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING;
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        updatePatternPreview(getPatternEncodingMode());
        if (menuHost != null && getSlots(WcwtSlotSemantics.WCWT_CELL_UPGRADE).contains(slot)) {
            menuHost.persistStorageCellItem();
            broadcastChanges();
        }
    }

    @Override
    public boolean isValidForSlot(Slot slot, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && getSlots(WcwtSlotSemantics.WCWT_CELL_UPGRADE).contains(slot)) {
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null && "ae2importexportcard".equals(key.getNamespace())
                    && (key.getPath().contains("import_card") || key.getPath().contains("export_card"))) {
                return true;
            }
        }
        return super.isValidForSlot(slot, stack);
    }
    
    /**
     * 应用样板倍增器操作
     * 基于ExtendedAE的ContainerPatternModifier.modify()实现
     */
    public void applyPatternMultiplier(PatternMultiplierButton.MultiplierType type) {
        // 根据类型确定操作参数
        int scale;
        boolean divide;
        
        switch (type) {
            case TIMES_2:
                scale = 2;
                divide = false;
                break;
            case TIMES_3:
                scale = 3;
                divide = false;
                break;
            case TIMES_5:
                scale = 5;
                divide = false;
                break;
            case DIVIDE_2:
                scale = 2;
                divide = true;
                break;
            case DIVIDE_3:
                scale = 3;
                divide = true;
                break;
            case DIVIDE_5:
                scale = 5;
                divide = true;
                break;
            case EQUALS_1:
                restoreProcessingPatternRatio();
                return;
            case SWAP:
                rotateProcessingPatternOutputs();
                return;
            default:
                return;
        }
        
        // 对所有样板槽位应用倍增/除法
        modifyPatterns(scale, divide);
    }
    
    /**
     * 倍增或除法样板
     * 基于ExtendedAE的实现
     */
    private void modifyPatterns(int scale, boolean divide) {
        if (scale <= 0) {
            return;
        }
        
        for (var slot : getPatternCacheSlots()) {
            var stack = slot.getItem();
            var detail = PatternDetailsHelper.decodePattern(stack, getPlayer().level());

            var advPattern = AdvAeBridge.applyScale(stack, getPlayer().level(), scale, divide);
            if (advPattern != null && !advPattern.isEmpty()) {
                slot.set(advPattern);
                continue;
            }
            
            // 只处理加工样板
            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                var input = process.getSparseInputs().toArray(new GenericStack[0]);
                var output = process.getOutputs().toArray(new GenericStack[0]);
                
                // 检查是否可以进行修改
                if (checkCanModify(input, scale, divide) && checkCanModify(output, scale, divide)) {
                    var modifiedInput = new GenericStack[input.length];
                    var modifiedOutput = new GenericStack[output.length];
                    
                    modifyStacks(input, modifiedInput, scale, divide);
                    modifyStacks(output, modifiedOutput, scale, divide);
                    
                    // 编码新样板
                    var newPattern = PatternDetailsHelper.encodeProcessingPattern(
                        java.util.Arrays.stream(modifiedInput).toList(),
                        java.util.Arrays.stream(modifiedOutput).toList()
                    );
                    
                    slot.set(newPattern);
                }
            }
        }
        broadcastChanges();
    }
    
    /**
     * 检查是否可以进行倍增/除法操作
     */
    private static boolean checkCanModify(GenericStack[] stacks, int scale, boolean divide) {
        if (divide) {
            // 除法：所有数量必须能被scale整除
            for (var stack : stacks) {
                if (stack != null && stack.amount() % scale != 0) {
                    return false;
                }
            }
        } else {
            // 乘法：所有数量乘以scale后不能超过最大值
            for (var stack : stacks) {
                if (stack != null) {
                    long maxAmount = 999999L * stack.what().getAmountPerUnit();
                    if (stack.amount() * scale > maxAmount) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * 修改GenericStack数组的数量
     */
    private static void modifyStacks(GenericStack[] source, GenericStack[] destination, int scale, boolean divide) {
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                long newAmount = divide ? source[i].amount() / scale : source[i].amount() * scale;
                destination[i] = new GenericStack(source[i].what(), newAmount);
            }
        }
    }
    
    /**
     * 复制样板：将当前选中的缓存样板复制 1 份，生成到高级编码区左侧的复制结果槽。
     */
    public void copyPattern() {
        var host = getMenuHost();
        if (host == null) {
            return;
        }
        var cacheSlots = getPatternCacheSlots();
        var copySlots = getSlots(WcwtSlotSemantics.COPY_PATTERN);
        int selectedIndex = host.getSelectedPatternIndex();
        if (selectedIndex < 0 || selectedIndex >= cacheSlots.size() || copySlots.isEmpty()) {
            logAdvanced("copy invalid selection selectedIndex={} cacheSize={} copySlots={}",
                    selectedIndex, cacheSlots.size(), copySlots.size());
            return;
        }

        var source = cacheSlots.get(selectedIndex).getItem();
        if (source.isEmpty() || PatternDetailsHelper.decodePattern(source, getPlayer().level()) == null) {
            logAdvanced("copy source invalid selectedIndex={} source={}",
                    selectedIndex, source);
            host.clearSelection();
            broadcastChanges();
            return;
        }

        if (!hasBlankPatternForEncoding()) {
            tryFillBlankPatternFromNetwork();
        }
        if (!hasBlankPatternForEncoding()) {
            logAdvanced("copy no blank pattern available after autofill");
            return;
        }

        var outputSlot = copySlots.get(0);
        var existing = outputSlot.getItem();
        var copy = source.copyWithCount(1);

        if (existing.isEmpty()) {
            consumeBlankPatternForEncoding();
            outputSlot.set(copy);
            logAdvanced("copy copied pattern into empty output slot, selectedIndex={}",
                    selectedIndex);
            broadcastChanges();
            return;
        }

        if (ItemStack.isSameItemSameComponents(existing, copy) && existing.getCount() < existing.getMaxStackSize()) {
            consumeBlankPatternForEncoding();
            existing.grow(1);
            outputSlot.set(existing);
            logAdvanced("copy stacked copied pattern into output slot, selectedIndex={}, newCount={}",
                    selectedIndex, existing.getCount());
            broadcastChanges();
        }
    }

    /**
     * 高级编码 UI 打开时，样板缓存槽左键只记录选中状态，不再搬走原缓存样板。
     */
    public void selectPatternForAdvancedCoding(int cacheIndex) {
        var host = getMenuHost();
        var cacheSlots = getPatternCacheSlots();
        if (host == null) {
            return;
        }
        if (cacheIndex < 0 || cacheIndex >= cacheSlots.size()) {
            host.clearSelection();
            return;
        }

        var selected = cacheSlots.get(cacheIndex).getItem();
        if (selected.isEmpty() || PatternDetailsHelper.decodePattern(selected, getPlayer().level()) == null) {
            host.clearSelection();
            broadcastChanges();
            return;
        }

        host.setSelectedPatternIndex(cacheIndex);
        broadcastChanges();
    }

    public boolean trySelectPatternForAdvancedCodingFromCacheSlot(int slotIndex) {
        var cacheSlots = getPatternCacheSlots();
        if (slotIndex < 0 || slotIndex >= cacheSlots.size()) {
            return false;
        }
        selectPatternForAdvancedCoding(slotIndex);
        return true;
    }

    public void convertSelectedPatternToResonating() {
        var host = getMenuHost();
        if (host == null) {
            return;
        }
        var cacheSlots = getPatternCacheSlots();
        int converted = 0;
        boolean storageFull = false;
        for (var cacheSlot : cacheSlots) {
            ItemStack source = cacheSlot.getItem();
            if (source.isEmpty()) {
                continue;
            }
            ItemStack resonating = CrystalScienceBridge.encodeResonating(source);
            if (resonating.isEmpty()) {
                continue;
            }
            Slot targetSlot = findFirstEmptyResonatingStorageSlot();
            if (targetSlot == null) {
                storageFull = true;
                break;
            }
            targetSlot.set(resonating.copyWithCount(1));
            cacheSlot.remove(1);
            converted++;
        }

        if (converted == 0) {
            notifyPlayer(storageFull
                    ? "message.wcwt.rlpc.resonating_storage_full"
                    : "message.wcwt.rlpc.resonating_only_processing");
            broadcastChanges();
            return;
        }

        int selectedIndex = host.getSelectedPatternIndex();
        if (selectedIndex >= 0 && selectedIndex < cacheSlots.size() && cacheSlots.get(selectedIndex).getItem().isEmpty()) {
            host.clearSelection();
        }

        notifyPlayer("message.wcwt.rlpc.resonating_converted", converted);
        if (storageFull) {
            notifyPlayer("message.wcwt.rlpc.resonating_storage_full");
        }
        broadcastChanges();
    }

    public void convertSelectedPatternToOverload(int[] inputIdOnlySlots, int[] outputIdOnlySlots) {
        var host = getMenuHost();
        var selectedSlot = getSelectedPatternCacheSlot();
        if (host == null || selectedSlot == null) {
            notifyPlayer("message.wcwt.rlpc.no_selected_pattern");
            return;
        }

        ItemStack source = selectedSlot.getItem();
        if (source.isEmpty()) {
            host.clearSelection();
            broadcastChanges();
            return;
        }

        boolean sourceWasOverload = LightningTechBridge.isOverloadPattern(source);
        ItemStack overload = LightningTechBridge.convertToOverload(
                source,
                getPlayer().level(),
                getPlayer().registryAccess(),
                inputIdOnlySlots,
                outputIdOnlySlots);
        if (overload.isEmpty()) {
            notifyPlayer("message.wcwt.rlpc.overload_unsupported");
            return;
        }

        selectedSlot.set(overload);
        if (!sourceWasOverload) {
            notifyPlayer("message.wcwt.rlpc.overload_converted");
        }
        broadcastChanges();
    }

    /**
     * 收到客户端方向变更包后调用：
     *   1. 解码当前选中的缓存样板（普通加工 / AAE 高级加工 都可）；
     *   2. 把 (key,dir) 合并进 directionMap；
     *   3. 通过 AdvPatternDetailsEncoder 重新编码出 AdvProcessingPattern，写回原缓存槽。
     * 仅在装载 advanced_ae 模组时才生效；否则忽略（避免 NoClassDefFoundError）。
     */
    public void updateCopyPatternDirection(AEKey key, @Nullable Direction dir) {
        if (!ModList.get().isLoaded("advanced_ae")) {
            logAdvanced("direction advanced_ae not loaded");
            return;
        }
        var host = getMenuHost();
        if (host == null) {
            logAdvanced("direction host is null");
            return;
        }
        var cacheSlots = getPatternCacheSlots();
        int selectedIndex = host.getSelectedPatternIndex();
        if (selectedIndex < 0 || selectedIndex >= cacheSlots.size()) {
            logAdvanced("direction invalid selection selectedIndex={} cacheSize={}",
                    selectedIndex, cacheSlots.size());
            return;
        }

        var slot = cacheSlots.get(selectedIndex);
        var stack = slot.getItem();
        if (stack.isEmpty()) {
            logAdvanced("direction selected slot empty selectedIndex={}",
                    selectedIndex);
            host.clearSelection();
            broadcastChanges();
            return;
        }

        var newStack = AdvAeBridge.applyDirection(stack, getPlayer().level(), key, dir);
        if (newStack != null && !newStack.isEmpty()) {
            slot.set(newStack);
            logAdvanced("direction updated selectedIndex={} key={} dir={} old={} new={}",
                    selectedIndex, key, dir, stack, newStack);
            broadcastChanges();
        } else {
            logAdvanced("direction applyDirection returned null/empty selectedIndex={} key={} dir={} stack={}",
                    selectedIndex, key, dir, stack);
        }
    }

    /**
     * AAE 桥接（反射调用，避免 compile-time 依赖 AAE jar）。
     * 把样板（普通 AEProcessingPattern 或 AAE 的 AdvProcessingPattern）按方向重编码为 Adv 样板。
     *
     * 调用的目标 API：
     *   net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern
     *     - List&lt;GenericStack&gt; getSparseInputs()
     *     - List&lt;GenericStack&gt; getSparseOutputs()
     *     - LinkedHashMap&lt;AEKey,Direction&gt; getDirectionMap()
     *   net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder
     *     - static ItemStack encodeProcessingPattern(List, List, HashMap)
     */
    private static final class AdvAeBridge {
        private static volatile boolean inited = false;
        private static Class<?> advPatternClass;
        private static java.lang.reflect.Method advGetSparseInputs;
        private static java.lang.reflect.Method advGetSparseOutputs;
        private static java.lang.reflect.Method advGetDirectionMap;
        private static java.lang.reflect.Method encoderEncode;

        private static synchronized boolean init() {
            if (inited) return advPatternClass != null && encoderEncode != null;
            inited = true;
            try {
                advPatternClass = Class.forName(
                        "net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern");
                advGetSparseInputs  = advPatternClass.getMethod("getSparseInputs");
                advGetSparseOutputs = advPatternClass.getMethod("getSparseOutputs");
                advGetDirectionMap  = advPatternClass.getMethod("getDirectionMap");

                Class<?> encoderClass = Class.forName(
                        "net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder");
                encoderEncode = encoderClass.getMethod(
                        "encodeProcessingPattern",
                        java.util.List.class, java.util.List.class, HashMap.class);
                return true;
            } catch (Throwable t) {
                advPatternClass = null;
                encoderEncode = null;
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        static ItemStack applyDirection(ItemStack stack, net.minecraft.world.level.Level level,
                                        AEKey key, @Nullable Direction dir) {
            if (!init()) return null;
            try {
                var detail = PatternDetailsHelper.decodePattern(stack, level);
                if (detail == null) return null;

                java.util.List<GenericStack> sparseInputs;
                java.util.List<GenericStack> sparseOutputs;
                LinkedHashMap<AEKey, Direction> dirMap;

                if (advPatternClass.isInstance(detail)) {
                    sparseInputs  = (java.util.List<GenericStack>) advGetSparseInputs.invoke(detail);
                    sparseOutputs = (java.util.List<GenericStack>) advGetSparseOutputs.invoke(detail);
                    dirMap = (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail);
                } else if (detail instanceof appeng.crafting.pattern.AEProcessingPattern proc) {
                    sparseInputs  = proc.getSparseInputs();
                    sparseOutputs = proc.getSparseOutputs();
                    dirMap = new LinkedHashMap<>();
                    for (var input : sparseInputs) {
                        if (input != null) dirMap.putIfAbsent(input.what(), null);
                    }
                } else {
                    return null; // 合成 / 锻造 / 切石样板不支持方向编码
                }

                dirMap.put(key, dir);

                Object result = encoderEncode.invoke(
                        null, sparseInputs, sparseOutputs, new HashMap<>(dirMap));
                return result instanceof ItemStack is ? is : null;
            } catch (Throwable t) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        static ItemStack applyReplace(ItemStack stack, net.minecraft.world.level.Level level,
                                      AEKey replaceWhat, @Nullable AEKey replaceWith) {
            if (!init()) return null;
            try {
                var detail = PatternDetailsHelper.decodePattern(stack, level);
                if (detail == null || !advPatternClass.isInstance(detail)) return null;

                var sparseInputs = replaceInStacks(
                        (List<GenericStack>) advGetSparseInputs.invoke(detail), replaceWhat, replaceWith);
                var sparseOutputs = replaceInStacks(
                        (List<GenericStack>) advGetSparseOutputs.invoke(detail), replaceWhat, replaceWith);
                var dirMap = new LinkedHashMap<>(
                        (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));
                if (dirMap.containsKey(replaceWhat)) {
                    var dir = dirMap.remove(replaceWhat);
                    if (replaceWith != null) {
                        dirMap.put(replaceWith, dir);
                    }
                }

                Object result = encoderEncode.invoke(
                        null, sparseInputs, sparseOutputs, new HashMap<>(dirMap));
                return result instanceof ItemStack is ? is : null;
            } catch (Throwable t) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        static ItemStack applyScale(ItemStack stack, net.minecraft.world.level.Level level,
                                    int scale, boolean divide) {
            if (!init()) return null;
            try {
                var detail = PatternDetailsHelper.decodePattern(stack, level);
                if (detail == null || !advPatternClass.isInstance(detail)) return null;

                var input = ((List<GenericStack>) advGetSparseInputs.invoke(detail)).toArray(new GenericStack[0]);
                var output = ((List<GenericStack>) advGetSparseOutputs.invoke(detail)).toArray(new GenericStack[0]);
                if (!checkCanModify(input, scale, divide) || !checkCanModify(output, scale, divide)) {
                    return null;
                }

                var scaledInput = new GenericStack[input.length];
                var scaledOutput = new GenericStack[output.length];
                modifyStacks(input, scaledInput, scale, divide);
                modifyStacks(output, scaledOutput, scale, divide);
                var dirMap = new LinkedHashMap<>(
                        (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));

                Object result = encoderEncode.invoke(
                        null,
                        java.util.Arrays.stream(scaledInput).toList(),
                        java.util.Arrays.stream(scaledOutput).toList(),
                        new HashMap<>(dirMap));
                return result instanceof ItemStack is ? is : null;
            } catch (Throwable t) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        static ItemStack applyRestoreRatio(ItemStack stack, net.minecraft.world.level.Level level) {
            if (!init()) return null;
            try {
                var detail = PatternDetailsHelper.decodePattern(stack, level);
                if (detail == null || !advPatternClass.isInstance(detail)) return null;

                var input = (List<GenericStack>) advGetSparseInputs.invoke(detail);
                var output = (List<GenericStack>) advGetSparseOutputs.invoke(detail);
                long gcd = computeSharedGcd(input, output);
                if (gcd <= 1L) {
                    return null;
                }
                var dirMap = new LinkedHashMap<>(
                        (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));

                Object result = encoderEncode.invoke(
                        null,
                        divideStacks(input, gcd),
                        divideStacks(output, gcd),
                        new HashMap<>(dirMap));
                return result instanceof ItemStack is ? is : null;
            } catch (Throwable t) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        static ItemStack applyRotateOutputs(ItemStack stack, net.minecraft.world.level.Level level) {
            if (!init()) return null;
            try {
                var detail = PatternDetailsHelper.decodePattern(stack, level);
                if (detail == null || !advPatternClass.isInstance(detail)) return null;

                var input = (List<GenericStack>) advGetSparseInputs.invoke(detail);
                var output = ((List<GenericStack>) advGetSparseOutputs.invoke(detail)).toArray(new GenericStack[0]);
                var rotatedOutput = rotateOutputs(output);
                var dirMap = new LinkedHashMap<>(
                        (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));

                Object result = encoderEncode.invoke(
                        null,
                        input,
                        java.util.Arrays.stream(rotatedOutput).toList(),
                        new HashMap<>(dirMap));
                return result instanceof ItemStack is ? is : null;
            } catch (Throwable t) {
                return null;
            }
        }
    }
    
    /**
     * 批量替换样板中的输入/输出。左侧 ghost 为空时不执行；右侧 ghost 为空表示删除匹配项。
     */
    public void replaceInPatterns(@Nullable AEKey replaceWhat, @Nullable AEKey replaceWith) {
        if (replaceWhat == null) {
            return;
        }
        
        for (var slot : getPatternCacheSlots()) {
            var stack = slot.getItem();
            var detail = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (detail == null) {
                continue;
            }

            var advPattern = AdvAeBridge.applyReplace(stack, getPlayer().level(), replaceWhat, replaceWith);
            if (advPattern != null && !advPattern.isEmpty()) {
                slot.set(advPattern);
                continue;
            }

            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                var newPattern = replaceProcessingPattern(process, replaceWhat, replaceWith);
                if (!newPattern.isEmpty()) {
                    slot.set(newPattern);
                }
            } else if (detail instanceof appeng.crafting.pattern.AECraftingPattern craft) {
                var newPattern = replaceCraftingPattern(craft, replaceWhat, replaceWith);
                if (!newPattern.isEmpty()) {
                    slot.set(newPattern);
                }
            }
        }
        broadcastChanges();
    }

    public void changePatternMode(int mode, boolean value) {
        for (var slot : getPatternCacheSlots()) {
            var stack = slot.getItem();
            var detail = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (detail instanceof appeng.crafting.pattern.AECraftingPattern craft) {
                var input = craft.getSparseInputs().toArray(new GenericStack[0]);
                var output = craft.getPrimaryOutput();
                try {
                    var newPattern = PatternDetailsHelper.encodeCraftingPattern(
                            getCraftingRecipe(craft),
                            itemize(java.util.Arrays.stream(input).toList()),
                            itemize(output),
                            mode == 0 ? value : craft.canSubstitute(),
                            mode == 1 ? value : craft.canSubstituteFluids());
                    slot.set(newPattern);
                } catch (Exception ignored) {
                }
            }
        }
        broadcastChanges();
    }

    private void restoreProcessingPatternRatio() {
        for (var slot : getPatternCacheSlots()) {
            var stack = slot.getItem();
            var detail = PatternDetailsHelper.decodePattern(stack, getPlayer().level());

            var advPattern = AdvAeBridge.applyRestoreRatio(stack, getPlayer().level());
            if (advPattern != null && !advPattern.isEmpty()) {
                slot.set(advPattern);
                continue;
            }

            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                long gcd = computeSharedGcd(process.getSparseInputs(), process.getSparseOutputs());
                if (gcd <= 1L) {
                    continue;
                }
                slot.set(PatternDetailsHelper.encodeProcessingPattern(
                        divideStacks(process.getSparseInputs(), gcd),
                        divideStacks(process.getSparseOutputs(), gcd)));
            }
        }
        broadcastChanges();
    }

    private static long computeSharedGcd(List<GenericStack> inputs, List<GenericStack> outputs) {
        long gcd = 0L;
        gcd = updateGcd(gcd, inputs);
        gcd = updateGcd(gcd, outputs);
        return gcd;
    }

    private static long updateGcd(long currentGcd, List<GenericStack> stacks) {
        long gcd = currentGcd;
        for (var stack : stacks) {
            if (stack == null || stack.amount() <= 0L) {
                continue;
            }
            gcd = gcd == 0L ? stack.amount() : gcd(gcd, stack.amount());
            if (gcd == 1L) {
                return 1L;
            }
        }
        return gcd;
    }

    private static long gcd(long a, long b) {
        long left = Math.abs(a);
        long right = Math.abs(b);
        while (right != 0L) {
            long temp = left % right;
            left = right;
            right = temp;
        }
        return left;
    }

    private static List<GenericStack> divideStacks(List<GenericStack> stacks, long divisor) {
        return stacks.stream()
                .map(stack -> stack == null ? null : new GenericStack(stack.what(), stack.amount() / divisor))
                .toList();
    }

    private void rotateProcessingPatternOutputs() {
        for (var slot : getPatternCacheSlots()) {
            var stack = slot.getItem();
            var detail = PatternDetailsHelper.decodePattern(stack, getPlayer().level());

            var advPattern = AdvAeBridge.applyRotateOutputs(stack, getPlayer().level());
            if (advPattern != null && !advPattern.isEmpty()) {
                slot.set(advPattern);
                continue;
            }

            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                slot.set(PatternDetailsHelper.encodeProcessingPattern(
                        process.getSparseInputs(),
                        java.util.Arrays.stream(rotateOutputs(process.getSparseOutputs().toArray(new GenericStack[0]))).toList()));
            }
        }
        broadcastChanges();
    }

    private static GenericStack[] rotateOutputs(GenericStack[] outputs) {
        int nonEmptyCount = 0;
        for (var output : outputs) {
            if (output != null) {
                nonEmptyCount++;
            }
        }
        if (nonEmptyCount < 2) {
            return outputs;
        }

        var rotated = java.util.Arrays.copyOf(outputs, outputs.length);
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] == null) {
                continue;
            }
            for (int j = 1; j < outputs.length; j++) {
                var next = outputs[(i + j) % outputs.length];
                if (next != null) {
                    rotated[i] = new GenericStack(next.what(), next.amount());
                    break;
                }
            }
        }
        return rotated;
    }

    private java.util.List<Slot> getPatternCacheSlots() {
        return getSlots(WcwtSlotSemantics.WCWT_PATTERN_CACHE);
    }

    @Nullable
    private Slot getSelectedPatternCacheSlot() {
        var host = getMenuHost();
        var cacheSlots = getPatternCacheSlots();
        if (host == null) {
            return null;
        }
        int selectedIndex = host.getSelectedPatternIndex();
        if (selectedIndex < 0 || selectedIndex >= cacheSlots.size()) {
            return null;
        }
        return cacheSlots.get(selectedIndex);
    }

    @Nullable
    private Slot findFirstEmptyResonatingStorageSlot() {
        for (var slot : getSlots(WcwtSlotSemantics.WCWT_RESONATING_STORAGE)) {
            if (!slot.hasItem()) {
                return slot;
            }
        }
        return null;
    }

    private void notifyPlayer(String translationKey, Object... args) {
        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(translationKey, args));
        }
    }

    private ItemStack replaceProcessingPattern(appeng.crafting.pattern.AEProcessingPattern process,
                                               AEKey replaceWhat, @Nullable AEKey replaceWith) {
        var replacedInput = replaceInStacks(process.getSparseInputs(), replaceWhat, replaceWith);
        var replacedOutput = replaceInStacks(process.getSparseOutputs(), replaceWhat, replaceWith);
        try {
            return PatternDetailsHelper.encodeProcessingPattern(replacedInput, replacedOutput);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack replaceCraftingPattern(appeng.crafting.pattern.AECraftingPattern craft,
                                             AEKey replaceWhat, @Nullable AEKey replaceWith) {
        if (!(replaceWhat instanceof AEItemKey) || (replaceWith != null && !(replaceWith instanceof AEItemKey))) {
            return ItemStack.EMPTY;
        }
        var recipe = getCraftingRecipe(craft);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        var replacedInput = itemize(replaceInStacks(craft.getSparseInputs(), replaceWhat, replaceWith));
        var output = craft.getPrimaryOutput();
        if (output == null || !(output.what() instanceof AEItemKey outputKey)) {
            return ItemStack.EMPTY;
        }

        try {
            var newPattern = PatternDetailsHelper.encodeCraftingPattern(
                    recipe,
                    replacedInput,
                    outputKey.toStack((int) output.amount()),
                    craft.canSubstitute(),
                    craft.canSubstituteFluids());
            new appeng.crafting.pattern.AECraftingPattern(AEItemKey.of(newPattern), getPlayer().level());
            return newPattern;
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }
    
    // ─── 元件工作台操作 ─────────────────────────────────────────────

    /** 获取当前放入元件槽的 ItemStack（可能为 AIR）。 */
    private ItemStack getStorageCellItem() {
        if (menuHost != null) {
            return menuHost.getStorageCellItem();
        }
        var slots = getSlots(WcwtSlotSemantics.WCWT_STORAGE_CELL);
        return slots.isEmpty() ? ItemStack.EMPTY : slots.get(0).getItem();
    }

    /**
     * 分区存储：把当前 Cell 里已存储的物品种类填入 Cell 的 filter config（类比 AE2 Cell Workbench 的 partition 按钮）。
     */
    public void partitionCell() {
        var cellStack = getStorageCellItem();
        if (cellStack.isEmpty() || !(cellStack.getItem() instanceof ICellWorkbenchItem cwi)) return;

        var configInv = menuHost != null ? menuHost.getConfig() : cwi.getConfigInventory(cellStack);
        if (configInv == null) return;

        var cellInv = StorageCells.getCellInventory(cellStack, null);
        if (cellInv == null) {
            configInv.clear();
        } else {
            int slot = 0;
            for (var entry : cellInv.getAvailableStacks()) {
                if (slot >= configInv.size()) break;
                configInv.setStack(slot++, new GenericStack(entry.getKey(), 0));
            }
            // 清除剩余 config 槽
            while (slot < configInv.size()) {
                configInv.setStack(slot++, null);
            }
        }

        // 把更改写回元件 Item（ICellWorkbenchItem.getConfigInventory 通常直接操作 DataComponent）
        if (menuHost != null) {
            menuHost.persistStorageCellItem();
        }
        broadcastChanges();
    }

    /**
     * 清除元件 filter config。
     */
    public void clearCellConfig() {
        var configInv = menuHost != null ? menuHost.getConfig() : null;
        if (configInv != null) {
            configInv.clear();
        }
        if (menuHost != null) {
            menuHost.persistStorageCellItem();
        }
        broadcastChanges();
    }

    /**
     * 设置（或清除）当前 Cell 的 config 槽中第 {@code idx} 格的过滤模板。
     * <ul>
     *   <li>{@code key == null} → 清除该格</li>
     *   <li>否则 → 写入 {@code new GenericStack(key, 0)}（数量 0 表示纯过滤模板）</li>
     * </ul>
     */
    public void setCellConfigSlot(int idx, @Nullable AEKey key) {
        var actualConfig = menuHost != null ? menuHost.getActualCellConfig() : null;
        if (actualConfig == null || idx < 0 || idx >= actualConfig.size()) return;
        if (key == null) {
            actualConfig.setStack(idx, null);
        } else if (actualConfig.isAllowedIn(idx, key)) {
            actualConfig.setStack(idx, new GenericStack(key, 0));
        } else {
            return;
        }
        if (menuHost != null) {
            menuHost.syncCellConfigMirrorFromActual();
            menuHost.persistStorageCellItem();
        }
        broadcastChanges();
    }

    /**
     * 切换复制模式：CLEAR_ON_REMOVE ↔ KEEP_ON_REMOVE，并通过 GuiSync 同步给客户端。
     */
    public void toggleCellCopyMode() {
        cellCopyMode = (cellCopyMode == CopyMode.CLEAR_ON_REMOVE)
                ? CopyMode.KEEP_ON_REMOVE
                : CopyMode.CLEAR_ON_REMOVE;
        if (menuHost != null) {
            menuHost.setCellCopyMode(cellCopyMode);
        }
        broadcastChanges();
    }

    private static List<GenericStack> replaceInStacks(List<GenericStack> source,
                                                      AEKey replaceWhat,
                                                      @Nullable AEKey replaceWith) {
        return source.stream()
                .map(stack -> replaceStack(stack, replaceWhat, replaceWith))
                .toList();
    }

    @Nullable
    private static GenericStack replaceStack(@Nullable GenericStack source,
                                             AEKey replaceWhat,
                                             @Nullable AEKey replaceWith) {
        if (source == null) {
            return null;
        }
        if (!source.what().equals(replaceWhat)) {
            return new GenericStack(source.what(), source.amount());
        }
        return replaceWith == null ? null : new GenericStack(replaceWith, source.amount());
    }

    private static ItemStack[] itemize(List<GenericStack> stacks) {
        var items = new ItemStack[stacks.size()];
        for (int i = 0; i < stacks.size(); i++) {
            items[i] = itemize(stacks.get(i));
        }
        return items;
    }

    private static ItemStack itemize(@Nullable GenericStack stack) {
        if (stack != null && stack.what() instanceof AEItemKey itemKey) {
            return itemKey.toStack((int) stack.amount());
        }
        return ItemStack.EMPTY;
    }

    private static class EncodedPatternFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && PatternDetailsHelper.isEncodedPattern(stack);
        }
    }

    private static final class ExtendedAePlusUploadBridge {
        private static final String UTIL_CLASS = "com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil";

        static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern,
                                             appeng.api.networking.IGrid grid) {
            try {
                var method = Class.forName(UTIL_CLASS).getMethod("uploadPatternToMatrix",
                        ServerPlayer.class, ItemStack.class, appeng.api.networking.IGrid.class);
                Object value = method.invoke(null, player, pattern, grid);
                return value instanceof Boolean uploaded && uploaded;
            } catch (Throwable ignored) {
                return false;
            }
        }

        static boolean matrixContainsPattern(appeng.api.networking.IGrid grid, ItemStack pattern) {
            try {
                var method = Class.forName(UTIL_CLASS).getDeclaredMethod("matrixContainsPattern",
                        appeng.api.networking.IGrid.class, ItemStack.class);
                method.setAccessible(true);
                Object value = method.invoke(null, grid, pattern);
                return value instanceof Boolean contains && contains;
            } catch (Throwable ignored) {
                return false;
            }
        }

        @Nullable
        static String getProviderDisplayName(PatternContainer provider) {
            try {
                var method = Class.forName(UTIL_CLASS).getMethod("getProviderDisplayName", PatternContainer.class);
                Object value = method.invoke(null, provider);
                return value instanceof String text ? text : null;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static final class NeoEcoUploadBridge {
        private static final String STORAGE_SERVICE_CLASS = "cn.dancingsnow.neoecoae.api.IECOPatternStorageService";
        private static final String STORAGE_CLASS = "cn.dancingsnow.neoecoae.api.IECOPatternStorage";
        private static final String PATTERN_BUS_CLASS =
                "cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity";

        static boolean uploadPatternToEcoStorage(appeng.api.networking.IGrid grid, ItemStack pattern) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends appeng.api.networking.IGridService> serviceClass =
                        (Class<? extends appeng.api.networking.IGridService>) Class.forName(STORAGE_SERVICE_CLASS);
                Object storageService = grid.getService(serviceClass);
                if (storageService == null) {
                    return false;
                }
                Object storage = storageService.getClass().getMethod("getPatternStorage").invoke(storageService);
                if (storage == null) {
                    return false;
                }
                Class<?> storageClass = Class.forName(STORAGE_CLASS);
                Object value = storageClass.getMethod("insertPattern", ItemStack.class).invoke(storage, pattern);
                return value instanceof Boolean uploaded && uploaded;
            } catch (Throwable ignored) {
                return false;
            }
        }

        static boolean isEcoPatternProvider(PatternContainer provider) {
            try {
                return Class.forName(PATTERN_BUS_CLASS).isInstance(provider);
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    private static final class CrystalScienceBridge {
        private static final String DETAILS_CLASS =
                "io.github.lounode.ae2cs.common.me.crafting.ResonatingPatternDetails";
        private static volatile Method encodeMethod;
        private static volatile boolean initAttempted;

        static ItemStack encodeResonating(ItemStack sourcePattern) {
            if (!init()) {
                return ItemStack.EMPTY;
            }
            try {
                Object result = encodeMethod.invoke(null, sourcePattern);
                return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        private static synchronized boolean init() {
            if (initAttempted) {
                return encodeMethod != null;
            }
            initAttempted = true;
            try {
                encodeMethod = Class.forName(DETAILS_CLASS).getMethod("encode", ItemStack.class);
            } catch (Throwable ignored) {
                encodeMethod = null;
            }
            return encodeMethod != null;
        }
    }

    private static final class LightningTechBridge {
        private static final String RESOLVER_CLASS =
                "com.moakiee.ae2lt.overload.pattern.Ae2PlainPatternResolver";
        private static final String SERVICE_CLASS =
                "com.moakiee.ae2lt.overload.pattern.PatternConversionService";
        private static final String ENCODED_PATTERN_CLASS =
                "com.moakiee.ae2lt.overload.model.EncodedOverloadPattern";
        private static final String MATCH_MODE_CLASS =
                "com.moakiee.ae2lt.overload.model.MatchMode";
        private static final String ITEMS_CLASS =
                "com.moakiee.ae2lt.registry.ModItems";
        private static final String OVERLOAD_ITEM_CLASS =
                "com.moakiee.ae2lt.item.OverloadPatternItem";

        static ItemStack convertToOverload(ItemStack sourcePattern,
                                           net.minecraft.world.level.Level level,
                                           net.minecraft.core.HolderLookup.Provider registries,
                                           int[] inputIdOnlySlots,
                                           int[] outputIdOnlySlots) {
            try {
                Object resolver = Class.forName(RESOLVER_CLASS)
                        .getConstructor(net.minecraft.world.level.Level.class)
                        .newInstance(level);

                ItemStack plainSource = resolvePlainSourceStack(sourcePattern, registries);
                if (plainSource.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                Object parsed = resolver.getClass().getMethod("resolve", ItemStack.class).invoke(resolver, plainSource);
                var details = PatternDetailsHelper.decodePattern(plainSource, level);
                if (details == null || details instanceof appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern) {
                    return ItemStack.EMPTY;
                }

                Object service = Class.forName(SERVICE_CLASS).getConstructor().newInstance();
                Class<?> matchModeClass = Class.forName(MATCH_MODE_CLASS);
                Object strictMode = Enum.valueOf((Class<Enum>) matchModeClass.asSubclass(Enum.class), "STRICT");
                Object idOnlyMode = Enum.valueOf((Class<Enum>) matchModeClass.asSubclass(Enum.class), "ID_ONLY");
                Object builder = Class.forName(ENCODED_PATTERN_CLASS).getMethod("builder").invoke(null);

                Method inputMethod = builder.getClass().getMethod("input", int.class, matchModeClass);
                Method outputMethod = builder.getClass().getMethod("output", int.class, matchModeClass);

                Object existingPayload = readExistingPayload(sourcePattern);
                Object existingEncodedPattern = existingPayload == null
                        ? null
                        : existingPayload.getClass().getMethod("encodedPattern").invoke(existingPayload);

                var inputs = details.getInputs();
                for (int slot = 0; slot < inputs.length; slot++) {
                    boolean present = false;
                    for (var possible : inputs[slot].getPossibleInputs()) {
                        if (possible != null && possible.what() instanceof AEItemKey) {
                            present = true;
                            break;
                        }
                    }
                    if (present) {
                        Object mode = contains(inputIdOnlySlots, slot)
                                ? idOnlyMode
                                : existingEncodedPattern != null
                                ? existingEncodedPattern.getClass()
                                        .getMethod("inputModeOrDefault", int.class)
                                        .invoke(existingEncodedPattern, slot)
                                : strictMode;
                        inputMethod.invoke(builder, slot, mode);
                    }
                }

                var outputs = details.getOutputs();
                for (int slot = 0; slot < outputs.size(); slot++) {
                    if (outputs.get(slot) != null && outputs.get(slot).what() instanceof AEItemKey) {
                        Object mode = contains(outputIdOnlySlots, slot)
                                ? idOnlyMode
                                : existingEncodedPattern != null
                                ? existingEncodedPattern.getClass()
                                        .getMethod("outputModeOrDefault", int.class)
                                        .invoke(existingEncodedPattern, slot)
                                : strictMode;
                        outputMethod.invoke(builder, slot, mode);
                    }
                }

                Object encodedPattern = builder.getClass().getMethod("build").invoke(builder);
                Object overloadItemHolder = Class.forName(ITEMS_CLASS).getField("OVERLOAD_PATTERN").get(null);
                Object overloadItem = overloadItemHolder.getClass().getMethod("get").invoke(overloadItemHolder);
                Object stack = service.getClass().getMethod("createOverloadPatternStack",
                                overloadItem.getClass(),
                                parsed.getClass(),
                                encodedPattern.getClass())
                        .invoke(service, overloadItem, parsed, encodedPattern);
                return stack instanceof ItemStack result ? result : ItemStack.EMPTY;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        static boolean isOverloadPattern(ItemStack stack) {
            try {
                return Class.forName(OVERLOAD_ITEM_CLASS).isInstance(stack.getItem());
            } catch (Throwable ignored) {
                return false;
            }
        }

        @Nullable
        private static ItemStack resolvePlainSourceStack(ItemStack sourcePattern,
                                                         net.minecraft.core.HolderLookup.Provider registries) {
            if (!isOverloadPattern(sourcePattern)) {
                return sourcePattern;
            }
            try {
                Object payload = readExistingPayload(sourcePattern);
                if (payload == null) {
                    return ItemStack.EMPTY;
                }
                Object sourceSnapshot = payload.getClass().getMethod("sourcePattern").invoke(payload);
                Object plain = sourceSnapshot.getClass()
                        .getMethod("toItemStack", net.minecraft.core.HolderLookup.Provider.class)
                        .invoke(sourceSnapshot, registries);
                return plain instanceof ItemStack stack ? stack : ItemStack.EMPTY;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        @Nullable
        private static Object readExistingPayload(ItemStack sourcePattern) {
            if (!isOverloadPattern(sourcePattern)) {
                return null;
            }
            try {
                Object overloadItem = sourcePattern.getItem();
                Object optional = overloadItem.getClass().getMethod("readPayload", ItemStack.class)
                        .invoke(overloadItem, sourcePattern);
                return optional.getClass().getMethod("orElse", Object.class).invoke(optional, new Object[]{null});
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static boolean contains(int[] values, int slot) {
            if (values == null) {
                return false;
            }
            for (int value : values) {
                if (value == slot) {
                    return true;
                }
            }
            return false;
        }
    }

    private static volatile Field craftingRecipeHolderField;

    @SuppressWarnings("unchecked")
    @Nullable
    private static RecipeHolder<CraftingRecipe> getCraftingRecipe(appeng.crafting.pattern.AECraftingPattern craft) {
        try {
            var field = craftingRecipeHolderField;
            if (field == null) {
                field = appeng.crafting.pattern.AECraftingPattern.class.getDeclaredField("recipeHolder");
                field.setAccessible(true);
                craftingRecipeHolderField = field;
            }
            Object value = field.get(craft);
            return value instanceof RecipeHolder<?> holder
                    ? (RecipeHolder<CraftingRecipe>) holder
                    : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
