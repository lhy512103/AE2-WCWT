package com.lhy.wcwt.helpers;

import appeng.api.config.CopyMode;
import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.config.PowerMultiplier;
import appeng.api.features.Locatables;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.MEStorage;
import appeng.api.storage.SupplierStorage;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.WirelessCraftingTerminalMenuHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;
import com.lhy.wcwt.api.ICraftingLockHost;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.api.IPatternCachingHost;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import appeng.blockentity.qnb.QuantumBridgeBlockEntity;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.me.cluster.implementations.QuantumCluster;
import appeng.me.storage.NullInventory;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.init.ModComponents;
import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.api.TextConstants;
import appeng.core.localization.PlayerMessages;
public class WirelessComprehensiveWorkTerminalMenuHost extends WirelessCraftingTerminalMenuHost
        implements ISegmentedInventory, IViewCellStorage, IExtendedUIHost, IPatternCachingHost, ICraftingLockHost, IConfigInvHost,
        IPatternTerminalMenuHost, IPatternTerminalLogicHost {
    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        CompoundTag stackTag = stack.getOrCreateTag();
        CompoundTag root = stackTag.getCompound(ModComponents.ROOT_TAG);
        stackTag.put(ModComponents.ROOT_TAG, root);
        return root;
    }

    @Nullable
    private static CompoundTag getRootTag(ItemStack stack) {
        CompoundTag stackTag = stack.getTag();
        if (stackTag == null || !stackTag.contains(ModComponents.ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return stackTag.getCompound(ModComponents.ROOT_TAG);
    }

    private static final boolean DEBUG_REPO = Boolean.getBoolean("wcwt.debug.repo");
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");
    private static final boolean DEBUG_TOOLKIT = Boolean.getBoolean("wcwt.debug.toolkit");
    private static final long PERF_LOG_THRESHOLD_NS = 1_000_000L;
    private static final long STORAGE_PERF_LOG_THRESHOLD_NS = 5_000_000L;
    private static final long STORAGE_CACHE_TICKS = Math.max(0L, Long.getLong("wcwt.storageCacheTicks", 5L));
    /**
     * 短暂链路抖动时保留最近一次稳定联网快照，避免 repo 在 1~2 拍内被清空成灰格。
     * 3 tick ~= 150ms，足够吞掉本次日志里看到的瞬时 false/true 抖动，又不会把真实断线拖得太久。
     */
    private static final long TRANSIENT_DISCONNECT_GRACE_TICKS = 3L;
    private static final String PLAYER_PERSISTED_TAG = "PlayerPersisted";
    private static final String WCWT_PLAYER_DATA_TAG = WcwtMod.MOD_ID;
    private static final String PLAYER_TOOLKIT_DATA_TAG = "shared_toolkit";
    private static final String PLAYER_PENDING_EXTENDED_UI_TAG = "pending_extended_ui";
    private static final String TOOLKIT_SIZE_TAG = "size";
    private static final String TOOLKIT_ITEMS_TAG = "items";
    private static final String TOOLKIT_SLOT_TAG = "slot";
    private static final String TOOLKIT_STACK_TAG = "stack";
    private static final String SINGULARITY_TAG = "singularity";
    private static final String PICKUP_CONFIG_TAG = "pickup_config";
    private static final String INSERT_CONFIG_TAG = "insert_config";
    private static final String PATTERN_ENCODING_LOGIC_TAG = "pattern_encoding_logic";

    private record LinkStatus(boolean connected, @Nullable net.minecraft.network.chat.Component statusDescription) {
        private static LinkStatus ofConnected() {
            return new LinkStatus(true, null);
        }

        private static LinkStatus ofDisconnected() {
            return new LinkStatus(false, null);
        }

        private static LinkStatus ofDisconnected(@Nullable net.minecraft.network.chat.Component description) {
            return new LinkStatus(false, description);
        }
    }

    private record ActionHostResult(@Nullable IActionHost host, Status status) {
        private static ActionHostResult valid(IActionHost host) {
            return new ActionHostResult(host, Status.Valid);
        }

        private static ActionHostResult invalid(Status status) {
            return new ActionHostResult(null, status);
        }

        private boolean invalid() {
            return status != Status.Valid;
        }
    }

    private record LongResult(long result, Status status) {
        private static LongResult valid(long result) {
            return new LongResult(result, Status.Valid);
        }

        private static LongResult invalid(Status status) {
            return new LongResult(0L, status);
        }

        private boolean valid() {
            return status == Status.Valid;
        }
    }

    private enum Status {
        Valid(null),
        GenericInvalid(null),
        NotPowered(TextConstants.NETWORK_NOT_POWERED),
        NoUpgrade(TextConstants.NO_QNB_UPGRADE),
        BridgeNotFound(TextConstants.NO_QNB),
        DifferentNetworks(TextConstants.DIFFERENT_NETWORKS),
        NoSingularity(TextConstants.SINGULARITY_NOT_PRESENT);

        @Nullable
        private final net.minecraft.network.chat.Component error;

        Status(@Nullable net.minecraft.network.chat.Component error) {
            this.error = error;
        }

        private boolean isValid() {
            return this == Valid;
        }

        private LinkStatus toLinkStatus() {
            return isValid() ? LinkStatus.ofConnected() : LinkStatus.ofDisconnected(error);
        }

        private boolean is(LinkStatus status) {
            return status != null && status.statusDescription() == error;
        }
    }
    
    // 定义各种存储库存的ResourceLocation标识符
    public static final ResourceLocation INV_AE2WTLIB_ARMOR = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "ae2wtlib_armor");
    public static final ResourceLocation INV_DECORATIVE_ARMOR = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "decorative_armor");
    public static final ResourceLocation INV_CURIOS = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "curios");
    public static final ResourceLocation INV_ADVANCED_PATTERN = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "advanced_pattern");
    public static final ResourceLocation INV_PATTERN_CACHE = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_cache");
    public static final ResourceLocation INV_TOOLKIT = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "toolkit");
    public static final ResourceLocation INV_STORAGE_CELL = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "storage_cell");
    public static final ResourceLocation INV_RESONATING_PATTERN_CACHE = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "resonating_pattern_cache");
    public static final ResourceLocation INV_MANUAL_SMITHING = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "manual_smithing");
    public static final ResourceLocation INV_MANUAL_ANVIL = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "manual_anvil");
    public static final ResourceLocation INV_TRASH = com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "trash");
    /** 缠绕态奇点槽位库存标识（与 ae2wtlib 保持一致，复用其 ResourceLocation） */
    public static final ResourceLocation INV_SINGULARITY = com.lhy.wcwt.util.ResourceLocationCompat.id("ae2wtlib", "singularity");

    // 缠绕态奇点槽（1个槽位，用于量子桥接卡联网）
    private final SupplierInternalInventory singularityInv;
    private final MEStorage quantumAwareStorage;
    @Nullable
    private IActionHost quantumBridge;
    private LinkStatus quantumStatus = LinkStatus.ofDisconnected();
    private LinkStatus effectiveLinkStatus = LinkStatus.ofDisconnected();
    @Nullable
    private IGridNode cachedStableActionableNode;
    private LinkStatus cachedStableLinkStatus = LinkStatus.ofDisconnected();
    private long lastStableConnectionTick = Long.MIN_VALUE;
    private long lastConnectedAccessPointUpdateTick = Long.MIN_VALUE;
    private boolean forceConnectedAccessPointUpdate;
    private int skippedConnectedAccessPointUpdates;
    private String lastRepoDebugState = "";
    private long lastRepoDebugTick = Long.MIN_VALUE;
    private int suppressedRepoDebugLogs;
    // AE2WTLIB装备槽 (头盔, 胸甲, 护腿, 靴子, 副手) - 5个槽位
    private final SupplierInternalInventory ae2wtlibArmorInv;
    // 装饰性装备槽 (头盔, 胸甲, 护腿, 靴子) - 4个槽位
    private final SupplierInternalInventory decorativeArmorInv;
    // Curios槽位 - 1个槽位
    private final SupplierInternalInventory curiosInv;
    // 高级样板编码槽 (复制样板, 替换输入, 替换输出) - 3个槽位
    private final SupplierInternalInventory advancedPatternInv;
    // 样板缓存区 - 36个槽位（4行×9列，界面每次显示2行）
    private final SupplierInternalInventory patternCacheInv;
    // 工具包 - 默认64个槽位，实际数量由服务端配置控制
    private final SupplierInternalInventory toolkitInv;
    // AE2 原版 VIEW_CELL 槽位 - 5个槽位
    private final SupplierInternalInventory viewCellInv;
    // 元件工作台 - 存储元件槽（1个槽位）
    private final SupplierInternalInventory storageCellInv;
    // 谐振样板缓存区 - 21个槽位（3行×7列）
    private final SupplierInternalInventory resonatingPatternCacheInv;
    // 左上手动锻造台工作区 - 3个槽位
    private final SupplierInternalInventory manualSmithingInv;
    // 左上手动铁砧工作区 - 2个槽位
    private final SupplierInternalInventory manualAnvilInv;
    private final AppEngInternalInventory trashInv = new AppEngInternalInventory(27);
    private final ConfigInventory magnetPickupConfig = ConfigInventory.configTypes(AEKeyType.items()::equals, 27,
            this::updateMagnetPickupConfig);
    private final ConfigInventory magnetInsertConfig = ConfigInventory.configTypes(AEKeyType.items()::equals, 27,
            this::updateMagnetInsertConfig);
    // 元件工作台 - AE2 CellWorkbench 同款 63 格 config 镜像
    private final GenericStackInv cellConfigInv = new GenericStackInv(this::cellConfigChanged,
            GenericStackInv.Mode.CONFIG_TYPES, 63);
    private final PatternEncodingLogic patternEncodingLogic = new PatternEncodingLogic(this);
    private ConfigInventory cachedCellConfig = null;
    private boolean cellConfigLocked = false;
    private CopyMode cellCopyMode = CopyMode.CLEAR_ON_REMOVE;
    /** 元件自身的升级卡库存缓存（参考 AAE PortableCellWorkbenchMenuHost.getCachedUpgrades）。
     *  元件被换出/换入时由 {@link #invalidateCellCaches()} 清空。 */
    private IUpgradeInventory cachedCellUpgrades = null;
    
    // 扩展UI状态
    private ExtendedUIType currentExtendedUI = ExtendedUIType.NONE;
    // 样板选中索引（用于高级编码）
    private int selectedPatternIndex = -1;
    // 合成网格锁定状态
    /** 默认未锁定：JEI 工作台类配方先进入样板编码区；锁定后再进手动合成 3×3，并由 AE2 配方包从 ME/背包取料 */
    private boolean craftingGridLocked;
    /** 样板管理区“启用上传样板功能”开关，持久化到终端物品自身。 */
    private boolean patternManagementUploadEnabled;
    /** 样板管理区显示模式，持久化到终端物品自身。 */
    private int patternManagementDisplayMode;
    /** 样板管理区是否显示样板槽，持久化到终端物品自身。 */
    private boolean patternManagementShowSlots;
    /** 样板管理区搜索模式，持久化到终端物品自身。 */
    private int patternManagementSearchMode;
    /** 左上手动工作区模式。 */
    private int manualWorkspaceMode;
    /** 左上铁砧工作区当前命名文本。 */
    private String manualAnvilName;
    
    public WirelessComprehensiveWorkTerminalMenuHost(Player player, @Nullable Integer inventorySlot,
                                                      ItemStack itemStack,
                                                      BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, inventorySlot, itemStack, returnToMainMenu);
        // 样板管理区之外，主物品展示区依赖 AE2 的增量仓库同步。
        // 这里若按 ItemStack 引用缓存底层库存，在量子桥/WAP/链接状态切换时可能继续指向旧网格，
        // 导致仓库视图刷新滞后，直到重开菜单才恢复。
        this.quantumAwareStorage = new TimedStorage(new SupplierStorage(this::getDynamicQuantumAwareStorage));

        // 说明：下列子库存都以终端物品 NBT 为后端，过去每次访问都用 createInventory(...) 从 NBT
        // 现读一份全新的 AppEngInternalInventory。这会让客户端收到 slot 同步包时，set() 写入的那份
        // 库存随即被丢弃、getItem() 又从 NBT 重建，导致量子桥卡升级槽下方各扩展 UI 面板的槽位
        // 普遍出现"放入/取出后不刷新，要重开界面才更新"的问题。
        // 由于 getItemStack() 在菜单生命周期内是稳定实例（AE2 ItemMenuHost 还会维持引用相等），
        // 这里改为只构建一次并复用（memoize），让这份内存库存成为唯一数据源，既即时刷新又仍写回 NBT。
        // 初始化缠绕态奇点槽（单槽，叠加上限 1）
        this.singularityInv = new SupplierInternalInventory(memoize(() -> createSingularityInv(player, getItemStack())));

        // 初始化AE2WTLIB装备槽位
        this.ae2wtlibArmorInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "ae2wtlib_armor", 5)));
        
        // 初始化装饰性装备槽位
        this.decorativeArmorInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "decorative_armor", 4)));
        
        // 初始化Curios槽位
        this.curiosInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "curios", 1)));
        
        // 初始化高级样板编码槽位
        this.advancedPatternInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "advanced_pattern", 3)));
        
        // 初始化样板缓存区槽位 (36个槽位，4行×9列；界面显示2行并通过滑块滚动)
        this.patternCacheInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "pattern_cache", 36)));

        this.toolkitInv = new SupplierInternalInventory(
                memoize(() -> createToolkitInventory(player, getItemStack())));

        this.viewCellInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "view_cell", 5)));

        // 初始化元件工作台存储元件槽位 (1个槽位)
        this.storageCellInv = new SupplierInternalInventory(
                memoize(() -> createStorageCellInventory(player, getItemStack())));

        this.resonatingPatternCacheInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "resonating_pattern_cache", 21)));

        this.manualSmithingInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "manual_smithing", 3)));

        this.manualAnvilInv = new SupplierInternalInventory(
                memoize(() -> createInventory(player, getItemStack(), "manual_anvil", 2)));
        patternEncodingLogic.readFromNBT(getOrCreateDataTag(PATTERN_ENCODING_LOGIC_TAG));
        magnetPickupConfig.readFromChildTag(getOrCreateDataTag(PICKUP_CONFIG_TAG), "");
        magnetInsertConfig.readFromChildTag(getOrCreateDataTag(INSERT_CONFIG_TAG), "");
        this.craftingGridLocked = getDataBoolean(ModComponents.CRAFTING_GRID_LOCKED, false);
        this.patternManagementUploadEnabled = getDataBoolean(ModComponents.PATTERN_MANAGEMENT_UPLOAD_ENABLED, true);
        this.patternManagementDisplayMode = getDataInt(ModComponents.PATTERN_MANAGEMENT_DISPLAY_MODE, 1);
        this.patternManagementShowSlots = getDataBoolean(ModComponents.PATTERN_MANAGEMENT_SHOW_SLOTS, true);
        this.patternManagementSearchMode = getDataInt(ModComponents.PATTERN_MANAGEMENT_SEARCH_MODE, 2);
        this.manualWorkspaceMode = getDataInt(ModComponents.MANUAL_WORKSPACE_MODE, 0);
        this.manualAnvilName = getDataString(ModComponents.MANUAL_ANVIL_NAME, "");
        this.currentExtendedUI = consumePendingExtendedUi(player);
        if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: host init player={}, consumedPendingUi={}, locator={}",
                    player.getScoreboardName(), this.currentExtendedUI, inventorySlot);
        }

        // 父类构造期间会通过动态分派调用到本类的 updateConnectedAccessPoint/updateLinkStatus，
        // 但本类字段会在 super(...) 返回后才按这里的默认值重新初始化，导致首帧链路状态被覆盖回“断线”。
        // WAP-only 场景下自动合成子菜单打开得很早，若不在构造末尾重算一次，就可能拿到过期状态。
        refreshMenuSyncState();
    }

    @Override
    public MEStorage getInventory() {
        return quantumAwareStorage;
    }

    @Override
    public boolean onBroadcastChanges(AbstractContainerMenu menu) {
        if (!isClientSide()) {
            refreshMenuSyncState();
        }

        if (!canStayOpenOnCurrentLink()) {
            if (!isClientSide()) {
                var description = getCurrentLinkStatusDescription();
                getPlayer().displayClientMessage(description != null ? description : PlayerMessages.OutOfRange.text(), true);
            }
            return false;
        }

        setPowerDrainPerTick(getPowerDrainPerTick());
        return drainPower();
    }

    @Override
    public InternalInventory getViewCellStorage() {
        return viewCellInv;
    }

    @Nullable
    private MEStorage getQuantumAwareStorageFromStack(ItemStack stack) {
        updateConnectedAccessPointState();
        IGridNode node = getActionableNode();
        if (node == null || node.getGrid() == null) {
            return NullInventory.of();
        }
        return node.getGrid().getStorageService().getInventory();
    }

    @Nullable
    private MEStorage getDynamicQuantumAwareStorage() {
        return getQuantumAwareStorageFromStack(getItemStack());
    }

    private final class TimedStorage implements MEStorage {
        private final MEStorage delegate;
        @Nullable
        private KeyCounter cachedAvailableStacks;
        private long cachedAvailableStacksTick = Long.MIN_VALUE;

        private TimedStorage(MEStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isPreferredStorageFor(AEKey what, appeng.api.networking.security.IActionSource source) {
            return delegate.isPreferredStorageFor(what, source);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode,
                           appeng.api.networking.security.IActionSource source) {
            long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
            long inserted = delegate.insert(what, amount, mode, source);
            if (mode == Actionable.MODULATE && inserted != 0) {
                updateCachedStack(what, inserted);
            }
            logStorageMutation("insert", what, amount, inserted, mode, startNs);
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode,
                            appeng.api.networking.security.IActionSource source) {
            long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
            long extracted = delegate.extract(what, amount, mode, source);
            if (mode == Actionable.MODULATE && extracted != 0) {
                updateCachedStack(what, -extracted);
            }
            logStorageMutation("extract", what, amount, extracted, mode, startNs);
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
            int before = DEBUG_PERF ? out.size() : 0;
            KeyCounter snapshot = getCachedAvailableStacks();
            out.addAll(snapshot);
            if (DEBUG_PERF) {
                long elapsedNs = System.nanoTime() - startNs;
                if (elapsedNs >= STORAGE_PERF_LOG_THRESHOLD_NS) {
                    WcwtMod.LOGGER.info(
                            "WCWT perf: storage.getAvailableStacks(out) player={}, totalMs={}, keysBefore={}, keysAfter={}",
                            getPlayer().getScoreboardName(),
                            formatPerfMs(elapsedNs),
                            before,
                            out.size());
                }
            }
        }

        @Override
        public net.minecraft.network.chat.Component getDescription() {
            return delegate.getDescription();
        }

        @Override
        public KeyCounter getAvailableStacks() {
            long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
            KeyCounter result = copyCounter(getCachedAvailableStacks());
            if (DEBUG_PERF) {
                long elapsedNs = System.nanoTime() - startNs;
                if (elapsedNs >= STORAGE_PERF_LOG_THRESHOLD_NS) {
                    WcwtMod.LOGGER.info(
                            "WCWT perf: storage.getAvailableStacks player={}, totalMs={}, keys={}",
                            getPlayer().getScoreboardName(),
                            formatPerfMs(elapsedNs),
                            result.size());
                }
            }
            return result;
        }

        private KeyCounter getCachedAvailableStacks() {
            if (STORAGE_CACHE_TICKS <= 0) {
                return delegate.getAvailableStacks();
            }
            long tick = getPlayer().level().getGameTime();
            if (cachedAvailableStacks != null && tick - cachedAvailableStacksTick <= STORAGE_CACHE_TICKS) {
                return cachedAvailableStacks;
            }
            long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
            KeyCounter fresh = delegate.getAvailableStacks();
            cachedAvailableStacks = fresh;
            cachedAvailableStacksTick = tick;
            if (DEBUG_PERF) {
                long elapsedNs = System.nanoTime() - startNs;
                if (elapsedNs >= STORAGE_PERF_LOG_THRESHOLD_NS) {
                    WcwtMod.LOGGER.info(
                            "WCWT perf: storage.cache refresh player={}, totalMs={}, keys={}, cacheTicks={}",
                            getPlayer().getScoreboardName(),
                            formatPerfMs(elapsedNs),
                            fresh.size(),
                            STORAGE_CACHE_TICKS);
                }
            }
            return fresh;
        }

        private void updateCachedStack(AEKey what, long delta) {
            if (cachedAvailableStacks == null || STORAGE_CACHE_TICKS <= 0) {
                return;
            }

            long current = cachedAvailableStacks.get(what);
            long next = current + delta;
            if (next <= 0) {
                cachedAvailableStacks.remove(what);
            } else {
                cachedAvailableStacks.set(what, next);
            }
        }

        private void logStorageMutation(String action, AEKey what, long requested, long changed, Actionable mode,
                                        long startNs) {
            if (!DEBUG_PERF) {
                return;
            }
            long elapsedNs = System.nanoTime() - startNs;
            if (elapsedNs >= PERF_LOG_THRESHOLD_NS) {
                WcwtMod.LOGGER.info(
                        "WCWT perf: storage.{} player={}, totalMs={}, requested={}, changed={}, mode={}, key={}",
                        action,
                        getPlayer().getScoreboardName(),
                        formatPerfMs(elapsedNs),
                        requested,
                        changed,
                        mode,
                        what);
            }
        }

        private KeyCounter copyCounter(KeyCounter source) {
            KeyCounter copy = new KeyCounter();
            copy.addAll(source);
            return copy;
        }
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        // CraftingStatusMenu 等子菜单在打开瞬间会立刻取 actionableNode；若不先刷新量子状态，
        // quantumStatus 仍是上一刻的「未连接」，会导致 subMenu grid==null 无法正常打开。
        //
        // 但自动合成规划会在后台线程里再次通过 IActionSource.machine().getActionableNode()
        // 读取请求者节点来枚举可用样板。这里如果在后台线程里重新扫描 WAP/量子状态，
        // 会得到不稳定结果，表现为：库存快照正确、却完全识别不到样板，只显示“成品自身缺失”。
        // 因此只在服务端主线程刷新缓存；后台线程只读取主线程已经算好的 currentAccessPoint/quantumBridge。
        if (!getPlayer().level().isClientSide()) {
            var server = getPlayer().getServer();
            if (server == null || server.isSameThread()) {
                updateConnectedAccessPointState();
            }
            if (quantumStatus != null && quantumStatus.connected() && quantumBridge != null) {
                IGridNode quantumNode = quantumBridge.getActionableNode();
                if (quantumNode != null && quantumNode.getGrid() != null) {
                    rememberStableConnection(quantumNode, LinkStatus.ofConnected());
                    return quantumNode;
                }
            }
        }
        IGridNode superNode = super.getActionableNode();
        if (superNode != null && superNode.getGrid() != null) {
            rememberStableConnection(superNode, LinkStatus.ofConnected());
            return superNode;
        }
        if (allowsTransientDisconnectGrace(getLinkStatus())
                && isWithinTransientDisconnectGrace()
                && cachedStableActionableNode != null
                && cachedStableActionableNode.getGrid() != null) {
            debugRepoState("getActionableNode/grace");
            return cachedStableActionableNode;
        }
        return null;
    }

    private void updateLinkStatusState() {
        if (getPlayer().level().isClientSide()) {
            effectiveLinkStatus = null;
            debugRepoState("updateLinkStatus/client-pass-through");
            return;
        }
        LinkStatus current = super.getActionableNode() != null ? LinkStatus.ofConnected() : LinkStatus.ofDisconnected();
        effectiveLinkStatus = current;
        if (effectiveLinkStatus.connected()) {
            rememberStableConnection(resolveCurrentPreferredNode(), effectiveLinkStatus);
            debugRepoState("updateLinkStatus/super-connected");
            return;
        }
        if (quantumStatus != null && !quantumStatus.equals(LinkStatus.ofDisconnected())) {
            effectiveLinkStatus = quantumStatus;
        }
        if (!effectiveLinkStatus.connected()
                && allowsTransientDisconnectGrace(effectiveLinkStatus)
                && isWithinTransientDisconnectGrace()
                && hasStableConnectionSnapshot()) {
            effectiveLinkStatus = cachedStableLinkStatus.connected() ? cachedStableLinkStatus : LinkStatus.ofConnected();
            debugRepoState("updateLinkStatus/grace");
            return;
        }
        if (effectiveLinkStatus.connected()) {
            rememberStableConnection(resolveCurrentPreferredNode(), effectiveLinkStatus);
        }
        debugRepoState("updateLinkStatus/fallback");
    }

    public LinkStatus getLinkStatus() {
        return effectiveLinkStatus != null ? effectiveLinkStatus : LinkStatus.ofDisconnected();
    }

    public boolean isCurrentLinkConnected() {
        return getLinkStatus().connected();
    }

    public int getCurrentLinkStatusCode() {
        LinkStatus status = getLinkStatus();
        if (shouldPreferOutOfRangeMessage(status)) {
            return 6;
        }
        if (status == null || status.connected()) {
            return 0;
        }
        if (Status.NotPowered.is(status)) {
            return 1;
        }
        if (Status.NoUpgrade.is(status)) {
            return 2;
        }
        if (Status.BridgeNotFound.is(status)) {
            return 3;
        }
        if (Status.DifferentNetworks.is(status)) {
            return 4;
        }
        if (Status.NoSingularity.is(status)) {
            return 5;
        }
        return -1;
    }

    /**
     * 供物品打开前置判断使用：
     * 服务端只要普通无线链路可用，或量子链路已连通/仅网络未供电，就允许打开菜单。
     */
    public boolean canOpenFromAnyLink() {
        if (getPlayer().level().isClientSide()) {
            return false;
        }
        forceConnectedAccessPointUpdate = true;
        try {
            updateConnectedAccessPointState();
        } finally {
            forceConnectedAccessPointUpdate = false;
        }
        IGridNode vanillaNode = super.getActionableNode();
        if (vanillaNode != null && vanillaNode.getGrid() != null) {
            return true;
        }
        return quantumStatus != null && (quantumStatus.connected() || Status.NotPowered.is(quantumStatus));
    }

    @Nullable
    public net.minecraft.network.chat.Component getCurrentLinkStatusDescription() {
        LinkStatus status = getLinkStatus();
        if (shouldPreferOutOfRangeMessage(status)) {
            return PlayerMessages.OutOfRange.text();
        }
        return status == null ? null : status.statusDescription();
    }

    private void updateConnectedAccessPointState() {
        if (getPlayer().level().isClientSide()) {
            debugRepoState("updateConnectedAccessPoint/client-pass-through");
            return;
        }
        long tick = getPlayer().level().getGameTime();
        if (!forceConnectedAccessPointUpdate && lastConnectedAccessPointUpdateTick == tick) {
            skippedConnectedAccessPointUpdates++;
            return;
        }
        lastConnectedAccessPointUpdateTick = tick;
        super.rangeCheck();
        quantumStatus = isQuantumLinked();
        IGridNode preferredNode = resolveCurrentPreferredNode();
        if (preferredNode != null && preferredNode.getGrid() != null) {
            rememberStableConnection(preferredNode, LinkStatus.ofConnected());
        }
        debugRepoState("updateConnectedAccessPoint");
    }

    private boolean canStayOpenOnCurrentLink() {
        return quantumStatus != null && (quantumStatus.connected() || Status.NotPowered.is(quantumStatus))
                || super.getActionableNode() != null;
    }

    private boolean shouldPreferOutOfRangeMessage(@Nullable LinkStatus status) {
        if (status == null || status.connected() || Status.NotPowered.is(status)) {
            return false;
        }
        if (!(Status.NoSingularity.is(status) || Status.NoUpgrade.is(status))) {
            return false;
        }
        if (super.getActionableNode() != null) {
            return false;
        }
        if (!(getItemStack().getItem() instanceof WirelessComprehensiveWorkTerminalItem item)) {
            return false;
        }
        return item.getLinkedGrid(getItemStack(), getPlayer().level(), null) != null;
    }

    /**
     * AE2 的 {@code MEStorageMenu.broadcastChanges()} 会先读取 host 的链路状态/库存，
     * 然后才在更底层的 {@code AEBaseMenu.broadcastChanges()} 中调用 item host 的 {@code tick()}。
     * 对无线终端来说，这会让首帧同步偶尔使用到上一拍的连接状态。
     */
    public void refreshMenuSyncState() {
        long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
        long updateConnectedNs = 0L;
        long updateLinkNs = 0L;
        long stageStartNs = DEBUG_PERF ? System.nanoTime() : 0L;
        forceConnectedAccessPointUpdate = true;
        try {
            updateConnectedAccessPointState();
        } finally {
            forceConnectedAccessPointUpdate = false;
        }
        if (DEBUG_PERF) {
            updateConnectedNs = System.nanoTime() - stageStartNs;
            stageStartNs = System.nanoTime();
        }
        updateLinkStatusState();
        if (DEBUG_PERF) {
            updateLinkNs = System.nanoTime() - stageStartNs;
            long totalNs = System.nanoTime() - startNs;
            if (totalNs >= PERF_LOG_THRESHOLD_NS
                    || updateConnectedNs >= PERF_LOG_THRESHOLD_NS
                    || updateLinkNs >= PERF_LOG_THRESHOLD_NS) {
                var player = getPlayer();
                WcwtMod.LOGGER.info(
                        "WCWT perf: refreshMenuSyncState player={}, totalMs={}, updateConnectedMs={}, updateLinkMs={}, quantumConnected={}, linkConnected={}, hasStableNode={}",
                        player != null ? player.getScoreboardName() : "<unknown>",
                        String.format(java.util.Locale.ROOT, "%.3f", totalNs / 1_000_000.0D),
                        String.format(java.util.Locale.ROOT, "%.3f", updateConnectedNs / 1_000_000.0D),
                        String.format(java.util.Locale.ROOT, "%.3f", updateLinkNs / 1_000_000.0D),
                        quantumStatus != null && quantumStatus.connected(),
                        getLinkStatus() != null && getLinkStatus().connected(),
                        cachedStableActionableNode != null && cachedStableActionableNode.getGrid() != null);
            }
        }
        debugRepoState("refreshMenuSyncState");
    }

    public int consumeSkippedConnectedAccessPointUpdates() {
        int skipped = skippedConnectedAccessPointUpdates;
        skippedConnectedAccessPointUpdates = 0;
        return skipped;
    }

    private static String formatPerfMs(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    @Nullable
    private IGridNode resolveCurrentPreferredNode() {
        if (quantumStatus != null && quantumStatus.connected() && quantumBridge != null) {
            IGridNode quantumNode = quantumBridge.getActionableNode();
            if (quantumNode != null && quantumNode.getGrid() != null) {
                return quantumNode;
            }
        }
        IGridNode superNode = super.getActionableNode();
        if (superNode != null && superNode.getGrid() != null) {
            return superNode;
        }
        return null;
    }

    private void rememberStableConnection(@Nullable IGridNode node, LinkStatus status) {
        if (node == null || node.getGrid() == null || !status.connected()) {
            return;
        }
        cachedStableActionableNode = node;
        cachedStableLinkStatus = status;
        lastStableConnectionTick = getPlayer().level().getGameTime();
    }

    private boolean hasStableConnectionSnapshot() {
        return cachedStableActionableNode != null && cachedStableActionableNode.getGrid() != null;
    }

    private boolean isWithinTransientDisconnectGrace() {
        if (!hasStableConnectionSnapshot()) {
            return false;
        }
        long tick = getPlayer().level().getGameTime();
        return tick - lastStableConnectionTick <= TRANSIENT_DISCONNECT_GRACE_TICKS;
    }

    private static boolean allowsTransientDisconnectGrace(@Nullable LinkStatus status) {
        if (status == null || status.connected()) {
            return true;
        }
        if (Status.NotPowered.is(status)) {
            return false;
        }
        var description = status.statusDescription();
        return description == null;
    }

    private void debugRepoState(String source) {
        if (!DEBUG_REPO) {
            return;
        }
        long tick = getPlayer().level().getGameTime();
        String currentState = "superLink=" + (super.getActionableNode() != null)
                + ", quantumStatus=" + quantumStatus
                + ", effectiveLink=" + effectiveLinkStatus
                + ", quantumBridge=" + (quantumBridge != null)
                + ", actionableNode=" + (super.getActionableNode() != null)
                + ", levelClient=" + getPlayer().level().isClientSide();
        boolean stateChanged = !currentState.equals(lastRepoDebugState);
        boolean sampleDue = tick - lastRepoDebugTick >= 20L;
        boolean changedDue = stateChanged && tick - lastRepoDebugTick >= 5L;
        if (lastRepoDebugTick != Long.MIN_VALUE && !sampleDue && !changedDue) {
            suppressedRepoDebugLogs++;
            return;
        }
        int suppressed = suppressedRepoDebugLogs;
        suppressedRepoDebugLogs = 0;
        lastRepoDebugTick = tick;
        lastRepoDebugState = currentState;
        WcwtMod.LOGGER.info(
                "WCWT repo debug: host source={} player={} {} suppressed={}",
                source,
                getPlayer().getScoreboardName(),
                currentState,
                suppressed);
    }

    private ActionHostResult findQuantumBridge(long frequency) {
        IActionHost bridge = Locatables.quantumNetworkBridges().get(getPlayer().level(), frequency);
        if (bridge == null) {
            bridge = Locatables.quantumNetworkBridges().get(getPlayer().level(), -frequency);
        }
        return bridge == null ? ActionHostResult.invalid(Status.BridgeNotFound) : ActionHostResult.valid(bridge);
    }

    private LongResult getQEFrequency() {
        CompoundTag root = getRootTag(getItemStack());
        ItemStack singularity = root != null && root.contains(SINGULARITY_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(SINGULARITY_TAG))
                : ItemStack.EMPTY;
        if (singularity.isEmpty()) {
            return LongResult.invalid(Status.NoSingularity);
        }
        CompoundTag tag = singularity.getTag();
        if (tag == null || !tag.contains(QuantumBridgeBlockEntity.TAG_FREQUENCY, Tag.TAG_LONG)) {
            return LongResult.invalid(Status.GenericInvalid);
        }
        return LongResult.valid(tag.getLong(QuantumBridgeBlockEntity.TAG_FREQUENCY));
    }

    private LinkStatus isQuantumLinked() {
        Status status = Status.Valid;
        if (!AE2wtlibAPI.hasQuantumBridgeCard(this::getUpgrades)) {
            status = Status.NoUpgrade;
        }

        LongResult frequency = getQEFrequency();
        if (!frequency.valid()) {
            status = status.isValid() ? frequency.status() : Status.GenericInvalid;
        }
        if (!status.isValid()) {
            return status.toLinkStatus();
        }

        long id = frequency.result();
        if (quantumBridge instanceof QuantumCluster cluster) {
            if (cluster.getCenter() == null) {
                quantumBridge = null;
                return Status.BridgeNotFound.toLinkStatus();
            }
            long otherId = cluster.getCenter().getQEFrequency();
            if (otherId != id && otherId != -id) {
                quantumBridge = null;
            }
        }
        if (quantumBridge == null) {
            var result = findQuantumBridge(id);
            quantumBridge = result.host();
            if (result.invalid()) {
                return result.status().toLinkStatus();
            }
        }

        if (quantumBridge == null || quantumBridge.getActionableNode() == null
                || quantumBridge.getActionableNode().getGrid() == null) {
            return Status.BridgeNotFound.toLinkStatus();
        }

        var linkedGrid = getItemStack().getItem() instanceof WirelessComprehensiveWorkTerminalItem item
                ? item.getLinkedGrid(getItemStack(), getPlayer().level(), null)
                : null;
        var quantumGrid = quantumBridge.getActionableNode().getGrid();
        // 仅靠量子隧穿、没有范围内 WAP 时，linkedGrid 与量子侧网格偶尔会判成「两套网」而实际上应走量子。
        // 若玩家当前没有任何可用的无线接入点附着点，则不拦截，允许纯量子链路。
        if (linkedGrid != null
                && quantumGrid != linkedGrid
                && super.getActionableNode() != null) {
            return Status.DifferentNetworks.toLinkStatus();
        }
        if (!quantumGrid.getEnergyService().isNetworkPowered()) {
            return Status.NotPowered.toLinkStatus();
        }
        return LinkStatus.ofConnected();
    }

    protected double getPowerDrainPerTick() {
        if (quantumStatus != null && (quantumStatus.connected() || Status.NotPowered.is(quantumStatus))) {
            return 22.5;
        }
        return 0.5;
    }

    public boolean consumeIdlePower(Actionable action) {
        if (action == Actionable.SIMULATE) {
            rechargeFromQuantumGrid();
        }
        boolean success = action == Actionable.SIMULATE || drainPower();
        if (action == Actionable.SIMULATE) {
            rechargeFromQuantumGrid();
        }
        return success;
    }

    private void rechargeFromQuantumGrid() {
        if (quantumStatus == null || (!quantumStatus.connected() && !Status.NotPowered.is(quantumStatus))) {
            return;
        }
        if (!(getItemStack().getItem() instanceof AEBasePoweredItem item)) {
            return;
        }
        IGridNode node = getActionableNode();
        if (node == null || node.getGrid() == null) {
            return;
        }

        double missing = item.getAEMaxPower(getItemStack()) - item.getAECurrentPower(getItemStack());
        if (missing <= 0) {
            return;
        }
        var energyService = node.getGrid().getEnergyService();
        double safePower = energyService.getStoredPower() - energyService.getMaxStoredPower() / 2;
        if (safePower <= 0) {
            return;
        }
        double extracted = energyService.extractAEPower(Math.min(missing, safePower),
                Actionable.MODULATE, PowerMultiplier.ONE);
        item.injectAEPower(getItemStack(), extracted, Actionable.MODULATE);
    }
    
    // 实现IExtendedUIHost接口
    @Override
    public ExtendedUIType getCurrentExtendedUI() {
        return currentExtendedUI;
    }
    
    @Override
    public void setCurrentExtendedUI(ExtendedUIType type) {
        this.currentExtendedUI = type;
        // 当关闭扩展UI时，清除样板选中状态
        if (type == ExtendedUIType.NONE) {
            clearSelection();
        }
    }
    
    // 实现IPatternCachingHost接口
    @Override
    public InternalInventory getPatternCacheInventory() {
        return patternCacheInv;
    }
    
    @Override
    public int getSelectedPatternIndex() {
        return selectedPatternIndex;
    }
    
    @Override
    public void setSelectedPatternIndex(int index) {
        this.selectedPatternIndex = index;
    }
    
    // 实现ICraftingLockHost接口
    @Override
    public boolean isCraftingGridLocked() {
        return craftingGridLocked;
    }
    
    @Override
    public void setCraftingGridLocked(boolean locked) {
        this.craftingGridLocked = locked;
        setDataBoolean(ModComponents.CRAFTING_GRID_LOCKED, locked);
    }

    public boolean isPatternManagementUploadEnabled() {
        return patternManagementUploadEnabled;
    }

    public void setPatternManagementUploadEnabled(boolean enabled) {
        this.patternManagementUploadEnabled = enabled;
        setDataBoolean(ModComponents.PATTERN_MANAGEMENT_UPLOAD_ENABLED, enabled);
    }

    public int getPatternManagementDisplayMode() {
        return patternManagementDisplayMode;
    }

    public void setPatternManagementDisplayMode(int mode) {
        this.patternManagementDisplayMode = mode;
        setDataInt(ModComponents.PATTERN_MANAGEMENT_DISPLAY_MODE, mode);
    }

    public boolean isPatternManagementShowSlots() {
        return patternManagementShowSlots;
    }

    public void setPatternManagementShowSlots(boolean showSlots) {
        this.patternManagementShowSlots = showSlots;
        setDataBoolean(ModComponents.PATTERN_MANAGEMENT_SHOW_SLOTS, showSlots);
    }

    public int getPatternManagementSearchMode() {
        return patternManagementSearchMode;
    }

    public void setPatternManagementSearchMode(int mode) {
        this.patternManagementSearchMode = mode;
        setDataInt(ModComponents.PATTERN_MANAGEMENT_SEARCH_MODE, mode);
    }

    public int getManualWorkspaceMode() {
        return manualWorkspaceMode;
    }

    public void setManualWorkspaceMode(int mode) {
        this.manualWorkspaceMode = mode;
        setDataInt(ModComponents.MANUAL_WORKSPACE_MODE, mode);
    }

    public String getManualAnvilName() {
        return manualAnvilName;
    }

    public void setManualAnvilName(String name) {
        this.manualAnvilName = name == null ? "" : name;
        setDataString(ModComponents.MANUAL_ANVIL_NAME, this.manualAnvilName);
    }

    private CompoundTag getOrCreateRootData() {
        CompoundTag stackTag = getItemStack().getOrCreateTag();
        CompoundTag root = stackTag.getCompound(ModComponents.ROOT_TAG);
        stackTag.put(ModComponents.ROOT_TAG, root);
        return root;
    }

    private CompoundTag getOrCreateDataTag(String key) {
        CompoundTag root = getOrCreateRootData();
        CompoundTag tag = root.getCompound(key);
        root.put(key, tag);
        return tag;
    }

    private CompoundTag getDataTag(String key) {
        CompoundTag stackTag = getItemStack().getTag();
        if (stackTag == null || !stackTag.contains(ModComponents.ROOT_TAG, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        CompoundTag root = stackTag.getCompound(ModComponents.ROOT_TAG);
        return root.contains(key, Tag.TAG_COMPOUND) ? root.getCompound(key) : new CompoundTag();
    }

    private boolean getDataBoolean(String key, boolean fallback) {
        CompoundTag root = getOrCreateRootData();
        return root.contains(key, Tag.TAG_BYTE) ? root.getBoolean(key) : fallback;
    }

    private int getDataInt(String key, int fallback) {
        CompoundTag root = getOrCreateRootData();
        return root.contains(key, Tag.TAG_INT) ? root.getInt(key) : fallback;
    }

    private String getDataString(String key, String fallback) {
        CompoundTag root = getOrCreateRootData();
        return root.contains(key, Tag.TAG_STRING) ? root.getString(key) : fallback;
    }

    private void setDataBoolean(String key, boolean value) {
        getOrCreateRootData().putBoolean(key, value);
    }

    private void setDataInt(String key, int value) {
        getOrCreateRootData().putInt(key, value);
    }

    private void setDataString(String key, String value) {
        if (value == null || value.isBlank()) {
            CompoundTag root = getOrCreateRootData();
            root.remove(key);
        } else {
            getOrCreateRootData().putString(key, value);
        }
    }
    
    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(INV_SINGULARITY)) {
            return singularityInv;
        } else if (id.equals(INV_AE2WTLIB_ARMOR)) {
            return ae2wtlibArmorInv;
        } else if (id.equals(INV_DECORATIVE_ARMOR)) {
            return decorativeArmorInv;
        } else if (id.equals(INV_CURIOS)) {
            return curiosInv;
        } else if (id.equals(INV_ADVANCED_PATTERN)) {
            return advancedPatternInv;
        } else if (id.equals(INV_PATTERN_CACHE)) {
            return patternCacheInv;
        } else if (id.equals(INV_TOOLKIT)) {
            return toolkitInv;
        } else if (id.equals(INV_STORAGE_CELL)) {
            return storageCellInv;
        } else if (id.equals(INV_RESONATING_PATTERN_CACHE)) {
            return resonatingPatternCacheInv;
        } else if (id.equals(INV_MANUAL_SMITHING)) {
            return manualSmithingInv;
        } else if (id.equals(INV_MANUAL_ANVIL)) {
            return manualAnvilInv;
        } else if (id.equals(INV_TRASH)) {
            return trashInv;
        } else {
            // 尝试从父类获取（例如crafting grid）
            return super.getSubInventory(id);
        }
    }

    public ItemStack getStorageCellItem() {
        return storageCellInv.getStackInSlot(0);
    }

    public ICellWorkbenchItem getStorageCellWorkbenchItem() {
        var stack = getStorageCellItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ICellWorkbenchItem cell) {
            return cell;
        }
        return null;
    }

    public ConfigInventory getMagnetPickupConfig() {
        return magnetPickupConfig;
    }

    public ConfigInventory getMagnetInsertConfig() {
        return magnetInsertConfig;
    }

    public IncludeExclude getMagnetPickupMode() {
        return getDataBoolean("pickup_mode_whitelist", false) ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    public IncludeExclude getMagnetInsertMode() {
        return getDataBoolean("insert_mode_whitelist", false) ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    public void toggleMagnetPickupMode() {
        setDataBoolean("pickup_mode_whitelist", toggle(getMagnetPickupMode()) == IncludeExclude.WHITELIST);
    }

    public void toggleMagnetInsertMode() {
        setDataBoolean("insert_mode_whitelist", toggle(getMagnetInsertMode()) == IncludeExclude.WHITELIST);
    }

    public void copyMagnetPickupToInsert() {
        CompoundTag tag = getDataTag(PICKUP_CONFIG_TAG);
        magnetInsertConfig.readFromChildTag(tag, "");
        updateMagnetInsertConfig();
    }

    public void copyMagnetInsertToPickup() {
        CompoundTag tag = getDataTag(INSERT_CONFIG_TAG);
        magnetPickupConfig.readFromChildTag(tag, "");
        updateMagnetPickupConfig();
    }

    public void switchMagnetFilters() {
        CompoundTag pickupTag = getDataTag(PICKUP_CONFIG_TAG);
        CompoundTag insertTag = getDataTag(INSERT_CONFIG_TAG);
        magnetPickupConfig.readFromChildTag(insertTag, "");
        magnetInsertConfig.readFromChildTag(pickupTag, "");
        updateMagnetPickupConfig();
        updateMagnetInsertConfig();
    }

    private IncludeExclude toggle(IncludeExclude includeExclude) {
        return includeExclude == IncludeExclude.WHITELIST ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST;
    }

    private void updateMagnetPickupConfig() {
        CompoundTag tag = getOrCreateDataTag(PICKUP_CONFIG_TAG);
        magnetPickupConfig.writeToChildTag(tag, "");
    }

    private void updateMagnetInsertConfig() {
        CompoundTag tag = getOrCreateDataTag(INSERT_CONFIG_TAG);
        magnetInsertConfig.writeToChildTag(tag, "");
    }

    public void setCellCopyMode(CopyMode mode) {
        this.cellCopyMode = mode;
        if (getStorageCellWorkbenchItem() == null && mode == CopyMode.CLEAR_ON_REMOVE) {
            cellConfigInv.clear();
        }
    }

    @Override
    public GenericStackInv getConfig() {
        return cellConfigInv;
    }

    public ConfigInventory getActualCellConfig() {
        if (cachedCellConfig == null) {
            var cell = getStorageCellWorkbenchItem();
            var stack = getStorageCellItem();
            if (cell == null || stack.isEmpty()) {
                return null;
            }
            cachedCellConfig = cell.getConfigInventory(stack);
        }
        return cachedCellConfig;
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return patternEncodingLogic;
    }

    @Override
    public net.minecraft.world.level.Level getLevel() {
        return getPlayer().level();
    }

    @Override
    public void markForSave() {
        var tag = getOrCreateDataTag(PATTERN_ENCODING_LOGIC_TAG);
        patternEncodingLogic.writeToNBT(tag);
    }

    public void syncCellConfigMirrorFromActual() {
        if (cellConfigLocked) {
            return;
        }
        cellConfigLocked = true;
        try {
            var actual = getActualCellConfig();
            if (actual != null) {
                copy(actual, cellConfigInv);
            }
        } finally {
            cellConfigLocked = false;
        }
    }

    /**
     * 返回元件自身的升级卡 inventory（lazily 读取一次后缓存）。
     * 元件未放入 → 返回 {@link UpgradeInventories#empty()}（size==0），
     * 配合 {@link com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu#isSlotEnabled} 让 8 个升级槽全部消失。
     */
    public IUpgradeInventory getCellUpgrades() {
        if (cachedCellUpgrades != null) return cachedCellUpgrades;
        var cell = getStorageCellWorkbenchItem();
        var stack = getStorageCellItem();
        if (cell == null || stack.isEmpty()) {
            return cachedCellUpgrades = UpgradeInventories.empty();
        }
        var inv = cell.getUpgrades(stack);
        return cachedCellUpgrades = (inv != null ? inv : UpgradeInventories.empty());
    }

    private void invalidateCellCaches() {
        cachedCellConfig = null;
        cachedCellUpgrades = null;
    }

    /**
     * 元件的升级卡/config 都写在元件 ItemStack 自己的 DataComponent 上。
     * 这些内部组件变化不会自动触发外层终端的 storageCellInv 保存，所以需要显式重写一遍当前槽位，
     * 让终端 ItemStack 上的 STORAGE_CELL_INV 组件和菜单同步都拿到新元件栈。
     */
    public void persistStorageCellItem() {
        var stack = storageCellInv.getStackInSlot(0);
        storageCellInv.setItemDirect(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    private void storageCellChanged() {
        if (cellConfigLocked) {
            return;
        }

        cellConfigLocked = true;
        try {
            invalidateCellCaches();
            var actual = getActualCellConfig();
            if (actual != null) {
                if (!actual.isEmpty()) {
                    copy(actual, cellConfigInv);
                } else {
                    copy(cellConfigInv, actual);
                    copy(actual, cellConfigInv);
                }
            } else if (cellCopyMode == CopyMode.CLEAR_ON_REMOVE) {
                cellConfigInv.clear();
            }
        } finally {
            cellConfigLocked = false;
        }
    }

    private void cellConfigChanged() {
        if (cellConfigLocked) {
            return;
        }

        cellConfigLocked = true;
        try {
            var actual = getActualCellConfig();
            if (actual != null) {
                copy(cellConfigInv, actual);
                copy(actual, cellConfigInv);
            }
        } finally {
            cellConfigLocked = false;
        }
    }

    public static void copy(GenericStackInv from, GenericStackInv to) {
        for (int i = 0; i < Math.min(from.size(), to.size()); i++) {
            var fromStack = from.getStack(i);
            to.setStack(i, fromStack);
        }
        for (int i = from.size(); i < to.size(); i++) {
            to.setStack(i, null);
        }
    }
    
    /** 创建单槽缠绕态奇点库存，数据持久化到物品 NBT。 */
    private static InternalInventory createSingularityInv(Player player, ItemStack stack) {
        final AppEngInternalInventory[] invRef = new AppEngInternalInventory[1];
        var inv = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
                CompoundTag root = getOrCreateRootTag(stack);
                CompoundTag singularityTag = new CompoundTag();
                invRef[0].getStackInSlot(0).save(singularityTag);
                root.put("singularity", singularityTag);
            }
            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
            }
            @Override
            public boolean isClientSide() { return player.level().isClientSide(); }
        }, 1, 1);
        invRef[0] = inv;
        CompoundTag root = getRootTag(stack);
        inv.setItemDirect(0, root != null && root.contains("singularity", Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound("singularity"))
                : ItemStack.EMPTY);
        return inv;
    }

    private InternalInventory createStorageCellInventory(Player player, ItemStack stack) {
        final AppEngInternalInventory[] invRef = new AppEngInternalInventory[1];
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
                invRef[0].writeToNBT(getOrCreateRootTag(stack), ModComponents.STORAGE_CELL_INV);
            }

            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
                storageCellChanged();
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, 1);
        invRef[0] = inventory;
        inventory.readFromNBT(getOrCreateRootTag(stack), ModComponents.STORAGE_CELL_INV);
        storageCellChanged();
        return inventory;
    }

    private InternalInventory createToolkitInventory(Player player, ItemStack stack) {
        int toolkitSlots = WcwtServerConfig.toolkitSlotCount();
        final AppEngInternalInventory[] invRef = new AppEngInternalInventory[1];
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
                saveSharedToolkitInventory(player, stack, invRef[0]);
            }
            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, toolkitSlots);
        invRef[0] = inventory;
        loadSharedToolkitInventory(player, stack, inventory);
        inventory.setFilter(new ToolkitItemFilter());
        return inventory;
    }

    /**
     * 把一个会重复构建库存的 supplier 包成只构建一次并复用的版本。
     * 终端子库存以物品 NBT 为后端，菜单生命周期内 getItemStack() 是稳定实例，
     * 复用同一份内存库存可避免客户端 slot 同步被"用完即弃"的库存丢弃。
     */
    private static java.util.function.Supplier<InternalInventory> memoize(
            java.util.function.Supplier<InternalInventory> delegate) {
        return new java.util.function.Supplier<>() {
            private InternalInventory cached;

            @Override
            public InternalInventory get() {
                InternalInventory current = cached;
                if (current == null) {
                    current = delegate.get();
                    cached = current;
                }
                return current;
            }
        };
    }

    private static InternalInventory createInventory(Player player, ItemStack stack, String inventoryName, int size) {
        var componentType = switch (inventoryName) {
            case "ae2wtlib_armor" -> ModComponents.AE2WTLIB_ARMOR_INV;
            case "decorative_armor" -> ModComponents.DECORATIVE_ARMOR_INV;
            case "curios" -> ModComponents.CURIOS_INV;
            case "advanced_pattern" -> ModComponents.ADVANCED_PATTERN_INV;
            case "pattern_cache" -> ModComponents.PATTERN_CACHE_INV;
            case "toolkit" -> ModComponents.TOOLKIT_INV;
            case "view_cell" -> ModComponents.VIEW_CELL_INV;
            case "storage_cell"  -> ModComponents.STORAGE_CELL_INV;
            case "resonating_pattern_cache" -> ModComponents.RESONATING_PATTERN_CACHE_INV;
            case "manual_smithing" -> ModComponents.MANUAL_SMITHING_INV;
            case "manual_anvil" -> ModComponents.MANUAL_ANVIL_INV;
            default -> throw new IllegalArgumentException("Unknown inventory name: " + inventoryName);
        };
        
        final AppEngInternalInventory[] invRef = new AppEngInternalInventory[1];
        var inventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
                invRef[0].writeToNBT(getOrCreateRootTag(stack), componentType);
            }
            @Override
            public void onChangeInventory(InternalInventory inv, int slot) {
            }

            @Override
            public boolean isClientSide() {
                return player.level().isClientSide();
            }
        }, size);
        invRef[0] = inventory;
        
        inventory.readFromNBT(getOrCreateRootTag(stack), componentType);
        return inventory;
    }

    private static void loadSharedToolkitInventory(Player player, ItemStack terminalStack, AppEngInternalInventory inventory) {
        if (player.level().isClientSide()) {
            loadToolkitInventoryMirror(terminalStack, inventory);
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: loaded toolkit from item mirror on client for player={}, nonEmptySlots={}",
                        player.getScoreboardName(), countNonEmptySlots(inventory));
            }
            return;
        }
        var persisted = getOrCreateWcwtPlayerData(player);
        var sharedToolkitTag = persisted.getCompound(PLAYER_TOOLKIT_DATA_TAG);
        boolean hasSharedToolkit = !sharedToolkitTag.isEmpty();
        if (!hasSharedToolkit) {
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: no shared toolkit for player={}, attempting legacy migration",
                        player.getScoreboardName());
            }
            migrateLegacyToolkitIntoPlayer(player, terminalStack, inventory, persisted);
            return;
        }
        if (sharedToolkitTag.contains(TOOLKIT_ITEMS_TAG, Tag.TAG_LIST)) {
            readSharedToolkitSlots(player, sharedToolkitTag, inventory);
        } else {
            readLegacySharedToolkitContents(player, terminalStack, sharedToolkitTag, inventory);
        }
        saveToolkitInventoryMirror(terminalStack, inventory);
        if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: loaded shared toolkit for player={}, slotCount={}, nonEmptySlots={}",
                    player.getScoreboardName(), inventory.size(), countNonEmptySlots(inventory));
        }
    }

    private static void migrateLegacyToolkitIntoPlayer(Player player, ItemStack terminalStack,
                                                       AppEngInternalInventory inventory, CompoundTag persisted) {
        if (!persisted.contains(PLAYER_TOOLKIT_DATA_TAG)) {
            persisted.put(PLAYER_TOOLKIT_DATA_TAG, new CompoundTag());
        }
        inventory.readFromNBT(getOrCreateRootTag(terminalStack), ModComponents.TOOLKIT_INV);
        saveSharedToolkitInventory(player, terminalStack, inventory);
        CompoundTag root = getRootTag(terminalStack);
        if (root != null && root.contains(ModComponents.TOOLKIT_INV, Tag.TAG_LIST)) {
            root.remove(ModComponents.TOOLKIT_INV);
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: migrated legacy item-bound toolkit into shared store for player={}, nonEmptySlots={}",
                        player.getScoreboardName(), countNonEmptySlots(inventory));
            }
        }
    }

    private static void saveSharedToolkitInventory(Player player, ItemStack terminalStack, AppEngInternalInventory inventory) {
        var persisted = getOrCreateWcwtPlayerData(player);
        CompoundTag serialized = new CompoundTag();
        serialized.putInt(TOOLKIT_SIZE_TAG, inventory.size());
        ListTag items = new ListTag();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt(TOOLKIT_SLOT_TAG, slot);
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            entry.put(TOOLKIT_STACK_TAG, stackTag);
            items.add(entry);
        }
        serialized.put(TOOLKIT_ITEMS_TAG, items);
        persisted.put(PLAYER_TOOLKIT_DATA_TAG, serialized);
        saveToolkitInventoryMirror(terminalStack, inventory);
        if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: saved shared toolkit for player={}, slotCount={}, nonEmptySlots={}",
                    player.getScoreboardName(), inventory.size(), countNonEmptySlots(inventory));
        }
    }

    private static void readSharedToolkitSlots(Player player, CompoundTag sharedToolkitTag,
                                               AppEngInternalInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.setItemDirect(slot, ItemStack.EMPTY);
        }
        ListTag items = sharedToolkitTag.getList(TOOLKIT_ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt(TOOLKIT_SLOT_TAG);
            if (slot < 0 || slot >= inventory.size() || !entry.contains(TOOLKIT_STACK_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack stack = ItemStack.of(entry.getCompound(TOOLKIT_STACK_TAG));
            inventory.setItemDirect(slot, stack);
        }
    }

    private static void readLegacySharedToolkitContents(Player player, ItemStack terminalStack, CompoundTag sharedToolkitTag,
                                                        AppEngInternalInventory inventory) {
        inventory.readFromNBT(sharedToolkitTag, ModComponents.TOOLKIT_INV);
        saveSharedToolkitInventory(player, terminalStack, inventory);
        if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: upgraded legacy shared toolkit codec data for player={}, nonEmptySlots={}",
                    player.getScoreboardName(), countNonEmptySlots(inventory));
        }
    }

    private static void loadToolkitInventoryMirror(ItemStack terminalStack, AppEngInternalInventory inventory) {
        inventory.readFromNBT(getOrCreateRootTag(terminalStack), ModComponents.TOOLKIT_INV);
    }

    private static void saveToolkitInventoryMirror(ItemStack terminalStack, AppEngInternalInventory inventory) {
        inventory.writeToNBT(getOrCreateRootTag(terminalStack), ModComponents.TOOLKIT_INV);
    }

    private static int countNonEmptySlots(AppEngInternalInventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static CompoundTag getOrCreateWcwtPlayerData(Player player) {
        var persisted = player.getPersistentData().getCompound(PLAYER_PERSISTED_TAG);
        player.getPersistentData().put(PLAYER_PERSISTED_TAG, persisted);
        var wcwtData = persisted.getCompound(WCWT_PLAYER_DATA_TAG);
        persisted.put(WCWT_PLAYER_DATA_TAG, wcwtData);
        return wcwtData;
    }

    public static void setPendingExtendedUi(Player player, ExtendedUIType type) {
        var wcwtData = getOrCreateWcwtPlayerData(player);
        wcwtData.putInt(PLAYER_PENDING_EXTENDED_UI_TAG, type.ordinal());
        if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: set pending UI player={}, type={}",
                    player.getScoreboardName(), type);
        }
    }

    private static ExtendedUIType consumePendingExtendedUi(Player player) {
        var wcwtData = getOrCreateWcwtPlayerData(player);
        int ordinal = wcwtData.getInt(PLAYER_PENDING_EXTENDED_UI_TAG);
        wcwtData.remove(PLAYER_PENDING_EXTENDED_UI_TAG);
        if (ordinal >= 0 && ordinal < ExtendedUIType.values().length) {
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: consume pending UI player={}, ordinal={}",
                        player.getScoreboardName(), ordinal);
            }
            return ExtendedUIType.values()[ordinal];
        }
        if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: no pending UI for player={}", player.getScoreboardName());
        }
        return ExtendedUIType.NONE;
    }

    private static final class ToolkitItemFilter implements appeng.util.inv.filter.IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return ToolkitItemRules.mayPlace(slot, stack);
        }
    }

    /** @deprecated 请使用 {@link ToolkitItemRules#mayPlace(int, ItemStack)} 或 {@link ToolkitItemRules#isBaseToolkitCandidate(ItemStack)} */
    @Deprecated
    public static boolean isToolkitItem(ItemStack stack) {
        return ToolkitItemRules.isBaseToolkitCandidate(stack);
    }
}

