package com.lhy.wcwt.menu;

import appeng.api.config.CopyMode;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.core.sync.packets.FillCraftingGridFromRecipePacket;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.InventoryAction;
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
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CosmeticArmorReworkedBridge;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.compat.JecSearchCompat;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.EncodePatternPacket;
import com.lhy.wcwt.network.OpenEaepProviderSelectScreenPacket;
import com.lhy.wcwt.network.PatternEncodingModePacket;
import com.lhy.wcwt.network.PatternEncodingOptionPacket;
import com.lhy.wcwt.network.PatternProviderFocusPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.TopActionPacket;
import com.lhy.wcwt.util.PatternUploadMetadata;
import com.lhy.wcwt.util.PatternProviderSorts;
import com.mojang.datafixers.util.Pair;
import com.extendedae_plus.util.PatternProviderDataUtil;
import com.extendedae_plus.util.uploadPattern.MatrixUploadUtil;
import com.extendedae_plus.util.uploadPattern.ProviderUploadUtil;
import com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.TextConstants;
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
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import com.lhy.wcwt.compat.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.CraftingInput;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.SlotItemHandler;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.SingleRecipeInput;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.SmithingRecipeInput;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import com.google.common.math.LongMath;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class WirelessComprehensiveWorkTerminalMenu extends CraftingTermMenu implements IOptionalSlotHost {
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");
    private static final boolean DEBUG_BLANK_PATTERN_SYNC =
            Boolean.getBoolean("wcwt.debug.blankPatternSync");
    private static final long PERF_LOG_THRESHOLD_NS = 1_000_000L;
    private static final int PATTERN_PROVIDER_SYNC_INTERVAL_TICKS = 20;
    private static final long PATTERN_PROVIDER_SYNC_SUBSCRIPTION_TICKS = 100L;
    private static final long INVENTORY_SYNC_INTERVAL_TICKS =
            Math.max(1L, Long.getLong("wcwt.inventorySyncIntervalTicks", 1L));
    private static final boolean DEBUG_PERF_SKIPPED_INVENTORY_SYNC =
            Boolean.getBoolean("wcwt.debug.perfSkippedInventorySync");

    public static final String TYPE_ID = "wireless_comprehensive_work_terminal";
    public static final String TOP_ACTION = "topAction";
    private static final String ACTION_CLEAR_MANUAL_TO_PLAYER = "clearManualToPlayer";
    public static final String ACTION_SYNC_QUICK_MOVE_OPTIONS = "syncQuickMoveOptions";
    public static final int QUICK_MOVE_TO_COSMETIC_ARMOR = 1;
    public static final int QUICK_MOVE_TO_CARD_BOX = 1 << 1;
    public static final int QUICK_MOVE_TO_TOOLKIT = 1 << 2;
    private static final boolean DEBUG_ENCODE = Boolean.getBoolean("wcwt.debug.encode");
    private static final boolean DEBUG_ADVANCED = Boolean.getBoolean("wcwt.debug.advanced");
    private static final String DEFAULT_CRAFTING_PROVIDER_SEARCH_KEY = "crafting";
    private static final Gson GSON = new Gson();

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
    private int syncedPatternEncodingMode = EncodingMode.PROCESSING.ordinal();
    private boolean syncedLinkConnected;
    private int syncedLinkStatusCode = -1;
    private final List<RecipeHolder<StonecutterRecipe>> stonecuttingRecipes = new ArrayList<>();

    /** 处理样板：合并相同输入材料（JEI 编码与手动编辑时行为对齐 AE2 EncodingHelper）。 */
    private boolean processingMaterialsMerge;

    /**
     * 防止合并写入触发 {@link #broadcastChanges()} 时递归合并。
     */
    private boolean wcwtMergeProcessingGuard;
    /**
     * 刷新样板预览槽时会写回 {@link #patternPreviewSlot}，该写入本身也会触发 slot change 回调。
     * 用这个守卫避免 updatePatternPreview -> onSlotChange -> updatePatternPreview 的递归卡死。
     */
    private boolean patternPreviewUpdateGuard;
    private static final Object EMPTY_PATTERN_PREVIEW_KEY = new Object();
    private EncodingMode cachedPatternPreviewMode;
    private List<Object> cachedPatternPreviewSignature = List.of();
    /**
     * 样板管理区不是实时容器，而是服务端构建的供应器快照。
     * 打开界面期间定期推送一次，避免只能重开终端才能看到外部变化。
     */
    private int patternProviderSyncCooldown;
    private long patternProviderSyncSubscriptionUntilTick;
    private long lastPatternProviderRequestTick = Long.MIN_VALUE;
    private long lastInventorySyncTick = Long.MIN_VALUE;
    private final ManualSmithingMenuBridge manualSmithingBridge;
    private final ManualAnvilMenuBridge manualAnvilBridge;
    private int manualAnvilCost;
    private String manualAnvilName = "";
    private int syncedManualWorkspaceMode = ManualWorkspaceMode.CRAFTING.ordinal();
    private int syncedManualAnvilCost;
    private int manualQuickCraftSlotIndex = -1;
    private int manualQuickCraftsRemaining;
    private int clientQuickMoveOptions = QUICK_MOVE_TO_COSMETIC_ARMOR
            | QUICK_MOVE_TO_CARD_BOX
            | QUICK_MOVE_TO_TOOLKIT;

    public WirelessComprehensiveWorkTerminalMenu(int id, Inventory ip, WirelessComprehensiveWorkTerminalMenuHost host) {
        super(com.lhy.wcwt.init.ModMenus.WCWT_MENU.get(), id, ip, host, false);
        this.menuHost = host;
        this.patternEncodingLogic = host.getLogic();
        this.manualSmithingBridge = new ManualSmithingMenuBridge();
        this.manualAnvilBridge = new ManualAnvilMenuBridge();
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
        var cellUpgradeSupplier = new appeng.util.inv.SupplierInternalInventory(host::getCellUpgrades);
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
                var slot = new PatternCacheSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
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
            @Override
            public int get() {
                return menuHost != null && menuHost.isCurrentLinkConnected() ? 1 : 0;
            }

            @Override
            public void set(int v) {
                syncedLinkConnected = v != 0;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return menuHost != null ? menuHost.getCurrentLinkStatusCode() : -1;
            }

            @Override
            public void set(int v) {
                syncedLinkStatusCode = v;
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
                return manualAnvilCost;
            }

            @Override
            public void set(int value) {
                syncedManualAnvilCost = value;
            }
        });
        registerClientAction(TOP_ACTION, TopActionPacket.Action.class, this::handleTopAction);
        registerClientAction(ACTION_CLEAR_MANUAL_TO_PLAYER, this::clearManualWorkspaceToPlayerInventory);
        registerClientAction(ACTION_SYNC_QUICK_MOVE_OPTIONS, Integer.class, this::setClientQuickMoveOptions);

        // 创建玩家物品栏槽位
        this.createPlayerInventorySlots(ip);
        if (menuHost != null) {
            manualAnvilName = menuHost.getManualAnvilName();
            syncedManualWorkspaceMode = menuHost.getManualWorkspaceMode();
        }
        applyManualWorkspaceSlotActivation(ManualWorkspaceMode.fromOrdinal(syncedManualWorkspaceMode));
        updateManualWorkspaceResults();
    }

    public void syncQuickMoveOptionsFromClient(int options) {
        clientQuickMoveOptions = options;
        sendClientAction(ACTION_SYNC_QUICK_MOVE_OPTIONS, options);
    }

    private void setClientQuickMoveOptions(Integer options) {
        clientQuickMoveOptions = options == null ? 0 : options;
    }

    @Override
    protected void handleNetworkInteraction(ServerPlayer player, @Nullable AEKey clickedKey, InventoryAction action) {
        if (clickedKey instanceof AEItemKey clickedItem && getCarried().isEmpty()) {
            if (action == InventoryAction.SHIFT_CLICK) {
                if (tryMoveNetworkStackToOpenCardBox(clickedItem)) {
                    return;
                }
                moveOneNetworkStackToMainPlayerInventory(clickedItem);
                return;
            }
            if (action == InventoryAction.MOVE_REGION) {
                int playerInvSlots = player.getInventory().items.size();
                for (int slotNum = 0; slotNum < playerInvSlots; ++slotNum) {
                    if (!moveOneNetworkStackToMainPlayerInventory(clickedItem)) {
                        break;
                    }
                }
                return;
            }
        }
        super.handleNetworkInteraction(player, clickedKey, action);
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
        public void setByPlayer(ItemStack newStack) {
            ItemStack oldStack = getItem().copy();
            player.onEquipItem(equipmentSlot, oldStack, newStack);
            super.setByPlayer(newStack);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            return (stack.isEmpty()
                    || player.isCreative()
                    || !stack.isEnchanted())
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

    private static class OffhandSlot extends Slot implements WcwtActivatableSlot {
        private boolean active = true;

        OffhandSlot(Container inventory, int slot) {
            super(inventory, slot, 0, 0);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
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

    private final class ManualWorkspaceAppEngSlot extends AppEngSlot {
        private final java.util.function.Predicate<ItemStack> mayPlacePredicate;
        private ItemStack clientDisplayStack = ItemStack.EMPTY;

        private ManualWorkspaceAppEngSlot(InternalInventory inventory, int invSlot,
                                          java.util.function.Predicate<ItemStack> mayPlacePredicate) {
            super(inventory, invSlot);
            this.mayPlacePredicate = mayPlacePredicate;
            if (WirelessComprehensiveWorkTerminalMenu.this.isClientSide()) {
                this.clientDisplayStack = super.getItem().copy();
            }
        }

        @Override
        public ItemStack getItem() {
            if (WirelessComprehensiveWorkTerminalMenu.this.isClientSide()) {
                return clientDisplayStack;
            }
            return super.getItem();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack) && mayPlacePredicate.test(stack);
        }

        @Override
        public void set(ItemStack stack) {
            if (WirelessComprehensiveWorkTerminalMenu.this.isClientSide()) {
                clientDisplayStack = stack.copy();
                return;
            }
            super.set(stack);
            notifyManualWorkspaceChanged();
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            super.setByPlayer(stack);
            notifyManualWorkspaceChanged();
        }

        @Override
        public void clearStack() {
            if (WirelessComprehensiveWorkTerminalMenu.this.isClientSide()) {
                clientDisplayStack = ItemStack.EMPTY;
                return;
            }
            super.clearStack();
            notifyManualWorkspaceChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            if (WirelessComprehensiveWorkTerminalMenu.this.isClientSide()) {
                ItemStack removed = clientDisplayStack.split(amount);
                if (clientDisplayStack.isEmpty()) {
                    clientDisplayStack = ItemStack.EMPTY;
                }
                return removed;
            }
            ItemStack removed = super.remove(amount);
            if (!removed.isEmpty()) {
                setChanged();
                notifyManualWorkspaceChanged();
            }
            return removed;
        }

        private void notifyManualWorkspaceChanged() {
            if (WirelessComprehensiveWorkTerminalMenu.this.isClientSide()) {
                return;
            }
            WirelessComprehensiveWorkTerminalMenu.this.syncManualWorkspaceChanges();
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
            manualSmithingBridge.takeResult(player, stack, true);
            syncManualWorkspaceChanges();
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
            manualAnvilBridge.takeResult(player, stack, true);
            syncManualWorkspaceChanges();
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
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.createResult();
        }

        private boolean mayPickupResult(Player player) {
            return this.getSlot(3).mayPickup(player);
        }

        private void takeResult(Player player, ItemStack stack, boolean restockInputs) {
            ItemStack oneResult = stack.copyWithCount(1);
            var inv = menuHost == null ? null
                    : menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
            ItemStack[] before = new ItemStack[3];
            if (inv != null) {
                for (int i = 0; i < before.length; i++) {
                    before[i] = inv.getStackInSlot(i).copy();
                }
            }
            var resultSlot = this.getSlot(3);
            this.resultSlots.setItem(0, oneResult.copy());
            resultSlot.remove(oneResult.getCount());
            resultSlot.onTake(player, oneResult);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                    player.level().random.nextFloat() * 0.1F + 0.9F);
            if (inv != null) {
                for (int i = 0; i < 3; i++) {
                    inv.setItemDirect(i, this.inputSlots.getItem(i).copy());
                }
                if (restockInputs) {
                    restockManualInputs(inv, before);
                }
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
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.createResult();
        }

        private ResultContainer getResultContainer() {
            return this.resultSlots;
        }

        private boolean mayPickupResult(Player player) {
            return this.getSlot(2).mayPickup(player);
        }

        private void takeResult(Player player, ItemStack stack, boolean restockInputs) {
            ItemStack oneResult = stack.copyWithCount(1);
            var inv = menuHost == null ? null
                    : menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
            ItemStack[] before = new ItemStack[2];
            if (inv != null) {
                before[0] = inv.getStackInSlot(0).copy();
                before[1] = inv.getStackInSlot(1).copy();
            }
            var resultSlot = this.getSlot(2);
            this.resultSlots.setItem(0, oneResult.copy());
            resultSlot.remove(oneResult.getCount());
            resultSlot.onTake(player, oneResult);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F,
                    player.level().random.nextFloat() * 0.1F + 0.9F);
            if (inv != null) {
                inv.setItemDirect(0, this.inputSlots.getItem(0).copy());
                inv.setItemDirect(1, this.inputSlots.getItem(1).copy());
                if (restockInputs) {
                    restockManualInputs(inv, before);
                }
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

    public record ClientLinkStatus(boolean connected, @Nullable Component statusDescription) {
    }

    public record ClientKeyTypeSelection(Map<AEKeyType, Boolean> keyTypes) {
        public List<AEKeyType> enabledSet() {
            return keyTypes.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .toList();
        }
    }

    public ClientLinkStatus getLinkStatus() {
        if (isClientSide()) {
            return new ClientLinkStatus(syncedLinkConnected, decodeSyncedLinkStatusDescription(syncedLinkStatusCode));
        }
        return new ClientLinkStatus(
                menuHost != null && menuHost.isCurrentLinkConnected(),
                menuHost != null ? menuHost.getCurrentLinkStatusDescription() : null);
    }

    @Nullable
    private static Component decodeSyncedLinkStatusDescription(int code) {
        return switch (code) {
            case 1 -> TextConstants.NETWORK_NOT_POWERED;
            case 2 -> TextConstants.NO_QNB_UPGRADE;
            case 3 -> TextConstants.NO_QNB;
            case 4 -> TextConstants.DIFFERENT_NETWORKS;
            case 5 -> TextConstants.SINGULARITY_NOT_PRESENT;
            case 6 -> PlayerMessages.OutOfRange.text();
            default -> null;
        };
    }

    public ClientKeyTypeSelection getClientKeyTypeSelection() {
        var keyTypes = new LinkedHashMap<AEKeyType, Boolean>();
        keyTypes.put(AEKeyType.items(), true);
        keyTypes.put(AEKeyType.fluids(), true);
        return new ClientKeyTypeSelection(keyTypes);
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
        syncManualWorkspaceChanges();
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

    private boolean isLinked() {
        return menuHost != null && menuHost.getActionableNode() != null && menuHost.getActionableNode().getGrid() != null;
    }

    private appeng.api.networking.energy.IEnergySource getWcwtEnergySource() {
        return this.powerSource;
    }

    private static boolean isBlankPattern(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(AEItems.BLANK_PATTERN.asItem());
    }

    private static boolean isSameStack(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    private CraftingContainer createCraftingInput(ItemStack[] ingredients) {
        var container = new TransientCraftingContainer(this, 3, 3);
        for (int i = 0; i < ingredients.length; i++) {
            container.setItem(i, ingredients[i]);
        }
        return container;
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
        if (clientRepo != null && isLinked()) {
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
            ModNetworking.sendToServer(new PatternEncodingModePacket(mode));
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
        super.slotsChanged(inventory);
        updateManualWorkspaceResults();
        if (requiresImmediateInventorySync(inventory)) {
            forceInventorySyncOnNextBroadcast();
        }
    }

    private boolean requiresImmediateInventorySync(Container inventory) {
        if (menuHost == null || inventory == null) {
            return false;
        }
        return inventory == menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING)
                || inventory == menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL)
                || inventory == menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_PATTERN_CACHE);
    }

    private void updateManualWorkspaceResults() {
        updateManualSmithingResult();
        updateManualAnvilResult();
    }

    private void syncManualWorkspaceChanges() {
        updateManualWorkspaceResults();
        forceInventorySyncOnNextBroadcast();
        broadcastChanges();
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

    private void updateManualSmithingResult() {
        if (menuHost == null) {
            return;
        }
        var inv = menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
        if (inv == null) {
            return;
        }
        manualSmithingBridge.syncFrom(inv);
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
        String filtered = net.minecraft.SharedConstants.filterText(name);
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

        clearPatternEncodingOutputSlot();

        patternEncodingLogic.getEncodedInputInv().clear();
        patternEncodingLogic.getEncodedOutputInv().clear();
        updatePatternPreview(getPatternEncodingMode());
        broadcastChanges();
    }

    private void clearPatternEncodingOutputSlot() {
        if (encodedPatternSlot == null) {
            return;
        }
        var outputStack = encodedPatternSlot.getItem();
        if (outputStack.isEmpty()
                || (!PatternDetailsHelper.isEncodedPattern(outputStack) && !isBlankPattern(outputStack))) {
            return;
        }

        if (isClientSide()) {
            encodedPatternSlot.set(ItemStack.EMPTY);
            return;
        }

        ItemStack remainingBlanks = returnBlankPatternToBlankSlotOrNetwork(outputStack.getCount());
        encodedPatternSlot.set(remainingBlanks);
        if (remainingBlanks.isEmpty()) {
            forceInventorySyncOnNextBroadcast();
        }
    }

    private ItemStack returnBlankPatternToBlankSlotOrNetwork(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack remainingBlanks = AEItems.BLANK_PATTERN.stack(count);
        if (blankPatternSlot != null && blankPatternSlot.mayPlace(remainingBlanks)) {
            remainingBlanks = blankPatternSlot.safeInsert(remainingBlanks);
            if (remainingBlanks.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        try {
            var blankKey = AEItemKey.of(AEItems.BLANK_PATTERN.asItem());
            long inserted = StorageHelper.poweredInsert(getWcwtEnergySource(), storage, blankKey,
                    remainingBlanks.getCount(), getActionSource());
            if (inserted > 0) {
                remainingBlanks.shrink((int) Math.min(inserted, remainingBlanks.getCount()));
            }
        } catch (Exception ignored) {
        }
        return remainingBlanks;
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
                ModNetworking.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
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
        boolean didInventorySync = shouldRunInventorySync();
        if (DEBUG_BLANK_PATTERN_SYNC && isServerSide()) {
            logBlankPatternSync("broadcast.preSuper",
                    "didInventorySync={}, lastInventorySyncTick={}, gameTick={}, blankSlot={}, encodedSlot={}, linkStatus={}",
                    didInventorySync,
                    lastInventorySyncTick,
                    getPlayer() != null ? getPlayer().level().getGameTime() : -1L,
                    summarizeItem(blankPatternSlot != null ? blankPatternSlot.getItem() : ItemStack.EMPTY),
                    summarizeItem(encodedPatternSlot != null ? encodedPatternSlot.getItem() : ItemStack.EMPTY),
                    menuHost != null ? menuHost.getCurrentLinkStatusDescription() : null);
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
        if (!subscribe || !WcwtServerConfig.patternProviderActiveRefresh()) {
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
                                                                       boolean requireAutoUploadUniqueMatch) {
        String query = resolveUploadSearchTextFromPattern(encodedPattern, searchText);
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
        if (requireAutoUploadUniqueMatch && !isEaepAutoUploadUniqueMatchEnabled()) {
            return UploadAttemptResult.NO_TARGET;
        }

        String targetName = matchingGroupNames.get(0);
        var candidateTargets = matchingTargets.stream()
                .filter(target -> targetName.equals(target.providerName()))
                .toList();
        ItemStack uploadStack = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);
        for (var target : candidateTargets) {
            if (insertEncodedPattern(target.provider(), uploadStack)) {
                int insertedSlot = findLastInsertedPatternSlot(target.provider(), uploadStack);
                return new UploadAttemptResult(true, true, targetName, target.providerId(), insertedSlot);
            }
        }
        return new UploadAttemptResult(false, true, targetName, candidateTargets.get(0).providerId(), -1);
    }

    private UploadAttemptResult uploadEncodedPatternToPreferredProvider(ItemStack encodedPattern, long providerId,
                                                                        @Nullable String providerName) {
        if (providerId <= 0 || encodedPattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return UploadAttemptResult.NO_TARGET;
        }

        var providers = listUploadProviders(false);
        int index = (int) providerId - 1;
        if (index < 0 || index >= providers.size()) {
            return UploadAttemptResult.NO_TARGET;
        }

        PatternContainer targetProvider = providers.get(index);
        String targetName = normalizeProviderSearchText(providerName);
        if (targetName == null) {
            targetName = targetProvider.getTerminalGroup().name().getString();
        }
        var targetGroup = targetProvider.getTerminalGroup();
        var candidateTargets = new ArrayList<ProviderTarget>();
        candidateTargets.add(new ProviderTarget(providerId, targetProvider, targetName));
        for (int i = 0; i < providers.size(); i++) {
            if (i == index) {
                continue;
            }
            PatternContainer provider = providers.get(i);
            if (Objects.equals(targetGroup, provider.getTerminalGroup())) {
                candidateTargets.add(new ProviderTarget(i + 1L, provider, targetName));
            }
        }

        ItemStack uploadStack = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);
        for (var target : candidateTargets) {
            if (insertEncodedPattern(target.provider(), uploadStack)) {
                int insertedSlot = findLastInsertedPatternSlot(target.provider(), uploadStack);
                return new UploadAttemptResult(true, true, targetName, target.providerId(), insertedSlot);
            }
        }
        return new UploadAttemptResult(false, true, targetName, providerId, -1);
    }

    private static boolean isEaepAutoUploadUniqueMatchEnabled() {
        if (!ModList.get().isLoaded("extendedae_plus")) {
            return true;
        }
        try {
            Path cfgPath = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                    .resolve("extendedae_plus/pinned_providers.json");
            if (!Files.exists(cfgPath)) {
                return true;
            }
            JsonObject obj = GSON.fromJson(Files.readString(cfgPath), JsonObject.class);
            if (obj == null) {
                return true;
            }
            JsonElement element = obj.get("auto_upload_unique_match");
            return element == null || !element.isJsonPrimitive() || element.getAsBoolean();
        } catch (Exception ignored) {
            return true;
        }
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
        private static final MatrixUploadResult FAILURE = new MatrixUploadResult(MatrixUploadState.FAILURE, -1, -1);

        private static MatrixUploadResult uploaded(long providerId, int slot) {
            return new MatrixUploadResult(MatrixUploadState.UPLOADED, providerId, slot);
        }
    }

    private enum MatrixUploadState {
        UPLOADED,
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
        return insertEncodedPatternsOnePerSlot(inv, encodedPattern) > 0;
    }

    private static int insertEncodedPatternsOnePerSlot(InternalInventory inv, ItemStack encodedPattern) {
        if (encodedPattern.isEmpty() || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return 0;
        }
        int inserted = 0;
        for (int slot = 0; slot < inv.size() && inserted < encodedPattern.getCount(); slot++) {
            if (!inv.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            ItemStack single = encodedPattern.copy();
            single.setCount(1);
            if (!new EncodedPatternFilter().allowInsert(inv, slot, single)) {
                continue;
            }
            ItemStack remainder = inv.insertItem(slot, single, false);
            if (remainder.isEmpty()) {
                inserted++;
            }
        }
        return inserted;
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

        patternPreviewSlot = new PatternTermSlot(getPlayer(), getActionSource(), this.powerSource,
                getHost().getInventory(), encodedInputs, this);
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
        syncPatternCacheClientDisplayStack(slotID, stack);
        updatePatternPreview(getPatternEncodingMode());
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        if (isClientSide()) {
            for (Slot slot : getPatternCacheSlots()) {
                int menuSlot = slots.indexOf(slot);
                if (menuSlot >= 0 && menuSlot < items.size()) {
                    syncPatternCacheClientDisplayStack(menuSlot, items.get(menuSlot));
                }
            }
        }
        updatePatternPreview(getPatternEncodingMode());
    }

    private void syncPatternCacheClientDisplayStack(int slotID, ItemStack stack) {
        if (isClientSide()
                && slotID >= 0
                && slotID < slots.size()
                && slots.get(slotID) instanceof PatternCacheSlot cacheSlot) {
            cacheSlot.setClientDisplayStack(stack);
        }
    }

    public void encodePattern(EncodingMode mode) {
        encodePattern(mode, false, "", false);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText) {
        encodePattern(mode, uploadEnabled, providerSearchText, false);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              boolean fallbackToEditSlot) {
        encodePattern(mode, uploadEnabled, providerSearchText, fallbackToEditSlot, -1L, "");
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              boolean fallbackToEditSlot, long preferredProviderId,
                              @Nullable String preferredProviderName) {
        encodePattern(mode, uploadEnabled, providerSearchText, fallbackToEditSlot, preferredProviderId,
                preferredProviderName, false);
    }

    public void encodePattern(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                              boolean fallbackToEditSlot, long preferredProviderId,
                              @Nullable String preferredProviderName,
                              boolean useEaepUploadScreen) {
        if (isClientSide()) {
            logEncode("client clicked encode, mode={}", mode);
            ModNetworking.sendToServer(new EncodePatternPacket(mode, uploadEnabled, providerSearchText,
                    fallbackToEditSlot, preferredProviderId, preferredProviderName == null ? "" : preferredProviderName,
                    useEaepUploadScreen));
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
        stampEncodedPatternPlayer(encodedPattern);

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
        String resolvedProviderSearchText = resolvePatternUploadSearchText(mode, providerSearchText, recipeId);
        writePatternUploadMetadata(encodedPattern, resolvedProviderSearchText);

        if (uploadEnabled && useEaepUploadScreen) {
            openEaepProviderSelectScreenForEncodedPattern(mode, encodedPattern, consumeEditPattern,
                    resolvedProviderSearchText);
            return;
        }

        UploadAttemptResult uploadAttempt = UploadAttemptResult.NO_TARGET;
        if (uploadEnabled && mode == EncodingMode.PROCESSING && preferredProviderId > 0) {
            uploadAttempt = uploadEncodedPatternToPreferredProvider(encodedPattern, preferredProviderId,
                    preferredProviderName);
            if (completeProviderUploadIfDone(mode, encodedPattern, consumeEditPattern, resolvedProviderSearchText,
                    uploadAttempt)) {
                return;
            }
        }

        if (uploadEnabled && mode != EncodingMode.PROCESSING) {
            MatrixUploadResult matrixUploadResult = uploadEncodedPatternToMatrix(encodedPattern);
            if (matrixUploadResult.state() == MatrixUploadState.UPLOADED) {
                consumePatternForUpload(consumeEditPattern);
                patternEncodingLogic.setMode(mode);
                syncedPatternEncodingMode = mode.ordinal();
                updatePatternPreview(mode);
                tryFillBlankPatternFromNetwork();
                if (getPlayer() instanceof ServerPlayer serverPlayer) {
                    ModNetworking.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
                    if (matrixUploadResult.providerId() > 0 && matrixUploadResult.slot() >= 0) {
                        ModNetworking.sendToPlayer(serverPlayer,
                                new PatternProviderFocusPacket(matrixUploadResult.providerId(), matrixUploadResult.slot()));
                    }
                }
                logEncode("matrix upload result={}, mode={}, encoded={}",
                        matrixUploadResult.state(), mode, encodedPattern);
                broadcastChanges();
                return;
            }
        }

        if (uploadEnabled && mode == EncodingMode.PROCESSING) {
            if (uploadAttempt == UploadAttemptResult.NO_TARGET) {
                uploadAttempt = uploadEncodedPatternToMatchingProvider(encodedPattern, resolvedProviderSearchText, false);
            }
            if (completeProviderUploadIfDone(mode, encodedPattern, consumeEditPattern, resolvedProviderSearchText,
                    uploadAttempt)) {
                return;
            }
        }
        if (uploadEnabled && mode != EncodingMode.PROCESSING && preferredProviderId > 0
                && uploadAttempt == UploadAttemptResult.NO_TARGET) {
            uploadAttempt = uploadEncodedPatternToPreferredProvider(encodedPattern, preferredProviderId,
                    preferredProviderName);
            if (completeProviderUploadIfDone(mode, encodedPattern, consumeEditPattern, resolvedProviderSearchText,
                    uploadAttempt)) {
                return;
            }
        }
        if (uploadEnabled && mode != EncodingMode.PROCESSING && uploadAttempt == UploadAttemptResult.NO_TARGET) {
            uploadAttempt = uploadEncodedPatternToMatchingProvider(encodedPattern, resolvedProviderSearchText, true);
            if (completeProviderUploadIfDone(mode, encodedPattern, consumeEditPattern, resolvedProviderSearchText,
                    uploadAttempt)) {
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

    private boolean completeProviderUploadIfDone(EncodingMode mode,
                                                 ItemStack encodedPattern,
                                                 boolean consumeEditPattern,
                                                 @Nullable String resolvedProviderSearchText,
                                                 UploadAttemptResult uploadAttempt) {
        if (!uploadAttempt.uploaded()) {
            return false;
        }
        consumePatternForUpload(consumeEditPattern);
        patternEncodingLogic.setMode(mode);
        syncedPatternEncodingMode = mode.ordinal();
        updatePatternPreview(mode);
        tryFillBlankPatternFromNetwork();
        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "extendedae_plus.screen.upload.auto_upload_success", uploadAttempt.providerName()));
            ModNetworking.sendToPlayer(serverPlayer, PatternProviderListPacket.buildForPlayer(serverPlayer));
            if (uploadAttempt.providerId() > 0 && uploadAttempt.slot() >= 0) {
                ModNetworking.sendToPlayer(serverPlayer,
                        new PatternProviderFocusPacket(uploadAttempt.providerId(), uploadAttempt.slot()));
            }
        }
        logEncode("uploaded mode={}, search={}, provider={}, encoded={}",
                mode, resolvedProviderSearchText, uploadAttempt.providerName(), encodedPattern);
        broadcastChanges();
        return true;
    }

    private boolean openEaepProviderSelectScreenForEncodedPattern(EncodingMode mode,
                                                                  ItemStack encodedPattern,
                                                                  boolean consumeEditPattern,
                                                                  @Nullable String resolvedProviderSearchText) {
        if (!ModList.get().isLoaded("extendedae_plus") || !(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        List<PatternContainer> providers = ProviderUploadUtil.listAvailableProvidersFromPlayerNetwork(serverPlayer);
        if (providers.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.wcwt.eaep_provider_missing"), true);
            return false;
        }

        var entries = new ArrayList<OpenEaepProviderSelectScreenPacket.Entry>(providers.size());
        for (int i = 0; i < providers.size(); i++) {
            PatternContainer provider = providers.get(i);
            int emptySlots = getAvailablePatternSlots(provider);
            if (emptySlots <= 0) {
                continue;
            }
            entries.add(new OpenEaepProviderSelectScreenPacket.Entry(
                    -1L - i,
                    getUploadProviderDisplayName(provider),
                    emptySlots));
        }
        if (entries.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.wcwt.eaep_provider_missing"), true);
            return false;
        }

        ItemStack pendingPattern = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);
        if (ProviderUploadUtil.beginPendingCtrlQUpload(serverPlayer, pendingPattern) == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.wcwt.eaep_pending_failed"), true);
            return false;
        }

        String searchText = normalizeProviderSearchText(resolvedProviderSearchText);
        if (searchText != null) {
            RecipeTypeNameConfig.setLastProviderSearchKey(searchText);
        } else if (mode != EncodingMode.PROCESSING) {
            RecipeTypeNameConfig.presetCraftingProviderSearchKey();
        }

        consumePatternForUpload(consumeEditPattern);
        patternEncodingLogic.setMode(mode);
        syncedPatternEncodingMode = mode.ordinal();
        updatePatternPreview(mode);
        tryFillBlankPatternFromNetwork();
        ModNetworking.sendToPlayer(serverPlayer, new OpenEaepProviderSelectScreenPacket(entries));
        broadcastChanges();
        return true;
    }

    private boolean hasBlankPatternForEncoding() {
        return isBlankPattern(blankPatternSlot.getItem());
    }

    private void writePatternUploadMetadata(ItemStack encodedPattern,
                                            @Nullable String providerSearchText) {
        PatternUploadMetadata.write(encodedPattern, providerSearchText);
    }

    private void stampEncodedPatternPlayer(ItemStack encodedPattern) {
        if (!encodedPattern.isEmpty() && getPlayer() instanceof ServerPlayer serverPlayer) {
            encodedPattern.getOrCreateTag().putString("encodePlayer", serverPlayer.getName().getString());
        }
    }

    private void writePatternCacheSlot(Slot slot, ItemStack encodedPattern) {
        if (encodedPattern.isEmpty()) {
            return;
        }
        stampEncodedPatternPlayer(encodedPattern);
        slot.set(encodedPattern);
        slot.setChanged();
        forceCachePatternSync();
    }

    @Nullable
    private String resolvePatternUploadSearchText(EncodingMode mode,
                                                  @Nullable String providerSearchText,
                                                  @Nullable ResourceLocation recipeId) {
        String normalized = normalizeProviderSearchText(providerSearchText);
        if (normalized != null) {
            return normalized;
        }
        String fromRecipe = recipeId != null ? resolveEaepProviderSearchKey(recipeId.toString()) : null;
        if (fromRecipe != null) {
            return fromRecipe;
        }
        if (mode != EncodingMode.PROCESSING) {
            return resolveEaepProviderSearchKey(DEFAULT_CRAFTING_PROVIDER_SEARCH_KEY);
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
        var input = createCraftingInput(ingredients);
        var level = getPlayer().level();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        return recipe != null ? recipe.getId() : null;
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
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).orElse(null);
        return recipe != null ? recipe.getId() : null;
    }

    @Nullable
    private static String normalizeProviderSearchText(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    private static String resolveEaepProviderSearchKey(@Nullable String rawKey) {
        String normalized = normalizeProviderSearchText(rawKey);
        if (normalized == null) {
            return null;
        }
        if (!ModList.get().isLoaded("extendedae_plus")) {
            return normalized;
        }
        String value = RecipeTypeNameConfig.resolveSearchKeyAlias(normalized);
        return value != null ? normalizeProviderSearchText(value) : normalized;
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
            long extracted = StorageHelper.poweredExtraction(getWcwtEnergySource(), storage, blankKey, space, getActionSource());
            if (extracted <= 0) {
                if (DEBUG_BLANK_PATTERN_SYNC) {
                    logBlankPatternSync("autofill.skip_no_extract",
                            "space={}, current={}, linkStatus={}",
                            space,
                            summarizeItem(current),
                            menuHost != null ? menuHost.getCurrentLinkStatusDescription() : null);
                }
                return;
            }

            int toInsert = (int) Math.min(extracted, space);
            ItemStack slotStack = blankPatternSlot.getItem();
            if (slotStack.isEmpty()) {
                blankPatternSlot.set(AEItems.BLANK_PATTERN.stack(toInsert));
            } else if (isBlankPattern(slotStack)) {
                slotStack.grow(toInsert);
                blankPatternSlot.set(slotStack);
            } else {
                StorageHelper.poweredInsert(getWcwtEnergySource(), storage, blankKey, extracted, getActionSource());
                return;
            }

            long leftover = extracted - toInsert;
            if (leftover > 0) {
                StorageHelper.poweredInsert(getWcwtEnergySource(), storage, blankKey, leftover, getActionSource());
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
                        menuHost != null ? menuHost.getCurrentLinkStatusDescription() : null);
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

    /**
     * 样板缓存区的底层库存是 {@link appeng.util.inv.SupplierInternalInventory}，每次访问都会用
     * {@code createInventory(...)} 从终端物品 NBT 现读一份全新的 {@link AppEngInternalInventory}。
     * 服务端因为会把改动写回 NBT，所以重开界面能看到；但客户端收到 slot 同步包时
     * {@code set()} 只是写进一份临时库存，紧接着 {@code getItem()} 又从 NBT 重建，导致实时显示丢失。
     *
     * 这里参考手动工作区 {@link ManualWorkspaceAppEngSlot} 的做法：客户端单独维护
     * {@code clientDisplayStack}，让 {@code getItem()/set()} 走它，从而即时刷新显示。
     */
    private class PatternCacheSlot extends RestrictedInputSlot {
        private ItemStack clientDisplayStack = ItemStack.EMPTY;

        PatternCacheSlot(PlacableItemType which, InternalInventory inv, int invSlot) {
            super(which, inv, invSlot);
            if (isClientSide()) {
                this.clientDisplayStack = super.getItem().copy();
            }
        }

        private void setClientDisplayStack(ItemStack stack) {
            if (isClientSide()) {
                this.clientDisplayStack = stack.copy();
            }
        }

        @Override
        public ItemStack getItem() {
            if (isClientSide()) {
                return clientDisplayStack;
            }
            return super.getItem();
        }

        @Override
        public void set(ItemStack stack) {
            if (isClientSide()) {
                clientDisplayStack = stack.copy();
                return;
            }
            super.set(stack);
            forceCachePatternSync();
        }

        @Override
        public void clearStack() {
            if (isClientSide()) {
                clientDisplayStack = ItemStack.EMPTY;
                return;
            }
            super.clearStack();
            forceCachePatternSync();
        }

        @Override
        public ItemStack remove(int amount) {
            if (isClientSide()) {
                ItemStack removed = clientDisplayStack.split(amount);
                if (clientDisplayStack.isEmpty()) {
                    clientDisplayStack = ItemStack.EMPTY;
                }
                return removed;
            }
            // AE2 AppEngSlot.remove() 不会触发 setChanged/onSlotChange，
            // 而 shift 双击（PICKUP_ALL）的批量取出全部走 remove()，
            // 不强制同步就会被节流的 broadcastChanges 拖到重开界面才刷新。
            ItemStack removed = super.remove(amount);
            if (!removed.isEmpty()) {
                forceCachePatternSync();
            }
            return removed;
        }
    }

    /** 样板缓存槽内容在服务端变化后，强制下一次完整同步，避免客户端要重开界面才刷新。 */
    private void forceCachePatternSync() {
        if (isServerSide()) {
            forceInventorySyncOnNextBroadcast();
        }
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
        if (isClientSide()) {
            ModNetworking.sendToServer(new TopActionPacket(TopActionPacket.Action.CLEAR_MANUAL_WORKSPACE));
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
        if (menuHost == null) {
            return;
        }
        var target = getCurrentManualWorkspaceInputInventory();
        if (target == null) {
            return;
        }
        var storage = menuHost.getInventory();
        var energy = getWcwtEnergySource();
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
            if (inserted > 0) {
                ItemStack remaining = stack.copy();
                remaining.shrink((int) Math.min(inserted, Integer.MAX_VALUE));
                target.setItemDirect(i, remaining);
            }
        }
        syncManualWorkspaceChanges();
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
        syncManualWorkspaceChanges();
    }

    private void restockManualInputs(InternalInventory inventory, ItemStack[] before) {
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
            if (!current.isEmpty() && !isSameStack(previous, current)) {
                continue;
            }
            int missing = previous.getCount() - current.getCount();
            if (missing <= 0) {
                continue;
            }
            AEItemKey key = AEItemKey.of(previous);
            if (key == null || filter != null && !filter.isListed(key)) {
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(getWcwtEnergySource(), storage, key, missing, getActionSource());
            if (extracted <= 0) {
                continue;
            }
            ItemStack remainder = inventory.insertItem(slot, key.toStack((int) Math.min(extracted, Integer.MAX_VALUE)),
                    false);
            if (!remainder.isEmpty()) {
                StorageHelper.poweredInsert(getWcwtEnergySource(), storage, key, remainder.getCount(), getActionSource());
            }
        }
    }

    @Nullable
    private InternalInventory getCurrentManualWorkspaceInputInventory() {
        if (menuHost == null) {
            return null;
        }
        return switch (getManualWorkspaceMode()) {
            case CRAFTING -> getCraftingMatrix();
            case SMITHING -> menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
            case ANVIL -> menuHost.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
        };
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
            ModNetworking.sendToServer(new TopActionPacket(TopActionPacket.Action.OPEN_MAGNET_MENU));
            return;
        }
        openWcwtSubMenu(ModMenus.WCWT_MAGNET_MENU.get());
    }

    public void openWcwtTrashMenu() {
        if (isClientSide()) {
            ModNetworking.sendToServer(new TopActionPacket(TopActionPacket.Action.OPEN_TRASH_MENU));
            return;
        }
        openWcwtSubMenu(ModMenus.WCWT_TRASH_MENU.get());
    }

    public void updatePatternPreview(EncodingMode mode) {
        if (patternPreviewSlot == null || patternEncodingLogic == null || patternPreviewUpdateGuard) {
            return;
        }
        patternPreviewUpdateGuard = true;
        try {
            List<Object> signature = getPatternPreviewSignature(mode);
            ItemStack preview;
            if (mode == cachedPatternPreviewMode && signature.equals(cachedPatternPreviewSignature)) {
                patternPreviewSlot.setActive(mode != EncodingMode.PROCESSING);
                return;
            } else {
                preview = switch (mode) {
                    case CRAFTING -> getCraftingPatternPreview();
                    case SMITHING_TABLE -> getSmithingPatternPreview();
                    case STONECUTTING -> getStonecuttingPatternPreview();
                    case PROCESSING -> ItemStack.EMPTY;
                };
                cachedPatternPreviewMode = mode;
                cachedPatternPreviewSignature = signature;
            }
            if (!ItemStack.matches(patternPreviewSlot.getItem(), preview)) {
                patternPreviewSlot.set(preview);
            }
            patternPreviewSlot.setActive(mode != EncodingMode.PROCESSING);
        } finally {
            patternPreviewUpdateGuard = false;
        }
    }

    private List<Object> getPatternPreviewSignature(EncodingMode mode) {
        if (patternEncodingLogic == null) {
            return List.of();
        }

        var inputs = patternEncodingLogic.getEncodedInputInv();
        return switch (mode) {
            case CRAFTING -> {
                List<Object> signature = new ArrayList<>(9);
                for (int slot = 0; slot < 9; slot++) {
                    signature.add(getPatternPreviewSignatureKey(inputs.getStack(slot)));
                }
                yield signature;
            }
            case SMITHING_TABLE -> {
                List<Object> signature = new ArrayList<>(3);
                for (int slot = 0; slot < 3; slot++) {
                    signature.add(getPatternPreviewSignatureKey(inputs.getStack(slot)));
                }
                yield signature;
            }
            case STONECUTTING -> {
                List<Object> signature = new ArrayList<>(2);
                signature.add(getPatternPreviewSignatureKey(inputs.getStack(0)));
                signature.add(patternEncodingLogic.getStonecuttingRecipeId());
                yield signature;
            }
            case PROCESSING -> List.of();
        };
    }

    private static Object getPatternPreviewSignatureKey(@Nullable GenericStack stack) {
        return stack == null ? EMPTY_PATTERN_PREVIEW_KEY : stack.what();
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

        var input = createCraftingInput(ingredients);
        var level = getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElse(null);
        return recipe == null ? ItemStack.EMPTY : recipe.assemble(input, level.registryAccess());
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
        return recipe == null ? ItemStack.EMPTY : recipe.assemble(input, level.registryAccess());
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
        return recipe == null ? ItemStack.EMPTY : recipe.getSecond().getResultItem(level.registryAccess()).copy();
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

        var input = createCraftingInput(ingredients);
        var level = getPlayer().level();
        CraftingRecipe recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElse(null);
        if (recipe == null) {
            logEncode("crafting recipe not found, ingredients={}",
                    java.util.Arrays.toString(ingredients));
            return ItemStack.EMPTY;
        }

        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            logEncode("crafting recipe assembled empty");
            return ItemStack.EMPTY;
        }
        logEncode("crafting recipe found result={}", result);
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
        var output = AEItemKey.of(recipe.assemble(input, level.registryAccess()));
        logEncode("smithing recipe found output={}", output);
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
        var recipeHolder = selectedRecipeId == null ? null : level.getRecipeManager()
                .getRecipeFor(RecipeType.STONECUTTING, input, level, selectedRecipeId)
                .orElse(null);
        if (recipeHolder == null) {
            logEncode("stonecutting recipe not found, input={}", inputKey);
            return ItemStack.EMPTY;
        }
        var recipe = recipeHolder.getSecond();
        var output = AEItemKey.of(recipe.getResultItem(level.registryAccess()));
        logEncode("stonecutting recipe found id={}, output={}", recipeHolder.getFirst(), output);
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
                inputs,
                outputs);
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
        for (var recipe : level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, recipeInput, level)) {
            stonecuttingRecipes.add(new RecipeHolder<>(recipe.getId(), recipe));
        }

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
                new FillCraftingGridFromRecipePacket(null, templates, false).serverPacketData(sp);
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
        if (isServerSide() && getManualWorkspaceMode() != ManualWorkspaceMode.CRAFTING) {
            syncManualWorkspaceChanges();
        }
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
            if (isManualResultSlot(sourceSlot)) {
                return quickMoveManualResult(player, sourceSlot);
            }
            if (isPlayerEquipmentOrOffhandSlot(sourceSlot)) {
                return quickMoveEquipmentOrOffhandToMainPlayerInventory(player, sourceSlot);
            }
            if (sourceSlot.hasItem() && !isPlayerArmorSlot(sourceSlot)) {
                ItemStack sourceStack = sourceSlot.getItem();
                if (getSlotSemantic(sourceSlot) == SlotSemantics.STORAGE) {
                    ItemStack movedStorageToCardBox = tryMoveStackToOpenCardBox(player, sourceSlot, sourceStack);
                    if (!movedStorageToCardBox.isEmpty()) {
                        return movedStorageToCardBox;
                    }
                    return quickMoveStorageToMainPlayerInventory(player, sourceSlot, sourceStack);
                }

                ItemStack movedToCellUpgrade = tryMoveStackToOpenCardBox(player, sourceSlot, sourceStack);
                if (!movedToCellUpgrade.isEmpty()) {
                    return movedToCellUpgrade;
                }

                ItemStack movedToOpenExtendedUi = tryMoveStackToOpenExtendedUiSlots(player, sourceSlot, sourceStack);
                if (!movedToOpenExtendedUi.isEmpty()) {
                    return movedToOpenExtendedUi;
                }

                ItemStack movedCurioToPlayer = tryMoveCurioToPlayerInventoryFirst(player, sourceSlot, sourceStack);
                if (!movedCurioToPlayer.isEmpty()) {
                    return movedCurioToPlayer;
                }

                if (shouldQuickMoveMenuSlotOnlyToMainPlayerInventory(sourceSlot)) {
                    return tryMoveMenuSlotToMainPlayerInventoryFirst(player, sourceSlot, sourceStack);
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

    private boolean shouldQuickMoveMenuSlotOnlyToMainPlayerInventory(Slot sourceSlot) {
        var semantic = getSlotSemantic(sourceSlot);
        return semantic != SlotSemantics.PLAYER_HOTBAR
                && semantic != SlotSemantics.PLAYER_INVENTORY
                && semantic != SlotSemantics.STORAGE
                && semantic != SlotSemantics.CRAFTING_RESULT
                && !isPlayerEquipmentOrOffhandSlot(sourceSlot);
    }

    private ItemStack tryMoveMenuSlotToMainPlayerInventoryFirst(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty()
                || isPlayerHotbarOrStorageSemanticSlot(sourceSlot)
                || isPlayerEquipmentOrOffhandSlot(sourceSlot)
                || getSlotSemantic(sourceSlot) == SlotSemantics.CRAFTING_RESULT) {
            return ItemStack.EMPTY;
        }
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        int playerInventoryEnd = getPlayerInventoryEndMenuIndex();
        if (playerInventoryStart < 0 || playerInventoryEnd <= playerInventoryStart) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceStack.copy();
        if (moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, false)) {
            finishPriorityQuickMove(player, sourceSlot, sourceStack);
            return original;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack quickMoveStorageToMainPlayerInventory(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        int playerInventoryEnd = getPlayerInventoryEndMenuIndex();
        if (playerInventoryStart < 0 || playerInventoryEnd <= playerInventoryStart) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceStack.copy();
        if (moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, false)) {
            finishPriorityQuickMove(player, sourceSlot, sourceStack);
            return original;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack quickMoveManualResult(Player player, Slot sourceSlot) {
        if (isClientSide() || sourceSlot == null || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack currentResult = sourceSlot.getItem();
        if (manualQuickCraftSlotIndex != sourceSlot.index) {
            int perCraft = Math.max(1, currentResult.getCount());
            manualQuickCraftSlotIndex = sourceSlot.index;
            manualQuickCraftsRemaining = Math.max(1, currentResult.getMaxStackSize() / perCraft);
        }
        if (manualQuickCraftsRemaining <= 0) {
            manualQuickCraftSlotIndex = -1;
            return ItemStack.EMPTY;
        }

        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        int playerInventoryEnd = getPlayerInventoryEndMenuIndex();
        if (playerInventoryStart < 0 || playerInventoryEnd <= playerInventoryStart) {
            return ItemStack.EMPTY;
        }

        ItemStack original = currentResult.copy();
        ItemStack remaining = original.copy();
        if (!moveItemStackTo(remaining, playerInventoryStart, playerInventoryEnd, false)
                || remaining.getCount() == original.getCount()) {
            manualQuickCraftSlotIndex = -1;
            return ItemStack.EMPTY;
        }

        ItemStack taken = original.copy();
        taken.setCount(original.getCount() - remaining.getCount());
        if (sourceSlot instanceof ManualSmithingResultSlot) {
            manualSmithingBridge.takeResult(player, taken, true);
        } else {
            sourceSlot.onTake(player, taken);
        }
        manualQuickCraftsRemaining--;
        syncManualWorkspaceChanges();
        return original;
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

    private ItemStack tryMoveStackToOpenExtendedUiSlots(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty() || menuHost == null || !canPriorityMoveIntoExtendedSlot(sourceSlot)) {
            return ItemStack.EMPTY;
        }

        return switch (menuHost.getCurrentExtendedUI()) {
            case ADVANCED_CODING -> tryMoveStackToSemanticSlots(player, sourceSlot, sourceStack,
                    WcwtSlotSemantics.COPY_PATTERN,
                    WcwtSlotSemantics.WCWT_STORAGE_CELL);
            case COSMETIC_ARMOR -> shouldPriorityMoveToCosmeticArmor()
                    ? tryMoveStackToSemanticSlots(player, sourceSlot, sourceStack,
                            WcwtSlotSemantics.DECORATIVE_HELMET,
                            WcwtSlotSemantics.DECORATIVE_ARMOR,
                            WcwtSlotSemantics.DECORATIVE_SHIN_GUARDS,
                            WcwtSlotSemantics.DECORATIVE_BOOTS)
                    : ItemStack.EMPTY;
            case CURIOS -> tryMoveStackToSemanticSlots(player, sourceSlot, sourceStack,
                    WcwtSlotSemantics.AE_CURIOS);
            case TOOLKIT -> shouldPriorityMoveToToolkit()
                    ? tryToolkitQuickMoveShortcuts(player, sourceSlot, sourceStack)
                    : ItemStack.EMPTY;
            case RESONATING_LIGHTNING_PATTERN_CODING -> tryMoveStackToSemanticSlots(player, sourceSlot, sourceStack,
                    WcwtSlotSemantics.WCWT_RESONATING_STORAGE);
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack tryMoveStackToOpenCardBox(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (!shouldPriorityMoveToCardBox()) {
            return ItemStack.EMPTY;
        }
        return tryMoveStackToSemanticSlots(player, sourceSlot, sourceStack, SlotSemantics.TOOLBOX);
    }

    private ItemStack tryMoveStackToSemanticSlots(Player player, Slot sourceSlot, ItemStack sourceStack,
            appeng.menu.SlotSemantic... targetSemantics) {
        if (sourceStack.isEmpty()
                || !canPriorityMoveIntoExtendedSlot(sourceSlot)
                || targetSemantics == null
                || targetSemantics.length == 0) {
            return ItemStack.EMPTY;
        }
        for (var semantic : targetSemantics) {
            for (var targetSlot : getSlots(semantic)) {
                if (targetSlot == sourceSlot
                        || targetSlot.hasItem()
                        || !targetSlot.isActive()
                        || !targetSlot.mayPlace(sourceStack)) {
                    continue;
                }
                int targetMenuIndex = slots.indexOf(targetSlot);
                if (targetMenuIndex < 0) {
                    continue;
                }
                ItemStack original = sourceStack.copy();
                if (moveItemStackTo(sourceStack, targetMenuIndex, targetMenuIndex + 1, false)) {
                    finishPriorityQuickMove(player, sourceSlot, sourceStack);
                    return original;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean canPriorityMoveIntoExtendedSlot(Slot sourceSlot) {
        var semantic = getSlotSemantic(sourceSlot);
        return semantic == SlotSemantics.PLAYER_HOTBAR
                || semantic == SlotSemantics.PLAYER_INVENTORY
                || semantic == SlotSemantics.STORAGE;
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

    private boolean isPlayerEquipmentOrOffhandSlot(Slot slot) {
        return isPlayerArmorSlot(slot) || getSlots(WcwtSlotSemantics.AE2WTLIB_OFFHAND).contains(slot);
    }

    private boolean isCurioSlot(Slot slot) {
        return getSlots(WcwtSlotSemantics.AE_CURIOS).contains(slot);
    }

    private ItemStack tryToolkitQuickMoveShortcuts(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }
        if (!shouldPriorityMoveToToolkit()) {
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

    private boolean shouldPriorityMoveToCosmeticArmor() {
        return isClientQuickMoveOptionEnabled(QUICK_MOVE_TO_COSMETIC_ARMOR)
                && menuHost != null
                && menuHost.getCurrentExtendedUI() == IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR;
    }

    private boolean shouldPriorityMoveToCardBox() {
        return isClientQuickMoveOptionEnabled(QUICK_MOVE_TO_CARD_BOX)
                && menuHost != null
                && menuHost.getCurrentExtendedUI() == IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX;
    }

    private boolean shouldPriorityMoveToToolkit() {
        return isClientQuickMoveOptionEnabled(QUICK_MOVE_TO_TOOLKIT)
                && menuHost != null
                && menuHost.getCurrentExtendedUI() == IExtendedUIHost.ExtendedUIType.TOOLKIT;
    }

    private boolean isClientQuickMoveOptionEnabled(int option) {
        return (clientQuickMoveOptions & option) != 0;
    }

    private boolean tryMoveNetworkStackToOpenCardBox(AEItemKey clickedItem) {
        if (!shouldPriorityMoveToCardBox() || storage == null) {
            return false;
        }
        for (Slot targetSlot : getSlots(SlotSemantics.TOOLBOX)) {
            if (targetSlot.hasItem() || !targetSlot.isActive()) {
                continue;
            }
            ItemStack probe = clickedItem.toStack();
            if (!targetSlot.mayPlace(probe)) {
                continue;
            }
            int toExtract = Math.min(clickedItem.getMaxStackSize(), targetSlot.getMaxStackSize(probe));
            if (toExtract <= 0) {
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(
                    getWcwtEnergySource(), storage, clickedItem, toExtract, getActionSource(), Actionable.MODULATE);
            if (extracted <= 0) {
                return false;
            }
            targetSlot.set(clickedItem.toStack((int) Math.min(extracted, Integer.MAX_VALUE)));
            targetSlot.setChanged();
            broadcastChanges();
            return true;
        }
        return false;
    }

    private boolean moveOneNetworkStackToMainPlayerInventory(AEItemKey clickedItem) {
        if (storage == null) {
            return false;
        }
        Inventory playerInv = getPlayerInventory();
        ItemStack template = clickedItem.toStack();
        int targetSlot = findMainInventorySlotWithRemainingSpace(playerInv, template);
        int toExtract;
        if (targetSlot != -1) {
            toExtract = template.getMaxStackSize() - playerInv.getItem(targetSlot).getCount();
        } else {
            targetSlot = findMainInventoryFreeSlot(playerInv);
            if (targetSlot == -1) {
                return false;
            }
            toExtract = template.getMaxStackSize();
        }
        if (toExtract <= 0) {
            return false;
        }
        long extracted = StorageHelper.poweredExtraction(
                getWcwtEnergySource(), storage, clickedItem, toExtract, getActionSource(), Actionable.MODULATE);
        if (extracted <= 0) {
            return false;
        }
        ItemStack itemInSlot = playerInv.getItem(targetSlot);
        if (itemInSlot.isEmpty()) {
            playerInv.setItem(targetSlot, clickedItem.toStack((int) Math.min(extracted, Integer.MAX_VALUE)));
        } else {
            itemInSlot.grow((int) Math.min(extracted, Integer.MAX_VALUE));
        }
        return true;
    }

    private static int findMainInventorySlotWithRemainingSpace(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (!candidate.isEmpty()
                    && candidate.getCount() < candidate.getMaxStackSize()
                    && ItemStack.isSameItemSameTags(candidate, stack)) {
                return i;
            }
        }
        return -1;
    }

    private static int findMainInventoryFreeSlot(Inventory inventory) {
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack tryMoveCurioToPlayerInventoryFirst(Player player, Slot sourceSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty() || !isCurioSlot(sourceSlot)) {
            return ItemStack.EMPTY;
        }
        if (menuHost.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.CURIOS) {
            return ItemStack.EMPTY;
        }
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        int playerInventoryEnd = getPlayerInventoryEndMenuIndex();
        if (playerInventoryStart < 0 || playerInventoryEnd <= playerInventoryStart) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceStack.copy();
        if (moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, false)) {
            finishPriorityQuickMove(player, sourceSlot, sourceStack);
            return original;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack quickMoveEquipmentOrOffhandToMainPlayerInventory(Player player, Slot sourceSlot) {
        if (sourceSlot == null || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        int playerInventoryStart = getPlayerInventoryStartMenuIndex();
        int playerInventoryEnd = getPlayerInventoryEndMenuIndex();
        if (playerInventoryStart < 0 || playerInventoryEnd <= playerInventoryStart) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack original = sourceStack.copy();
        if (moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, false)) {
            finishPriorityQuickMove(player, sourceSlot, sourceStack);
            return original;
        }
        return ItemStack.EMPTY;
    }

    private boolean isPlayerHotbarOrStorageSemanticSlot(Slot slot) {
        var semantic = getSlotSemantic(slot);
        return semantic == SlotSemantics.PLAYER_HOTBAR || semantic == SlotSemantics.PLAYER_INVENTORY;
    }

    protected boolean isValidQuickMoveDestination(Slot candidateSlot, ItemStack stackToMove, boolean fromPlayerSide) {
        if (candidateSlot == null || !candidateSlot.mayPlace(stackToMove)) {
            return false;
        }
        if (isPlayerEquipmentOrOffhandSlot(candidateSlot)) {
            return false;
        }

        if (menuHost == null) {
            return true;
        }

        var semantic = getSlotSemantic(candidateSlot);
        if (semantic == WcwtSlotSemantics.WCWT_TOOLKIT
                && menuHost.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.TOOLKIT) {
            return false;
        }
        if (isDecorativeArmorSemantic(semantic)
                && menuHost.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR) {
            return false;
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

    private int getPlayerInventoryEndMenuIndex() {
        int start = getPlayerInventoryStartMenuIndex();
        if (start < 0) {
            return -1;
        }
        return Math.min(slots.size(), start + getPlayerInventory().items.size());
    }

    private ItemStack tryQuickMoveToolkitToPlayerBulk(Slot toolkitSlot, ItemStack toolkitStack) {
        if (toolkitStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int mainStorageStartMenu = getPlayerInventoryStartMenuIndex();
        int mainStorageEndMenu = getPlayerInventoryEndMenuIndex();
        if (mainStorageStartMenu < 0 || mainStorageEndMenu <= mainStorageStartMenu) {
            return ItemStack.EMPTY;
        }
        ItemStack original = toolkitStack.copy();
        if (moveItemStackTo(toolkitStack, mainStorageStartMenu, mainStorageEndMenu, false)) {
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
        return inv != null && idx >= 0 && idx < inv.size();
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
        boolean manualWorkspaceSlotChanged = slot == manualSmithingTemplateSlot
                || slot == manualSmithingBaseSlot
                || slot == manualSmithingAdditionSlot
                || slot == manualAnvilLeftSlot
                || slot == manualAnvilRightSlot;
        updatePatternPreview(getPatternEncodingMode());
        if (manualWorkspaceSlotChanged) {
            syncManualWorkspaceChanges();
        }
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
    public void applyPatternMultiplier(PatternMultiplierType type, boolean applyToEditorProcessing) {
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
                stampEncodedPatternPlayer(advPattern);
                slot.set(advPattern);
                continue;
            }
            
            // 只处理加工样板
            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                var input = process.getSparseInputs();
                var output = process.getOutputs();
                
                // 检查是否可以进行修改
                if (checkCanModify(input, scale, divide) && checkCanModify(output, scale, divide)) {
                    var modifiedInput = new GenericStack[input.length];
                    var modifiedOutput = new GenericStack[output.length];
                    
                    modifyStacks(input, modifiedInput, scale, divide);
                    modifyStacks(output, modifiedOutput, scale, divide);
                    
                    // 编码新样板
                    var newPattern = PatternDetailsHelper.encodeProcessingPattern(
                        modifiedInput,
                        modifiedOutput
                    );
                    stampEncodedPatternPlayer(newPattern);
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

        if (isSameStack(existing, copy) && existing.getCount() < existing.getMaxStackSize()) {
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
                getPlayer().level().registryAccess(),
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
     *     - static ItemStack encodeProcessingPattern(GenericStack[], GenericStack[], HashMap)
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
                        GenericStack[].class, GenericStack[].class, HashMap.class);
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

                GenericStack[] sparseInputs;
                GenericStack[] sparseOutputs;
                LinkedHashMap<AEKey, Direction> dirMap;

                if (advPatternClass.isInstance(detail)) {
                    sparseInputs  = (GenericStack[]) advGetSparseInputs.invoke(detail);
                    sparseOutputs = (GenericStack[]) advGetSparseOutputs.invoke(detail);
                    dirMap = new LinkedHashMap<>((LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));
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
                        java.util.Arrays.asList((GenericStack[]) advGetSparseInputs.invoke(detail)), replaceWhat, replaceWith)
                        .toArray(new GenericStack[0]);
                var sparseOutputs = replaceInStacks(
                        java.util.Arrays.asList((GenericStack[]) advGetSparseOutputs.invoke(detail)), replaceWhat, replaceWith)
                        .toArray(new GenericStack[0]);
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

                var input = (GenericStack[]) advGetSparseInputs.invoke(detail);
                var output = (GenericStack[]) advGetSparseOutputs.invoke(detail);
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
                        scaledInput,
                        scaledOutput,
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

                var input = (GenericStack[]) advGetSparseInputs.invoke(detail);
                var output = (GenericStack[]) advGetSparseOutputs.invoke(detail);
                long gcd = computeSharedGcd(java.util.Arrays.asList(input), java.util.Arrays.asList(output));
                if (gcd <= 1L) {
                    return null;
                }
                var dirMap = new LinkedHashMap<>(
                        (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));

                Object result = encoderEncode.invoke(
                        null,
                        divideStacks(java.util.Arrays.asList(input), gcd).toArray(new GenericStack[0]),
                        divideStacks(java.util.Arrays.asList(output), gcd).toArray(new GenericStack[0]),
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

                var input = (GenericStack[]) advGetSparseInputs.invoke(detail);
                var output = (GenericStack[]) advGetSparseOutputs.invoke(detail);
                var rotatedOutput = rotateOutputs(output);
                var dirMap = new LinkedHashMap<>(
                        (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail));

                Object result = encoderEncode.invoke(
                        null,
                        input,
                        rotatedOutput,
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
                writePatternCacheSlot(slot, advPattern);
                continue;
            }

            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                var newPattern = replaceProcessingPattern(process, replaceWhat, replaceWith);
                if (!newPattern.isEmpty()) {
                    writePatternCacheSlot(slot, newPattern);
                }
            } else if (detail instanceof appeng.crafting.pattern.AECraftingPattern craft) {
                var newPattern = replaceCraftingPattern(craft, replaceWhat, replaceWith);
                if (!newPattern.isEmpty()) {
                    writePatternCacheSlot(slot, newPattern);
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
                var recipe = resolveCraftingRecipe(craft);
                if (recipe == null) {
                    continue;
                }
                var input = craft.getSparseInputs();
                var output = craft.getPrimaryOutput();
                try {
                    var newPattern = PatternDetailsHelper.encodeCraftingPattern(
                            recipe,
                            itemize(java.util.Arrays.asList(input)),
                            itemize(output),
                            mode == 0 ? value : craft.canSubstitute(),
                            mode == 1 ? value : craft.canSubstituteFluids());
                    writePatternCacheSlot(slot, newPattern);
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
                writePatternCacheSlot(slot, advPattern);
                continue;
            }

            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                long gcd = computeSharedGcd(
                        java.util.Arrays.asList(process.getSparseInputs()),
                        java.util.Arrays.asList(process.getSparseOutputs()));
                if (gcd <= 1L) {
                    continue;
                }
                ItemStack newPattern = PatternDetailsHelper.encodeProcessingPattern(
                        divideStacks(java.util.Arrays.asList(process.getSparseInputs()), gcd).toArray(new GenericStack[0]),
                        divideStacks(java.util.Arrays.asList(process.getSparseOutputs()), gcd).toArray(new GenericStack[0]));
                writePatternCacheSlot(slot, newPattern);
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
                stampEncodedPatternPlayer(advPattern);
                slot.set(advPattern);
                continue;
            }

            if (detail instanceof appeng.crafting.pattern.AEProcessingPattern process) {
                ItemStack newPattern = PatternDetailsHelper.encodeProcessingPattern(
                        process.getSparseInputs(),
                        rotateOutputs(process.getSparseOutputs()));
                stampEncodedPatternPlayer(newPattern);
                slot.set(newPattern);
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
        var replacedInput = replaceInStacks(java.util.Arrays.asList(process.getSparseInputs()), replaceWhat, replaceWith);
        var replacedOutput = replaceInStacks(java.util.Arrays.asList(process.getSparseOutputs()), replaceWhat, replaceWith);
        try {
            return PatternDetailsHelper.encodeProcessingPattern(
                    replacedInput.toArray(new GenericStack[0]),
                    replacedOutput.toArray(new GenericStack[0]));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack replaceCraftingPattern(appeng.crafting.pattern.AECraftingPattern craft,
                                             AEKey replaceWhat, @Nullable AEKey replaceWith) {
        if (!(replaceWhat instanceof AEItemKey) || (replaceWith != null && !(replaceWith instanceof AEItemKey))) {
            return ItemStack.EMPTY;
        }
        var recipe = resolveCraftingRecipe(craft);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        var replacedInput = itemize(replaceInStacks(java.util.Arrays.asList(craft.getSparseInputs()), replaceWhat, replaceWith));
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
        } else {
            actualConfig.setStack(idx, new GenericStack(key, 0));
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
        static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern,
                                             appeng.api.networking.IGrid grid) {
            return ModList.get().isLoaded("extendedae_plus")
                    && MatrixUploadUtil.uploadPatternToMatrix(player, pattern, grid);
        }

        @Nullable
        static String getProviderDisplayName(PatternContainer provider) {
            return ModList.get().isLoaded("extendedae_plus")
                    ? PatternProviderDataUtil.getProviderDisplayName(provider)
                    : null;
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
                for (int slot = 0; slot < outputs.length; slot++) {
                    if (outputs[slot] != null && outputs[slot].what() instanceof AEItemKey) {
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

    @Nullable
    private CraftingRecipe resolveCraftingRecipe(appeng.crafting.pattern.AECraftingPattern craft) {
        var ingredients = itemize(java.util.Arrays.asList(craft.getSparseInputs()));
        var input = createCraftingInput(ingredients);
        var level = getPlayer().level();
        var output = craft.getPrimaryOutput();
        var expectedOutput = itemize(output);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .filter(recipe -> ItemStack.matches(expectedOutput, recipe.assemble(input, level.registryAccess())))
                .orElse(null);
    }
}
