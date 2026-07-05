package com.lhy.wcwt.menu;

import appeng.api.config.CopyMode;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.core.definitions.AEItems;
import appeng.helpers.InventoryAction;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.items.storage.ViewCellItem;
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
import appeng.util.CraftingRecipeUtil;
import appeng.util.Platform;
import appeng.util.inv.CarriedItemInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.prioritylist.IPartitionList;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.gui.widgets.PatternMultiplierButton;
import com.lhy.wcwt.compat.CosmeticArmorReworkedBridge;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.compat.ExtendedAePlusPatternMetadata;
import com.lhy.wcwt.compat.JecSearchCompat;
import com.lhy.wcwt.compat.WcwtMegaCellsCompat;
import com.lhy.wcwt.compat.WcwtPolymorphCompat;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.network.EncodePatternPacket;
import com.lhy.wcwt.network.PatternEncodingModePacket;
import com.lhy.wcwt.network.PatternEncodingOptionPacket;
import com.lhy.wcwt.network.PatternModePacket;
import com.lhy.wcwt.network.PatternProviderFocusPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.PatternProviderSlotSyncPacket;
import com.lhy.wcwt.network.TopActionPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket.RequestedIngredient;
import com.lhy.wcwt.pull.WcwtIngredientPriorities;
import com.lhy.wcwt.pull.WcwtMeIngredientExtraction;
import com.lhy.wcwt.pull.WcwtStackMatching;
import com.lhy.wcwt.util.PatternUploadMetadata;
import com.lhy.wcwt.util.PatternProviderSorts;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WirelessComprehensiveWorkTerminalMenu extends CraftingTermMenu implements IOptionalSlotHost {
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");
    private static final boolean DEBUG_BLANK_PATTERN_SYNC =
            Boolean.getBoolean("wcwt.debug.blankPatternSync");
    private static final boolean DEBUG_QUICKMOVE_UPGRADE =
            Boolean.getBoolean("wcwt.debug.quickMoveUpgrade");
    private static final long PERF_LOG_THRESHOLD_NS = 1_000_000L;
    private static final int PATTERN_PROVIDER_SYNC_INTERVAL_TICKS = 20;
    private static final long PATTERN_PROVIDER_SYNC_SUBSCRIPTION_TICKS = 100L;
    private static final long INVENTORY_SYNC_INTERVAL_TICKS =
            Math.max(1L, Long.getLong("wcwt.inventorySyncIntervalTicks", 1L));
    private static final boolean DEBUG_PERF_SKIPPED_INVENTORY_SYNC =
            Boolean.getBoolean("wcwt.debug.perfSkippedInventorySync");
    public static final int PATTERN_PROVIDER_VISIBLE_SLOTS = 36;

    public static final String TYPE_ID = "wireless_comprehensive_work_terminal";
    public static final String TOP_ACTION = "topAction";
    private static final String ACTION_CLEAR_MANUAL_TO_PLAYER = "clearManualToPlayer";
    private static final boolean DEBUG_ENCODE = Boolean.getBoolean("wcwt.debug.encode");
    private static final boolean DEBUG_ADVANCED = Boolean.getBoolean("wcwt.debug.advanced");
    private static final boolean DEBUG_PATTERN_UPLOAD = Boolean.getBoolean("wcwt.debug.patternUpload");
    private static final String DEFAULT_CRAFTING_PROVIDER_SEARCH_KEY = "crafting";

    /** 元件可装升级卡的最大格数（与 AE2 CellWorkbenchMenu 保持一致：8）。*/
    public static final int CELL_UPGRADE_SLOTS = 8;
    private static final int TOOLKIT_MEMORY_ITEM_MATCH_LIMIT = 256;
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

    public interface WcwtActivatableSlot {
        void setWcwtActive(boolean active);
    }

    public enum ManualWorkspaceMode {
        CRAFTING,
        SMITHING,
        ANVIL;

        public static ManualWorkspaceMode fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= values().length) {
                return CRAFTING;
            }
            return values()[ordinal];
        }
    }

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
    private AppEngSlot manualSmithingTemplateSlot;
    private AppEngSlot manualSmithingBaseSlot;
    private AppEngSlot manualSmithingAdditionSlot;
    private Slot manualSmithingResultSlot;
    private AppEngSlot manualAnvilLeftSlot;
    private AppEngSlot manualAnvilRightSlot;
    private Slot manualAnvilResultSlot;
    private PatternTermSlot patternPreviewSlot;
    private RestrictedInputSlot blankPatternSlot;
    private RestrictedInputSlot encodedPatternSlot;
    private final PatternProviderSlot[] patternProviderSlots =
            new PatternProviderSlot[PATTERN_PROVIDER_VISIBLE_SLOTS];
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
    private long lastInventorySyncTick = Long.MIN_VALUE;
    private final List<List<ItemStack>> manualCraftingSlotAlternatives = createEmptyManualCraftingAlternatives();
    private boolean manualCraftingItemSubstitution;
    private boolean manualCraftingFluidSubstitution;
    /**
     * {@link #listUploadProviders(boolean)} 的「同一服务端 tick 内」缓存。
     * 原因：vanilla {@code AbstractContainerMenu.broadcastChanges()} 每 tick 会遍历所有槽位调用
     * {@code slot.getItem()}，而 {@link PatternProviderSlot#getItem()} 会走 {@link #listUploadProviders(boolean)}
     * 做一次全网机器扫描 + 排序。36 个供应器槽 × 每 tick = 36 次全网扫描，大型网络下是 20~80ms 的主线程开销。
     * 同一 tick 内网络拓扑不会变，缓存到当前 gameTime 即可把 36 次压成 1 次。
     * 只缓存 {@code requireAvailableSlots=false} 这一热点变体；元件 ItemStack 仍由 getItem() 实时读取，不受影响。
     */
    private long cachedUploadProvidersTick = Long.MIN_VALUE;
    @Nullable
    private List<PatternContainer> cachedUploadProviders;
    /**
     * 上一次推送给客户端的样板供应器内容签名。
     * 订阅期间原本每 20 tick 雷打不动全量推一次，无论内容是否变化——这会在网络线程上重复序列化
     * 上千个带 NBT 的样板 ItemStack（profiler 中 MapCodec.encode 占 ~20%）。
     * 改为：先算一个轻量签名（遍历供应器槽做哈希，不拷贝/不序列化），与上次相同就整包跳过，
     * 内容变化时才构建并即时推送。客户端可见数据与刷新及时性完全不变，纯去掉重复推送的浪费。
     * {@link Long#MIN_VALUE} 表示「本会话尚未推送过」，用于保证订阅后至少推一次。
     */
    private long lastSyncedProviderSignature = Long.MIN_VALUE;
    private boolean initializingMenu = true;
    private final ManualSmithingMenuBridge manualSmithingBridge;
    private final ManualAnvilMenuBridge manualAnvilBridge;
    private int manualAnvilCost;
    private String manualAnvilName = "";
    private int syncedManualWorkspaceMode = ManualWorkspaceMode.CRAFTING.ordinal();
    private int syncedManualAnvilCost;

    public WirelessComprehensiveWorkTerminalMenu(int id, Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) {
        super(com.lhy.wcwt.init.ModMenus.WCWT_MENU.get(), id, ip, host, false);
        this.menuHost = host;
        this.patternEncodingLogic = host.getLogic();
        this.manualSmithingBridge = new ManualSmithingMenuBridge();
        this.manualAnvilBridge = new ManualAnvilMenuBridge();
        this.manualCraftingItemSubstitution = host.isManualCraftingItemSubstitution();
        this.manualCraftingFluidSubstitution = host.isManualCraftingFluidSubstitution();
        // 注意：不要在这里 new ToolboxMenu(this)！
        // 父类 MEStorageMenu 的构造器已经 new ToolboxMenu(this) 并添加了 9 个 TOOLBOX 槽，
        // 若在此重复创建则共有 18 个槽，界面会显示"两重"效果。

        addPlayerEquipmentSlots(ip);
        addCosmeticArmorSlots(ip);
        
        addCuriosSlots(ip);
        addManualWorkspaceSlots();

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
        for (int i = 0; i < patternProviderSlots.length; i++) {
            var slot = new PatternProviderSlot(i);
            patternProviderSlots[i] = slot;
            addSlot(slot, WcwtSlotSemantics.WCWT_PATTERN_PROVIDER);
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
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost != null ? menuHost.getManualWorkspaceMode() : syncedManualWorkspaceMode;
            }

            @Override
            public void set(int value) {
                syncedManualWorkspaceMode = value;
                if (isClientSide() && menuHost != null) {
                    menuHost.setManualWorkspaceMode(value);
                }
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return manualCraftingItemSubstitution ? 1 : 0;
            }

            @Override
            public void set(int value) {
                manualCraftingItemSubstitution = value != 0;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return manualCraftingFluidSubstitution ? 1 : 0;
            }

            @Override
            public void set(int value) {
                manualCraftingFluidSubstitution = value != 0;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return manualAnvilCost;
            }

            @Override
            public void set(int value) {
                syncedManualAnvilCost = value;
            }
        });
        registerClientAction(TOP_ACTION, TopActionPacket.Action.class, this::handleTopAction);
        registerClientAction(ACTION_CLEAR_MANUAL_TO_PLAYER, this::clearManualWorkspaceToPlayerInventory);

        // 创建玩家物品栏槽位
        this.createPlayerInventorySlots(ip);
        if (menuHost != null) {
            manualAnvilName = menuHost.getManualAnvilName();
            syncedManualWorkspaceMode = menuHost.getManualWorkspaceMode();
        }
        applyManualWorkspaceSlotActivation(ManualWorkspaceMode.fromOrdinal(syncedManualWorkspaceMode));
        updateManualWorkspaceResults();
        initializingMenu = false;
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
        addSlot(new OffhandSlot(inventory), WcwtSlotSemantics.AE2WTLIB_OFFHAND);
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
        }
    }

    private void addManualWorkspaceSlots() {
        if (menuHost == null) {
            return;
        }

        var smithingInv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
        if (smithingInv != null) {
            manualSmithingTemplateSlot = new ManualWorkspaceAppEngSlot(smithingInv, 0,
                    stack -> manualSmithingBridge.mayPlaceInInputSlot(0, stack));
            manualSmithingTemplateSlot.setActive(false);
            addSlot(manualSmithingTemplateSlot, WcwtSlotSemantics.WCWT_MANUAL_SMITHING_TEMPLATE);

            manualSmithingBaseSlot = new ManualWorkspaceAppEngSlot(smithingInv, 1,
                    stack -> manualSmithingBridge.mayPlaceInInputSlot(1, stack));
            manualSmithingBaseSlot.setActive(false);
            addSlot(manualSmithingBaseSlot, WcwtSlotSemantics.WCWT_MANUAL_SMITHING_BASE);

            manualSmithingAdditionSlot = new ManualWorkspaceAppEngSlot(smithingInv, 2,
                    stack -> manualSmithingBridge.mayPlaceInInputSlot(2, stack));
            manualSmithingAdditionSlot.setActive(false);
            addSlot(manualSmithingAdditionSlot, WcwtSlotSemantics.WCWT_MANUAL_SMITHING_ADDITION);
        }

        manualSmithingResultSlot = new ManualSmithingResultSlot();
        addSlot(manualSmithingResultSlot, WcwtSlotSemantics.WCWT_MANUAL_SMITHING_RESULT);

        var anvilInv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
        if (anvilInv != null) {
            manualAnvilLeftSlot = new ManualWorkspaceAppEngSlot(anvilInv, 0, stack -> true);
            manualAnvilLeftSlot.setActive(false);
            addSlot(manualAnvilLeftSlot, WcwtSlotSemantics.WCWT_MANUAL_ANVIL_LEFT);

            manualAnvilRightSlot = new ManualWorkspaceAppEngSlot(anvilInv, 1, stack -> true);
            manualAnvilRightSlot.setActive(false);
            addSlot(manualAnvilRightSlot, WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RIGHT);
        }

        manualAnvilResultSlot = new ManualAnvilResultSlot();
        addSlot(manualAnvilResultSlot, WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RESULT);
    }

    private static class PlayerArmorSlot extends Slot implements WcwtActivatableSlot {
        private final EquipmentSlot equipmentSlot;
        private final Player player;
        private boolean active = true;

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
            return 64;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(stack.getMaxStackSize(), 64);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(equipmentSlot, player);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
        }
    }

    private static class CosmeticArmorSlot extends Slot implements WcwtActivatableSlot {
        private final EquipmentSlot equipmentSlot;
        private final Player player;
        private boolean active = true;

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

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
        }
    }

    private static class OffhandSlot extends AppEngSlot implements WcwtActivatableSlot {
        private boolean active = true;

        OffhandSlot(Inventory inventory) {
            super(new WrappedPlayerEquipmentInventory(inventory), 40);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }

        @Override
        public boolean isActive() {
            return active && super.isActive();
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
            setActive(active);
        }
    }

    private static final class WrappedPlayerEquipmentInventory implements InternalInventory {
        private final Inventory playerInventory;

        private WrappedPlayerEquipmentInventory(Inventory playerInventory) {
            this.playerInventory = playerInventory;
        }

        @Override
        public int size() {
            return playerInventory.getContainerSize();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return slotIndex == 40 ? playerInventory.getItem(slotIndex) : ItemStack.EMPTY;
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (slotIndex == 40) {
                playerInventory.setItem(slotIndex, stack);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 40 ? playerInventory.getMaxStackSize() : 0;
        }
    }

    public static class WcwtCurioSlot extends SlotItemHandler implements WcwtActivatableSlot {
        private final String identifier;
        private final boolean canToggleRendering;
        private boolean renderStatus;
        private boolean active = true;

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

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
        }
    }

    public final class PatternProviderSlot extends Slot implements WcwtActivatableSlot {
        private final int visibleIndex;
        private boolean active;
        private long providerId = -1L;
        private int providerSlot = -1;
        private ItemStack clientDisplayStack = ItemStack.EMPTY;
        private boolean clientDisplayDirty;

        private PatternProviderSlot(int visibleIndex) {
            super(new SimpleContainer(1), 0, 0, 0);
            this.visibleIndex = visibleIndex;
        }

        public int visibleIndex() {
            return visibleIndex;
        }

        public long providerId() {
            return providerId;
        }

        public int providerSlot() {
            return providerSlot;
        }

        public void setClientDisplayStack(ItemStack stack) {
            ItemStack next = stack == null ? ItemStack.EMPTY : stack.copy();
            if (clientDisplayDirty && !ItemStack.matches(clientDisplayStack, next)) {
                return;
            }
            this.clientDisplayStack = next;
            this.clientDisplayDirty = false;
        }

        public void forceClientDisplayStack(ItemStack stack) {
            this.clientDisplayStack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.clientDisplayDirty = false;
        }

        public void setMapping(long providerId, int providerSlot) {
            this.providerId = providerId;
            this.providerSlot = providerSlot;
            this.active = providerId > 0 && providerSlot >= 0;
        }

        public void clearMapping() {
            this.providerId = -1L;
            this.providerSlot = -1;
            this.clientDisplayStack = ItemStack.EMPTY;
            this.clientDisplayDirty = false;
            this.active = false;
        }

        @Nullable
        private InternalInventory resolveInventory() {
            if (providerId <= 0 || providerSlot < 0 || isClientSide()) {
                return null;
            }
            var providers = listUploadProviders(false);
            int providerIndex = (int) providerId - 1;
            if (providerIndex < 0 || providerIndex >= providers.size()) {
                return null;
            }
            var inv = providers.get(providerIndex).getTerminalPatternInventory();
            return inv != null && providerSlot < inv.size() ? inv : null;
        }

        @Override
        public ItemStack getItem() {
            var inv = resolveInventory();
            if (inv == null) {
                return clientDisplayStack;
            }
            return inv.getStackInSlot(providerSlot);
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            var inv = resolveInventory();
            if (inv == null) {
                clientDisplayStack = stack == null ? ItemStack.EMPTY : stack.copy();
                clientDisplayDirty = true;
                return;
            }
            inv.setItemDirect(providerSlot, stack == null ? ItemStack.EMPTY : stack.copy());
        }

        @Override
        public ItemStack remove(int amount) {
            var inv = resolveInventory();
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            if (inv == null) {
                ItemStack extracted = clientDisplayStack.split(amount);
                if (!extracted.isEmpty()) {
                    clientDisplayDirty = true;
                }
                return extracted;
            }
            return inv.extractItem(providerSlot, amount, false);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return active
                    && !stack.isEmpty()
                    && appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(stack)
                    && (isClientSide() || resolveInventory() != null);
        }

        @Override
        public boolean mayPickup(Player player) {
            return active && !getItem().isEmpty();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
        }
    }

    private static final class ManualWorkspaceAppEngSlot extends AppEngSlot {
        private final java.util.function.Predicate<ItemStack> mayPlacePredicate;

        private ManualWorkspaceAppEngSlot(InternalInventory inventory, int invSlot,
                                          java.util.function.Predicate<ItemStack> mayPlacePredicate) {
            super(inventory, invSlot);
            this.mayPlacePredicate = mayPlacePredicate;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack) && mayPlacePredicate.test(stack);
        }
    }

    private final class ManualSmithingResultSlot extends Slot implements WcwtActivatableSlot {
        private boolean active;

        private ManualSmithingResultSlot() {
            super(manualSmithingBridge.getResultContainer(), 0, 0, 0);
            this.active = false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return active && manualSmithingBridge.mayPickupResult(player);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            manualSmithingBridge.takeResult(player, stack);
            updateManualWorkspaceResults();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
        }
    }

    private final class ManualAnvilResultSlot extends Slot implements WcwtActivatableSlot {
        private boolean active;

        private ManualAnvilResultSlot() {
            super(manualAnvilBridge.getResultContainer(), 0, 0, 0);
            this.active = false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return active && manualAnvilBridge.mayPickupResult(player);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            manualAnvilBridge.takeResult(player, stack);
            updateManualWorkspaceResults();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setWcwtActive(boolean active) {
            this.active = active;
        }
    }

    private final class ManualSmithingMenuBridge extends SmithingMenu {
        private ManualSmithingMenuBridge() {
            super(-1, WirelessComprehensiveWorkTerminalMenu.this.getPlayerInventory(), ContainerLevelAccess.NULL);
        }

        private ResultContainer getResultContainer() {
            return this.resultSlots;
        }

        private boolean mayPlaceInInputSlot(int slotIndex, ItemStack stack) {
            return slotIndex >= 0 && slotIndex < 3 && this.slots.get(slotIndex).mayPlace(stack);
        }

        private void syncFrom(InternalInventory inventory) {
            this.inputSlots.setItem(0, inventory.getStackInSlot(0).copy());
            this.inputSlots.setItem(1, inventory.getStackInSlot(1).copy());
            this.inputSlots.setItem(2, inventory.getStackInSlot(2).copy());
            this.createResult();
        }

        private void setSelectedRecipe(@Nullable RecipeHolder<SmithingRecipe> recipe) {
            setSmithingMenuSelectedRecipe(this, recipe);
        }

        private boolean mayPickupResult(Player player) {
            return this.getSlot(3).mayPickup(player);
        }

        private void takeResult(Player player, ItemStack stack) {
            var inv = menuHost == null ? null
                    : menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
            ItemStack[] before = new ItemStack[3];
            if (inv != null) {
                for (int i = 0; i < before.length; i++) {
                    before[i] = inv.getStackInSlot(i).copy();
                }
            }
            var resultSlot = this.getSlot(3);
            this.resultSlots.setItem(0, stack.copy());
            resultSlot.remove(stack.getCount());
            resultSlot.onTake(player, stack);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                    player.level().random.nextFloat() * 0.1F + 0.9F);
            if (inv != null) {
                for (int i = 0; i < 3; i++) {
                    inv.setItemDirect(i, this.inputSlots.getItem(i).copy());
                }
                restockManualWorkspaceInputsAeStyle(inv, before);
                this.syncFrom(inv);
            }
        }
    }

    private final class ManualAnvilMenuBridge extends AnvilMenu {
        ManualAnvilMenuBridge() {
            super(-1, WirelessComprehensiveWorkTerminalMenu.this.getPlayerInventory(), ContainerLevelAccess.NULL);
        }

        @Override
        protected boolean isValidBlock(net.minecraft.world.level.block.state.BlockState state) {
            return true;
        }

        @Override
        protected boolean mayPickup(Player player, boolean hasStack) {
            return super.mayPickup(player, hasStack);
        }

        @Override
        protected void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
        }

        private void syncFrom(ItemStack left, ItemStack right, String name) {
            this.inputSlots.setItem(0, left.copy());
            this.inputSlots.setItem(1, right.copy());
            this.setItemName(name);
            this.createResult();
        }

        private ResultContainer getResultContainer() {
            return this.resultSlots;
        }

        private boolean mayPickupResult(Player player) {
            return this.getSlot(2).mayPickup(player);
        }

        private void takeResult(Player player, ItemStack stack) {
            var inv = menuHost == null ? null
                    : menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
            ItemStack[] before = new ItemStack[2];
            if (inv != null) {
                for (int i = 0; i < before.length; i++) {
                    before[i] = inv.getStackInSlot(i).copy();
                }
            }
            // 直接走 AnvilMenu.onTake，确保原版经验消耗与输入扣除逻辑生效。
            this.onTake(player, stack);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F,
                    player.level().random.nextFloat() * 0.1F + 0.9F);
            if (inv != null) {
                inv.setItemDirect(0, this.inputSlots.getItem(0).copy());
                inv.setItemDirect(1, this.inputSlots.getItem(1).copy());
                restockManualWorkspaceInputsAeStyle(inv, before);
                this.syncFrom(inv.getStackInSlot(0), inv.getStackInSlot(1), manualAnvilName);
            }
            manualAnvilCost = this.getCost();
        }
    }
    
    public WirelessComprehensiveWorkTerminalMenu(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host) {
        super(menuType, id, ip, host, true);
        this.menuHost = null;
        this.patternEncodingLogic = host instanceof appeng.helpers.IPatternTerminalMenuHost patternHost
                ? patternHost.getLogic()
                : null;
        this.manualSmithingBridge = new ManualSmithingMenuBridge();
        this.manualAnvilBridge = new ManualAnvilMenuBridge();
    }
    
    public WirelessComprehensiveWorkTerminalMenuHost getMenuHost() {
        return menuHost;
    }

    private static List<List<ItemStack>> createEmptyManualCraftingAlternatives() {
        List<List<ItemStack>> result = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            result.add(List.of());
        }
        return result;
    }

    public void rememberManualCraftingAlternatives(List<RequestedIngredient> requestedIngredients) {
        if (isClientSide()) {
            return;
        }
        for (int i = 0; i < manualCraftingSlotAlternatives.size(); i++) {
            manualCraftingSlotAlternatives.set(i, List.of());
        }
        for (RequestedIngredient requested : requestedIngredients) {
            if (requested == null || requested.slotIndex() < 0 || requested.slotIndex() >= 9) {
                continue;
            }
            List<ItemStack> alternatives = WcwtIngredientPriorities.deduplicateItemAlternatives(
                    requested.alternatives().stream()
                            .filter(stack -> stack != null && !stack.isEmpty())
                            .map(stack -> stack.copyWithCount(1))
                            .toList());
            manualCraftingSlotAlternatives.set(requested.slotIndex(), alternatives);
        }
    }

    public ItemStack getToolkitMemoryStack(int toolkitIndex) {
        if (!isToolkitMemorySlot(toolkitIndex)) {
            return ItemStack.EMPTY;
        }
        var memory = toolkitMemoryInventory();
        if (memory == null || toolkitIndex < 0 || toolkitIndex >= memory.size()) {
            return ItemStack.EMPTY;
        }
        return memory.getStackInSlot(toolkitIndex);
    }

    public boolean hasToolkitMemory(int toolkitIndex) {
        return !getToolkitMemoryStack(toolkitIndex).isEmpty();
    }

    public void setToolkitMemorySlot(int toolkitIndex, boolean rememberFromSlot) {
        if (!isToolkitMemorySlot(toolkitIndex)) {
            return;
        }
        var memory = toolkitMemoryInventory();
        var toolkit = toolkitInventory();
        if (memory == null || toolkit == null || toolkitIndex < 0 || toolkitIndex >= memory.size()
                || toolkitIndex >= toolkit.size()) {
            return;
        }
        if (!rememberFromSlot) {
            memory.setItemDirect(toolkitIndex, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        ItemStack stack = toolkit.getStackInSlot(toolkitIndex);
        if (stack.isEmpty()) {
            return;
        }
        memory.setItemDirect(toolkitIndex, stack.copyWithCount(1));
        broadcastChanges();
    }

    private boolean toolkitMemoryMatches(int toolkitIndex, ItemStack stack) {
        if (!isToolkitMemorySlot(toolkitIndex)) {
            return true;
        }
        ItemStack memory = getToolkitMemoryStack(toolkitIndex);
        return memory.isEmpty() || (!stack.isEmpty() && memory.is(stack.getItem()));
    }

    private boolean isToolkitMemorySlot(int toolkitIndex) {
        return toolkitIndex >= ToolkitItemRules.DEDICATED_SLOT_COUNT;
    }

    @Nullable
    private InternalInventory toolkitInventory() {
        return menuHost == null ? null : menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT);
    }

    @Nullable
    private InternalInventory toolkitMemoryInventory() {
        return menuHost == null ? null : menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TOOLKIT_MEMORY);
    }

    public void setPatternProviderSlotSync(List<PatternProviderSlotSyncPacket.Mapping> mappings) {
        for (var slot : patternProviderSlots) {
            if (slot != null) {
                slot.clearMapping();
            }
        }
        for (var mapping : mappings) {
            int visibleSlot = mapping.visibleSlot();
            if (visibleSlot < 0 || visibleSlot >= patternProviderSlots.length) {
                continue;
            }
            patternProviderSlots[visibleSlot].setMapping(mapping.providerId(), mapping.providerSlot());
        }
    }

    public ManualWorkspaceMode getManualWorkspaceMode() {
        if (!isClientSide() && menuHost != null) {
            return ManualWorkspaceMode.fromOrdinal(menuHost.getManualWorkspaceMode());
        }
        return ManualWorkspaceMode.fromOrdinal(syncedManualWorkspaceMode);
    }

    public void setManualWorkspaceMode(ManualWorkspaceMode mode) {
        if (mode == null) {
            mode = ManualWorkspaceMode.CRAFTING;
        }
        syncedManualWorkspaceMode = mode.ordinal();
        applyManualWorkspaceSlotActivation(mode);
        if (isClientSide()) {
            if (menuHost != null) {
                menuHost.setManualWorkspaceMode(syncedManualWorkspaceMode);
            }
            updateManualWorkspaceResults();
            return;
        }
        if (menuHost != null) {
            menuHost.setManualWorkspaceMode(mode.ordinal());
        }
        updateManualWorkspaceResults();
        broadcastChanges();
    }

    public String getManualAnvilName() {
        return manualAnvilName;
    }

    public boolean setManualAnvilName(String name) {
        String normalized = normalizeManualAnvilName(name);
        if (Objects.equals(normalized, manualAnvilName)) {
            return false;
        }
        manualAnvilName = normalized;
        if (menuHost != null) {
            menuHost.setManualAnvilName(normalized);
        }
        manualAnvilBridge.setItemName(normalized);
        updateManualAnvilResult();
        broadcastChanges();
        return true;
    }

    public int getManualAnvilCost() {
        return isClientSide() ? syncedManualAnvilCost : manualAnvilCost;
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        if (DEBUG_QUICKMOVE_UPGRADE) {
            Slot targetSlot = slot >= 0 && slot < this.slots.size() ? this.getSlot(slot) : null;
            logQuickMoveUpgrade("doAction player={}, action={}, slot={}, id={}, target={}",
                    player.getScoreboardName(), action, slot, id, describeSlot(targetSlot));
        }
        if (shouldHandleManualCraftingResultAction(action, slot)) {
            handleManualCraftingResultAction(player, action);
            return;
        }
        super.doAction(player, action, slot, id);
    }

    @Override
    public boolean hasIngredient(net.minecraft.world.item.crafting.Ingredient ingredient,
                                 it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<Object> reservedAmounts) {
        if (getManualWorkspaceMode() == ManualWorkspaceMode.CRAFTING) {
            return super.hasIngredient(ingredient, reservedAmounts);
        }
        for (var slot : getCurrentManualWorkspaceInputSlots()) {
            var stackInSlot = slot.getItem();
            if (!stackInSlot.isEmpty() && ingredient.test(stackInSlot)) {
                var reservedAmount = reservedAmounts.getOrDefault(slot, 0);
                if (stackInSlot.getCount() > reservedAmount) {
                    reservedAmounts.merge(slot, 1, Integer::sum);
                    return true;
                }
            }
        }
        var clientRepo = getClientRepo();
        if (clientRepo != null && getLinkStatus().connected()) {
            for (var stack : clientRepo.getByIngredient(ingredient)) {
                var reservedAmount = reservedAmounts.getOrDefault(stack, 0);
                if (stack.getStoredAmount() - reservedAmount >= 1) {
                    reservedAmounts.merge(stack, 1, Integer::sum);
                    return true;
                }
            }
        }
        return false;
    }

    private List<Slot> getCurrentManualWorkspaceInputSlots() {
        List<Slot> slots = new ArrayList<>();
        switch (getManualWorkspaceMode()) {
            case CRAFTING -> slots.addAll(getSlots(SlotSemantics.CRAFTING_GRID));
            case SMITHING -> {
                addIfNotNull(slots, manualSmithingTemplateSlot);
                addIfNotNull(slots, manualSmithingBaseSlot);
                addIfNotNull(slots, manualSmithingAdditionSlot);
            }
            case ANVIL -> {
                addIfNotNull(slots, manualAnvilLeftSlot);
                addIfNotNull(slots, manualAnvilRightSlot);
            }
        }
        return slots;
    }

    private static void addIfNotNull(List<Slot> slots, @Nullable Slot slot) {
        if (slot != null) {
            slots.add(slot);
        }
    }

    public EncodingMode getPatternEncodingMode() {
        if (isClientSide()) {
            return EncodingMode.values()[syncedPatternEncodingMode];
        }
        return patternEncodingLogic == null ? EncodingMode.PROCESSING : patternEncodingLogic.getMode();
    }

    @Contract("null -> false")
    public boolean canModifyAmountForSlot(@Nullable Slot slot) {
        return isProcessingPatternSlot(slot) && slot.hasItem();
    }

    @Contract("null -> false")
    public boolean isProcessingPatternSlot(@Nullable Slot slot) {
        if (slot == null || getPatternEncodingMode() != EncodingMode.PROCESSING) {
            return false;
        }

        for (var processingOutputSlot : processingOutputSlots) {
            if (processingOutputSlot == slot) {
                return true;
            }
        }

        for (var processingInputSlot : processingInputSlots) {
            if (processingInputSlot == slot) {
                return true;
            }
        }

        return false;
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

    @Override
    public void slotsChanged(Container inventory) {
        updateManualWorkspaceResults();
        updatePatternPreview(getPatternEncodingMode());
        super.slotsChanged(inventory);
    }

    private void updateManualWorkspaceResults() {
        updateManualCraftingResult();
        updateManualSmithingResult();
        updateManualAnvilResult();
    }

    private boolean isManualResultSlot(Slot slot) {
        return slot == manualSmithingResultSlot || slot == manualAnvilResultSlot;
    }

    private void applyManualWorkspaceSlotActivation(ManualWorkspaceMode mode) {
        boolean crafting = mode == ManualWorkspaceMode.CRAFTING;
        boolean smithing = mode == ManualWorkspaceMode.SMITHING;
        boolean anvil = mode == ManualWorkspaceMode.ANVIL;

        setManualSlotActive(manualSmithingTemplateSlot, smithing);
        setManualSlotActive(manualSmithingBaseSlot, smithing);
        setManualSlotActive(manualSmithingAdditionSlot, smithing);
        setManualSlotActive(manualSmithingResultSlot, smithing);

        setManualSlotActive(manualAnvilLeftSlot, anvil);
        setManualSlotActive(manualAnvilRightSlot, anvil);
        setManualSlotActive(manualAnvilResultSlot, anvil);

        for (var slot : getSlots(SlotSemantics.CRAFTING_GRID)) {
            setManualSlotActive(slot, crafting);
        }
        for (var slot : getSlots(SlotSemantics.CRAFTING_RESULT)) {
            setManualSlotActive(slot, crafting);
        }
    }

    private static void setManualSlotActive(@Nullable Slot slot, boolean active) {
        if (slot == null) {
            return;
        }
        if (slot instanceof AppEngSlot appEngSlot) {
            appEngSlot.setActive(active);
        } else if (slot instanceof WcwtActivatableSlot activatableSlot) {
            activatableSlot.setWcwtActive(active);
        }
    }

    private void updateManualCraftingResult() {
        if (getManualWorkspaceMode() != ManualWorkspaceMode.CRAFTING) {
            return;
        }

        var ingredients = new ArrayList<ItemStack>(9);
        for (Slot slot : getSlots(SlotSemantics.CRAFTING_GRID)) {
            ingredients.add(slot.getItem().copy());
        }

        var input = CraftingInput.of(3, 3, ingredients);
        var level = getPlayer().level();
        RecipeHolder<CraftingRecipe> recipe =
                WcwtPolymorphCompat.getCraftingRecipe(this, input, level, getPlayer()).orElse(null);
        setCraftingTermCurrentRecipe(this, recipe);

        ItemStack result = recipe == null
                ? ItemStack.EMPTY
                : recipe.value().assemble(input, level.registryAccess());
        for (Slot slot : getSlots(SlotSemantics.CRAFTING_RESULT)) {
            slot.set(result.copy());
        }
    }

    private void updateManualSmithingResult() {
        if (menuHost == null) {
            return;
        }
        var inv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
        if (inv == null) {
            return;
        }
        manualSmithingBridge.syncFrom(inv);
        updateManualSmithingPolymorphResult(inv);
    }

    private void updateManualSmithingPolymorphResult(InternalInventory inventory) {
        if (getManualWorkspaceMode() != ManualWorkspaceMode.SMITHING) {
            return;
        }
        var input = new SmithingRecipeInput(
                inventory.getStackInSlot(0).copy(),
                inventory.getStackInSlot(1).copy(),
                inventory.getStackInSlot(2).copy());
        var level = getPlayer().level();
        RecipeHolder<SmithingRecipe> recipe =
                WcwtPolymorphCompat.getSmithingRecipe(this, input, level, getPlayer()).orElse(null);
        if (recipe == null) {
            manualSmithingBridge.setSelectedRecipe(null);
            manualSmithingBridge.getResultContainer().setItem(0, ItemStack.EMPTY);
            return;
        }

        ItemStack result = recipe.value().assemble(input, level.registryAccess());
        if (!result.isItemEnabled(level.enabledFeatures())) {
            manualSmithingBridge.setSelectedRecipe(null);
            manualSmithingBridge.getResultContainer().setItem(0, ItemStack.EMPTY);
            return;
        }
        manualSmithingBridge.setSelectedRecipe(recipe);
        manualSmithingBridge.getResultContainer().setRecipeUsed(recipe);
        manualSmithingBridge.getResultContainer().setItem(0, result);
    }

    private void updateManualAnvilResult() {
        if (menuHost == null) {
            return;
        }
        var inv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
        if (inv == null) {
            return;
        }

        var left = inv.getStackInSlot(0);
        var right = inv.getStackInSlot(1);
        if (left.isEmpty()) {
            manualAnvilBridge.syncFrom(ItemStack.EMPTY, ItemStack.EMPTY, manualAnvilName);
            manualAnvilCost = 0;
            return;
        }

        manualAnvilBridge.syncFrom(left, right, manualAnvilName);
        manualAnvilCost = manualAnvilBridge.getCost();
    }

    private static String normalizeManualAnvilName(String name) {
        if (name == null) {
            return "";
        }
        String filtered = net.minecraft.util.StringUtil.filterText(name);
        return filtered.length() <= AnvilMenu.MAX_NAME_LENGTH
                ? filtered
                : filtered.substring(0, AnvilMenu.MAX_NAME_LENGTH);
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

        if (encodedPatternSlot != null) {
            var encodedPattern = encodedPatternSlot.getItem();
            if (PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
                encodedPatternSlot.set(AEItems.BLANK_PATTERN.stack(encodedPattern.getCount()));
            }
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

    public boolean isManualCraftingItemSubstitution() {
        return manualCraftingItemSubstitution;
    }

    public void setManualCraftingItemSubstitution(boolean substitute) {
        if (manualCraftingItemSubstitution != substitute) {
            manualCraftingItemSubstitution = substitute;
            if (menuHost != null) {
                menuHost.setManualCraftingItemSubstitution(substitute);
            }
            broadcastChanges();
        }
    }

    public boolean isManualCraftingFluidSubstitution() {
        return manualCraftingFluidSubstitution;
    }

    public void setManualCraftingFluidSubstitution(boolean substitute) {
        if (manualCraftingFluidSubstitution != substitute) {
            manualCraftingFluidSubstitution = substitute;
            if (menuHost != null) {
                menuHost.setManualCraftingFluidSubstitution(substitute);
            }
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
        int skippedConnectionRefreshes = 0;
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
                // 内容签名去重：内容未变则跳过整包构建 + 序列化 + 发送，消除每秒重复推送上千样板的浪费。
                long signature = computePatternProviderSignature();
                if (signature != lastSyncedProviderSignature) {
                    PacketDistributor.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
                    lastSyncedProviderSignature = signature;
                    didProviderSync = true;
                }
                patternProviderSyncCooldown = PATTERN_PROVIDER_SYNC_INTERVAL_TICKS;
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
        boolean didInventorySync = shouldRunInventorySync();
        if (DEBUG_BLANK_PATTERN_SYNC && isServerSide()) {
            logBlankPatternSync("broadcast.preSuper",
                    "didInventorySync={}, lastInventorySyncTick={}, gameTick={}, blankSlot={}, encodedSlot={}, linkStatus={}",
                    didInventorySync,
                    lastInventorySyncTick,
                    getPlayer() != null ? getPlayer().level().getGameTime() : -1L,
                    summarizeItem(blankPatternSlot != null ? blankPatternSlot.getItem() : ItemStack.EMPTY),
                    summarizeItem(encodedPatternSlot != null ? encodedPatternSlot.getItem() : ItemStack.EMPTY),
                    getLinkStatus());
        }
        if (didInventorySync) {
            super.broadcastChanges();
        } else if (menuHost != null) {
            menuHost.consumeSkippedConnectedAccessPointUpdates();
        }
        if (DEBUG_PERF && isServerSide() && menuHost != null) {
            skippedConnectionRefreshes = menuHost.consumeSkippedConnectedAccessPointUpdates();
        }
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
                    || didProviderSync
                    || (!didInventorySync && DEBUG_PERF_SKIPPED_INVENTORY_SYNC)) {
                String playerName = getPlayer() != null ? getPlayer().getScoreboardName() : "<unknown>";
                WcwtMod.LOGGER.info(
                        "WCWT perf: broadcastChanges player={}, totalMs={}, preSuperMs={}, superMs={}, refreshMs={}, mergeMs={}, providerSyncMs={}, providerSync={}, inventorySync={}, inventorySyncInterval={}, skippedConnectionRefreshes={}, cooldownBefore={}, cooldownAfter={}, mode={}, processingMerge={}",
                        playerName,
                        formatPerfMs(totalNs),
                        formatPerfMs(preSuperNs),
                        formatPerfMs(superNs),
                        formatPerfMs(refreshNs),
                        formatPerfMs(mergeNs),
                        formatPerfMs(providerSyncNs),
                        didProviderSync,
                        didInventorySync,
                        INVENTORY_SYNC_INTERVAL_TICKS,
                        skippedConnectionRefreshes,
                        cooldownBefore,
                        patternProviderSyncCooldown,
                        getPatternEncodingMode(),
                        processingMaterialsMerge);
            }
        }
    }

    private boolean shouldRunInventorySync() {
        if (!isServerSide() || INVENTORY_SYNC_INTERVAL_TICKS <= 1L) {
            return true;
        }
        long tick = getPlayer().level().getGameTime();
        if (lastInventorySyncTick == Long.MIN_VALUE
                || tick - lastInventorySyncTick >= INVENTORY_SYNC_INTERVAL_TICKS) {
            lastInventorySyncTick = tick;
            return true;
        }
        return false;
    }

    private static String formatPerfMs(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    public void requestPatternProviderSyncSubscription() {
        requestPatternProviderSyncSubscription(true);
    }

    public void requestPatternProviderSyncSubscription(boolean subscribe) {
        if (!isServerSide() || !(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long gameTime = serverPlayer.serverLevel().getGameTime();
        if (!subscribe) {
            lastPatternProviderRequestTick = gameTime;
            return;
        }
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
        return WcwtServerConfig.patternProviderActiveRefresh()
                && serverPlayer.serverLevel().getGameTime() <= patternProviderSyncSubscriptionUntilTick;
    }

    /**
     * 计算当前样板供应器内容的轻量签名，用于推送去重。
     * 只遍历供应器与其槽位做整数哈希，不拷贝、不序列化 ItemStack——比构建整包便宜几个数量级。
     * 捕获的变化维度：供应器集合（providerId + size）、每个非空槽的（槽号 + 物品 + 数量 + 数据组件）。
     * 组件哈希覆盖样板 NBT 变化，因此样板被编辑/替换也会反映为签名变化。
     */
    private long computePatternProviderSignature() {
        var providers = listUploadProviders(false);
        long hash = 1469598103934665603L; // FNV-like 起始值
        hash = hash * 1099511628211L + providers.size();
        for (int p = 0; p < providers.size(); p++) {
            var provider = providers.get(p);
            InternalInventory inv = provider.getTerminalPatternInventory();
            if (inv == null) {
                hash = hash * 1099511628211L + 17L;
                continue;
            }
            hash = hash * 1099511628211L + inv.size();
            for (int i = 0; i < inv.size(); i++) {
                var stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                hash = hash * 1099511628211L + i;
                hash = hash * 1099511628211L + stack.getCount();
                // ItemStack.hashItemAndComponents 是 MC 为「物品+组件(含 NBT)」提供的规范哈希，
                // AE2 的 AEItemKey 也用它，比 getComponents().hashCode() 更稳定可靠。
                hash = hash * 1099511628211L + ItemStack.hashItemAndComponents(stack);
            }
        }
        return hash;
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

        ItemStack uploadStack = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);

        var grid = getMenuGrid();
        if (grid == null) {
            return MatrixUploadResult.FAILURE;
        }

        try {
            EcoUploadDuplicateResult ecoDuplicate = findEcoDuplicatePattern(grid, uploadStack);
            if (ecoDuplicate.duplicate()) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.eco_pattern_duplicate"));
                return MatrixUploadResult.uploaded(ecoDuplicate.providerId(), ecoDuplicate.slot());
            }
            if (NeoEcoUploadBridge.uploadPatternToEcoStorage(grid, uploadStack.copy())) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.eco_pattern_uploaded"));
                return findEcoUploadResult(uploadStack);
            }
            if (ExtendedAePlusUploadBridge.matrixContainsPattern(grid, uploadStack)) {
                serverPlayer.sendSystemMessage(Component.translatable("extendedae_plus.message.matrix.duplicate"));
                return returnBlankPatternFromMatrixUpload(uploadStack.getCount())
                        ? MatrixUploadResult.DUPLICATE_RETURNED
                        : MatrixUploadResult.DUPLICATE_ABORTED;
            }
            if (ExtendedAePlusUploadBridge.uploadPatternToMatrix(serverPlayer, uploadStack.copy(), grid)) {
                return findMatrixUploadResult(uploadStack);
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

    private UploadAttemptResult uploadEncodedPatternToMatchingProvider(ItemStack encodedPattern, String searchText,
                                                                       long preferredProviderId) {
        String query = normalizeProviderSearchText(resolveUploadSearchTextFromPattern(encodedPattern, searchText));
        if (query == null) {
            query = "";
        }
        if (encodedPattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return UploadAttemptResult.NO_TARGET;
        }

        var providers = listUploadProviders(false);
        PatternContainer preferredProvider = getUploadProviderByOrdinal(providers, preferredProviderId);
        logPatternUploadDebug(
                "server upload target lookup preferredProviderId={}, preferredFound={}, query={}, providers={}",
                preferredProviderId, preferredProvider != null, query, providers.size());
        if (preferredProvider != null) {
            return uploadEncodedPatternToProviderGroup(encodedPattern, providers, preferredProvider,
                    getUploadProviderDisplayName(preferredProvider), preferredProviderId);
        }

        if (query.isEmpty()) {
            return UploadAttemptResult.NO_TARGET;
        }
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
            logPatternUploadDebug("server upload search ambiguous query={}, matchingGroupNames={}, matchingTargets={}",
                    query, matchingGroupNames, matchingTargets.size());
            return UploadAttemptResult.NO_TARGET;
        }

        String targetName = matchingGroupNames.get(0);
        var candidateTargets = matchingTargets.stream()
                .filter(target -> targetName.equals(target.providerName()))
                .toList();
        return uploadEncodedPatternToTargets(encodedPattern, targetName, candidateTargets);
    }

    private UploadAttemptResult uploadEncodedPatternToProviderGroup(ItemStack encodedPattern,
                                                                    List<PatternContainer> providers,
                                                                    PatternContainer targetProvider,
                                                                    String targetName,
                                                                    long targetProviderId) {
        var candidateTargets = new ArrayList<ProviderTarget>();
        candidateTargets.add(new ProviderTarget(targetProviderId, targetProvider, targetName));
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            if (provider == targetProvider) {
                continue;
            }
            String providerName = getUploadProviderDisplayName(provider);
            if (targetName.equals(providerName)) {
                candidateTargets.add(new ProviderTarget(i + 1L, provider, providerName));
            }
        }
        logPatternUploadDebug("server upload provider group targetProviderId={}, targetName={}, candidates={}",
                targetProviderId, targetName, candidateTargets.size());
        return uploadEncodedPatternToTargets(encodedPattern, targetName, candidateTargets);
    }

    private UploadAttemptResult uploadEncodedPatternToTargets(ItemStack encodedPattern,
                                                              String targetName,
                                                              List<ProviderTarget> candidateTargets) {
        if (candidateTargets.isEmpty()) {
            return UploadAttemptResult.NO_TARGET;
        }
        ItemStack uploadStack = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);
        for (var target : candidateTargets) {
            if (insertEncodedPattern(target.provider(), uploadStack)) {
                int insertedSlot = findLastInsertedPatternSlot(target.provider(), uploadStack);
                logPatternUploadDebug("server upload inserted targetProviderId={}, targetName={}, insertedSlot={}",
                        target.providerId(), target.providerName(), insertedSlot);
                return new UploadAttemptResult(true, true, targetName, target.providerId(), insertedSlot);
            }
            logPatternUploadDebug("server upload target full/invalid targetProviderId={}, targetName={}",
                    target.providerId(), target.providerName());
        }
        return new UploadAttemptResult(false, true, targetName, candidateTargets.get(0).providerId(), -1);
    }

    private String resolveUploadSearchTextFromPattern(ItemStack encodedPattern, @Nullable String fallbackSearchText) {
        String fromPattern = normalizeProviderSearchText(PatternUploadMetadata.getProviderSearchText(encodedPattern));
        if (fromPattern != null) {
            return fromPattern;
        }
        String normalizedFallback = normalizeProviderSearchText(fallbackSearchText);
        return normalizedFallback == null ? "" : normalizedFallback;
    }

    private List<PatternContainer> listUploadProviders(boolean requireAvailableSlots) {
        // requireAvailableSlots=true 的变体依赖每槽空位实时计数，不缓存，直接重新扫描。
        if (requireAvailableSlots) {
            return scanUploadProviders(true);
        }
        // 热点路径（broadcastChanges 每 tick 经由 PatternProviderSlot.getItem() 调用 36 次）：
        // 同一服务端 tick 内复用上一次扫描结果，把全网扫描从 36 次/tick 压成 1 次/tick。
        if (isServerSide() && getPlayer() != null) {
            long tick = getPlayer().level().getGameTime();
            if (cachedUploadProviders != null && cachedUploadProvidersTick == tick) {
                return cachedUploadProviders;
            }
            List<PatternContainer> providers = scanUploadProviders(false);
            cachedUploadProviders = providers;
            cachedUploadProvidersTick = tick;
            return providers;
        }
        return scanUploadProviders(false);
    }

    private List<PatternContainer> scanUploadProviders(boolean requireAvailableSlots) {
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
            if (!stack.isEmpty() && PatternUploadMetadata.isSamePatternIgnoringUploadData(stack, encodedPattern)) {
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
            if (!stack.isEmpty() && PatternUploadMetadata.isSamePatternIgnoringUploadData(stack, encodedPattern)) {
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
        return JecSearchCompat.contains(providerName, query);
    }

    private static String getUploadProviderDisplayName(PatternContainer provider) {
        String bridgeName = ExtendedAePlusUploadBridge.getProviderDisplayName(provider);
        if (bridgeName != null && !bridgeName.isBlank()) {
            return bridgeName;
        }
        return provider.getTerminalGroup().name().getString();
    }

    @Nullable
    private static PatternContainer getUploadProviderByOrdinal(List<PatternContainer> providers, long providerId) {
        int index = (int) providerId - 1;
        if (index < 0 || index >= providers.size()) {
            return null;
        }
        return providers.get(index);
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
        encodePattern(mode, uploadEnabled, providerSearchText, -1, false);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              boolean fallbackToEditSlot) {
        encodePattern(mode, uploadEnabled, providerSearchText, -1, fallbackToEditSlot);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              long preferredProviderId, boolean fallbackToEditSlot) {
        encodePattern(mode, uploadEnabled, providerSearchText, preferredProviderId, "", fallbackToEditSlot);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              long preferredProviderId, String uploadProviderName, boolean fallbackToEditSlot) {
        if (isClientSide()) {
            logEncode("client clicked encode, mode={}", mode);
            PacketDistributor.sendToServer(new EncodePatternPacket(mode, uploadEnabled, providerSearchText,
                    preferredProviderId, uploadProviderName, fallbackToEditSlot));
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

        ResourceLocation recipeId = resolveEncodedPatternRecipeId(mode);
        String rawProviderSearchText = resolvePatternUploadSearchText(mode, providerSearchText, recipeId);
        String resolvedProviderSearchText = normalizeProviderSearchText(rawProviderSearchText);
        logPatternUploadDebug(
                "server encode resolved player={}, mode={}, uploadEnabled={}, preferredProviderId={}, uploadProviderName={}, fallbackToEditSlot={}, packetSearchText={}, recipeId={}, rawSearchText={}, resolvedSearchText={}",
                getPlayer().getScoreboardName(), mode, uploadEnabled, preferredProviderId, uploadProviderName,
                fallbackToEditSlot, providerSearchText, recipeId, rawProviderSearchText, resolvedProviderSearchText);
        ExtendedAePlusPatternMetadata.writeEncoder(encodedPattern, getPlayer().getScoreboardName());
        writePatternUploadMetadata(encodedPattern, resolvedProviderSearchText);
        logPatternUploadDebug("server encoded metadata player={}, metadata={}, encoded={}",
                getPlayer().getScoreboardName(), PatternUploadMetadata.getProviderSearchText(encodedPattern), encodedPattern);

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
                    PacketDistributor.sendToPlayer(serverPlayer,
                            PatternProviderListPacket.buildForPlayer(serverPlayer, resolvedProviderSearchText));
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
            uploadAttempt = uploadEncodedPatternToMatchingProvider(encodedPattern, resolvedProviderSearchText,
                    preferredProviderId);
            logPatternUploadDebug(
                    "server upload attempt player={}, uploaded={}, hadTarget={}, providerName={}, providerId={}, slot={}, resolvedSearchText={}, preferredProviderId={}",
                    getPlayer().getScoreboardName(), uploadAttempt.uploaded(), uploadAttempt.hadTarget(),
                    uploadAttempt.providerName(), uploadAttempt.providerId(), uploadAttempt.slot(),
                    resolvedProviderSearchText, preferredProviderId);
            if (uploadAttempt.uploaded()) {
                String statusProviderName = uploadStatusProviderName(uploadProviderName, uploadAttempt.providerName());
                consumePatternForUpload(consumeEditPattern);
                patternEncodingLogic.setMode(mode);
                syncedPatternEncodingMode = mode.ordinal();
                updatePatternPreview(mode);
                tryFillBlankPatternFromNetwork();
                if (getPlayer() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "extendedae_plus.screen.upload.auto_upload_success", statusProviderName));
                    PacketDistributor.sendToPlayer(serverPlayer,
                            PatternProviderListPacket.buildForPlayer(serverPlayer, resolvedProviderSearchText));
                    if (uploadAttempt.providerId() > 0 && uploadAttempt.slot() >= 0) {
                        PacketDistributor.sendToPlayer(serverPlayer,
                                new PatternProviderFocusPacket(uploadAttempt.providerId(), uploadAttempt.slot()));
                    }
                }
                logEncode("uploaded mode={}, search={}, provider={}, encoded={}",
                        mode, resolvedProviderSearchText, uploadAttempt.providerName(), encodedPattern);
                broadcastChanges();
                return;
            }
        }

        boolean preferEditSlot = uploadEnabled && fallbackToEditSlot;
        if (uploadEnabled && uploadAttempt.hadTarget() && getPlayer() instanceof ServerPlayer serverPlayer) {
            String statusProviderName = uploadStatusProviderName(uploadProviderName, uploadAttempt.providerName());
            serverPlayer.sendSystemMessage(Component.translatable(
                    "extendedae_plus.screen.upload.auto_upload_failed", statusProviderName));
        }

        storeEncodedPatternLocally(mode, encodedPattern, consumeEditPattern, preferEditSlot,
                uploadAttempt.hadTarget() ? "upload_failed_local_fallback" : "local_encode");
    }

    private boolean hasBlankPatternForEncoding() {
        return AEItems.BLANK_PATTERN.is(blankPatternSlot.getItem());
    }

    private static String uploadStatusProviderName(@Nullable String clientProviderName, String fallbackProviderName) {
        String normalized = normalizeProviderSearchText(clientProviderName);
        return normalized != null ? normalized : fallbackProviderName;
    }

    private void writePatternUploadMetadata(ItemStack encodedPattern,
                                            @Nullable String providerSearchText) {
        PatternUploadMetadata.write(encodedPattern, providerSearchText);
    }

    @Nullable
    private String resolvePatternUploadSearchText(EncodingMode mode,
                                                  @Nullable String providerSearchText,
                                                  @Nullable ResourceLocation recipeId) {
        String normalized = normalizeProviderSearchText(providerSearchText);
        if (normalized != null) {
            return normalized;
        }
        String fromRecipe = recipeId != null ? normalizeProviderSearchText(recipeId.toString()) : null;
        if (fromRecipe != null) {
            return fromRecipe;
        }
        if (mode != EncodingMode.PROCESSING) {
            return DEFAULT_CRAFTING_PROVIDER_SEARCH_KEY;
        }
        return null;
    }

    @Nullable
    private ResourceLocation resolveEncodedPatternRecipeId(EncodingMode mode) {
        return switch (mode) {
            case CRAFTING -> resolveCraftingRecipeId();
            case SMITHING_TABLE -> resolveSmithingRecipeId();
            case STONECUTTING -> patternEncodingLogic != null ? patternEncodingLogic.getStonecuttingRecipeId() : null;
            case PROCESSING -> null;
        };
    }

    @Nullable
    private ResourceLocation resolveCraftingRecipeId() {
        if (patternEncodingLogic == null) {
            return null;
        }
        var ingredients = new ItemStack[9];
        boolean valid = false;
        for (int slot = 0; slot < ingredients.length; slot++) {
            GenericStack stack = patternEncodingLogic.getEncodedInputInv().getStack(slot);
            if (stack == null) {
                ingredients[slot] = ItemStack.EMPTY;
                continue;
            }
            if (!(stack.what() instanceof AEItemKey itemKey)) {
                return null;
            }
            ingredients[slot] = itemKey.toStack(1);
            valid = true;
        }
        if (!valid) {
            return null;
        }
        var input = CraftingInput.of(3, 3, java.util.Arrays.asList(ingredients));
        var level = getPlayer().level();
        var recipe = WcwtPolymorphCompat.getCraftingRecipe(this, input, level, getPlayer()).orElse(null);
        return recipe != null ? recipe.id() : null;
    }

    @Nullable
    private ResourceLocation resolveSmithingRecipeId() {
        if (patternEncodingLogic == null
                || !(patternEncodingLogic.getEncodedInputInv().getKey(0) instanceof AEItemKey template)
                || !(patternEncodingLogic.getEncodedInputInv().getKey(1) instanceof AEItemKey base)
                || !(patternEncodingLogic.getEncodedInputInv().getKey(2) instanceof AEItemKey addition)) {
            return null;
        }
        var input = new SmithingRecipeInput(template.toStack(), base.toStack(), addition.toStack());
        var level = getPlayer().level();
        var recipe = WcwtPolymorphCompat.getSmithingRecipe(this, input, level, getPlayer()).orElse(null);
        return recipe != null ? recipe.id() : null;
    }

    @Nullable
    private static String normalizeProviderSearchText(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
                if (DEBUG_BLANK_PATTERN_SYNC) {
                    logBlankPatternSync("autofill.skip_full",
                            "current={}, slotLimit={}",
                            summarizeItem(current),
                            blankInv.getSlotLimit(0));
                }
                return;
            }

            AEItemKey blankKey = AEItemKey.of(AEItems.BLANK_PATTERN.asItem());
            long extracted = StorageHelper.poweredExtraction(energySource, storage, blankKey, space, getActionSource());
            if (extracted <= 0) {
                if (DEBUG_BLANK_PATTERN_SYNC) {
                    logBlankPatternSync("autofill.skip_no_extract",
                            "space={}, current={}, linkStatus={}",
                            space,
                            summarizeItem(current),
                            getLinkStatus());
                }
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
            if (DEBUG_BLANK_PATTERN_SYNC) {
                logBlankPatternSync("autofill.success",
                        "space={}, extracted={}, toInsert={}, leftover={}, before={}, after={}, linkStatus={}",
                        space,
                        extracted,
                        toInsert,
                        leftover,
                        summarizeItem(current),
                        summarizeItem(blankPatternSlot.getItem()),
                        getLinkStatus());
            }
            if (initializingMenu) {
                if (DEBUG_BLANK_PATTERN_SYNC) {
                    logBlankPatternSync("autofill.defer_broadcast",
                            "initializingMenu=true, blankSlot={}, encodedSlot={}",
                            summarizeItem(blankPatternSlot.getItem()),
                            summarizeItem(encodedPatternSlot != null ? encodedPatternSlot.getItem() : ItemStack.EMPTY));
                }
                forceInventorySyncOnNextBroadcast();
                return;
            }
            forceInventorySyncOnNextBroadcast();
            broadcastChanges();
        } catch (Exception e) {
            com.lhy.wcwt.WcwtMod.LOGGER.debug("WCWT blank pattern auto-fill failed", e);
        }
    }

    /**
     * 自动回填空白样板会同时改动 blank slot 和底层 ME 库存。
     * 这里强制下一次 broadcastChanges 走完整 AE 菜单同步，避免库存同步节流让客户端 repo/link 状态滞后一拍，
     * 出现“空白样板已补回，但主网络区仍整片发灰”的假断线显示。
     */
    private void forceInventorySyncOnNextBroadcast() {
        if (DEBUG_BLANK_PATTERN_SYNC) {
            logBlankPatternSync("force_inventory_sync",
                    "previousLastInventorySyncTick={}, gameTick={}, blankSlot={}, encodedSlot={}",
                    lastInventorySyncTick,
                    getPlayer() != null ? getPlayer().level().getGameTime() : -1L,
                    summarizeItem(blankPatternSlot != null ? blankPatternSlot.getItem() : ItemStack.EMPTY),
                    summarizeItem(encodedPatternSlot != null ? encodedPatternSlot.getItem() : ItemStack.EMPTY));
        }
        lastInventorySyncTick = Long.MIN_VALUE;
    }

    private void logBlankPatternSync(String stage, String message, Object... args) {
        if (!DEBUG_BLANK_PATTERN_SYNC || !isServerSide()) {
            return;
        }
        Object[] finalArgs = new Object[args.length + 2];
        finalArgs[0] = getPlayer() != null ? getPlayer().getScoreboardName() : "<unknown>";
        finalArgs[1] = stage;
        System.arraycopy(args, 0, finalArgs, 2, args.length);
        WcwtMod.LOGGER.info("WCWT blank sync: player={}, stage={}, " + message, finalArgs);
    }

    private static String summarizeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getHoverName().getString() + " x" + stack.getCount();
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
            case CLEAR_MANUAL_WORKSPACE -> clearManualWorkspaceToNetwork();
            default -> {
            }
        }
    }

    @Override
    public void clearCraftingGrid() {
        if (getManualWorkspaceMode() == ManualWorkspaceMode.CRAFTING) {
            super.clearCraftingGrid();
            return;
        }
        if (isClientSide()) {
            PacketDistributor.sendToServer(new TopActionPacket(TopActionPacket.Action.CLEAR_MANUAL_WORKSPACE));
            return;
        }
        clearManualWorkspaceToNetwork();
    }

    @Override
    public void clearToPlayerInventory() {
        if (getManualWorkspaceMode() == ManualWorkspaceMode.CRAFTING) {
            super.clearToPlayerInventory();
            return;
        }
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_MANUAL_TO_PLAYER);
            return;
        }
        clearManualWorkspaceToPlayerInventory();
    }

    private void clearManualWorkspaceToNetwork() {
        moveManualWorkspaceToNetwork(getManualWorkspaceMode());
    }

    private void clearManualWorkspaceToPlayerInventory() {
        if (menuHost == null) {
            return;
        }
        var target = getCurrentManualWorkspaceInputInventory();
        if (target == null) {
            return;
        }
        var playerInv = new PlayerInternalInventory(getPlayerInventory());
        for (int i = 0; i < target.size(); ++i) {
            for (int emptyLoop = 0; emptyLoop < 2; ++emptyLoop) {
                boolean allowEmpty = emptyLoop == 1;
                final int hotbarSize = 9;
                for (int j = hotbarSize; j-- > 0;) {
                    if (playerInv.getStackInSlot(j).isEmpty() == allowEmpty) {
                        target.setItemDirect(i, playerInv.getSlotInv(j).addItems(target.getStackInSlot(i)));
                    }
                }
                for (int j = hotbarSize; j < Inventory.INVENTORY_SIZE; ++j) {
                    if (playerInv.getStackInSlot(j).isEmpty() == allowEmpty) {
                        target.setItemDirect(i, playerInv.getSlotInv(j).addItems(target.getStackInSlot(i)));
                    }
                }
            }
        }
        updateManualWorkspaceResults();
        broadcastChanges();
    }

    private void restockManualWorkspaceInputsAeStyle(InternalInventory inventory, ItemStack[] before) {
        if (isClientSide() || menuHost == null || before == null) {
            return;
        }
        var filter = ViewCellItem.createItemFilter(getViewCells());
        for (int slot = 0; slot < Math.min(inventory.size(), before.length); slot++) {
            ItemStack previous = before[slot];
            if (previous == null || previous.isEmpty()) {
                continue;
            }
            ItemStack current = inventory.getStackInSlot(slot);
            // 对齐 AE 工作台结果槽逻辑：
            // 只有当本次操作把该输入槽真正耗空时，才从网络补回 1 个相同材料；
            // 不维持输入槽原有堆叠数量。
            if (!current.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(previous);
            if (key == null || filter != null && !filter.isListed(key)) {
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(energySource, storage, key, 1, getActionSource());
            if (extracted <= 0) {
                continue;
            }
            ItemStack remainder = inventory.insertItem(slot, key.toStack(1), false);
            if (!remainder.isEmpty()) {
                StorageHelper.poweredInsert(energySource, storage, key, remainder.getCount(), getActionSource());
            }
        }
    }

    @Nullable
    private InternalInventory getCurrentManualWorkspaceInputInventory() {
        return getManualWorkspaceInputInventory(getManualWorkspaceMode());
    }

    @Nullable
    private InternalInventory getManualWorkspaceInputInventory(ManualWorkspaceMode mode) {
        if (menuHost == null) {
            return null;
        }
        return switch (mode) {
            case CRAFTING -> getCraftingMatrix();
            case SMITHING -> menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
            case ANVIL -> menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
        };
    }

    public boolean moveManualWorkspaceToNetwork(ManualWorkspaceMode mode) {
        if (menuHost == null || mode == null) {
            return false;
        }
        var target = getManualWorkspaceInputInventory(mode);
        if (target == null) {
            return false;
        }
        boolean changed = false;
        var storage = menuHost.getInventory();
        var energy = getEnergySource();
        var source = getActionSource();
        for (int i = 0; i < target.size(); i++) {
            ItemStack stack = target.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            var key = AEItemKey.of(stack);
            if (key == null) {
                continue;
            }
            long inserted = StorageHelper.poweredInsert(energy, storage, key, stack.getCount(), source);
            if (inserted <= 0) {
                continue;
            }
            ItemStack remaining = stack.copy();
            remaining.shrink((int) Math.min(inserted, Integer.MAX_VALUE));
            target.setItemDirect(i, remaining);
            changed = true;
        }
        if (changed) {
            updateManualWorkspaceResults();
            broadcastChanges();
        }
        return changed;
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
        boolean valid = false;
        for (int slot = 0; slot < ingredients.length; slot++) {
            GenericStack stack = patternEncodingLogic.getEncodedInputInv().getStack(slot);
            if (stack == null) {
                ingredients[slot] = ItemStack.EMPTY;
            } else if (stack.what() instanceof AEItemKey itemKey) {
                ingredients[slot] = itemKey.toStack(1);
                valid = true;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (!valid) {
            return ItemStack.EMPTY;
        }

        var input = CraftingInput.of(3, 3, java.util.Arrays.asList(ingredients));
        var level = getPlayer().level();
        var recipe = WcwtPolymorphCompat.getCraftingRecipe(this, input, level, getPlayer()).orElse(null);
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
        var recipe = WcwtPolymorphCompat.getSmithingRecipe(this, input, level, getPlayer()).orElse(null);
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
        RecipeHolder<CraftingRecipe> recipe =
                WcwtPolymorphCompat.getCraftingRecipe(this, input, level, getPlayer()).orElse(null);
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
        var recipe = WcwtPolymorphCompat.getSmithingRecipe(this, input, level, getPlayer()).orElse(null);
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

    private static void logPatternUploadDebug(String message, Object... args) {
        if (DEBUG_PATTERN_UPLOAD) {
            com.lhy.wcwt.WcwtMod.LOGGER.info("WCWT pattern upload debug: " + message, args);
        }
    }

    private static void logAdvanced(String message, Object... args) {
        if (DEBUG_ADVANCED) {
            com.lhy.wcwt.WcwtMod.LOGGER.info("WCWT advanced debug: " + message, args);
        }
    }

    private static void logQuickMoveUpgrade(String message, Object... args) {
        if (DEBUG_QUICKMOVE_UPGRADE) {
            com.lhy.wcwt.WcwtMod.LOGGER.info("WCWT quickMove upgrade debug: " + message, args);
        }
    }

    private boolean isUpgradeLikeSemantic(@Nullable appeng.menu.SlotSemantic semantic) {
        return semantic == SlotSemantics.UPGRADE
                || semantic == SlotSemantics.VIEW_CELL
                || semantic == AE2wtlibSlotSemantics.SINGULARITY
                || semantic == WcwtSlotSemantics.WCWT_CELL_UPGRADE;
    }

    private String describeSlot(@Nullable Slot slot) {
        if (slot == null) {
            return "<null>";
        }
        int menuPos = this.slots.indexOf(slot);
        var semantic = getSlotSemantic(slot);
        String semanticName = semantic == null ? "<null>" : semantic.toString();
        return "menuPos=" + menuPos + ",slotIdx=" + slot.index + ",semantic=" + semanticName
                + ",stack=" + describeStack(slot.getItem());
    }

    private static String describeStack(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getCount() + "x" + (key == null ? stack.getItem().toString() : key.toString());
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
            if (DEBUG_QUICKMOVE_UPGRADE && sourceSlot.hasItem()) {
                logQuickMoveUpgrade("start player={}, slotIndex={}, source={}",
                        player.getScoreboardName(), slotIndex, describeSlot(sourceSlot));
            }
            // 禁止从升级类/显示元件类槽位 shift 快速移出物品。原版客户端 shift 点击只发包，
            // 而像 Inventory Essentials 这类整理/批量转移模组会程序化地遍历槽位直接调用
            // quickMoveStack，从而把升级卡或显示元件误当成可批量转移的物品掏出来。在服务端源头拦截，
            // 不影响往这些槽内 shift 放入物品（那走的是 canMoveTo 目标判定）。
            if (isUpgradeLikeSemantic(getSlotSemantic(sourceSlot))) {
                return ItemStack.EMPTY;
            }
            if (isPlayerArmorSlot(sourceSlot)) {
                return quickMovePlayerEquipmentToInventory(player, sourceSlot);
            }
            if (sourceSlot == manualAnvilResultSlot) {
                ItemStack result = quickMoveManualAnvilResult(player);
                updateManualWorkspaceResults();
                broadcastChanges();
                return result;
            }
            if (sourceSlot == manualSmithingResultSlot) {
                ItemStack result = quickMoveManualSmithingResult(player);
                updateManualWorkspaceResults();
                broadcastChanges();
                return result;
            }
            if (sourceSlot instanceof PatternProviderSlot patternProviderSlot) {
                return quickMovePatternProviderSlot(player, patternProviderSlot);
            }
            if (sourceSlot.hasItem() && !isPlayerArmorSlot(sourceSlot)) {
                ItemStack sourceStack = sourceSlot.getItem();
                boolean fromPlayerInventory = isPlayerHotbarOrStorageSemanticSlot(sourceSlot);
                ItemStack movedCurioToPlayer = tryMoveCurioToPlayerInventoryFirst(player, sourceSlot, sourceStack);
                if (!movedCurioToPlayer.isEmpty()) {
                    return movedCurioToPlayer;
                }

                if (!fromPlayerInventory) {
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

    private ItemStack quickMovePatternProviderSlot(Player player, PatternProviderSlot sourceSlot) {
        if (!sourceSlot.isActive() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceSlot.getItem().copy();
        ItemStack toInsert = original.copy();
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        if (playerInventoryStart < 0 || !moveItemStackTo(toInsert, playerInventoryStart, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        int moved = original.getCount() - toInsert.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        sourceSlot.remove(moved);
        sourceSlot.setChanged();
        return original;
    }

    private ItemStack quickMovePlayerEquipmentToInventory(Player player, Slot sourceSlot) {
        if (!sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        if (playerInventoryStart < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack original = sourceStack.copy();
        if (!moveItemStackTo(sourceStack, playerInventoryStart, slots.size(), false)) {
            return ItemStack.EMPTY;
        }
        finishPriorityQuickMove(player, sourceSlot, sourceStack);
        return original;
    }

    private ItemStack quickMoveManualSmithingResult(Player player) {
        if (manualSmithingResultSlot == null
                || !manualSmithingResultSlot.hasItem()
                || !manualSmithingResultSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack resultStack = manualSmithingResultSlot.getItem().copy();
        ItemStack toInsert = resultStack.copy();
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        if (playerInventoryStart < 0
                || !moveItemStackTo(toInsert, playerInventoryStart, slots.size(), true)
                || !toInsert.isEmpty()) {
            return ItemStack.EMPTY;
        }

        manualSmithingBridge.takeResult(player, resultStack.copy());
        return resultStack;
    }

    private ItemStack quickMoveManualAnvilResult(Player player) {
        if (manualAnvilResultSlot == null || !manualAnvilResultSlot.hasItem() || !manualAnvilResultSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack resultStack = manualAnvilResultSlot.getItem().copy();
        ItemStack toInsert = resultStack.copy();
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        if (playerInventoryStart < 0 || !moveItemStackTo(toInsert, playerInventoryStart, slots.size(), true)) {
            return ItemStack.EMPTY;
        }

        manualAnvilBridge.takeResult(player, resultStack.copy());
        return resultStack;
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

    private boolean isExtendedUiSlotUnavailable(@Nullable appeng.menu.SlotSemantic semantic) {
        if (menuHost == null) {
            return false;
        }
        var currentUI = menuHost.getCurrentExtendedUI();
        if (semantic == WcwtSlotSemantics.WCWT_TOOLKIT) {
            return currentUI != IExtendedUIHost.ExtendedUIType.TOOLKIT;
        }
        if (semantic == WcwtSlotSemantics.AE_CURIOS) {
            return currentUI != IExtendedUIHost.ExtendedUIType.CURIOS;
        }
        if (isDecorativeArmorSemantic(semantic)) {
            return currentUI != IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR;
        }
        if (semantic == WcwtSlotSemantics.COPY_PATTERN
                || semantic == WcwtSlotSemantics.WCWT_STORAGE_CELL
                || semantic == WcwtSlotSemantics.WCWT_CELL_UPGRADE) {
            return currentUI != IExtendedUIHost.ExtendedUIType.ADVANCED_CODING;
        }
        if (semantic == WcwtSlotSemantics.WCWT_RESONATING_STORAGE) {
            return currentUI != IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING;
        }
        return false;
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

    private boolean isOffhandSlot(Slot slot) {
        return getSlots(WcwtSlotSemantics.AE2WTLIB_OFFHAND).contains(slot);
    }

    private static boolean isManualSmithingOrAnvilInputSemantic(@Nullable appeng.menu.SlotSemantic semantic) {
        return semantic == WcwtSlotSemantics.WCWT_MANUAL_SMITHING_TEMPLATE
                || semantic == WcwtSlotSemantics.WCWT_MANUAL_SMITHING_BASE
                || semantic == WcwtSlotSemantics.WCWT_MANUAL_SMITHING_ADDITION
                || semantic == WcwtSlotSemantics.WCWT_MANUAL_ANVIL_LEFT
                || semantic == WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RIGHT;
    }

    private boolean shouldHandleManualCraftingResultAction(InventoryAction action, int slotIndex) {
        if (getManualWorkspaceMode() != ManualWorkspaceMode.CRAFTING
                || !isCraftingResultAction(action)
                || !isManualCraftingReplacementEnabled()
                || slotIndex < 0
                || slotIndex >= slots.size()) {
            return false;
        }
        return getSlots(SlotSemantics.CRAFTING_RESULT).contains(slots.get(slotIndex));
    }

    private static boolean isCraftingResultAction(InventoryAction action) {
        return action == InventoryAction.CRAFT_SHIFT
                || action == InventoryAction.CRAFT_ALL
                || action == InventoryAction.CRAFT_ITEM
                || action == InventoryAction.CRAFT_STACK;
    }

    private boolean isManualCraftingReplacementEnabled() {
        return isManualCraftingItemSubstitution() || isManualCraftingFluidSubstitution();
    }

    private void handleManualCraftingResultAction(ServerPlayer player, InventoryAction action) {
        InternalInventory craftingMatrix = getCraftingMatrix();
        ItemStack initialOutput = getManualCraftingOutput();
        if (initialOutput.isEmpty()) {
            return;
        }

        int howManyPerCraft = Math.max(1, initialOutput.getCount());
        int maxTimesToCraft;
        InternalInventory target;
        if (action == InventoryAction.CRAFT_SHIFT || action == InventoryAction.CRAFT_ALL) {
            target = new PlayerInternalInventory(player.getInventory());
            if (action == InventoryAction.CRAFT_SHIFT) {
                maxTimesToCraft = Math.max(1, initialOutput.getMaxStackSize() / howManyPerCraft);
            } else {
                maxTimesToCraft = Math.max(1,
                        initialOutput.getMaxStackSize() / howManyPerCraft * Inventory.INVENTORY_SIZE);
            }
        } else {
            target = new CarriedItemInventory(this);
            maxTimesToCraft = action == InventoryAction.CRAFT_STACK
                    ? Math.max(1, initialOutput.getMaxStackSize() / howManyPerCraft)
                    : 1;
        }

        for (int crafted = 0; crafted < maxTimesToCraft; crafted++) {
            CraftingInput input = createManualCraftingInput(craftingMatrix);
            RecipeHolder<CraftingRecipe> recipe = getCurrentRecipe();
            if (recipe == null || !recipe.value().matches(input, player.level())) {
                updateManualCraftingResult();
                recipe = getCurrentRecipe();
                input = createManualCraftingInput(craftingMatrix);
                if (recipe == null || !recipe.value().matches(input, player.level())) {
                    return;
                }
            }

            ItemStack output = recipe.value().assemble(input, player.level().registryAccess());
            if (output.isEmpty() || !ItemStack.isSameItemSameComponents(initialOutput, output)) {
                return;
            }
            if (!target.simulateAdd(output).isEmpty()) {
                return;
            }

            ItemStack craftedStack = craftManualCraftingItem(player, recipe, output);
            if (craftedStack.isEmpty()) {
                return;
            }

            ItemStack extra = target.addItems(craftedStack);
            if (!extra.isEmpty()) {
                Platform.spawnDrops(player.level(), player.blockPosition(), List.of(extra));
                return;
            }
        }
    }

    private ItemStack getManualCraftingOutput() {
        for (Slot resultSlot : getSlots(SlotSemantics.CRAFTING_RESULT)) {
            ItemStack stack = resultSlot.getItem();
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private CraftingInput createManualCraftingInput(InternalInventory craftingMatrix) {
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < Math.min(9, craftingMatrix.size()); i++) {
            items.add(craftingMatrix.getStackInSlot(i).copy());
        }
        while (items.size() < 9) {
            items.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, items);
    }

    private ItemStack craftManualCraftingItem(Player player, RecipeHolder<CraftingRecipe> recipe, ItemStack output) {
        InternalInventory craftingMatrix = getCraftingMatrix();
        ItemStack[] before = snapshotManualCraftingGrid(craftingMatrix);
        ItemStack crafted = output.copy();

        crafted.onCraftedBy(player.level(), player, crafted.getCount());
        EventHooks.firePlayerCraftingEvent(player, crafted, craftingMatrix.toContainer());

        consumeManualCraftingIngredients(player, craftingMatrix, recipe);
        restockManualCraftingInputs(craftingMatrix, before, recipe, crafted);
        slotsChanged(craftingMatrix.toContainer());
        forceInventorySyncOnNextBroadcast();
        broadcastChanges();
        return crafted;
    }

    private static ItemStack[] snapshotManualCraftingGrid(InternalInventory craftingMatrix) {
        ItemStack[] before = new ItemStack[Math.min(9, craftingMatrix.size())];
        for (int i = 0; i < before.length; i++) {
            before[i] = craftingMatrix.getStackInSlot(i).copy();
        }
        return before;
    }

    private void consumeManualCraftingIngredients(Player player, InternalInventory craftingMatrix,
            RecipeHolder<CraftingRecipe> recipe) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(9, craftingMatrix.size()); i++) {
            items.set(i, craftingMatrix.getStackInSlot(i).copy());
        }
        var positioned = CraftingInput.ofPositioned(3, 3, items);

        CommonHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remainingItems;
        try {
            remainingItems = recipe.value().getRemainingItems(positioned.input());
        } finally {
            CommonHooks.setCraftingPlayer(null);
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int slotIndex = y * 3 + x;
                if (slotIndex >= craftingMatrix.size()) {
                    continue;
                }
                int remainderIndex = (y - positioned.top()) * 3 + (x - positioned.left());
                craftingMatrix.extractItem(slotIndex, 1, false);
                if (remainderIndex < 0 || remainderIndex >= remainingItems.size()) {
                    continue;
                }
                ItemStack remainingInSlot = remainingItems.get(remainderIndex);
                if (remainingInSlot.isEmpty()) {
                    continue;
                }
                if (craftingMatrix.getStackInSlot(slotIndex).isEmpty()) {
                    craftingMatrix.setItemDirect(slotIndex, remainingInSlot.copy());
                } else if (!player.getInventory().add(remainingInSlot.copy())) {
                    player.drop(remainingInSlot.copy(), false);
                }
            }
        }
    }

    private void restockManualCraftingInputs(InternalInventory craftingMatrix, ItemStack[] before,
            RecipeHolder<CraftingRecipe> recipe, ItemStack expectedOutput) {
        if (isClientSide() || menuHost == null || before == null) {
            return;
        }
        var filter = ViewCellItem.createItemFilter(getViewCells());
        KeyCounter availableStacks = null;
        ManualCraftingRecipeAlternatives recipeAlternatives = isManualCraftingItemSubstitution()
                ? new ManualCraftingRecipeAlternatives(recipe)
                : null;
        for (int slot = 0; slot < Math.min(craftingMatrix.size(), before.length); slot++) {
            ItemStack previous = before[slot];
            if (previous == null || previous.isEmpty()) {
                continue;
            }
            ItemStack current = craftingMatrix.getStackInSlot(slot);
            if (!current.isEmpty()) {
                if (isManualCraftingFluidSubstitution()
                        && tryFillManualCraftingContainerFromFluid(craftingMatrix, slot, previous, current, before, recipe,
                                expectedOutput)) {
                    continue;
                }
                continue;
            }
            if (availableStacks == null) {
                availableStacks = storage.getAvailableStacks();
            }
            ItemStack replacement = extractManualCraftingReplacement(slot, previous, before, recipe, expectedOutput,
                    filter, availableStacks, recipeAlternatives);
            if (!replacement.isEmpty()) {
                craftingMatrix.setItemDirect(slot, replacement);
            }
        }
    }

    private ItemStack extractManualCraftingReplacement(int slot, ItemStack previous, ItemStack[] before,
            RecipeHolder<CraftingRecipe> recipe, ItemStack expectedOutput, @Nullable IPartitionList filter,
            KeyCounter availableStacks, @Nullable ManualCraftingRecipeAlternatives recipeAlternatives) {
        ItemStack exact = extractManualCraftingExactReplacement(previous, filter);
        if (!exact.isEmpty()) {
            return exact;
        }
        if (!isManualCraftingItemSubstitution()) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> alternatives = manualCraftingAlternativesFor(slot, previous);
        ItemStack rememberedReplacement = extractManualCraftingAlternativeReplacement(slot, before, recipe,
                expectedOutput, filter, availableStacks, alternatives);
        if (!rememberedReplacement.isEmpty()) {
            return rememberedReplacement;
        }

        List<ItemStack> recipeDerivedAlternatives = recipeAlternatives == null
                ? List.of()
                : recipeAlternatives.getAlternatives(previous);
        if (recipeDerivedAlternatives.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> expandedAlternatives = mergeManualCraftingAlternatives(alternatives, recipeDerivedAlternatives);
        if (expandedAlternatives.size() == alternatives.size()) {
            return ItemStack.EMPTY;
        }
        return extractManualCraftingAlternativeReplacement(slot, before, recipe, expectedOutput, filter,
                availableStacks, expandedAlternatives);
    }

    private ItemStack extractManualCraftingAlternativeReplacement(int slot, ItemStack[] before,
            RecipeHolder<CraftingRecipe> recipe, ItemStack expectedOutput, @Nullable IPartitionList filter,
            KeyCounter availableStacks, List<ItemStack> alternatives) {
        if (alternatives.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var wideIngredient = WcwtMeIngredientExtraction.ingredientFromItemStacks(alternatives);
        for (AEItemKey candidate : manualCraftingReplacementCandidates(alternatives, wideIngredient, filter,
                availableStacks)) {
            ItemStack candidateStack = candidate.toStack();
            if (!isManualCraftingReplacementValid(slot, candidateStack, before, recipe, expectedOutput)) {
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(energySource, storage, candidate, 1, getActionSource());
            if (extracted > 0) {
                return candidate.toStack(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> mergeManualCraftingAlternatives(List<ItemStack> remembered,
            List<ItemStack> recipeDerived) {
        List<ItemStack> merged = new ArrayList<>(remembered.size() + recipeDerived.size());
        merged.addAll(WcwtIngredientPriorities.deduplicateItemAlternatives(remembered));
        for (ItemStack alternative : WcwtIngredientPriorities.deduplicateItemAlternatives(recipeDerived)) {
            if (!containsSameItemAndComponents(merged, alternative)) {
                merged.add(alternative.copyWithCount(1));
            }
        }
        return merged;
    }

    private static boolean containsSameItemAndComponents(List<ItemStack> stacks, ItemStack candidate) {
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, candidate)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack extractManualCraftingExactReplacement(ItemStack previous, @Nullable IPartitionList filter) {
        AEItemKey key = AEItemKey.of(previous);
        if (key == null || filter != null && !filter.isListed(key)) {
            return ItemStack.EMPTY;
        }
        long extracted = StorageHelper.poweredExtraction(energySource, storage, key, 1, getActionSource());
        return extracted > 0 ? key.toStack(1) : ItemStack.EMPTY;
    }

    private List<ItemStack> manualCraftingAlternativesFor(int slot, ItemStack previous) {
        if (slot < 0 || slot >= manualCraftingSlotAlternatives.size()) {
            return List.of();
        }
        List<ItemStack> alternatives = manualCraftingSlotAlternatives.get(slot);
        if (alternatives.isEmpty()) {
            return List.of();
        }
        var wideIngredient = WcwtMeIngredientExtraction.ingredientFromItemStacks(alternatives);
        return WcwtStackMatching.matchesAnyAlternative(previous, alternatives, wideIngredient)
                ? alternatives
                : List.of();
    }

    private List<AEItemKey> manualCraftingReplacementCandidates(List<ItemStack> alternatives,
            @Nullable net.minecraft.world.item.crafting.Ingredient wideIngredient, @Nullable IPartitionList filter,
            KeyCounter availableStacks) {
        List<AEItemKey> candidates = new ArrayList<>();
        for (ItemStack alternative : WcwtIngredientPriorities.deduplicateItemAlternatives(alternatives)) {
            AEItemKey key = AEItemKey.of(alternative);
            if (key != null
                    && availableStacks.get(key) > 0
                    && (filter == null || filter.isListed(key))
                    && !candidates.contains(key)) {
                candidates.add(key);
            }
        }
        if (WcwtStackMatching.requiresExactItemKeyMatch(alternatives)) {
            return candidates;
        }
        for (Object2LongMap.Entry<AEKey> entry : availableStacks) {
            if (entry.getLongValue() <= 0 || !(entry.getKey() instanceof AEItemKey itemKey)) {
                continue;
            }
            if (filter != null && !filter.isListed(itemKey)) {
                continue;
            }
            if (!WcwtStackMatching.matchesItemKey(itemKey, alternatives, wideIngredient)
                    || candidates.contains(itemKey)) {
                continue;
            }
            candidates.add(itemKey);
        }
        return candidates;
    }

    private boolean isManualCraftingReplacementValid(int slot, ItemStack candidate, ItemStack[] before,
            RecipeHolder<CraftingRecipe> recipe, ItemStack expectedOutput) {
        if (candidate.isEmpty()) {
            return false;
        }
        List<ItemStack> adjusted = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < before.length ? before[i] : ItemStack.EMPTY;
            adjusted.add(i == slot ? candidate.copyWithCount(1) : stack.copy());
        }
        CraftingInput input = CraftingInput.of(3, 3, adjusted);
        return recipe.value().matches(input, getPlayer().level())
                && ItemStack.isSameItemSameComponents(
                        expectedOutput,
                        recipe.value().assemble(input, getPlayer().level().registryAccess()));
    }

    private boolean tryFillManualCraftingContainerFromFluid(InternalInventory craftingMatrix, int slot,
            ItemStack previous, ItemStack current, ItemStack[] before, RecipeHolder<CraftingRecipe> recipe,
            ItemStack expectedOutput) {
        if (current.getCount() != 1) {
            return false;
        }
        GenericStack contained = ContainerItemStrategies.getContainedStack(previous, AEKeyType.fluids());
        if (contained == null || !(contained.what() instanceof AEFluidKey fluidKey) || contained.amount() <= 0) {
            return false;
        }
        int amount = (int) Math.min(Integer.MAX_VALUE, contained.amount());
        ItemStack filledContainer = fillManualCraftingFluidContainer(current, fluidKey, amount);
        if (filledContainer.isEmpty()) {
            return false;
        }
        if (!isManualCraftingReplacementValid(slot, filledContainer, before, recipe, expectedOutput)) {
            return false;
        }
        long available = StorageHelper.poweredExtraction(energySource, storage, fluidKey, amount, getActionSource(),
                Actionable.SIMULATE);
        if (available < amount) {
            return false;
        }
        long extracted = StorageHelper.poweredExtraction(energySource, storage, fluidKey, amount, getActionSource());
        if (extracted < amount) {
            if (extracted > 0) {
                StorageHelper.poweredInsert(energySource, storage, fluidKey, extracted, getActionSource());
            }
            return false;
        }
        craftingMatrix.setItemDirect(slot, filledContainer.copyWithCount(1));
        return true;
    }

    private ItemStack fillManualCraftingFluidContainer(ItemStack emptyContainer, AEFluidKey fluidKey, int amount) {
        ItemStack filled = emptyContainer.copyWithCount(1);
        var fluidHandler = FluidUtil.getFluidHandler(filled).orElse(null);
        if (fluidHandler != null) {
            int fillable = fluidHandler.fill(fluidKey.toStack(amount), Actionable.SIMULATE.getFluidAction());
            if (fillable >= amount) {
                int filledAmount = fluidHandler.fill(fluidKey.toStack(amount), Actionable.MODULATE.getFluidAction());
                if (filledAmount >= amount) {
                    return fluidHandler.getContainer();
                }
            }
        }

        if (emptyContainer.is(Items.BUCKET)) {
            ItemStack bucket = FluidUtil.getFilledBucket(fluidKey.toStack(amount));
            if (!bucket.isEmpty()) {
                return bucket.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static final class ManualCraftingRecipeAlternatives {
        private final RecipeHolder<CraftingRecipe> recipe;
        private final HashMap<AEItemKey, List<ItemStack>> cache = new HashMap<>();
        private @Nullable List<Ingredient> ingredients;

        private ManualCraftingRecipeAlternatives(RecipeHolder<CraftingRecipe> recipe) {
            this.recipe = recipe;
        }

        private List<ItemStack> getAlternatives(ItemStack previous) {
            AEItemKey previousKey = AEItemKey.of(previous);
            if (previousKey == null) {
                return List.of();
            }
            return cache.computeIfAbsent(previousKey, ignored -> collectAlternatives(previous));
        }

        private List<ItemStack> collectAlternatives(ItemStack previous) {
            List<ItemStack> result = new ArrayList<>();
            for (Ingredient ingredient : getIngredients()) {
                if (ingredient == null || ingredient.isEmpty() || !ingredient.test(previous)) {
                    continue;
                }
                for (ItemStack candidate : ingredient.getItems()) {
                    if (!candidate.isEmpty() && !containsSameItemAndComponents(result, candidate)) {
                        result.add(candidate.copyWithCount(1));
                    }
                }
            }
            return result;
        }

        private List<Ingredient> getIngredients() {
            if (ingredients == null) {
                ingredients = List.copyOf(CraftingRecipeUtil.getIngredients(recipe.value()));
            }
            return ingredients;
        }
    }

    @Override
    protected boolean isValidQuickMoveDestination(Slot candidateSlot, ItemStack stackToMove, boolean fromPlayerSide) {
        if (candidateSlot instanceof PatternProviderSlot) {
            return false;
        }
        if (!fromPlayerSide && isOffhandSlot(candidateSlot)) {
            return false;
        }
        var candidateSemantic = getSlotSemantic(candidateSlot);
        if (fromPlayerSide
                && !getLinkStatus().connected()
                && isManualSmithingOrAnvilInputSemantic(candidateSemantic)) {
            return false;
        }
        if (isExtendedUiSlotUnavailable(candidateSemantic)) {
            return false;
        }
        if (!super.isValidQuickMoveDestination(candidateSlot, stackToMove, fromPlayerSide)) {
            if (DEBUG_QUICKMOVE_UPGRADE && isUpgradeLikeSemantic(getSlotSemantic(candidateSlot))) {
                logQuickMoveUpgrade("reject(super) fromPlayerSide={}, moving={}, candidate={}",
                        fromPlayerSide, describeStack(stackToMove), describeSlot(candidateSlot));
            }
            return false;
        }

        if (menuHost == null) {
            if (DEBUG_QUICKMOVE_UPGRADE && isUpgradeLikeSemantic(getSlotSemantic(candidateSlot))) {
                logQuickMoveUpgrade("accept(noHost) fromPlayerSide={}, moving={}, candidate={}",
                        fromPlayerSide, describeStack(stackToMove), describeSlot(candidateSlot));
            }
            return true;
        }

        var semantic = candidateSemantic;
        if (DEBUG_QUICKMOVE_UPGRADE && isUpgradeLikeSemantic(semantic)) {
            logQuickMoveUpgrade("accept fromPlayerSide={}, moving={}, candidate={}",
                    fromPlayerSide, describeStack(stackToMove), describeSlot(candidateSlot));
        }
        return true;
    }

    private static boolean isDecorativeArmorSemantic(@Nullable appeng.menu.SlotSemantic semantic) {
        return semantic == WcwtSlotSemantics.DECORATIVE_HELMET
                || semantic == WcwtSlotSemantics.DECORATIVE_ARMOR
                || semantic == WcwtSlotSemantics.DECORATIVE_SHIN_GUARDS
                || semantic == WcwtSlotSemantics.DECORATIVE_BOOTS;
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
        if (tryMoveStackIntoToolkitLogicalSlots(sourceSlot, sourceStack, original, lookup,
                toolkitMemoryInsertionOrder(sourceStack), true) != ItemStack.EMPTY) {
            return original;
        }
        if (tryMoveStackIntoToolkitLogicalSlots(sourceSlot, sourceStack, original, lookup,
                ToolkitItemRules.insertionIndexOrder(sourceStack), false) != ItemStack.EMPTY) {
            return original;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack tryMoveStackIntoToolkitLogicalSlots(Slot sourceSlot, ItemStack sourceStack, ItemStack original,
                                                          Slot[] lookup, int[] logicalIndexes,
                                                          boolean requireMemoryMatch) {
        for (int logicalIndex : logicalIndexes) {
            if (logicalIndex < 0 || logicalIndex >= lookup.length) {
                continue;
            }
            if (!toolkitMemoryMatches(logicalIndex, sourceStack)) {
                continue;
            }
            if (!requireMemoryMatch && hasToolkitMemory(logicalIndex)) {
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

    private int[] toolkitMemoryInsertionOrder(ItemStack stack) {
        var memory = toolkitMemoryInventory();
        if (memory == null || stack.isEmpty()) {
            return new int[0];
        }
        int limit = Math.min(memory.size(), TOOLKIT_MEMORY_ITEM_MATCH_LIMIT);
        int[] tmp = new int[limit];
        int count = 0;
        for (int slot = ToolkitItemRules.DEDICATED_SLOT_COUNT; slot < limit; slot++) {
            if (!memory.getStackInSlot(slot).isEmpty() && toolkitMemoryMatches(slot, stack)) {
                tmp[count++] = slot;
            }
        }
        int[] out = new int[count];
        System.arraycopy(tmp, 0, out, 0, count);
        return out;
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
        if (DEBUG_QUICKMOVE_UPGRADE && isUpgradeLikeSemantic(getSlotSemantic(slot))) {
            logQuickMoveUpgrade("upgrade slot changed slot={}", describeSlot(slot));
        }
        if (slot == manualSmithingTemplateSlot
                || slot == manualSmithingBaseSlot
                || slot == manualSmithingAdditionSlot
                || slot == manualAnvilLeftSlot
                || slot == manualAnvilRightSlot) {
            updateManualWorkspaceResults();
        }
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
    public void applyPatternMultiplier(PatternMultiplierButton.MultiplierType type, boolean applyToEditorProcessing) {
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
                restoreProcessingPatternRatio(applyToEditorProcessing);
                return;
            case SWAP:
                rotateProcessingPatternOutputs(applyToEditorProcessing);
                return;
            default:
                return;
        }
        
        // 对所有样板槽位应用倍增/除法
        modifyPatterns(scale, divide, applyToEditorProcessing);
    }
    
    /**
     * 倍增或除法样板
     * 基于ExtendedAE的实现
     */
    private void modifyPatterns(int scale, boolean divide, boolean applyToEditorProcessing) {
        if (scale <= 0) {
            return;
        }

        if (applyToEditorProcessing) {
            applyMultiplierToCurrentProcessingConfig(scale, divide);
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

    private void applyMultiplierToCurrentProcessingConfig(int scale, boolean divide) {
        if (patternEncodingLogic == null || getPatternEncodingMode() != EncodingMode.PROCESSING) {
            return;
        }

        var inputInv = patternEncodingLogic.getEncodedInputInv();
        var outputInv = patternEncodingLogic.getEncodedOutputInv();
        var input = copyConfigStacks(inputInv);
        var output = copyConfigStacks(outputInv);
        if (!hasAnyStack(input) || !hasAnyStack(output)) {
            return;
        }
        if (!checkCanModify(input, scale, divide) || !checkCanModify(output, scale, divide)) {
            return;
        }

        var modifiedInput = new GenericStack[input.length];
        var modifiedOutput = new GenericStack[output.length];
        modifyStacks(input, modifiedInput, scale, divide);
        modifyStacks(output, modifiedOutput, scale, divide);
        writeConfigStacks(inputInv, modifiedInput);
        writeConfigStacks(outputInv, modifiedOutput);
        updatePatternPreview(EncodingMode.PROCESSING);
    }

    private static GenericStack[] copyConfigStacks(appeng.util.ConfigInventory inventory) {
        var result = new GenericStack[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            result[i] = stack == null ? null : new GenericStack(stack.what(), stack.amount());
        }
        return result;
    }

    private static boolean hasAnyStack(GenericStack[] stacks) {
        for (var stack : stacks) {
            if (stack != null) {
                return true;
            }
        }
        return false;
    }

    private static void writeConfigStacks(appeng.util.ConfigInventory inventory, GenericStack[] stacks) {
        inventory.beginBatch();
        try {
            for (int i = 0; i < inventory.size(); i++) {
                inventory.setStack(i, i < stacks.length ? stacks[i] : null);
            }
        } finally {
            inventory.endBatch();
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

    public void handlePatternMode(int mode, boolean value) {
        switch (mode) {
            case PatternModePacket.MODE_PATTERN_ITEM_SUBSTITUTIONS,
                    PatternModePacket.MODE_PATTERN_FLUID_SUBSTITUTIONS -> changePatternCacheSubstitutionMode(mode, value);
            case PatternModePacket.MODE_MANUAL_ITEM_SUBSTITUTION -> setManualCraftingItemSubstitution(value);
            case PatternModePacket.MODE_MANUAL_FLUID_SUBSTITUTION -> setManualCraftingFluidSubstitution(value);
            default -> {
            }
        }
    }

    private void changePatternCacheSubstitutionMode(int mode, boolean value) {
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
                            mode == PatternModePacket.MODE_PATTERN_ITEM_SUBSTITUTIONS ? value : craft.canSubstitute(),
                            mode == PatternModePacket.MODE_PATTERN_FLUID_SUBSTITUTIONS
                                    ? value
                                    : craft.canSubstituteFluids());
                    slot.set(newPattern);
                } catch (Exception ignored) {
                }
            }
        }
        broadcastChanges();
    }

    private void restoreProcessingPatternRatio(boolean applyToEditorProcessing) {
        if (applyToEditorProcessing) {
            restoreCurrentProcessingPatternRatio();
        }

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

    private void restoreCurrentProcessingPatternRatio() {
        if (patternEncodingLogic == null || getPatternEncodingMode() != EncodingMode.PROCESSING) {
            return;
        }

        var inputInv = patternEncodingLogic.getEncodedInputInv();
        var outputInv = patternEncodingLogic.getEncodedOutputInv();
        var input = java.util.Arrays.stream(copyConfigStacks(inputInv))
                .filter(Objects::nonNull)
                .toList();
        var output = java.util.Arrays.stream(copyConfigStacks(outputInv))
                .filter(Objects::nonNull)
                .toList();
        if (input.isEmpty() || output.isEmpty()) {
            return;
        }

        long gcd = computeSharedGcd(input, output);
        if (gcd <= 1L) {
            return;
        }

        writeConfigStacks(inputInv, densifyStacks(divideStacks(input, gcd), inputInv.size()));
        writeConfigStacks(outputInv, densifyStacks(divideStacks(output, gcd), outputInv.size()));
        updatePatternPreview(EncodingMode.PROCESSING);
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

    private void rotateProcessingPatternOutputs(boolean applyToEditorProcessing) {
        if (applyToEditorProcessing) {
            rotateCurrentProcessingPatternOutputs();
        }

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

    private void rotateCurrentProcessingPatternOutputs() {
        if (patternEncodingLogic == null || getPatternEncodingMode() != EncodingMode.PROCESSING) {
            return;
        }

        var outputInv = patternEncodingLogic.getEncodedOutputInv();
        var output = copyConfigStacks(outputInv);
        if (!hasAtLeastTwoStacks(output)) {
            return;
        }

        writeConfigStacks(outputInv, rotateOutputs(output));
        updatePatternPreview(EncodingMode.PROCESSING);
    }

    private static boolean hasAtLeastTwoStacks(GenericStack[] stacks) {
        int count = 0;
        for (var stack : stacks) {
            if (stack != null && ++count >= 2) {
                return true;
            }
        }
        return false;
    }

    private static GenericStack[] densifyStacks(List<GenericStack> sparse, int size) {
        var dense = new GenericStack[size];
        for (int i = 0; i < sparse.size() && i < size; i++) {
            var stack = sparse.get(i);
            dense[i] = stack == null ? null : new GenericStack(stack.what(), stack.amount());
        }
        return dense;
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

    public void cycleCellCompressionCutoff(boolean towardMoreCompressed) {
        var cellStack = getStorageCellItem();
        if (cellStack.isEmpty()) {
            return;
        }
        if (WcwtMegaCellsCompat.switchCompressionCutoff(cellStack, towardMoreCompressed)) {
            if (menuHost != null) {
                menuHost.persistStorageCellItem();
            }
            broadcastChanges();
        }
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
    private static volatile Field craftingTermCurrentRecipeField;
    private static volatile Field smithingMenuSelectedRecipeField;

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

    private static void setCraftingTermCurrentRecipe(CraftingTermMenu menu,
                                                     @Nullable RecipeHolder<CraftingRecipe> recipe) {
        try {
            var field = craftingTermCurrentRecipeField;
            if (field == null) {
                field = CraftingTermMenu.class.getDeclaredField("currentRecipe");
                field.setAccessible(true);
                craftingTermCurrentRecipeField = field;
            }
            field.set(menu, recipe);
        } catch (ReflectiveOperationException | SecurityException ignored) {
        }
    }

    private static void setSmithingMenuSelectedRecipe(SmithingMenu menu,
                                                      @Nullable RecipeHolder<SmithingRecipe> recipe) {
        try {
            var field = smithingMenuSelectedRecipeField;
            if (field == null) {
                field = SmithingMenu.class.getDeclaredField("selectedRecipe");
                field.setAccessible(true);
                smithingMenuSelectedRecipeField = field;
            }
            field.set(menu, recipe);
        } catch (ReflectiveOperationException | SecurityException ignored) {
        }
    }
}
