package com.lhy.wcwt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Tooltip;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.me.items.CraftingTermScreen;
import appeng.client.gui.style.Blitter;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.network.ServerboundPacket;
import appeng.client.gui.NumberEntryType;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.slot.AppEngSlot;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.Point;
import appeng.core.localization.ButtonToolTips;
import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import appeng.integration.abstraction.ItemListMod;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.api.stacks.AEKeyType;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.gui.ScrollingUpgradesPanel;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.compat.ExtendedAePlusUploadCompat;
import com.lhy.wcwt.compat.JecSearchCompat;
import com.lhy.wcwt.compat.WcwtOptionalFeatureGates;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.client.WcwtKeybindings;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import com.lhy.wcwt.client.gui.panels.*;
import com.lhy.wcwt.client.gui.widgets.*;
import com.lhy.wcwt.client.gui.widgets.IconButton;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.CraftingLockPacket;
import com.lhy.wcwt.network.EncodePatternPacket;
import com.lhy.wcwt.network.ExtendedUIPacket;
import com.lhy.wcwt.network.ManualAnvilNamePacket;
import com.lhy.wcwt.network.ManualWorkspaceModePacket;
import com.lhy.wcwt.network.OpenToolkitHotkeyPacket;
import com.lhy.wcwt.network.PatternMultiplierPacket;
import com.lhy.wcwt.network.PatternModePacket;
import com.lhy.wcwt.network.PatternManagementActionPacket;
import com.lhy.wcwt.network.PatternManagementUploadSettingPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.PatternProviderSlotSyncPacket;
import com.lhy.wcwt.network.PatternSelectionPacket;
import com.lhy.wcwt.network.PatternEncodingOptionPacket;
import com.lhy.wcwt.network.StonecuttingRecipeSelectionPacket;
import com.lhy.wcwt.network.ToolkitNetworkToolDepositPacket;
import com.lhy.wcwt.network.ToolkitMemorySlotPacket;
import com.lhy.wcwt.network.CycleProcessingOutputPacket;
import com.lhy.wcwt.network.WirelessSettingsPacket;
import com.lhy.wcwt.util.PatternUploadMetadata;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu.WcwtActivatableSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.network.PacketDistributor;
import appeng.parts.encoding.EncodingMode;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.google.common.primitives.Longs;

/**
 * 无线综合工作终端界面
 * 集成了多个AE2附属模组的功能
 */
public class WirelessComprehensiveWorkTerminalScreen extends CraftingTermScreen<WirelessComprehensiveWorkTerminalMenu> {
    private static final String STYLE_PATH = "/screens/wcwt/wireless_comprehensive_work_terminal.json";
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");
    private static final boolean DEBUG_SLOT_HIT = Boolean.getBoolean("wcwt.debug.slotHit");
    private static final boolean DEBUG_PATTERN_UPLOAD = Boolean.getBoolean("wcwt.debug.patternUpload");
    /**
     * 诊断「工具包界面关闭时存取延迟极高」用：每秒聚合一次每帧驱动服务端同步的几个动作的触发次数，
     * 同时打印工具包面板状态，方便对比开/关时哪个动作在每帧翻转。开关：-Dwcwt.debug.frameSync=true
     */
    private static final boolean DEBUG_FRAME_SYNC = Boolean.getBoolean("wcwt.debug.frameSync");
    private static final long PERF_LOG_THRESHOLD_NS = 1_000_000L;
    private static final long PATTERN_PROVIDER_REFRESH_DEBOUNCE_MS = 180L;
    private static final long PATTERN_PROVIDER_SUBSCRIPTION_KEEPALIVE_MS = 3_000L;

    // === DEBUG_FRAME_SYNC 计数器（仅诊断用，每秒 flush 一次） ===
    private long frameSyncWindowStartMs;
    private int frameSyncFrames;
    private int frameSyncProviderListRequests;
    private int frameSyncProviderSlotPacketSends;
    private int frameSyncProviderSlotRebuilds;
    private int frameSyncRepoUpdateViews;
    
    // 扩展UI按钮
    private ExtendedUIButton advancedCodingButton;
    private ExtendedUIButton cosmeticArmorButton;
    private ExtendedUIButton curiosButton;
    private ExtendedUIButton toolboxButton;
    private ExtendedUIButton toolkitButton;
    private ExtendedUIButton resonatingLightningPatternCodingButton;
    private FavoriteItemsButton favoriteItemsButton;
    private ViewCellsToggleButton viewCellsToggleButton;
    private WcwtUniversalTerminalButton universalTerminalButton;
    private boolean viewCellsVisible = WcwtClientConfig.lastViewCellsPanelVisible();
    private @Nullable ViewCellsVisibilityWidget viewCellsVisibilityWidget;
    private @Nullable Renderable viewCellsOverlayRenderable;
    private @Nullable Renderable toolkitMemoryOverlayRenderable;

    /** 4 个样板模式 tab 按钮中**最顶**的那个（modeTabButton3）。扩展按钮 Y 锚到它的顶部。*/
    private TabButton topModeTabButton;
    /** 4 个样板模式 tab 按钮中**最底**的那个（modeTabButton0）。用于反推升级槽底部位置。*/
    private TabButton bottomModeTabButton;
    private TabButton tabCrafting;
    private TabButton tabProcessing;
    private TabButton tabSmithing;
    private TabButton tabStonecutting;
    private EncodingMode patternEncodingMode = EncodingMode.PROCESSING;
    private EncodingMode lastSyncedPatternEncodingMode = EncodingMode.PROCESSING;
    
    // 合成锁定按钮
    private CraftingLockButton craftingLockButton;
    private IconButton manualCraftingButton;
    private IconButton manualSmithingButton;
    private IconButton manualAnvilButton;
    private EditBox manualAnvilNameField;
    private boolean syncingManualAnvilNameField;
    private @Nullable AbstractWidget clearCraftingGridButton;
    private @Nullable AbstractWidget clearToPlayerInvButton;
    private ActionButton encodePatternButton;
    private ActionButton clearPatternEncodingButton;
    private WcwtProcessingMaterialsMergeButton processingMaterialsMergeButton;
    private ActionButton cycleProcessingOutputButton;
    private ToggleButton patternSubstitutionButton;
    private ToggleButton patternFluidSubstitutionButton;
    private ToggleButton manualPatternSubstitutionButton;
    private ToggleButton manualPatternFluidSubstitutionButton;
    
    // 样板倍增按钮
    private PatternMultiplierButton[] multiplierButtons;
    
    // 过滤显示按钮
    private ItemDisplayButton itemDisplayButton;
    private FluidDisplayButton fluidDisplayButton;
    private OtherTypesDisplayButton otherTypesDisplayButton;
    private TopActionButton muteButton;
    private TopActionButton wirelessTerminalSettingsButton;
    private TopActionButton magnetCardMenuButton;
    private TopActionButton trashButton;
    
    // 扩展UI面板
    private AdvancedCodingPanel advancedCodingPanel;
    private CosmeticArmorPanel cosmeticArmorPanel;
    private CuriosPanel curiosPanel;
    private ToolboxPanel toolboxPanel;
    private ToolkitPanel toolkitPanel;
    private ResonatingLightningPatternCodingPanel resonatingLightningPatternCodingPanel;

    // 滚动升级槽面板（当前恢复为 WTLib 原版实现）
    // 如果以后想恢复“奇点槽始终占位、不随量子桥卡出现而跳位”的自定义行为：
    // 1. 把这里改回 `private WcwtScrollingUpgradesPanel upgradesPanel;`
    // 2. 构造器里把 `new ScrollingUpgradesPanel(...)` 改回 `new WcwtScrollingUpgradesPanel(...)`
    private ScrollingUpgradesPanel upgradesPanel;
    /** 元件工作台升级槽滚动面板：显示在右侧 WTLib 升级槽面板正下方，默认 2 行。 */
    private CellScrollingUpgradesPanel cellUpgradesPanel;
    /** 样板缓存区滑块：36槽库存，主界面只显示2行×9列。 */
    private Scrollbar patternCacheScrollbar;
    private Scrollbar patternEncodingScrollbar;
    private Scrollbar stonecuttingPatternScrollbar;
    /** 饰品界面滑块：Curios 真实槽位按 6 列、9 行分页显示。 */
    private Scrollbar curiosScrollbar;
    /** 工具包界面滑块：工具按 6 列、9 行分页显示。 */
    private Scrollbar toolkitScrollbar;
    /** 谐振过载编码器面板内的过载条目滑块由面板自身维护，这里只需要终端里的实体槽定位。 */
    /** 样板管理区滑块：按供应器行滚动。 */
    private Scrollbar patternManagementScrollbar;
    private AETextField patternManageSearchField;
    private AETextField patternManageMappingField;
    private int extendedUiAvailabilityMask = -1;
    private int lastExtendedUiButtonMask = -1;
    private IExtendedUIHost.ExtendedUIType lastExtendedUiVisibilityType = null;
    private boolean lastExtendedUiToolkitInManagementArea;
    private boolean extendedUiVisibilityDirty = true;
    private boolean panelSlotActivityDirty = true;
    private boolean lastAdvancedPanelOpen;
    private boolean lastResonatingPanelOpen;
    private boolean lastCosmeticPanelOpen;
    private boolean lastCuriosPanelOpen;
    private boolean lastToolkitPanelOpen;
    private boolean lastToolkitInManagementArea;
    private boolean lastToolboxPanelOpen;
    private boolean lastCellUpgradesPanelVisible;
    private int lastCuriosSlotScroll = Integer.MIN_VALUE;
    private int lastToolkitSlotScroll = Integer.MIN_VALUE;
    private int lastManagementToolkitSlotScroll = Integer.MIN_VALUE;
    private boolean patternProviderSlotLayoutDirty = true;
    private boolean lastPatternProviderSlotsInToolkitArea;
    private int lastPatternProviderSlotScroll = Integer.MIN_VALUE;
    private int lastPatternProviderRowCount = -1;
    private final List<PatternProviderListPacket.Entry> patternProviders = new ArrayList<>();
    private final List<PatternManagementRow> patternManagementRows = new ArrayList<>();
    private final Set<PatternManagementSlotKey> patternManagementSearchHighlightSlots = new HashSet<>();
    private long selectedPatternProviderId = -1;
    private int selectedPatternProviderSlot = -1;
    private long focusedPatternProviderId = -1;
    private int focusedPatternProviderSlot = -1;
    private long focusedPatternProviderUntilMs;
    private boolean requestedPatternProviders;
    private long lastPatternProviderRefreshRequestMs;
    private long lastPatternProviderSubscriptionRequestMs;
    private String resolvedPatternManagementSearchText = "";
    private List<PatternProviderSlotSyncPacket.Mapping> lastPatternProviderSlotSync = List.of();
    private boolean forcePatternProviderSlotSnapshot;
    private final ExtendedPanelLayout mainLayout = ExtendedPanelLayout.load(
            ResourceLocation.fromNamespaceAndPath("ae2", "screens/wcwt/wireless_comprehensive_work_terminal.json"));
    private final ExtendedPanelLayout managementToolkitLayout = ExtendedPanelLayout.load("wcwt_management_toolkit.json");
    private ExtendedPanelLayout.Rect patternManagementPage =
            new ExtendedPanelLayout.Rect(176, 210, 160, 71);
    private ExtendedPanelLayout.Rect patternManagementScrollbarRect =
            new ExtendedPanelLayout.Rect(343, 210, 12, 71);
    private ExtendedPanelLayout.Rect pinnedRowOverlayRect =
            new ExtendedPanelLayout.Rect(7, 272, 324, 18);
    private ExtendedPanelLayout.Rect managementToolkitBackgroundRect =
            new ExtendedPanelLayout.Rect(176, 210, 162, 72);
    private ExtendedPanelLayout.Rect managementToolkitSlotRect =
            new ExtendedPanelLayout.Rect(177, 211, 16, 16);
    private ExtendedPanelLayout.Rect managementToolkitScrollbarRect =
            new ExtendedPanelLayout.Rect(343, 221, 12, 60);
    private ExtendedPanelLayout.Rect toolkitMemoryButton =
            new ExtendedPanelLayout.Rect(0, 0, 12, 12);
    private ExtendedPanelLayout.Rect managementToolkitMemoryButton =
            new ExtendedPanelLayout.Rect(343, 209, 12, 12);
    private ExtendedPanelLayout.Rect patternManagementAddButton =
            new ExtendedPanelLayout.Rect(291, 185, 30, 11);
    private ExtendedPanelLayout.Rect patternManagementReloadButton =
            new ExtendedPanelLayout.Rect(322, 185, 30, 11);
    private ExtendedPanelLayout.Rect patternManagementDeleteButton =
            new ExtendedPanelLayout.Rect(291, 196, 30, 11);
    private ExtendedPanelLayout.Rect patternManagementCancelButton =
            new ExtendedPanelLayout.Rect(322, 196, 30, 11);
    private ExtendedPanelLayout.Rect patternManagementUploadButton =
            new ExtendedPanelLayout.Rect(285, 2, 14, 15);
    private ExtendedPanelLayout.Rect patternManagementUiButton =
            new ExtendedPanelLayout.Rect(306, 2, 14, 15);
    private ExtendedPanelLayout.Rect patternManagementHighlightButton =
            new ExtendedPanelLayout.Rect(337, 3, 6, 11);
    private ExtendedPanelLayout.Rect patternManagementDisplayModeButton =
            new ExtendedPanelLayout.Rect(175, 194, 12, 12);
    private ExtendedPanelLayout.Rect patternManagementDisplaySlotsButton =
            new ExtendedPanelLayout.Rect(189, 194, 12, 12);
    private ExtendedPanelLayout.Rect patternManagementAutoUploadButton =
            new ExtendedPanelLayout.Rect(203, 194, 12, 12);
    private ExtendedPanelLayout.Rect patternManagementSearchModeButton =
            new ExtendedPanelLayout.Rect(217, 194, 12, 12);
    private ExtendedPanelLayout.Rect batchItemReplacementButton =
            new ExtendedPanelLayout.Rect(163, 158, 12, 12);
    private ExtendedPanelLayout.Rect batchFluidReplacementButton =
            new ExtendedPanelLayout.Rect(163, 176, 12, 12);
    private PatternManagementDisplayMode patternManagementDisplayMode = PatternManagementDisplayMode.VISIBLE;
    private PatternManagementSearchMode patternManagementSearchMode = PatternManagementSearchMode.IN_OUT;
    private boolean patternManagementShowSlots = true;
    private boolean patternManagementUploadEnabled = true;
    private @Nullable Boolean pendingPatternManagementUploadEnabled;
    private @Nullable PatternManagementDisplayMode pendingPatternManagementDisplayMode;
    private @Nullable Boolean pendingPatternManagementShowSlots;
    private @Nullable PatternManagementSearchMode pendingPatternManagementSearchMode;
    private boolean batchItemSubstitutions;
    private boolean batchFluidSubstitutions;
    private boolean toolkitMemoryMode;

    private static final int PATTERN_CACHE_COLS = 9;
    private static final int PATTERN_CACHE_VISIBLE_ROWS = 2;
    private static final int PATTERN_CACHE_SLOT_X = 176;
    private static final int PATTERN_CACHE_SLOT_BOTTOM = 142;
    /** 相对样板缓存区背景的纵向微调（负数 = 整体上移）；槽位绘制与 {@link #isMouseOverPatternCache} 共用。 */
    private static final int PATTERN_CACHE_SLOT_Y_OFFSET = -1;
    private static final int PATTERN_CACHE_SCROLLBAR_HEIGHT = 34;
    private static final int PATTERN_MANAGEMENT_ROW_H = 18;
    /** 样板管理列表中，单个供应器每行最多显示的样板槽列数。 */
    private static final int PATTERN_MANAGEMENT_COLS = 9;
    /** 样板槽底图为 {@code wcwt_management.png} 单格 18×18（{@link #PATTERN_MANAGEMENT_SLOT_BG_SIZE}）。 */
    private static final int PATTERN_MANAGEMENT_SLOT_BG_SIZE = 18;
    /** 悬停 / 点击 / 高亮判定区域：16×16，居中落在底图内。 */
    private static final int PATTERN_MANAGEMENT_SLOT_HIT_SIZE = 16;
    /** {@link #PATTERN_MANAGEMENT_SLOT_HIT_SIZE}×{@link #PATTERN_MANAGEMENT_SLOT_HIT_SIZE} 在 {@link #PATTERN_MANAGEMENT_SLOT_BG_SIZE} 槽内的边距。 */
    private static final int PATTERN_MANAGEMENT_SLOT_HIT_INSET =
            (PATTERN_MANAGEMENT_SLOT_BG_SIZE - PATTERN_MANAGEMENT_SLOT_HIT_SIZE) / 2;
    /** 判定区相对「居中」位置整体向左平移的像素（仅 X）。 */
    private static final int PATTERN_MANAGEMENT_SLOT_HIT_X_OFFSET = -1;
    private static final int PATTERN_MANAGEMENT_SLOT_STEP = 18;
    private static final int PATTERN_MANAGEMENT_HEADER_Y_OFFSET = -1;
    private static final int PATTERN_MANAGEMENT_SLOT_Y_OFFSET = 0;
    private static final int PATTERN_MANAGEMENT_HIGHLIGHT_ICON_X_OFFSET = 4;
    private static final int BUTTON_PRESS_OFFSET_Y = 1;
    private static final int TOOLKIT_MEMORY_TOGGLE_OFF_U = 0;
    private static final int TOOLKIT_MEMORY_TOGGLE_ON_U = 16;
    private static final int TOOLKIT_MEMORY_TOGGLE_V = 48;
    private static final int TOOLKIT_MEMORY_TOGGLE_SOURCE_SIZE = 16;
    private static final int ANVIL_TOO_EXPENSIVE_COST = 40;
    private static final long FOCUSED_PATTERN_FLASH_PERIOD_MS = 480L;
    private static final long PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT_PERIOD_MS = 4000L;
    private static final int PATTERN_MANAGEMENT_SEARCH_MATCH_ALPHA = 0xA0;
    private static final int PATTERN_MANAGEMENT_SEARCH_MATCH_BG_ALPHA = 0x3C;
    private static final boolean DEBUG_REPO = Boolean.getBoolean("wcwt.debug.repo");
    private static final int PLAYER_INVENTORY_SLOT_HIT_SIZE = 18;
    private static final float TOOLKIT_MEMORY_GHOST_ALPHA = 0.38F;
    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);
    private static final ResourceLocation CURIOS_INVENTORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("curios", "textures/gui/curios/inventory.png");
    private static final ResourceLocation EAE_ICONS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("extendedae", "textures/guis/nicons.png");
    private static final ResourceLocation AE2_STATES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/states.png");
    private static final ResourceLocation AE2_CHECKBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/checkbox.png");
    private static final ResourceLocation AAE_STATES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("advanced_ae", "textures/guis/states.png");
    private static final ResourceLocation WCWT_GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wireless_comprehensive_work_terminal_gui.png");
    private static final ResourceLocation WCWT_PATTERN_MANAGEMENT_HIDDEN_BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/background1.png");
    private static final ResourceLocation WCWT_EXTENDED_CRAFTING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_extended_crafting.png");
    private static final ResourceLocation WCWT_MANAGEMENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_management.png");
    private static final ResourceLocation WCWT_TOOLS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_tools.png");
    private static final ResourceLocation WCWT_STATES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");
    private static final int SELECTED_PATTERN_BG_U = 32;
    private static final int SELECTED_PATTERN_BG_V = 128;
    private static final int SELECTED_PATTERN_BG_SIZE = 18;
    private static final ResourceLocation AE2_PATTERN_MODES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/pattern_modes.png");
    private static final ResourceLocation WTLIB_ICONS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/wtlib/guis/icons.png");
    private static final int AE2_RADIO_UNCHECKED_U = 28;
    private static final int AE2_RADIO_UNCHECKED_V = 0;
    private static final int AE2_RADIO_UNCHECKED_FOCUS_U = 42;
    private static final int AE2_RADIO_UNCHECKED_FOCUS_V = 0;
    private static final int AE2_RADIO_CHECKED_FOCUS_U = 42;
    private static final int AE2_RADIO_CHECKED_FOCUS_V = 14;
    private static final int AE2_RADIO_SIZE = 14;
    private static final int PATTERN_ENCODING_BG_WIDTH = 124;
    private static final int PATTERN_ENCODING_BG_HEIGHT = 66;
    private static final int PATTERN_ENCODING_BG_X_OFFSET_FROM_INPUT = -16;
    private static final int PATTERN_ENCODING_BG_Y_OFFSET_FROM_INPUT = -7;
    private static final int STONECUTTING_RESULT_COLS = 4;
    private static final int STONECUTTING_RESULT_ROWS = 2;
    private static final int STONECUTTING_RESULT_SLOT_W = 20;
    private static final int STONECUTTING_RESULT_SLOT_H = 22;
    private static final int STONECUTTING_RESULT_SRC_X = 124;
    private static final int STONECUTTING_RESULT_SRC_Y = 140;
    private static final int PATTERN_OPTION_BUTTON_SIZE = 16;
    
    // 样板选择状态（保留以备后续功能使用）
    private boolean advancedCodingMode = false; // 是否处于高级编码模式
    private boolean patternSelectionLockedMode = false;
    private int selectedPatternCacheIndex = -1;
    private int lastDebugRepoColumns = -1;
    private int lastDebugRepoSlots = -1;
    private int lastDebugRepoVisibleEntries = -1;
    private boolean lastDebugRepoConnected;
    private boolean lastObservedLinkConnected;
    private boolean observedLinkConnectionOnce;
    private @Nullable Slot lastAeNetworkToolkitDoubleClickSlot;
    private long lastAeNetworkToolkitDoubleClickMs;
    private final Set<AEKey> craftableIndicatorKeys = new HashSet<>();
    private @Nullable Method meStorageUpdateScrollbarMethod;
    private static volatile @Nullable Field compositeWidgetsField;
    private ItemStack lastEncodedPatternForUploadSync = ItemStack.EMPTY;
    private @Nullable String lastEncodedPatternUploadSearchText;
    private boolean attemptedRestoreManagementToolkitOpenState;
    private boolean rebuildingFavoriteRepoView;
    
    public WirelessComprehensiveWorkTerminalScreen(WirelessComprehensiveWorkTerminalMenu menu, 
                                                     Inventory playerInventory, 
                                                     Component title) {
        this(menu, playerInventory, title, StyleManager.loadStyleDoc(STYLE_PATH));
    }
    
    private WirelessComprehensiveWorkTerminalScreen(WirelessComprehensiveWorkTerminalMenu menu,
                                                     Inventory playerInventory,
                                                     Component title,
                                                     ScreenStyle style) {
        super(menu, playerInventory, title, style);
        WcwtFavorites.ensureLoaded();
        hookRepoUpdateListener();
        favoriteItemsButton = addToLeftToolbar(new FavoriteItemsButton(WcwtFavorites::isEnabled,
                btn -> toggleFavoritedItemsFirst()));
        viewCellsToggleButton = addToLeftToolbar(new ViewCellsToggleButton(
                () -> viewCellsVisible,
                btn -> toggleViewCellsPanel()));
        universalTerminalButton = addToLeftToolbar(new WcwtUniversalTerminalButton());
        installViewCellsVisibilityWidget();
        widgets.add("player", new PlayerEntityWidget(Objects.requireNonNull(Minecraft.getInstance().player)));

        var host = menu.getMenuHost();

        // 注册带滑块的滚动升级面板（当前使用 WTLib 原版实现）
        // 槽位顺序：先放 SINGULARITY 槽（ScrollingUpgradesPanel 约定第一槽为奇点槽），再放普通升级槽
        //
        // 如果以后想切回 WCWT 自定义稳定版，请把下面这行：
        //   upgradesPanel = new ScrollingUpgradesPanel(...)
        // 改成：
        //   upgradesPanel = new WcwtScrollingUpgradesPanel(...)
        if (host != null) {
            var allUpgradeSlots = new ArrayList<>(menu.getSlots(AE2wtlibSlotSemantics.SINGULARITY));
            allUpgradeSlots.addAll(menu.getSlots(SlotSemantics.UPGRADE));
            upgradesPanel = new ScrollingUpgradesPanel(allUpgradeSlots, host, widgets, host::getUpgrades);
            widgets.add("scrollingUpgrades", upgradesPanel);

            cellUpgradesPanel = new CellScrollingUpgradesPanel(
                    new ArrayList<>(menu.getSlots(WcwtSlotSemantics.WCWT_CELL_UPGRADE)), widgets,
                    this::getCurrentCellWorkbenchStack);
            widgets.add("cellScrollingUpgrades", cellUpgradesPanel);
        }

        patternCacheScrollbar = widgets.addScrollBar("caching_scrollbar", Scrollbar.SMALL);
        patternCacheScrollbar.setHeight(PATTERN_CACHE_SCROLLBAR_HEIGHT);
        patternCacheScrollbar.setCaptureMouseWheel(false);

        patternEncodingScrollbar = widgets.addScrollBar("processingPatternModeScrollbar", Scrollbar.SMALL);
        patternEncodingScrollbar.setRange(0, 24, 3);
        patternEncodingScrollbar.setCaptureMouseWheel(false);

        stonecuttingPatternScrollbar = widgets.addScrollBar("stonecuttingPatternModeScrollbar", Scrollbar.SMALL);
        stonecuttingPatternScrollbar.setRange(0, 0, STONECUTTING_RESULT_COLS);
        stonecuttingPatternScrollbar.setCaptureMouseWheel(false);

        patternManageSearchField = widgets.addTextField("manage_search");
        patternManageSearchField.setPlaceholder(Component.translatable("gui.wcwt.pattern_management.provider_search"));
        patternManageSearchField.setResponder(str -> onPatternManagementSearchChanged());
        patternManageSearchField.setMaxLength(64);

        patternManageMappingField = widgets.addTextField("manage_mapping");
        patternManageMappingField.setPlaceholder(Component.translatable("gui.wcwt.pattern_management.mapping_input"));
        patternManageMappingField.setMaxLength(64);
        patternManagementPage = mainLayout.widget("management_page", patternManagementPage, imageWidth, imageHeight);
        patternManagementScrollbarRect = mainLayout.widget("manage_scrollbar", patternManagementScrollbarRect,
                imageWidth, imageHeight);
        pinnedRowOverlayRect = mainLayout.widget("pinned_row_overlay", pinnedRowOverlayRect, imageWidth, imageHeight);
        managementToolkitBackgroundRect = managementToolkitLayout.widget("management_toolkit_background",
                managementToolkitBackgroundRect, imageWidth, imageHeight);
        managementToolkitSlotRect = managementToolkitLayout.slot("management_toolkit_slots", managementToolkitSlotRect,
                imageWidth, imageHeight);
        managementToolkitScrollbarRect = managementToolkitLayout.widget("management_toolkit_scrollbar",
                managementToolkitScrollbarRect, imageWidth, imageHeight);
        managementToolkitMemoryButton = managementToolkitLayout.widget("management_toolkit_memory",
                managementToolkitMemoryButton, imageWidth, imageHeight);
        patternManagementAddButton = mainLayout.widget("increase_mapping", patternManagementAddButton, imageWidth, imageHeight);
        patternManagementReloadButton = mainLayout.widget("heavy_load_mapping", patternManagementReloadButton, imageWidth, imageHeight);
        patternManagementDeleteButton = mainLayout.widget("delete_mapping", patternManagementDeleteButton, imageWidth, imageHeight);
        patternManagementCancelButton = mainLayout.widget("manage_cancel", patternManagementCancelButton, imageWidth, imageHeight);
        patternManagementUploadButton = mainLayout.widget("manage_upload_pattern", patternManagementUploadButton, imageWidth, imageHeight);
        patternManagementUiButton = mainLayout.widget("manage_open_ui", patternManagementUiButton, imageWidth, imageHeight);
        patternManagementHighlightButton = mainLayout.widget("manage_highlight_provider", patternManagementHighlightButton, imageWidth, imageHeight);
        patternManagementDisplayModeButton = mainLayout.widget("manage_display_mode", patternManagementDisplayModeButton, imageWidth, imageHeight);
        patternManagementDisplaySlotsButton = mainLayout.widget("manage_displays_slots", patternManagementDisplaySlotsButton, imageWidth, imageHeight);
        patternManagementAutoUploadButton = mainLayout.widget("manage_output_mode", patternManagementAutoUploadButton, imageWidth, imageHeight);
        patternManagementSearchModeButton = mainLayout.widget("automatic_upload", patternManagementSearchModeButton, imageWidth, imageHeight);
        batchItemReplacementButton = mainLayout.widget("pattern_Replace1", batchItemReplacementButton, imageWidth, imageHeight);
        batchFluidReplacementButton = mainLayout.widget("pattern_Replace2", batchFluidReplacementButton, imageWidth, imageHeight);

        patternManagementScrollbar = widgets.addScrollBar("manage_scrollbar", Scrollbar.SMALL);
        patternManagementScrollbar.setHeight(patternManagementPage.height());
        patternManagementScrollbar.setCaptureMouseWheel(false);

        curiosScrollbar = widgets.addScrollBar("curios_scrollbar", Scrollbar.SMALL);
        curiosScrollbar.setHeight(CuriosPanel.DEFAULT_SCROLLBAR_HEIGHT);
        curiosScrollbar.setCaptureMouseWheel(false);

        toolkitScrollbar = widgets.addScrollBar("toolkit_scrollbar", Scrollbar.SMALL);
        toolkitScrollbar.setHeight(ToolkitPanel.DEFAULT_SCROLLBAR_HEIGHT);
        toolkitScrollbar.setCaptureMouseWheel(false);
        syncPatternManagementSettingsFromMenu();
        prewarmKeyTypeSelectionScreenHeight();
        rebuildCraftableIndicatorCache();

        // 以下所有 widgets.add() 必须在构造函数中完成，确保 populateScreen() 在 init() 中运行时
        // compositeWidgets 已有内容，才能被正确定位显示。（AE2 的约定：composite widget 在构造函数注册）
        if (host != null) {
            craftingLockButton = new CraftingLockButton(host, btn -> {
                host.toggleCraftingGridLock();
                PacketDistributor.sendToServer(new CraftingLockPacket(host.isCraftingGridLocked()));
            });
            widgets.add("CRAFTING_Locking", craftingLockButton);
        }

        manualCraftingButton = new IconButton(0, 0, 12, 12,
                0, 32, 0, 32, 8, 8,
                WCWT_STATES_TEXTURE,
                Component.translatable("gui.ae2.CraftingTerminal"),
                btn -> switchManualWorkspaceMode(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING))
                .drawTextureUnscaledCentered()
                .disableHoverPressOffset();
        widgets.add("manual_mode_button0", manualCraftingButton);

        manualSmithingButton = new IconButton(0, 0, 12, 12,
                16, 32, 16, 32, 8, 8,
                WCWT_STATES_TEXTURE,
                Component.translatable("gui.wcwt.manual_workspace.smithing"),
                btn -> switchManualWorkspaceMode(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.SMITHING))
                .drawTextureUnscaledCentered()
                .disableHoverPressOffset();
        widgets.add("manual_mode_button1", manualSmithingButton);

        manualAnvilButton = new IconButton(0, 0, 12, 12,
                32, 32, 32, 32, 8, 8,
                WCWT_STATES_TEXTURE,
                Component.translatable("gui.wcwt.manual_workspace.anvil"),
                btn -> switchManualWorkspaceMode(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL))
                .drawTextureUnscaledCentered()
                .disableHoverPressOffset();
        widgets.add("manual_mode_button2", manualAnvilButton);

        manualAnvilNameField = new TransparentEditBox(font, 0, 0, 88, 12, Component.empty());
        manualAnvilNameField.setCanLoseFocus(true);
        manualAnvilNameField.setTextColor(-1);
        manualAnvilNameField.setTextColorUneditable(-1);
        manualAnvilNameField.setBordered(false);
        manualAnvilNameField.setMaxLength(AnvilMenu.MAX_NAME_LENGTH);
        manualAnvilNameField.setResponder(value -> {
            if (syncingManualAnvilNameField) {
                return;
            }
            if (menu.setManualAnvilName(value)) {
                PacketDistributor.sendToServer(new ManualAnvilNamePacket(value));
            }
        });
        manualAnvilNameField.setFocused(false);
        widgets.add("manual_anvil_name", manualAnvilNameField);

        encodePatternButton = new ActionButton(appeng.api.config.ActionItems.ENCODE, this::handleEncodePatternButton);
        encodePatternButton.setMessage(Component.translatable("gui.tooltips.ae2.Encode"));
        widgets.add("wcwtEncodePattern", encodePatternButton);

        clearPatternEncodingButton = new ActionButton(appeng.api.config.ActionItems.S_CLOSE, () -> {
            menu.clearPatternEncoding();
            PacketDistributor.sendToServer(new PatternEncodingOptionPacket(
                    PatternEncodingOptionPacket.ACTION_CLEAR, false));
        });
        clearPatternEncodingButton.setHalfSize(true);
        clearPatternEncodingButton.setDisableBackground(true);
        widgets.add("wcwtPatternClearPattern", clearPatternEncodingButton);

        processingMaterialsMergeButton = new WcwtProcessingMaterialsMergeButton(
                Component.translatable("gui.wcwt.processing_merge.on"),
                Component.translatable("gui.wcwt.processing_merge.desc.on"),
                Component.translatable("gui.wcwt.processing_merge.off"),
                Component.translatable("gui.wcwt.processing_merge.desc.off"),
                btn -> PacketDistributor.sendToServer(new PatternEncodingOptionPacket(
                        PatternEncodingOptionPacket.ACTION_PROCESSING_MERGE_MATERIALS,
                        !menu.isProcessingMaterialsMerge())));
        widgets.add("wcwtPatternMergeMaterials", processingMaterialsMergeButton);

        patternSubstitutionButton = createPatternSubstitutionButton();
        widgets.add("wcwtPatternSubstitutions", patternSubstitutionButton);

        patternFluidSubstitutionButton = createPatternFluidSubstitutionButton();
        widgets.add("wcwtPatternFluidSubstitutions", patternFluidSubstitutionButton);

        manualPatternSubstitutionButton = createManualPatternSubstitutionButton();
        widgets.add("wcwtManualPatternSubstitutions", manualPatternSubstitutionButton);

        manualPatternFluidSubstitutionButton = createManualPatternFluidSubstitutionButton();
        widgets.add("wcwtManualPatternFluidSubstitutions", manualPatternFluidSubstitutionButton);

        cycleProcessingOutputButton = new ActionButton(appeng.api.config.ActionItems.S_CYCLE_PROCESSING_OUTPUT,
                () -> {
                    menu.cycleProcessingOutput();
                    PacketDistributor.sendToServer(new CycleProcessingOutputPacket());
                });
        cycleProcessingOutputButton.setHalfSize(true);
        cycleProcessingOutputButton.setDisableBackground(true);
        widgets.add("processingCycleOutput", cycleProcessingOutputButton);

        // 样板模式 tab 按钮（合成 / 处理 / 锻造台 / 切石机）
        tabCrafting = new TabButton(
                appeng.client.gui.Icon.TAB_CRAFTING,
                Component.translatable("gui.ae2.CraftingPattern"),
                btn -> setPatternEncodingMode(EncodingMode.CRAFTING));
        tabCrafting.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton0", tabCrafting);
        // JSON: modeTabButton0 bottom:231（最大）→ 距底最远 → **最顶**的样板按钮
        topModeTabButton = tabCrafting;

        tabProcessing = new TabButton(
                appeng.client.gui.Icon.TAB_PROCESSING,
                Component.translatable("gui.ae2.ProcessingPattern"),
                btn -> setPatternEncodingMode(EncodingMode.PROCESSING));
        tabProcessing.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton1", tabProcessing);

        tabSmithing = new TabButton(
                appeng.client.gui.Icon.TAB_SMITHING,
                Component.translatable("gui.ae2.SmithingTablePattern"),
                btn -> setPatternEncodingMode(EncodingMode.SMITHING_TABLE));
        tabSmithing.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton2", tabSmithing);

        tabStonecutting = new TabButton(
                appeng.client.gui.Icon.TAB_STONECUTTING,
                Component.translatable("gui.ae2.StonecuttingPattern"),
                btn -> setPatternEncodingMode(EncodingMode.STONECUTTING));
        tabStonecutting.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton3", tabStonecutting);
        // JSON: modeTabButton3 bottom:168（最小）→ 最底的样板按钮
        bottomModeTabButton = tabStonecutting;

        // 样板倍增按钮
        multiplierButtons = new PatternMultiplierButton[8];
        var multiplierTypes = PatternMultiplierButton.MultiplierType.values();
        for (int i = 0; i < multiplierButtons.length; i++) {
            int index = i;
            multiplierButtons[i] = new PatternMultiplierButton(
                    multiplierTypes[i],
                    btn -> onMultiplierButtonClick(multiplierTypes[index]));
            widgets.add("Double_button" + i, multiplierButtons[i]);
        }

        // 显示过滤按钮
        itemDisplayButton = new ItemDisplayButton(btn -> selectKeyTypePreset(KeyTypePreset.ITEMS));
        widgets.add("item_display_button", itemDisplayButton);

        fluidDisplayButton = new FluidDisplayButton(btn -> selectKeyTypePreset(KeyTypePreset.FLUIDS));
        widgets.add("fluid_display_button", fluidDisplayButton);

        otherTypesDisplayButton = new OtherTypesDisplayButton(btn -> selectKeyTypePreset(KeyTypePreset.OTHERS));
        widgets.add("no_item_fluid_display_button", otherTypesDisplayButton);

        muteButton = new TopActionButton(WCWT_STATES_TEXTURE, 80, 0, 16, 16, 256, 256,
                Component.translatable("gui.wcwt.top_action.mute"), btn -> openExtremeSoundMuffler());
        muteButton.visible = ModList.get().isLoaded("extremesoundmuffler");
        widgets.add("turn_off_sound_button", muteButton);

        wirelessTerminalSettingsButton = new TopActionButton(AE2_STATES_TEXTURE, 32, 65, 16, 15, 256, 256,
                Component.translatable("gui.ae2wtlib.wireless_terminal_settings_title"),
                btn -> openWirelessTerminalSettings());
        widgets.add("wirelessTerminalSettingsButton", wirelessTerminalSettingsButton);

        magnetCardMenuButton = new TopActionButton(WTLIB_ICONS_TEXTURE, 0, 0, 16, 16, 128, 128,
                Component.translatable("gui.ae2wtlib.Magnet"),
                btn -> menu.openWcwtMagnetMenu());
        magnetCardMenuButton.visible = false;
        magnetCardMenuButton.active = false;
        widgets.add("magnetCardMenuButton", magnetCardMenuButton);

        trashButton = new TopActionButton(WTLIB_ICONS_TEXTURE, 0, 32, 16, 16, 128, 128,
                Component.translatable("gui.ae2wtlib.trash"),
                btn -> menu.openWcwtTrashMenu());
        widgets.add("trashButton", trashButton);

        installViewCellsVisibilityWidget();
    }

    private ToggleButton createPatternSubstitutionButton() {
        ToggleButton button = new ToggleButton(
                Icon.S_SUBSTITUTION_ENABLED,
                Icon.S_SUBSTITUTION_DISABLED,
                state -> {
                    menu.setPatternSubstitute(state);
                    PacketDistributor.sendToServer(new PatternEncodingOptionPacket(
                            PatternEncodingOptionPacket.ACTION_SUBSTITUTE, state));
                });
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(List.of(
                ButtonToolTips.SubstitutionsOn.text(),
                ButtonToolTips.SubstitutionsDescEnabled.text()));
        button.setTooltipOff(List.of(
                ButtonToolTips.SubstitutionsOff.text(),
                ButtonToolTips.SubstitutionsDescDisabled.text()));
        return button;
    }

    private ToggleButton createPatternFluidSubstitutionButton() {
        ToggleButton button = new ToggleButton(
                Icon.S_FLUID_SUBSTITUTION_ENABLED,
                Icon.S_FLUID_SUBSTITUTION_DISABLED,
                state -> {
                    menu.setPatternFluidSubstitute(state);
                    PacketDistributor.sendToServer(new PatternEncodingOptionPacket(
                            PatternEncodingOptionPacket.ACTION_FLUID_SUBSTITUTE, state));
                });
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescEnabled.text()));
        button.setTooltipOff(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescDisabled.text()));
        return button;
    }

    private ToggleButton createManualPatternSubstitutionButton() {
        ToggleButton button = new ToggleButton(
                Icon.S_SUBSTITUTION_ENABLED,
                Icon.S_SUBSTITUTION_DISABLED,
                state -> {
                    menu.setManualCraftingItemSubstitution(state);
                    PacketDistributor.sendToServer(new PatternModePacket(
                            PatternModePacket.MODE_MANUAL_ITEM_SUBSTITUTION, state));
                });
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(List.of(
                ButtonToolTips.SubstitutionsOn.text(),
                ButtonToolTips.SubstitutionsDescEnabled.text()));
        button.setTooltipOff(List.of(
                ButtonToolTips.SubstitutionsOff.text(),
                ButtonToolTips.SubstitutionsDescDisabled.text()));
        return button;
    }

    private ToggleButton createManualPatternFluidSubstitutionButton() {
        ToggleButton button = new ToggleButton(
                Icon.S_FLUID_SUBSTITUTION_ENABLED,
                Icon.S_FLUID_SUBSTITUTION_DISABLED,
                state -> {
                    menu.setManualCraftingFluidSubstitution(state);
                    PacketDistributor.sendToServer(new PatternModePacket(
                            PatternModePacket.MODE_MANUAL_FLUID_SUBSTITUTION, state));
                });
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescEnabled.text()));
        button.setTooltipOff(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescDisabled.text()));
        return button;
    }

    private void openExtremeSoundMuffler() {
        try {
            Class<?> common = Class.forName("com.leobeliik.extremesoundmuffler.SoundMufflerCommon");
            common.getMethod("openMainScreen").invoke(null);
        } catch (ReflectiveOperationException e) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.wcwt.top_action.unavailable"), true);
            }
        }
    }

    private void openWirelessTerminalSettings() {
        switchToScreen(new WcwtWirelessTerminalSettingsSubScreen(this));
    }

    private void toggleViewCellsPanel() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        viewCellsVisible = !viewCellsVisible;
        WcwtClientConfig.setLastViewCellsPanelVisible(viewCellsVisible);
        updateViewCellsPanelVisibility();
    }

    private void updateViewCellsPanelVisibility() {
        if (viewCellsVisibilityWidget != null) {
            viewCellsVisibilityWidget.setVisible(viewCellsVisible);
        }
        setSemanticSlotsHidden(SlotSemantics.VIEW_CELL, !viewCellsVisible);
        if (viewCellsToggleButton != null) {
            viewCellsToggleButton.setMessage(Component.translatable(viewCellsVisible
                    ? "gui.wcwt.view_cells.visible"
                    : "gui.wcwt.view_cells.hidden"));
        }
    }

    @SuppressWarnings("unchecked")
    private void installViewCellsVisibilityWidget() {
        try {
            Field field = compositeWidgetsField;
            if (field == null) {
                field = appeng.client.gui.WidgetContainer.class.getDeclaredField("compositeWidgets");
                field.setAccessible(true);
                compositeWidgetsField = field;
            }
            Object value = field.get(widgets);
            if (!(value instanceof Map<?, ?> rawMap)) {
                return;
            }
            Map<String, ICompositeWidget> compositeWidgets = (Map<String, ICompositeWidget>) rawMap;
            ICompositeWidget viewCells = compositeWidgets.get("viewCells");
            if (viewCells == null) {
                viewCellsToggleButton.visible = false;
                viewCellsToggleButton.active = false;
                return;
            }
            if (!(viewCells instanceof ViewCellsVisibilityWidget)) {
                viewCellsVisibilityWidget = new ViewCellsVisibilityWidget(viewCells, () -> viewCellsVisible);
                compositeWidgets.put("viewCells", viewCellsVisibilityWidget);
            } else {
                viewCellsVisibilityWidget = (ViewCellsVisibilityWidget) viewCells;
            }
            moveCompositeWidgetToEnd(compositeWidgets, "viewCells");
        } catch (ReflectiveOperationException | SecurityException ignored) {
            if (viewCellsToggleButton != null) {
                viewCellsToggleButton.visible = false;
                viewCellsToggleButton.active = false;
            }
        }
    }

    private static void moveCompositeWidgetToEnd(Map<String, ICompositeWidget> widgets, String id) {
        ICompositeWidget widget = widgets.remove(id);
        if (widget != null) {
            widgets.put(id, widget);
        }
    }

    private void installViewCellsOverlayRenderable() {
        if (viewCellsOverlayRenderable != null) {
            renderables.remove(viewCellsOverlayRenderable);
        }
        viewCellsOverlayRenderable = this::renderViewCellsOverlay;
        renderables.add(viewCellsOverlayRenderable);
    }

    private void installToolkitMemoryOverlayRenderable() {
        if (toolkitMemoryOverlayRenderable != null) {
            renderables.remove(toolkitMemoryOverlayRenderable);
        }
        toolkitMemoryOverlayRenderable = this::renderToolkitMemoryOverlay;
        renderables.add(toolkitMemoryOverlayRenderable);
    }

    private void renderViewCellsOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (viewCellsVisibilityWidget == null || !viewCellsVisibilityWidget.isVisible()) {
            return;
        }

        viewCellsVisibilityWidget.drawOverlay(guiGraphics, new Rect2i(leftPos, topPos, imageWidth, imageHeight),
                new Point(mouseX - leftPos, mouseY - topPos));
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(leftPos, topPos, 0);
        try {
            for (Slot slot : menu.getSlots(SlotSemantics.VIEW_CELL)) {
                if (!slot.isActive()) {
                    continue;
                }
                renderSlot(guiGraphics, slot);
                if (isHovering(slot, mouseX, mouseY)) {
                    renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
                }
            }
        } finally {
            pose.popPose();
        }
    }

    private static class WcwtWirelessTerminalSettingsSubScreen
            extends appeng.client.gui.AESubScreen<WirelessComprehensiveWorkTerminalMenu, WirelessComprehensiveWorkTerminalScreen> {
        private final AECheckbox pickBlock = widgets.addCheckbox("pickBlock",
                Component.translatable("gui.ae2wtlib.pick_block.text"), this::changeVisibility);
        private final AECheckbox craftIfMissing = widgets.addCheckbox("craftIfMissing",
                Component.translatable("gui.ae2wtlib.craft_if_missing.text"), this::save);
        private final AECheckbox restock = widgets.addCheckbox("restock",
                Component.translatable("gui.ae2wtlib.restock.text"), this::save);
        private final AECheckbox magnet = widgets.addCheckbox("magnet",
                Component.translatable("gui.ae2wtlib.magnet.text"), this::save);
        private final AECheckbox pickupToME = widgets.addCheckbox("pickupToME",
                Component.translatable("gui.ae2wtlib.pickup_to_me.text"), this::save);
        private final AECheckbox patternUploadFailFallbackToEditor = widgets.addCheckbox(
                "patternUploadFailFallbackToEditor",
                Component.translatable("wcwt.config.patternUploadFailFallbackToEditor"),
                this::saveClientSettings);
        private final AECheckbox patternMultiplierApplyToEditorProcessing = widgets.addCheckbox(
                "patternMultiplierApplyToEditorProcessing",
                Component.translatable("wcwt.config.patternMultiplierApplyToEditorProcessing"),
                this::saveClientSettings);
        private final AECheckbox autoSwitchManualWorkspaceOnRecipeTransfer = widgets.addCheckbox(
                "autoSwitchManualWorkspaceOnRecipeTransfer",
                Component.translatable("wcwt.config.autoSwitchManualWorkspaceOnRecipeTransfer"),
                this::saveClientSettings);
        private final AECheckbox expandToolkitInManagementArea = widgets.addCheckbox(
                "expandToolkitInManagementArea",
                Component.translatable("wcwt.config.expandToolkitInManagementArea"),
                this::saveClientSettings);

        WcwtWirelessTerminalSettingsSubScreen(WirelessComprehensiveWorkTerminalScreen parent) {
            super(parent, "/screens/wcwt/wireless_terminal_settings.json");
            widgets.add("back", new TabButton(Icon.BACK, getMenu().getHost().getMainMenuIcon().getHoverName(),
                    btn -> returnToParent()));

            ItemStack stack = stack();
            pickBlock.setSelected(stack.getOrDefault(AE2wtlibComponents.PICK_BLOCK, false));
            craftIfMissing.setSelected(stack.getOrDefault(AE2wtlibComponents.CRAFT_IF_MISSING, false));
            craftIfMissing.active = pickBlock.isSelected();
            restock.setSelected(stack.getOrDefault(AE2wtlibComponents.RESTOCK, false));
            magnet.setSelected(readMagnetSetting(stack, "magnet"));
            pickupToME.setSelected(readMagnetSetting(stack, "pickupToME"));
            patternUploadFailFallbackToEditor.setSelected(WcwtClientConfig.patternUploadFailFallbackToEditor());
            patternMultiplierApplyToEditorProcessing
                    .setSelected(WcwtClientConfig.patternMultiplierApplyToEditorProcessing());
            autoSwitchManualWorkspaceOnRecipeTransfer
                    .setSelected(WcwtClientConfig.autoSwitchManualWorkspaceOnRecipeTransfer());
            expandToolkitInManagementArea.setSelected(WcwtClientConfig.expandToolkitInManagementArea());
            refreshMagnetSettingsAvailability(stack);
        }

        private void refreshMagnetSettingsAvailability(ItemStack terminal) {
            boolean hasMagnetCard = !terminal.isEmpty() && WcwtWirelessFeatures.hasMagnetCardInstalled(terminal);
            magnet.active = hasMagnetCard;
            pickupToME.active = hasMagnetCard;
        }

        @Override
        protected void init() {
            super.init();
            leftPos = (width - imageWidth) / 2;
            topPos = (height - imageHeight) / 2;
            setSlotsHidden(SlotSemantics.TOOLBOX, true);
            refreshMagnetSettingsAvailability(stack());
        }

        private ItemStack stack() {
            return getMenu().getMenuHost() == null ? ItemStack.EMPTY : getMenu().getMenuHost().getItemStack();
        }

        private void changeVisibility() {
            craftIfMissing.active = pickBlock.isSelected();
            save();
        }

        private void save() {
            PacketDistributor.sendToServer(new WirelessSettingsPacket(
                    pickBlock.isSelected(), restock.isSelected(), magnet.isSelected(), pickupToME.isSelected(),
                    craftIfMissing.isSelected()));
        }

        private void saveClientSettings() {
            WcwtClientConfig.PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR
                    .set(patternUploadFailFallbackToEditor.isSelected());
            WcwtClientConfig.PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING
                    .set(patternMultiplierApplyToEditorProcessing.isSelected());
            WcwtClientConfig.AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER
                    .set(autoSwitchManualWorkspaceOnRecipeTransfer.isSelected());
            WcwtClientConfig.EXPAND_TOOLKIT_IN_MANAGEMENT_AREA
                    .set(expandToolkitInManagementArea.isSelected());
            WcwtClientConfig.SPEC.save();
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static boolean readMagnetSetting(ItemStack stack, String methodName) {
            try {
                Field componentField = Class.forName("de.mari_023.ae2wtlib.AE2wtlibAdditionalComponents")
                        .getField("MAGNET_SETTINGS");
                net.minecraft.core.component.DataComponentType component =
                        (net.minecraft.core.component.DataComponentType) componentField.get(null);
                Class<?> modeClass = Class.forName("de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode");
                Object fallback = Enum.valueOf((Class<Enum>) modeClass.asSubclass(Enum.class), "OFF");
                Object mode = stack.getOrDefault(component, fallback);
                return (boolean) modeClass.getMethod(methodName).invoke(mode);
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }
    }

    @Override
    public void init() {
        super.init();
        rebuildCraftableIndicatorCache();
        syncRepoRowSize();
        clearCraftingGridButton = resolveWidgetById("clearCraftingGrid");
        clearToPlayerInvButton = resolveWidgetById("clearToPlayerInv");
        // 升级槽 maxRows 与终端风格联动（参考 WTLib WCT/WAT/WET 的标准做法）。
        // 切换"小/中/大终端"会让 ME 网格行数变化 → getVisibleRows() 变化 → 升级槽行数也跟着变。
        if (upgradesPanel != null) {
            upgradesPanel.setMaxRows(Math.max(2, getVisibleRows()));
        }
        if (cellUpgradesPanel != null) {
            cellUpgradesPanel.setMaxRows(2);
        }
        // 扩展按钮位置依赖升级槽实际高度，必须在 setMaxRows 之后调用。
        initExtendedUIButtons();
        initializePanels();
        markExtendedUiLayoutDirty();
        installViewCellsVisibilityWidget();
        updateViewCellsPanelVisibility();
        installViewCellsOverlayRenderable();
        installToolkitMemoryOverlayRenderable();
        updateManualWorkspaceUi();
    }

    private void syncRepoRowSize() {
        int firstRepoRowY = Integer.MIN_VALUE;
        int columns = 0;

        for (Slot slot : menu.slots) {
            if (!(slot instanceof RepoSlot)) {
                continue;
            }

            if (firstRepoRowY == Integer.MIN_VALUE) {
                firstRepoRowY = slot.y;
            }

            if (slot.y != firstRepoRowY) {
                break;
            }

            columns++;
        }

        if (columns > 0) {
            repo.setRowSize(columns);
            repo.updateView();
        }

        if (DEBUG_REPO) {
            logRepoViewState("syncRepoRowSize", columns);
        }
    }

    private int getRepoColumns() {
        int firstRepoRowY = Integer.MIN_VALUE;
        int columns = 0;
        for (Slot slot : menu.slots) {
            if (!(slot instanceof RepoSlot)) {
                continue;
            }
            if (firstRepoRowY == Integer.MIN_VALUE) {
                firstRepoRowY = slot.y;
            }
            if (slot.y != firstRepoRowY) {
                break;
            }
            columns++;
        }
        return columns;
    }

    private void renderPinnedRowBackgroundOverlay(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        if (!repo.hasPinnedRow()) {
            return;
        }
        guiGraphics.blit(
                WCWT_GUI_TEXTURE,
                offsetX + pinnedRowOverlayRect.left(),
                offsetY + pinnedRowOverlayRect.top(),
                7,
                307,
                pinnedRowOverlayRect.width(),
                pinnedRowOverlayRect.height(),
                512,
                512);
    }

    private void hookRepoUpdateListener() {
        repo.setUpdateViewListener(() -> {
            rebuildFavoriteRepoView();
            invokeMeStorageUpdateScrollbar();
            rebuildCraftableIndicatorCache();
        });
    }

    private void rebuildFavoriteRepoView() {
        if (rebuildingFavoriteRepoView) {
            return;
        }
        if (!repo.isEnabled()) {
            return;
        }

        rebuildingFavoriteRepoView = true;
        try {
            if (!WcwtFavorites.isEnabled()) {
                return;
            }

            @SuppressWarnings("unchecked")
            var viewField = appeng.client.gui.me.common.Repo.class.getDeclaredField("view");
            viewField.setAccessible(true);
            var view = (List<GridInventoryEntry>) viewField.get(repo);
            if (view == null || view.size() < 2) {
                return;
            }

            List<GridInventoryEntry> favorited = new ArrayList<>();
            List<GridInventoryEntry> normal = new ArrayList<>();
            for (var entry : view) {
                if (entry == null || entry.getWhat() == null) {
                    continue;
                }
                if (WcwtFavorites.isFavorited(entry.getWhat())) {
                    favorited.add(entry);
                } else {
                    normal.add(entry);
                }
            }

            if (favorited.isEmpty()) {
                return;
            }

            view.clear();
            view.addAll(favorited);
            view.addAll(normal);
        } catch (ReflectiveOperationException ignored) {
        } finally {
            rebuildingFavoriteRepoView = false;
        }
    }

    private @Nullable GridInventoryEntry getDisplayedRepoEntry(RepoSlot repoSlot) {
        return repoSlot.getEntry();
    }

    private void toggleFavoritedItemsFirst() {
        WcwtFavorites.setEnabled(!WcwtFavorites.isEnabled());
        if (DEBUG_FRAME_SYNC) {
            frameSyncRepoUpdateViews++;
        }
        repo.updateView();
        invokeMeStorageUpdateScrollbar();
    }

    public boolean toggleFavoriteForHoveredRepoSlot() {
        if (isTypingInPatternManagementField()) {
            return false;
        }
        if (!(hoveredSlot instanceof RepoSlot repoSlot)) {
            return false;
        }
        GridInventoryEntry entry = getDisplayedRepoEntry(repoSlot);
        if (entry == null || entry.getWhat() == null) {
            return false;
        }
        WcwtFavorites.toggle(entry.getWhat());
        repo.updateView();
        invokeMeStorageUpdateScrollbar();
        return true;
    }

    private void invokeMeStorageUpdateScrollbar() {
        try {
            if (meStorageUpdateScrollbarMethod == null) {
                meStorageUpdateScrollbarMethod = MEStorageScreen.class.getDeclaredMethod("updateScrollbar");
                meStorageUpdateScrollbarMethod.setAccessible(true);
            }
            meStorageUpdateScrollbarMethod.invoke(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke MEStorageScreen.updateScrollbar()", e);
        }
    }

    private void rebuildCraftableIndicatorCache() {
        craftableIndicatorKeys.clear();
        if (!repo.isEnabled()) {
            return;
        }

        for (var entry : repo.getAllEntries()) {
            if (entry.isCraftable()) {
                craftableIndicatorKeys.add(entry.getWhat());
            }
        }
    }

    /** 扩展UI按钮每次 init() 重新注册（因为 addRenderableWidget 会被 clearWidgets() 清空）。*/
    private void initExtendedUIButtons() {
        var host = menu.getMenuHost();
        if (host == null) return;

        advancedCodingButton = new ExtendedUIButton(
                host, IExtendedUIHost.ExtendedUIType.ADVANCED_CODING,
                btn -> toggleExtendedUI(IExtendedUIHost.ExtendedUIType.ADVANCED_CODING));
        cosmeticArmorButton = new ExtendedUIButton(
                host, IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR,
                btn -> toggleExtendedUI(IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR));
        curiosButton = new ExtendedUIButton(
                host, IExtendedUIHost.ExtendedUIType.CURIOS,
                btn -> toggleExtendedUI(IExtendedUIHost.ExtendedUIType.CURIOS));
        toolboxButton = new ExtendedUIButton(
                host, IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX,
                btn -> toggleExtendedUI(IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX));
        toolkitButton = new ExtendedUIButton(
                host, IExtendedUIHost.ExtendedUIType.TOOLKIT,
                btn -> toggleExtendedUI(IExtendedUIHost.ExtendedUIType.TOOLKIT));
        resonatingLightningPatternCodingButton = new ExtendedUIButton(
                host, IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING,
                btn -> toggleExtendedUI(IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING));

        addRenderableWidget(advancedCodingButton);
        addRenderableWidget(cosmeticArmorButton);
        addRenderableWidget(curiosButton);
        addRenderableWidget(toolboxButton);
        addRenderableWidget(toolkitButton);
        addRenderableWidget(resonatingLightningPatternCodingButton);

        layoutExtendedUIButtons();

        // 调整渲染图层：将它们在 renderables 列表中移到最前面，
        // 从而被后渲染的四个样板按钮遮挡（图层放在下面）。
        renderables.remove(advancedCodingButton);
        renderables.remove(cosmeticArmorButton);
        renderables.remove(curiosButton);
        renderables.remove(toolboxButton);
        renderables.remove(toolkitButton);
        renderables.remove(resonatingLightningPatternCodingButton);

        renderables.add(0, resonatingLightningPatternCodingButton);
        renderables.add(0, toolkitButton);
        renderables.add(0, toolboxButton);
        renderables.add(0, curiosButton);
        renderables.add(0, cosmeticArmorButton);
        renderables.add(0, advancedCodingButton);
    }

    private void layoutExtendedUIButtons() {
        if (advancedCodingButton == null || cosmeticArmorButton == null || curiosButton == null
                || toolboxButton == null || toolkitButton == null
                || resonatingLightningPatternCodingButton == null) {
            return;
        }
        if (extendedUiAvailabilityMask < 0) {
            refreshExtendedUiAvailabilityMask();
        }
        int availableMask = extendedUiAvailabilityMask;
        if (!extendedUiVisibilityDirty && availableMask == lastExtendedUiButtonMask) {
            return;
        }

        // 扩展 UI 按钮位置优先读取 JSON 的 extended_functions_0..3。
        // 按钮不可用时会折叠，后面的按钮自动补位到前一个 JSON 槽位。
        final int EXT_BTN_GAP_TOP = 1;
        final int EXT_BTN_GAP_LEFT = 1;
        int fallbackX;
        if (topModeTabButton != null && topModeTabButton.getWidth() > 0) {
            int tabRight = topModeTabButton.getX() + topModeTabButton.getWidth();
            fallbackX = tabRight + EXT_BTN_GAP_LEFT + 4; // setX = 外框左 + 4
        } else {
            fallbackX = leftPos + 331 + 22 + EXT_BTN_GAP_LEFT + 4;
        }
        int panelHeight = (upgradesPanel != null) ? upgradesPanel.getBounds().getHeight() : 46;
        int fallbackY = topPos + panelHeight + EXT_BTN_GAP_TOP + 2; // setY = 外框顶 + 4
        int extBtnSpacing = 21;

        int visibleIndex = 0;
        visibleIndex = placeExtendedButton(advancedCodingButton, IExtendedUIHost.ExtendedUIType.ADVANCED_CODING,
                visibleIndex, fallbackX, fallbackY, extBtnSpacing);
        visibleIndex = placeExtendedButton(cosmeticArmorButton, IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR,
                visibleIndex, fallbackX, fallbackY, extBtnSpacing);
        visibleIndex = placeExtendedButton(curiosButton, IExtendedUIHost.ExtendedUIType.CURIOS,
                visibleIndex, fallbackX, fallbackY, extBtnSpacing);
        visibleIndex = placeExtendedButton(toolboxButton, IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX,
                visibleIndex, fallbackX, fallbackY, extBtnSpacing);
        visibleIndex = placeExtendedButton(toolkitButton, IExtendedUIHost.ExtendedUIType.TOOLKIT,
                visibleIndex, fallbackX, fallbackY, extBtnSpacing);
        placeExtendedButton(resonatingLightningPatternCodingButton,
                IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING,
                visibleIndex, fallbackX, fallbackY, extBtnSpacing);
        lastExtendedUiButtonMask = availableMask;
    }

    private int placeExtendedButton(ExtendedUIButton button, IExtendedUIHost.ExtendedUIType type, int visibleIndex,
                                    int fallbackX, int fallbackY, int spacing) {
        boolean available = isExtendedUIAvailable(type);
        button.visible = available;
        button.active = available;
        if (!available) {
            // 未安装的扩展：保持不可见，且勿依赖默认 (0,0)；否则 updateExtendedUIVisibility 曾一度把 visible 设回 true 会画到界面左上角
            button.setX(-10000);
            button.setY(-10000);
            return visibleIndex;
        }

        var fallback = new ExtendedPanelLayout.Rect(fallbackX - leftPos,
                fallbackY - topPos + visibleIndex * spacing, button.getWidth(), button.getHeight());
        var rect = mainLayout.widget("extended_functions_" + visibleIndex, fallback, imageWidth, imageHeight);
        if (rect.left() < -1000 || rect.top() < -1000) {
            rect = fallback;
        }
        // JSON 可软编码 left/间距意图；顶边不得低于 fallback（随滚动升级槽高度与终端行数变化），
        // 否则行数多时会与右侧升级卡槽重叠。
        int minTopGui = fallback.top();
        rect = new ExtendedPanelLayout.Rect(rect.left(), Math.max(rect.top(), minTopGui), rect.width(),
                rect.height());
        button.setX(leftPos + rect.left());
        button.setY(topPos + rect.top());
        return visibleIndex + 1;
    }

    private boolean isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType type) {
        if ((extendedUiAvailabilityMask & extendedUiMask(type)) == 0) {
            return false;
        }
        return switch (type) {
            case ADVANCED_CODING -> true;
            case COSMETIC_ARMOR -> WcwtOptionalFeatureGates.isCosmeticArmorAvailable();
            case CURIOS -> WcwtOptionalFeatureGates.isCuriosAvailable();
            case TOOL_SLOTS_BOX -> menu.getToolbox().isPresent();
            case TOOLKIT -> true;
            case RESONATING_LIGHTNING_PATTERN_CODING -> WcwtOptionalFeatureGates
                    .isResonatingLightningPatternCodingAvailable();
            case NONE -> false;
        };
    }

    private int refreshExtendedUiAvailabilityMask() {
        var host = menu.getMenuHost();
        if (host == null) {
            return updateExtendedUiAvailabilityMask(0);
        }
        int mask = 0;
        for (var type : IExtendedUIHost.ExtendedUIType.values()) {
            if (type != IExtendedUIHost.ExtendedUIType.NONE && isExtendedUiCardInstalledUncached(type)) {
                mask |= extendedUiMask(type);
            }
        }
        return updateExtendedUiAvailabilityMask(mask);
    }

    private int updateExtendedUiAvailabilityMask(int mask) {
        if (extendedUiAvailabilityMask != mask) {
            extendedUiAvailabilityMask = mask;
            markExtendedUiLayoutDirty();
            patternProviderSlotLayoutDirty = true;
        }
        return extendedUiAvailabilityMask;
    }

    private void markExtendedUiLayoutDirty() {
        extendedUiVisibilityDirty = true;
        panelSlotActivityDirty = true;
    }

    private static int extendedUiMask(IExtendedUIHost.ExtendedUIType type) {
        return switch (type) {
            case ADVANCED_CODING -> 1;
            case COSMETIC_ARMOR -> 1 << 1;
            case CURIOS -> 1 << 2;
            case TOOL_SLOTS_BOX -> 1 << 3;
            case TOOLKIT -> 1 << 4;
            case RESONATING_LIGHTNING_PATTERN_CODING -> 1 << 5;
            case NONE -> 0;
        };
    }
    
    private void initializePanels() {
        renderables.remove(advancedCodingPanel);
        renderables.remove(curiosPanel);
        renderables.remove(cosmeticArmorPanel);
        renderables.remove(toolboxPanel);
        renderables.remove(toolkitPanel);
        renderables.remove(resonatingLightningPatternCodingPanel);

        int guiRight = leftPos + imageWidth;
        int guiBottom = topPos + imageHeight;

        // ────────── 各扩展UI面板的整体微调偏移 ──────────
        // 改这几个常量即可调整面板贴位。负值=向左/向上，正值=向右/向下。
        final int ADV_OFFSET_X = -1, ADV_OFFSET_Y = -1;     // 高级编码
        final int CUR_OFFSET_X = -1, CUR_OFFSET_Y = -1;     // 饰品
        final int COS_OFFSET_X = -1, COS_OFFSET_Y = -27;    // 装饰盔甲
        final int TOOL_OFFSET_X = -2, TOOL_OFFSET_Y = -25;  // 卡槽箱
        final int TOOLKIT_OFFSET_X = -1, TOOLKIT_OFFSET_Y = -1; // 工具包
        final int RLPC_OFFSET_X = -1, RLPC_OFFSET_Y = -1;   // 谐振过载编码器

        advancedCodingPanel = new AdvancedCodingPanel(0, 0);
        advancedCodingPanel.setOnCloseAction(this::closeExtendedUIFromPanel);
        advancedCodingPanel.setMenuSupplier(() -> menu);
        advancedCodingPanel.init();
        advancedCodingPanel.setPosition(
                guiRight + ADV_OFFSET_X,
                guiBottom - advancedCodingPanel.getBounds().getHeight() + ADV_OFFSET_Y);
        renderables.add(advancedCodingPanel);

        curiosPanel = new CuriosPanel(0, 0);
        curiosPanel.setOnCloseAction(this::closeExtendedUIFromPanel);
        curiosPanel.init();
        curiosPanel.setPosition(
                guiRight + CUR_OFFSET_X,
                guiBottom - curiosPanel.getBounds().getHeight() + CUR_OFFSET_Y);
        renderables.add(curiosPanel);

        cosmeticArmorPanel = new CosmeticArmorPanel(0, 0);
        cosmeticArmorPanel.setOnCloseAction(this::closeExtendedUIFromPanel);
        cosmeticArmorPanel.init();
        cosmeticArmorPanel.setPosition(
                guiRight + COS_OFFSET_X,
                guiBottom - cosmeticArmorPanel.getBounds().getHeight() + COS_OFFSET_Y);
        renderables.add(cosmeticArmorPanel);

        toolboxPanel = new ToolboxPanel(0, 0);
        toolboxPanel.setOnCloseAction(this::closeExtendedUIFromPanel);
        toolboxPanel.init();
        toolboxPanel.setPosition(
                guiRight + TOOL_OFFSET_X,
                guiBottom - toolboxPanel.getBounds().getHeight() + TOOL_OFFSET_Y);
        renderables.add(toolboxPanel);

        toolkitPanel = new ToolkitPanel(0, 0);
        toolkitPanel.setOnCloseAction(this::closeExtendedUIFromPanel);
        toolkitPanel.init();
        toolkitPanel.setPosition(
                guiRight + TOOLKIT_OFFSET_X,
                guiBottom - toolkitPanel.getBounds().getHeight() + TOOLKIT_OFFSET_Y);
        renderables.add(toolkitPanel);

        resonatingLightningPatternCodingPanel = new ResonatingLightningPatternCodingPanel(0, 0);
        resonatingLightningPatternCodingPanel.setOnCloseAction(this::closeExtendedUIFromPanel);
        resonatingLightningPatternCodingPanel.setMenuSupplier(() -> menu);
        resonatingLightningPatternCodingPanel.init();
        resonatingLightningPatternCodingPanel.setPosition(
                guiRight + RLPC_OFFSET_X,
                guiBottom - resonatingLightningPatternCodingPanel.getBounds().getHeight() + RLPC_OFFSET_Y);
        renderables.add(resonatingLightningPatternCodingPanel);
    }

    public AdvancedCodingPanel getAdvancedCodingPanel() {
        return advancedCodingPanel;
    }

    private void closeExtendedUIFromPanel() {
        var host = menu.getMenuHost();
        if (host == null) {
            return;
        }

        host.closeExtendedUI();
        rememberManagementToolkitOpenState(false);
        PacketDistributor.sendToServer(new ExtendedUIPacket(IExtendedUIHost.ExtendedUIType.NONE));
        updateExtendedUIVisibility();
    }

    @Override
    public List<Rect2i> getExclusionZones() {
        var zones = super.getExclusionZones();

        addExtendedButtonExclusion(zones, advancedCodingButton);
        addExtendedButtonExclusion(zones, cosmeticArmorButton);
        addExtendedButtonExclusion(zones, curiosButton);
        addExtendedButtonExclusion(zones, toolboxButton);
        addExtendedButtonExclusion(zones, toolkitButton);
        addExtendedButtonExclusion(zones, resonatingLightningPatternCodingButton);

        addExtendedPanelExclusion(zones, advancedCodingPanel);
        addExtendedPanelExclusion(zones, cosmeticArmorPanel);
        addExtendedPanelExclusion(zones, curiosPanel);
        addExtendedPanelExclusion(zones, toolboxPanel);
        addExtendedPanelExclusion(zones, toolkitPanel);
        addExtendedPanelExclusion(zones, resonatingLightningPatternCodingPanel);

        return zones;
    }

    private static void addExtendedButtonExclusion(List<Rect2i> zones, ExtendedUIButton button) {
        if (button != null && button.visible) {
            zones.add(new Rect2i(button.getX() - 6, button.getY() - 6, 32, 31));
        }
    }

    private static void addExtendedPanelExclusion(List<Rect2i> zones, ExtendedUIPanel panel) {
        if (panel != null && panel.isVisible()) {
            var bounds = panel.getBounds();
            zones.add(new Rect2i(bounds.getX() - 2, bounds.getY() - 2, bounds.getWidth() + 4, bounds.getHeight() + 4));
        }
    }

    private static final class ViewCellsVisibilityWidget implements ICompositeWidget {
        private final ICompositeWidget delegate;
        private final java.util.function.BooleanSupplier visible;

        private ViewCellsVisibilityWidget(ICompositeWidget delegate, java.util.function.BooleanSupplier visible) {
            this.delegate = delegate;
            this.visible = visible;
        }

        @Override
        public boolean isVisible() {
            return visible.getAsBoolean() && delegate.isVisible();
        }

        private void setVisible(boolean ignored) {
        }

        @Override
        public void setPosition(Point position) {
            delegate.setPosition(position);
        }

        @Override
        public void setSize(int width, int height) {
            delegate.setSize(width, height);
        }

        @Override
        public Rect2i getBounds() {
            return isVisible() ? delegate.getBounds() : new Rect2i(0, 0, 0, 0);
        }

        @Override
        public void addExclusionZones(List<Rect2i> exclusionZones, Rect2i screenBounds) {
            if (isVisible()) {
                delegate.addExclusionZones(exclusionZones, screenBounds);
            }
        }

        @Override
        public void populateScreen(java.util.function.Consumer<AbstractWidget> addWidget,
                                   Rect2i bounds,
                                   appeng.client.gui.AEBaseScreen<?> screen) {
            delegate.populateScreen(addWidget, bounds, screen);
        }

        @Override
        public void tick() {
            if (isVisible()) {
                delegate.tick();
            }
        }

        @Override
        public void updateBeforeRender() {
            if (isVisible()) {
                delegate.updateBeforeRender();
            }
        }

        @Override
        public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        }

        private void drawOverlay(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
            if (isVisible()) {
                delegate.drawBackgroundLayer(guiGraphics, bounds, mouse);
                delegate.drawForegroundLayer(guiGraphics, bounds, mouse);
            }
        }

        @Override
        public void drawForegroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        }

        @Override
        public boolean onMouseDown(Point mousePos, int button) {
            return isVisible() && delegate.onMouseDown(mousePos, button);
        }

        @Override
        public boolean wantsAllMouseDownEvents() {
            return isVisible() && delegate.wantsAllMouseDownEvents();
        }

        @Override
        public boolean onMouseUp(Point mousePos, int button) {
            return isVisible() && delegate.onMouseUp(mousePos, button);
        }

        @Override
        public boolean wantsAllMouseUpEvents() {
            return isVisible() && delegate.wantsAllMouseUpEvents();
        }

        @Override
        public boolean onMouseDrag(Point mousePos, int button) {
            return isVisible() && delegate.onMouseDrag(mousePos, button);
        }

        @Override
        public boolean onMouseWheel(Point mousePos, double delta) {
            return isVisible() && delegate.onMouseWheel(mousePos, delta);
        }

        @Override
        public boolean wantsAllMouseWheelEvents() {
            return isVisible() && delegate.wantsAllMouseWheelEvents();
        }

        @Override
        public @Nullable Tooltip getTooltip(int mouseX, int mouseY) {
            return isVisible() ? delegate.getTooltip(mouseX, mouseY) : null;
        }
    }
    
    private void toggleExtendedUI(IExtendedUIHost.ExtendedUIType type) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        var host = menu.getMenuHost();
        if (host == null) return;
        
        // 如果点击的是当前已打开的UI，则关闭它
        IExtendedUIHost.ExtendedUIType newType;
        if (host.getCurrentExtendedUI() == type) {
            newType = IExtendedUIHost.ExtendedUIType.NONE;
            host.closeExtendedUI();
        } else {
            newType = type;
            host.setCurrentExtendedUI(type);
        }
        rememberManagementToolkitOpenState(newType == IExtendedUIHost.ExtendedUIType.TOOLKIT);
        
        // 发送网络数据包同步状态
        PacketDistributor.sendToServer(new ExtendedUIPacket(newType));
        markExtendedUiLayoutDirty();
        updateExtendedUIVisibility();
    }
    
    private void updateExtendedUIVisibility() {
        var host = menu.getMenuHost();
        if (host == null) return;

        int availableMask = refreshExtendedUiAvailabilityMask();
        layoutExtendedUIButtons();
        host.setCurrentExtendedUI(menu.getSyncedExtendedUIType());
        
        var currentUI = host.getCurrentExtendedUI();
        if (currentUI != IExtendedUIHost.ExtendedUIType.NONE && !isExtendedUIAvailable(currentUI)) {
            currentUI = IExtendedUIHost.ExtendedUIType.NONE;
            host.closeExtendedUI();
            PacketDistributor.sendToServer(new ExtendedUIPacket(currentUI));
        }
        boolean toolkitInManagementArea = isToolkitExpandedInManagementArea();

        // 这些是轻量运行态，保持每帧与当前 UI/选择同步；槽位重排再走下面的脏检查。
        advancedCodingMode = (currentUI == IExtendedUIHost.ExtendedUIType.ADVANCED_CODING);
        patternSelectionLockedMode = advancedCodingMode
                || currentUI == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING;
        if (!patternSelectionLockedMode) {
            selectedPatternCacheIndex = -1;
        }
        if (patternSelectionLockedMode && host.getSelectedPatternIndex() >= 0) {
            selectedPatternCacheIndex = host.getSelectedPatternIndex();
        }

        boolean visibilityStateChanged = extendedUiVisibilityDirty
                || lastExtendedUiVisibilityType != currentUI
                || lastExtendedUiToolkitInManagementArea != toolkitInManagementArea
                || lastExtendedUiButtonMask != availableMask;
        if (!visibilityStateChanged) {
            return;
        }
        boolean hideExtendedButtons = currentUI == IExtendedUIHost.ExtendedUIType.ADVANCED_CODING
                || currentUI == IExtendedUIHost.ExtendedUIType.CURIOS
                || (currentUI == IExtendedUIHost.ExtendedUIType.TOOLKIT && !toolkitInManagementArea)
                || currentUI == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING;

        // 高级编码 / 饰品 / 工具包界面打开时，隐藏右侧这组扩展按钮；关闭后再显示。（卡槽箱不隐藏按钮）
        // 仅对已启用的扩展刷新显隐；不可用项须保持隐藏（不可被 !hideExtendedButtons 误判为显示）
        if (advancedCodingButton != null) {
            advancedCodingButton.visible = !hideExtendedButtons
                    && isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType.ADVANCED_CODING);
            advancedCodingButton.active = advancedCodingButton.visible;
        }
        if (cosmeticArmorButton != null) {
            cosmeticArmorButton.visible = !hideExtendedButtons
                    && isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR);
            cosmeticArmorButton.active = cosmeticArmorButton.visible;
        }
        if (curiosButton != null) {
            curiosButton.visible = !hideExtendedButtons && isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType.CURIOS);
            curiosButton.active = curiosButton.visible;
        }
        if (toolboxButton != null) {
            toolboxButton.visible = !hideExtendedButtons && isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX);
            toolboxButton.active = toolboxButton.visible;
        }
        if (toolkitButton != null) {
            toolkitButton.visible = !hideExtendedButtons && isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType.TOOLKIT);
            toolkitButton.active = toolkitButton.visible;
        }
        if (favoriteItemsButton != null) {
            favoriteItemsButton.visible = true;
            favoriteItemsButton.active = favoriteItemsButton.visible;
        }
        if (viewCellsToggleButton != null) {
            viewCellsToggleButton.visible = viewCellsVisibilityWidget != null;
            viewCellsToggleButton.active = viewCellsToggleButton.visible;
        }
        if (universalTerminalButton != null) {
            universalTerminalButton.refresh(menu);
        }
        if (resonatingLightningPatternCodingButton != null) {
            resonatingLightningPatternCodingButton.visible = !hideExtendedButtons
                    && isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING);
            resonatingLightningPatternCodingButton.active = resonatingLightningPatternCodingButton.visible;
        }

        // 隐藏所有面板
        if (advancedCodingPanel != null) advancedCodingPanel.setVisible(false);
        if (cosmeticArmorPanel != null) cosmeticArmorPanel.setVisible(false);
        if (curiosPanel != null) curiosPanel.setVisible(false);
        if (toolboxPanel != null) toolboxPanel.setVisible(false);
        if (toolkitPanel != null) toolkitPanel.setVisible(false);
        if (resonatingLightningPatternCodingPanel != null) resonatingLightningPatternCodingPanel.setVisible(false);
        
        // 显示当前选中的面板
        switch (currentUI) {
            case ADVANCED_CODING:
                if (advancedCodingPanel != null) advancedCodingPanel.setVisible(true);
                break;
            case COSMETIC_ARMOR:
                if (cosmeticArmorPanel != null) cosmeticArmorPanel.setVisible(true);
                break;
            case CURIOS:
                if (curiosPanel != null) curiosPanel.setVisible(true);
                break;
            case TOOL_SLOTS_BOX:
                if (toolboxPanel != null) toolboxPanel.setVisible(true);
                break;
            case TOOLKIT:
                if (toolkitPanel != null) toolkitPanel.setVisible(!toolkitInManagementArea);
                break;
            case RESONATING_LIGHTNING_PATTERN_CODING:
                if (resonatingLightningPatternCodingPanel != null) {
                    resonatingLightningPatternCodingPanel.setVisible(true);
                }
                break;
            case NONE:
                // 所有面板都已隐藏
                break;
        }
        lastExtendedUiVisibilityType = currentUI;
        lastExtendedUiToolkitInManagementArea = toolkitInManagementArea;
        extendedUiVisibilityDirty = false;
        panelSlotActivityDirty = true;
        patternProviderSlotLayoutDirty = true;
    }

    public boolean handleExtendedUiHotkey(int keyCode, int scanCode) {
        if (matchesHotkey(WcwtKeybindings.OPEN_ADVANCED_CODING, keyCode, scanCode)) {
            return triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType.ADVANCED_CODING);
        }
        if (matchesHotkey(WcwtKeybindings.OPEN_COSMETIC_ARMOR, keyCode, scanCode)) {
            return triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR);
        }
        if (matchesHotkey(WcwtKeybindings.OPEN_CURIOS, keyCode, scanCode)) {
            return triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType.CURIOS);
        }
        if (matchesHotkey(WcwtKeybindings.OPEN_TOOL_SLOTS_BOX, keyCode, scanCode)) {
            return triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX);
        }
        if (matchesHotkey(WcwtKeybindings.OPEN_TOOLKIT, keyCode, scanCode)) {
            return triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType.TOOLKIT);
        }
        if (matchesHotkey(WcwtKeybindings.OPEN_RESONATING_LIGHTNING_PATTERN_CODING, keyCode, scanCode)) {
            return triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING);
        }
        return false;
    }

    public boolean isTypingInPatternManagementField() {
        return isMeTerminalSearchFieldFocused()
                || (patternManageSearchField != null && patternManageSearchField.isFocused())
                || (patternManageMappingField != null && patternManageMappingField.isFocused())
                || (manualAnvilNameField != null && manualAnvilNameField.isFocused());
    }

    private boolean isMeTerminalSearchFieldFocused() {
        try {
            Field sf = MEStorageScreen.class.getDeclaredField("searchField");
            sf.setAccessible(true);
            Object field = sf.get(this);
            return field instanceof AETextField textField && textField.isFocused();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean triggerExtendedUiHotkey(IExtendedUIHost.ExtendedUIType type) {
        refreshExtendedUiAvailabilityMask();
        if (!isExtendedUIAvailable(type)) {
            return false;
        }
        toggleExtendedUI(type);
        return true;
    }

    private static boolean matchesHotkey(com.mojang.blaze3d.platform.InputConstants.Key key, int keyCode, int scanCode) {
        return key != null && key.getType().getOrCreate(keyCode) != null;
    }

    private static boolean matchesHotkey(net.minecraft.client.KeyMapping mapping, int keyCode, int scanCode) {
        return mapping.matches(keyCode, scanCode);
    }
    
    private void onMultiplierButtonClick(PatternMultiplierButton.MultiplierType type) {
        // 发送样板倍增器数据包到服务端
        PacketDistributor.sendToServer(new PatternMultiplierPacket(
                type,
                WcwtClientConfig.patternMultiplierApplyToEditorProcessing()));
    }

    private void handleEncodePatternButton() {
        String searchKey = consumeEaepProviderSearchKey();
        boolean generatedFallbackSearchKey = false;
        if ((searchKey == null || searchKey.isBlank()) && patternManageSearchField != null) {
            String fieldValue = patternManageSearchField.getValue();
            if (fieldValue != null && !fieldValue.isBlank()) {
                searchKey = fieldValue.trim();
            }
        }
        if ((searchKey == null || searchKey.isBlank()) && patternEncodingMode != EncodingMode.PROCESSING) {
            searchKey = "crafting";
            generatedFallbackSearchKey = true;
        }
        String resolvedSearchKey = searchKey == null || searchKey.isBlank()
                ? ""
                : resolveClientProviderSearchText(searchKey);
        if (!resolvedSearchKey.isBlank()) {
            resolvedPatternManagementSearchText = resolvedSearchKey;
        }
        long preferredProviderId = preferredPatternProviderIdForUpload(searchKey);
        String uploadProviderName = uploadProviderNameForStatus(preferredProviderId, searchKey);
        if (searchKey != null && !searchKey.isBlank() && patternManageSearchField != null && !generatedFallbackSearchKey) {
            patternManageSearchField.setValue(searchKey);
        }
        if (encodePatternButton != null) {
            encodePatternButton.setFocused(false);
        }
        setFocused(null);
        logPatternUploadDebug("client encode send mode={}, uploadEnabled={}, searchKey={}, resolvedSearchKey={}, preferredProviderId={}, uploadProviderName={}, field={}, resolvedField={}",
                patternEncodingMode, patternManagementUploadEnabled, searchKey, resolvedSearchKey, preferredProviderId,
                uploadProviderName, currentPatternManagementSearchText(), resolvedPatternManagementSearchText);
        PacketDistributor.sendToServer(new EncodePatternPacket(patternEncodingMode, patternManagementUploadEnabled,
                resolvedSearchKey,
                preferredProviderId,
                uploadProviderName,
                WcwtClientConfig.patternUploadFailFallbackToEditor()));
    }

    @Nullable
    private String consumeEaepProviderSearchKey() {
        return ExtendedAePlusUploadCompat.consumeLastProviderSearchKey();
    }

    @Nullable
    private String resolveEaepProviderSearchKey(String rawKey) {
        return resolveClientProviderSearchText(rawKey);
    }

    private String resolveClientProviderSearchText(@Nullable String rawKey) {
        if (rawKey == null) {
            return "";
        }
        String normalized = rawKey.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        String resolved = ExtendedAePlusUploadCompat.resolveSearchKeyAlias(normalized);
        return resolved == null || resolved.isBlank() ? normalized : resolved.trim();
    }

    private void setPatternEncodingMode(EncodingMode mode) {
        patternEncodingMode = mode;
        lastSyncedPatternEncodingMode = mode;
        menu.setPatternEncodingMode(mode);
        updatePatternEncodingModeButtons();
    }

    private void syncPatternEncodingModeFromMenu() {
        EncodingMode syncedMode = menu.getPatternEncodingMode();
        if (syncedMode != lastSyncedPatternEncodingMode) {
            patternEncodingMode = syncedMode;
            lastSyncedPatternEncodingMode = syncedMode;
            updatePatternEncodingModeButtons();
        }
    }

    /**
     * EA+ 风格：将 JEI 当前指向的配料/书签（含配方书签）名称写入 ME 终端搜索框，并同步样板管理搜索框。
     * 快捷键由 {@link ModClientSetup} 在 {@code ScreenEvent.KeyPressed.Pre} 中监听，与 EA+「填充搜索」键位一致。
     */
    public boolean fillProviderSearchFromJeiIngredient() {
        String name = resolveJeiHoveredSearchName();
        if (name == null || name.isBlank()) {
            return false;
        }
        applyJeiNameToMeTerminalSearch(name);
        if (patternManageSearchField != null) {
            patternManageSearchField.setValue(name);
        }
        return true;
    }

    private void applyJeiNameToMeTerminalSearch(String name) {
        try {
            Field sf = MEStorageScreen.class.getDeclaredField("searchField");
            sf.setAccessible(true);
            Object field = sf.get(this);
            if (field instanceof AETextField textField) {
                textField.setValue(name);
            }
            Method setSearchText = MEStorageScreen.class.getDeclaredMethod("setSearchText", String.class);
            setSearchText.setAccessible(true);
            setSearchText.invoke(this, name);
            ItemListMod.setSearchText(name);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private String resolveJeiHoveredSearchName() {
        try {
            Class<?> proxyClass = Class.forName("com.extendedae_plus.integration.jei.JeiRuntimeProxy");
            if (proxyClass.getMethod("get").invoke(null) == null) {
                return null;
            }
            Method getName = proxyClass.getMethod("getTypedIngredientDisplayName", Object.class);

            Method getIngredient = proxyClass.getMethod("getIngredientUnderMouse");
            Object ingResult = getIngredient.invoke(null);
            if (ingResult instanceof Optional<?> optional && optional.isPresent()) {
                Object n = getName.invoke(null, optional.get());
                if (n instanceof String text && !text.isBlank()) {
                    return text;
                }
            }

            Method getRecipeBm = proxyClass.getMethod("getRecipeBookmarkUnderMouse");
            Object bmOpt = getRecipeBm.invoke(null);
            if (bmOpt instanceof Optional<?> obm && obm.isPresent()) {
                String fromRecipe = searchNameFromRecipeBookmark(obm.get());
                if (fromRecipe != null && !fromRecipe.isBlank()) {
                    return fromRecipe;
                }
            }
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private String searchNameFromRecipeBookmark(Object recipeBookmark) {
        try {
            Object holderOpt = recipeBookmark.getClass().getMethod("getRecipe").invoke(recipeBookmark);
            Object recipeBase = null;
            if (holderOpt instanceof Optional<?> ho && ho.isPresent()) {
                Object holder = ho.get();
                recipeBase = holder;
                try {
                    Object value = holder.getClass().getMethod("value").invoke(holder);
                    if (value instanceof Recipe<?> r) {
                        recipeBase = r;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (recipeBase instanceof Recipe<?> recipe) {
                String mapped = ExtendedAePlusUploadCompat.mapRecipeTypeToSearchKey(recipe);
                if (mapped != null && !mapped.isBlank()) {
                    return mapped;
                }
            }
            String derived = ExtendedAePlusUploadCompat.deriveSearchKeyFromUnknownRecipe(recipeBookmark);
            if (derived != null && !derived.isBlank()) {
                return derived;
            }
        } catch (Throwable ignored) {
        }
        return recipeBookmarkFallbackHoverName(recipeBookmark);
    }

    @Nullable
    private static String recipeBookmarkFallbackHoverName(Object recipeBookmark) {
        try {
            Object holderOpt = recipeBookmark.getClass().getMethod("getRecipe").invoke(recipeBookmark);
            if (!(holderOpt instanceof Optional<?> ho) || ho.isEmpty()) {
                return null;
            }
            Object holder = ho.get();
            Recipe<?> recipe = null;
            if (holder instanceof RecipeHolder<?> rh) {
                recipe = rh.value();
            } else if (holder instanceof Recipe<?> r) {
                recipe = r;
            }
            if (recipe == null) {
                return null;
            }
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) {
                return null;
            }
            var stack = recipe.getResultItem(level.registryAccess());
            if (!stack.isEmpty()) {
                return stack.getHoverName().getString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void updatePatternEncodingModeButtons() {
        if (tabCrafting != null) {
            tabCrafting.setSelected(patternEncodingMode == EncodingMode.CRAFTING);
        }
        if (tabProcessing != null) {
            tabProcessing.setSelected(patternEncodingMode == EncodingMode.PROCESSING);
        }
        if (tabSmithing != null) {
            tabSmithing.setSelected(patternEncodingMode == EncodingMode.SMITHING_TABLE);
        }
        if (tabStonecutting != null) {
            tabStonecutting.setSelected(patternEncodingMode == EncodingMode.STONECUTTING);
        }
    }

    private void updateManualWorkspaceUi() {
        var mode = menu.getManualWorkspaceMode();

        updateManualWorkspaceButtons(mode);
        updateManualCraftingControls(mode);
        updateManualCraftingSlots(mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING);
        updateManualSmithingSlots(mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.SMITHING);
        updateManualAnvilSlots(mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL);
        updateManualAnvilField(mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL);
    }

    private void switchManualWorkspaceMode(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode mode) {
        menu.setManualWorkspaceMode(mode);
        updateManualWorkspaceUi();
        PacketDistributor.sendToServer(new ManualWorkspaceModePacket(mode.ordinal()));
    }

    private void updateManualWorkspaceButtons(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode mode) {
        var first = mainLayout.widget("manual_mode_button0",
                new ExtendedPanelLayout.Rect(108, imageHeight - 168, 12, 12), imageWidth, imageHeight);
        var second = mainLayout.widget("manual_mode_button1",
                new ExtendedPanelLayout.Rect(92, imageHeight - 168, 12, 12), imageWidth, imageHeight);
        var third = mainLayout.widget("manual_mode_button2",
                new ExtendedPanelLayout.Rect(106, imageHeight - 168, 12, 12), imageWidth, imageHeight);

        if (manualCraftingButton != null) {
            manualCraftingButton.setX(leftPos + first.left());
            manualCraftingButton.setY(topPos + first.top());
            manualCraftingButton.setWidth(first.width());
            manualCraftingButton.setHeight(first.height());
            manualCraftingButton.visible = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL;
            manualCraftingButton.active = manualCraftingButton.visible;
        }
        if (manualSmithingButton != null) {
            manualSmithingButton.setX(leftPos + second.left());
            manualSmithingButton.setY(topPos + second.top());
            manualSmithingButton.setWidth(second.width());
            manualSmithingButton.setHeight(second.height());
            manualSmithingButton.visible = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING;
            manualSmithingButton.active = manualSmithingButton.visible;
        }
        if (manualAnvilButton != null) {
            manualAnvilButton.setX(leftPos + third.left());
            manualAnvilButton.setY(topPos + third.top());
            manualAnvilButton.setWidth(third.width());
            manualAnvilButton.setHeight(third.height());
            manualAnvilButton.visible = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.SMITHING;
            manualAnvilButton.active = manualAnvilButton.visible;
        }
    }

    private void updateManualCraftingControls(WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode mode) {
        if (craftingLockButton != null) {
            craftingLockButton.visible = true;
            craftingLockButton.active = true;
        }

        var clearGridRect = mainLayout.widget("clearCraftingGrid",
                new ExtendedPanelLayout.Rect(134, imageHeight - 214, 8, 8), imageWidth, imageHeight);
        var clearToPlayerRect = mainLayout.widget("clearToPlayerInv",
                new ExtendedPanelLayout.Rect(144, imageHeight - 214, 8, 8), imageWidth, imageHeight);
        var manualPatternSubstitutionsRect = mainLayout.widget("wcwtManualPatternSubstitutions",
                new ExtendedPanelLayout.Rect(134, imageHeight - 204, 8, 8), imageWidth, imageHeight);
        var manualPatternFluidSubstitutionsRect = mainLayout.widget("wcwtManualPatternFluidSubstitutions",
                new ExtendedPanelLayout.Rect(144, imageHeight - 204, 8, 8), imageWidth, imageHeight);
        var clearGridAnvilRect = mainLayout.widget("manual_clearCraftingGrid_anvil",
                new ExtendedPanelLayout.Rect(78, imageHeight - 168, 8, 8), imageWidth, imageHeight);
        var clearToPlayerAnvilRect = mainLayout.widget("manual_clearToPlayerInv_anvil",
                new ExtendedPanelLayout.Rect(88, imageHeight - 168, 8, 8), imageWidth, imageHeight);
        var activeClearGridRect = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL
                ? clearGridAnvilRect
                : clearGridRect;
        var activeClearToPlayerRect = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL
                ? clearToPlayerAnvilRect
                : clearToPlayerRect;

        if (clearCraftingGridButton != null) {
            clearCraftingGridButton.setX(leftPos + activeClearGridRect.left());
            clearCraftingGridButton.setY(topPos + activeClearGridRect.top());
            clearCraftingGridButton.visible = true;
            clearCraftingGridButton.active = true;
        }
        if (clearToPlayerInvButton != null) {
            clearToPlayerInvButton.setX(leftPos + activeClearToPlayerRect.left());
            clearToPlayerInvButton.setY(topPos + activeClearToPlayerRect.top());
            clearToPlayerInvButton.visible = true;
            clearToPlayerInvButton.active = true;
        }

        boolean showManualPatternOptions = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING
                && patternEncodingMode == EncodingMode.CRAFTING;
        if (manualPatternSubstitutionButton != null) {
            manualPatternSubstitutionButton.setX(leftPos + manualPatternSubstitutionsRect.left());
            manualPatternSubstitutionButton.setY(topPos + manualPatternSubstitutionsRect.top());
            manualPatternSubstitutionButton.visible = showManualPatternOptions;
            manualPatternSubstitutionButton.active = showManualPatternOptions;
            manualPatternSubstitutionButton.setState(menu.isManualCraftingItemSubstitution());
        }
        if (manualPatternFluidSubstitutionButton != null) {
            manualPatternFluidSubstitutionButton.setX(leftPos + manualPatternFluidSubstitutionsRect.left());
            manualPatternFluidSubstitutionButton.setY(topPos + manualPatternFluidSubstitutionsRect.top());
            manualPatternFluidSubstitutionButton.visible = showManualPatternOptions;
            manualPatternFluidSubstitutionButton.active = showManualPatternOptions;
            manualPatternFluidSubstitutionButton.setState(menu.isManualCraftingFluidSubstitution());
        }
    }

    private void updateManualCraftingSlots(boolean visible) {
        var base = mainLayout.slot("CRAFTING_GRID",
                new ExtendedPanelLayout.Rect(79, imageHeight - 214, 18, 18), imageWidth, imageHeight);
        var result = mainLayout.slot("CRAFTING_RESULT",
                new ExtendedPanelLayout.Rect(149, imageHeight - 196, 18, 18), imageWidth, imageHeight);

        var craftingSlots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
        for (int i = 0; i < craftingSlots.size(); i++) {
            if (visible) {
                showSlot(craftingSlots.get(i), base.left() + (i % 3) * 18, base.top() + (i / 3) * 18);
            } else {
                hideSlot(craftingSlots.get(i));
            }
        }

        for (var slot : menu.getSlots(SlotSemantics.CRAFTING_RESULT)) {
            if (visible) {
                showSlot(slot, result.left(), result.top());
            } else {
                hideSlot(slot);
            }
        }
    }

    private void updateManualSmithingSlots(boolean visible) {
        var template = mainLayout.widget("manual_smithing_template_slot",
                new ExtendedPanelLayout.Rect(79, imageHeight - 194, 18, 18), imageWidth, imageHeight);
        var base = mainLayout.widget("manual_smithing_base_slot",
                new ExtendedPanelLayout.Rect(97, imageHeight - 194, 18, 18), imageWidth, imageHeight);
        var addition = mainLayout.widget("manual_smithing_addition_slot",
                new ExtendedPanelLayout.Rect(115, imageHeight - 194, 18, 18), imageWidth, imageHeight);
        var result = mainLayout.widget("manual_smithing_result_slot",
                new ExtendedPanelLayout.Rect(149, imageHeight - 196, 18, 18), imageWidth, imageHeight);
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_SMITHING_TEMPLATE, visible, template.left(),
                template.top());
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_SMITHING_BASE, visible, base.left(), base.top());
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_SMITHING_ADDITION, visible, addition.left(),
                addition.top());
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_SMITHING_RESULT, visible, result.left(), result.top());
    }

    private void updateManualAnvilSlots(boolean visible) {
        var left = mainLayout.widget("manual_anvil_left_slot",
                new ExtendedPanelLayout.Rect(79, imageHeight - 194, 18, 18), imageWidth, imageHeight);
        var right = mainLayout.widget("manual_anvil_right_slot",
                new ExtendedPanelLayout.Rect(115, imageHeight - 194, 18, 18), imageWidth, imageHeight);
        var result = mainLayout.widget("manual_anvil_result_slot",
                new ExtendedPanelLayout.Rect(149, imageHeight - 196, 18, 18), imageWidth, imageHeight);
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_ANVIL_LEFT, visible, left.left(), left.top());
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RIGHT, visible, right.left(), right.top());
        updateSingleSemanticSlot(WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RESULT, visible, result.left(), result.top());
    }

    private void updateSingleSemanticSlot(appeng.menu.SlotSemantic semantic, boolean visible, int x, int y) {
        for (var slot : menu.getSlots(semantic)) {
            if (visible) {
                showSlot(slot, x, y);
            } else {
                hideSlot(slot);
            }
        }
    }

    private void updateManualAnvilField(boolean visible) {
        if (manualAnvilNameField == null) {
            return;
        }
        var rect = mainLayout.widget("manual_anvil_name",
                new ExtendedPanelLayout.Rect(78, imageHeight - 213, 88, 12), imageWidth, imageHeight);
        manualAnvilNameField.setX(leftPos + rect.left());
        manualAnvilNameField.setY(topPos + rect.top());
        manualAnvilNameField.setWidth(rect.width());
        manualAnvilNameField.setHeight(rect.height());
        manualAnvilNameField.setVisible(visible);
        manualAnvilNameField.active = visible;
        if (!visible) {
            manualAnvilNameField.setFocused(false);
            if (getFocused() == manualAnvilNameField) {
                setFocused(null);
            }
            return;
        }
        if (!manualAnvilNameField.isFocused()
                && !Objects.equals(manualAnvilNameField.getValue(), menu.getManualAnvilName())) {
            syncingManualAnvilNameField = true;
            try {
                manualAnvilNameField.setValue(menu.getManualAnvilName());
            } finally {
                syncingManualAnvilNameField = false;
            }
        }
    }

    private static final class TransparentEditBox extends EditBox {
        private TransparentEditBox(net.minecraft.client.gui.Font font, int x, int y, int width, int height,
                                   Component message) {
            super(font, x, y, width, height, message);
            setBordered(false);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (isVisible()) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
    
    /**
     * 仅高级编码面板专属的真实槽位（不含 CONFIG/替换 ghost —— 这些区域现在是面板内部自绘）。
     * 当面板打开时，这些槽位由 AdvancedCodingPanel.renderContent() 自行渲染，
     * 因此 renderSlot() 跳过它们，避免双重图标 / 错位。
     */
    private static final appeng.menu.SlotSemantic[] PANEL_SLOT_SEMANTICS = {
            com.lhy.wcwt.menu.WcwtSlotSemantics.COPY_PATTERN,
            com.lhy.wcwt.menu.WcwtSlotSemantics.WCWT_STORAGE_CELL
    };
    private static final appeng.menu.SlotSemantic[] RLPC_PANEL_SLOT_SEMANTICS = {
            com.lhy.wcwt.menu.WcwtSlotSemantics.WCWT_RESONATING_STORAGE
    };
    private static final appeng.menu.SlotSemantic[] COSMETIC_ARMOR_SLOT_SEMANTICS = {
            com.lhy.wcwt.menu.WcwtSlotSemantics.DECORATIVE_HELMET,
            com.lhy.wcwt.menu.WcwtSlotSemantics.DECORATIVE_ARMOR,
            com.lhy.wcwt.menu.WcwtSlotSemantics.DECORATIVE_SHIN_GUARDS,
            com.lhy.wcwt.menu.WcwtSlotSemantics.DECORATIVE_BOOTS
    };
    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (DEBUG_FRAME_SYNC) {
            recordFrameSyncTick();
        }
        if (universalTerminalButton != null) {
            universalTerminalButton.refresh(menu);
        }
        WcwtFavorites.ensureLoaded();
        refreshRepoViewAfterTransientReconnect();
        restoreManagementToolkitOpenStateIfNeeded();
        boolean syncedPatternManagementUploadEnabled = menu.isPatternManagementUploadEnabled();
        var syncedPatternManagementDisplayMode = patternManagementDisplayModeFromOrdinal(menu.getPatternManagementDisplayMode());
        boolean syncedPatternManagementShowSlots = menu.isPatternManagementShowSlots();
        var syncedPatternManagementSearchMode = patternManagementSearchModeFromOrdinal(menu.getPatternManagementSearchMode());
        var previousPatternManagementDisplayMode = patternManagementDisplayMode;
        boolean previousPatternManagementShowSlots = patternManagementShowSlots;
        var previousPatternManagementSearchMode = patternManagementSearchMode;
        if (pendingPatternManagementDisplayMode != null) {
            if (pendingPatternManagementDisplayMode == syncedPatternManagementDisplayMode) {
                pendingPatternManagementDisplayMode = null;
                patternManagementDisplayMode = syncedPatternManagementDisplayMode;
            } else {
                patternManagementDisplayMode = pendingPatternManagementDisplayMode;
            }
        } else {
            patternManagementDisplayMode = syncedPatternManagementDisplayMode;
        }
        if (pendingPatternManagementShowSlots != null) {
            if (pendingPatternManagementShowSlots == syncedPatternManagementShowSlots) {
                pendingPatternManagementShowSlots = null;
                patternManagementShowSlots = syncedPatternManagementShowSlots;
            } else {
                patternManagementShowSlots = pendingPatternManagementShowSlots;
            }
        } else {
            patternManagementShowSlots = syncedPatternManagementShowSlots;
        }
        if (pendingPatternManagementSearchMode != null) {
            if (pendingPatternManagementSearchMode == syncedPatternManagementSearchMode) {
                pendingPatternManagementSearchMode = null;
                patternManagementSearchMode = syncedPatternManagementSearchMode;
            } else {
                patternManagementSearchMode = pendingPatternManagementSearchMode;
            }
        } else {
            patternManagementSearchMode = syncedPatternManagementSearchMode;
        }
        if (pendingPatternManagementUploadEnabled != null) {
            if (pendingPatternManagementUploadEnabled == syncedPatternManagementUploadEnabled) {
                pendingPatternManagementUploadEnabled = null;
                patternManagementUploadEnabled = syncedPatternManagementUploadEnabled;
            } else {
                patternManagementUploadEnabled = pendingPatternManagementUploadEnabled;
            }
        } else {
            patternManagementUploadEnabled = syncedPatternManagementUploadEnabled;
        }
        if (previousPatternManagementDisplayMode != patternManagementDisplayMode
                || previousPatternManagementShowSlots != patternManagementShowSlots
                || previousPatternManagementSearchMode != patternManagementSearchMode) {
            rebuildPatternManagementRows();
            patternProviderSlotLayoutDirty = true;
        }
        if (magnetCardMenuButton != null) {
            var host = menu.getMenuHost();
            boolean hasMagnet = host != null && WcwtWirelessFeatures.hasMagnetCardInstalled(host.getItemStack());
            magnetCardMenuButton.visible = hasMagnet;
            magnetCardMenuButton.active = hasMagnet;
        }
        updateKeyTypeFilterButtons();
        syncPatternEncodingModeFromMenu();
        requestPatternProvidersIfNeeded();
        updateExtendedUIVisibility();
        updatePanelSlotActivity();
        updateManualWorkspaceUi();
        updatePatternEncodingModeButtons();
        updatePatternEncodingSlots();
        syncPatternManagementSearchFromEncodedPatternSlot();
        updatePatternCacheSlots();
        updatePatternManagement();
        updatePatternProviderSlots();
        if (DEBUG_REPO) {
            logRepoViewState("updateBeforeRender", -1);
        }
    }

    private void refreshRepoViewAfterTransientReconnect() {
        boolean connected = menu.getLinkStatus().connected();
        if (!observedLinkConnectionOnce) {
            observedLinkConnectionOnce = true;
            lastObservedLinkConnected = connected;
            if (DEBUG_REPO) {
                WcwtMod.LOGGER.info(
                        "WCWT repo debug: link init connected={} linkStatus={}",
                        connected,
                        menu.getLinkStatus());
            }
            return;
        }
        if (DEBUG_REPO && lastObservedLinkConnected != connected) {
            WcwtMod.LOGGER.info(
                    "WCWT repo debug: link transition prevConnected={} connected={} linkStatus={}",
                    lastObservedLinkConnected,
                    connected,
                    menu.getLinkStatus());
            logRepoViewState("linkTransitionBeforeRefresh", -1);
        }
        if (!lastObservedLinkConnected && connected) {
            repo.updateView();
            if (DEBUG_REPO) {
                logRepoViewState("transientReconnectRepoRefresh", -1);
            }
        }
        lastObservedLinkConnected = connected;
    }

    private void syncPatternManagementSettingsFromMenu() {
        patternManagementDisplayMode = patternManagementDisplayModeFromOrdinal(menu.getPatternManagementDisplayMode());
        patternManagementShowSlots = menu.isPatternManagementShowSlots();
        patternManagementUploadEnabled = menu.isPatternManagementUploadEnabled();
        patternManagementSearchMode = patternManagementSearchModeFromOrdinal(menu.getPatternManagementSearchMode());
        pendingPatternManagementDisplayMode = null;
        pendingPatternManagementShowSlots = null;
        pendingPatternManagementUploadEnabled = null;
        pendingPatternManagementSearchMode = null;
    }

    private void sendPatternManagementSettings() {
        pendingPatternManagementDisplayMode = patternManagementDisplayMode;
        pendingPatternManagementShowSlots = patternManagementShowSlots;
        pendingPatternManagementUploadEnabled = patternManagementUploadEnabled;
        pendingPatternManagementSearchMode = patternManagementSearchMode;
        PacketDistributor.sendToServer(new PatternManagementUploadSettingPacket(
                patternManagementUploadEnabled,
                patternManagementDisplayMode.ordinal(),
                patternManagementShowSlots,
                patternManagementSearchMode.ordinal()));
    }

    private static PatternManagementDisplayMode patternManagementDisplayModeFromOrdinal(int ordinal) {
        var values = PatternManagementDisplayMode.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PatternManagementDisplayMode.VISIBLE;
    }

    private static PatternManagementSearchMode patternManagementSearchModeFromOrdinal(int ordinal) {
        var values = PatternManagementSearchMode.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PatternManagementSearchMode.IN_OUT;
    }

    private enum KeyTypePreset {
        ITEMS,
        FLUIDS,
        OTHERS
    }

    private void selectKeyTypePreset(KeyTypePreset preset) {
        var synced = menu.getClientKeyTypeSelection();
        if (synced.keyTypes().isEmpty()) {
            return;
        }

        Set<AEKeyType> desired = switch (preset) {
            case ITEMS -> Set.of(AEKeyType.items());
            case FLUIDS -> Set.of(AEKeyType.fluids());
            case OTHERS -> {
                var others = new HashSet<AEKeyType>(synced.keyTypes().keySet());
                others.remove(AEKeyType.items());
                others.remove(AEKeyType.fluids());
                yield others;
            }
        };
        desired = desired.stream()
                .filter(synced.keyTypes()::containsKey)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        if (desired.isEmpty()) {
            desired = new HashSet<>(synced.keyTypes().keySet());
        }

        if (new HashSet<>(synced.enabledSet()).equals(desired)) {
            desired = new HashSet<>(synced.keyTypes().keySet());
        }

        for (var keyType : desired) {
            menu.selectKeyType(keyType, true);
        }
        for (var keyType : new ArrayList<>(synced.keyTypes().keySet())) {
            if (!desired.contains(keyType)) {
                menu.selectKeyType(keyType, false);
            }
        }

        updateKeyTypeFilterButtons();
        repo.updateView();
    }

    private void updateKeyTypeFilterButtons() {
        if (itemDisplayButton == null || fluidDisplayButton == null || otherTypesDisplayButton == null) {
            return;
        }

        var keyTypes = menu.getClientKeyTypeSelection().keyTypes();
        var enabled = new HashSet<>(menu.getClientKeyTypeSelection().enabledSet());
        boolean itemOnly = enabled.size() == 1 && enabled.contains(AEKeyType.items());
        boolean fluidOnly = enabled.size() == 1 && enabled.contains(AEKeyType.fluids());
        boolean otherOnly = !enabled.isEmpty()
                && !enabled.contains(AEKeyType.items())
                && !enabled.contains(AEKeyType.fluids())
                && keyTypes.keySet().stream()
                        .filter(type -> type != AEKeyType.items() && type != AEKeyType.fluids())
                        .allMatch(enabled::contains);
        itemDisplayButton.setChecked(itemOnly);
        fluidDisplayButton.setChecked(fluidOnly);
        otherTypesDisplayButton.setChecked(otherOnly);
    }

    private void requestPatternProvidersIfNeeded() {
        if (!requestedPatternProviders) {
            requestedPatternProviders = true;
            lastPatternProviderRefreshRequestMs = System.currentTimeMillis();
            lastPatternProviderSubscriptionRequestMs = lastPatternProviderRefreshRequestMs;
            requestPatternProviderList(true);
        }
    }

    private void recordFrameSyncTick() {
        long now = System.currentTimeMillis();
        frameSyncFrames++;
        if (frameSyncWindowStartMs == 0L) {
            frameSyncWindowStartMs = now;
            return;
        }
        if (now - frameSyncWindowStartMs < 1_000L) {
            return;
        }
        boolean toolkitPanelOpen = toolkitPanel != null && toolkitPanel.isVisible();
        boolean toolkitInManagementArea = isToolkitExpandedInManagementArea();
        WcwtMod.LOGGER.info(
                "WCWT frameSync(1s): frames={}, providerListRequests={}, providerSlotPacketSends={}, providerSlotRebuilds={}, repoUpdateViews={}, toolkitPanelOpen={}, toolkitInManagementArea={}, patternManagementActive={}, providers={}, mgmtRows={}",
                frameSyncFrames,
                frameSyncProviderListRequests,
                frameSyncProviderSlotPacketSends,
                frameSyncProviderSlotRebuilds,
                frameSyncRepoUpdateViews,
                toolkitPanelOpen,
                toolkitInManagementArea,
                isPatternManagementActive(),
                patternProviders.size(),
                patternManagementRows.size());
        frameSyncWindowStartMs = now;
        frameSyncFrames = 0;
        frameSyncProviderListRequests = 0;
        frameSyncProviderSlotPacketSends = 0;
        frameSyncProviderSlotRebuilds = 0;
        frameSyncRepoUpdateViews = 0;
    }

    private void refreshPatternProviders() {
        long now = System.currentTimeMillis();
        if (now - lastPatternProviderRefreshRequestMs < PATTERN_PROVIDER_REFRESH_DEBOUNCE_MS) {
            return;
        }
        requestedPatternProviders = true;
        lastPatternProviderRefreshRequestMs = now;
        lastPatternProviderSubscriptionRequestMs = now;
        requestPatternProviderList(true);
    }

    private void keepPatternProviderSubscriptionAlive() {
        if (!requestedPatternProviders || !isPatternManagementActive()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastPatternProviderSubscriptionRequestMs < PATTERN_PROVIDER_SUBSCRIPTION_KEEPALIVE_MS) {
            return;
        }

        lastPatternProviderSubscriptionRequestMs = now;
        requestPatternProviderList(true);
    }

    private void onPatternManagementSearchChanged() {
        resolvedPatternManagementSearchText = resolveClientProviderSearchText(currentPatternManagementSearchText());
        rebuildPatternManagementRows();
        patternProviderSlotLayoutDirty = true;
        refreshPatternProviders();
    }

    private void requestPatternProviderList(boolean subscribe) {
        if (DEBUG_FRAME_SYNC) {
            frameSyncProviderListRequests++;
        }
        PacketDistributor.sendToServer(new PatternProviderListPacket.Request(
                subscribe, resolvedPatternManagementSearchTextForRequest()));
    }

    private String currentPatternManagementSearchText() {
        return patternManageSearchField != null ? patternManageSearchField.getValue() : "";
    }

    private String resolvedPatternManagementSearchTextForRequest() {
        String resolved = resolveClientProviderSearchText(currentPatternManagementSearchText());
        resolvedPatternManagementSearchText = resolved;
        return resolved;
    }

    public void updatePatternProviders(List<PatternProviderListPacket.Entry> entries, String resolvedSearchText) {
        long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
        patternProviders.clear();
        patternProviders.addAll(entries);
        String incomingResolvedSearchText = resolvedSearchText == null ? "" : resolvedSearchText.trim();
        String currentSearchText = currentPatternManagementSearchText().trim();
        if (!incomingResolvedSearchText.isEmpty() && !currentSearchText.isEmpty()) {
            ExtendedAePlusUploadCompat.rememberAliasMapping(currentSearchText, incomingResolvedSearchText);
        }
        if (!incomingResolvedSearchText.isEmpty() || currentPatternManagementSearchText().trim().isEmpty()) {
            resolvedPatternManagementSearchText = incomingResolvedSearchText;
        }
        logPatternUploadDebug("client provider list received entries={}, searchField={}, resolvedSearchText={}, storedResolved={}",
                entries.size(), currentPatternManagementSearchText(), resolvedSearchText, resolvedPatternManagementSearchText);
        selectedPatternProviderId = patternProviders.stream().anyMatch(entry -> entry.providerId() == selectedPatternProviderId)
                ? selectedPatternProviderId : -1;
        selectedPatternProviderSlot = selectedPatternProviderId >= 0 ? selectedPatternProviderSlot : -1;
        focusedPatternProviderId = patternProviders.stream().anyMatch(entry -> entry.providerId() == focusedPatternProviderId)
                ? focusedPatternProviderId : -1;
        focusedPatternProviderSlot = focusedPatternProviderId >= 0 ? focusedPatternProviderSlot : -1;
        rebuildPatternManagementRows();
        forcePatternProviderSlotSnapshot = true;
        patternProviderSlotLayoutDirty = true;
        if (DEBUG_PERF) {
            long totalNs = System.nanoTime() - startNs;
            if (totalNs >= PERF_LOG_THRESHOLD_NS) {
                WcwtMod.LOGGER.info(
                        "WCWT perf: updatePatternProviders entries={}, totalMs={}, selectedProvider={}, focusedProvider={}",
                        entries.size(),
                        String.format(java.util.Locale.ROOT, "%.3f", totalNs / 1_000_000.0D),
                        selectedPatternProviderId,
                        focusedPatternProviderId);
            }
        }
    }

    public void focusPatternProviderSlot(long providerId, int slot) {
        focusedPatternProviderId = providerId;
        focusedPatternProviderSlot = slot;
        focusedPatternProviderUntilMs = System.currentTimeMillis() + 5000L;
        selectedPatternProviderId = providerId;
        selectedPatternProviderSlot = slot;
        scrollPatternManagementToProvider(providerId, slot);
    }

    /** 样板缓存区可见槽第一行左上角的 GUI Y（不含 {@link #topPos}）。 */
    private int patternCacheSlotsOriginY() {
        return imageHeight - PATTERN_CACHE_SLOT_BOTTOM + PATTERN_CACHE_SLOT_Y_OFFSET;
    }

    private void updatePatternCacheSlots() {
        var slots = getPatternCacheSlots();
        int totalRows = (slots.size() + PATTERN_CACHE_COLS - 1) / PATTERN_CACHE_COLS;
        int maxScroll = Math.max(0, totalRows - PATTERN_CACHE_VISIBLE_ROWS);
        if (patternCacheScrollbar != null) {
            patternCacheScrollbar.setRange(0, maxScroll, 1);
            patternCacheScrollbar.setVisible(maxScroll > 0);
        }

        int firstSlot = (patternCacheScrollbar != null ? patternCacheScrollbar.getCurrentScroll() : 0)
                * PATTERN_CACHE_COLS;
        int slotY = patternCacheSlotsOriginY();
        int visibleSlots = PATTERN_CACHE_COLS * PATTERN_CACHE_VISIBLE_ROWS;

        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            boolean visible = i >= firstSlot && i < firstSlot + visibleSlots;
            if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                appEngSlot.setActive(visible);
            }
            if (visible) {
                int visibleIndex = i - firstSlot;
                slot.x = PATTERN_CACHE_SLOT_X + (visibleIndex % PATTERN_CACHE_COLS) * 18;
                slot.y = slotY + (visibleIndex / PATTERN_CACHE_COLS) * 18;
            } else {
                slot.x = HIDDEN_SLOT_POS.getX();
                slot.y = HIDDEN_SLOT_POS.getY();
            }
        }
    }

    private void updatePatternEncodingSlots() {
        boolean processingVisible = patternEncodingMode == EncodingMode.PROCESSING;
        var processingInputSlots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_INPUTS);
        var processingOutputSlots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_OUTPUTS);
        int maxScroll = Math.max(0, processingInputSlots.size() / 3 - 3);
        if (patternEncodingScrollbar != null) {
            patternEncodingScrollbar.setRange(0, maxScroll, 3);
            patternEncodingScrollbar.setVisible(processingVisible && maxScroll > 0);
        }
        if (stonecuttingPatternScrollbar != null) {
            int rows = (menu.getStonecuttingRecipes().size() + STONECUTTING_RESULT_COLS - 1) / STONECUTTING_RESULT_COLS;
            stonecuttingPatternScrollbar.setRange(0, Math.max(0, rows - STONECUTTING_RESULT_ROWS), STONECUTTING_RESULT_ROWS);
            var inputBase = mainLayout.slot("PROCESSING_INPUTS",
                    new ExtendedPanelLayout.Rect(185, imageHeight - 213, 18, 18), imageWidth, imageHeight);
            stonecuttingPatternScrollbar.setPosition(new Point(
                    inputBase.left() + PATTERN_ENCODING_BG_X_OFFSET_FROM_INPUT + 109,
                    inputBase.top() + PATTERN_ENCODING_BG_Y_OFFSET_FROM_INPUT + 11));
            stonecuttingPatternScrollbar.setHeight(44);
            stonecuttingPatternScrollbar.setVisible(patternEncodingMode == EncodingMode.STONECUTTING
                    && rows > STONECUTTING_RESULT_ROWS);
        }

        int scroll = processingVisible && patternEncodingScrollbar != null
                ? patternEncodingScrollbar.getCurrentScroll()
                : 0;
        menu.updatePatternPreview(patternEncodingMode);
        updatePatternCraftingSlots(menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_CRAFTING_GRID),
                patternEncodingMode == EncodingMode.CRAFTING);
        updatePatternProcessingInputSlots(processingInputSlots, processingVisible, scroll);
        updatePatternEncodingOutputSlots(processingOutputSlots, processingVisible, scroll);
        updatePatternSmithingSlots(patternEncodingMode == EncodingMode.SMITHING_TABLE);
        updatePatternStonecuttingSlot(patternEncodingMode == EncodingMode.STONECUTTING);
        updatePatternEncodingPreviewSlot(menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PREVIEW), processingVisible);
        updateEncodedPatternSlot();
        if (encodePatternButton != null) {
            encodePatternButton.visible = true;
        }
        updatePatternEncodingOptionButtons();
        if (cycleProcessingOutputButton != null) {
            cycleProcessingOutputButton.visible = menu.canCycleProcessingOutputs();
            var bg = getPatternEncodingBackgroundOrigin();
            cycleProcessingOutputButton.setX(leftPos + bg.left() + 90);
            cycleProcessingOutputButton.setY(topPos + bg.top() + 6);
        }
    }

    private void updatePatternEncodingOptionButtons() {
        var bg = getPatternEncodingBackgroundOrigin();
        int buttonX = leftPos + bg.left();
        int buttonY = topPos + bg.top();

        boolean showClear = patternEncodingMode == EncodingMode.CRAFTING
                || patternEncodingMode == EncodingMode.PROCESSING
                || patternEncodingMode == EncodingMode.SMITHING_TABLE;
        if (clearPatternEncodingButton != null) {
            clearPatternEncodingButton.visible = showClear;
            clearPatternEncodingButton.setX(buttonX + switch (patternEncodingMode) {
                case CRAFTING -> 62;
                case PROCESSING -> 71;
                case SMITHING_TABLE -> 6;
                case STONECUTTING -> 0;
            });
            clearPatternEncodingButton.setY(buttonY + switch (patternEncodingMode) {
                case CRAFTING, PROCESSING -> 6;
                case SMITHING_TABLE -> 14;
                case STONECUTTING -> 0;
            });
        }

        if (processingMaterialsMergeButton != null) {
            boolean showMerge = patternEncodingMode == EncodingMode.PROCESSING;
            processingMaterialsMergeButton.visible = showMerge;
            processingMaterialsMergeButton.active = showMerge;
            processingMaterialsMergeButton.setMergeEnabled(menu.isProcessingMaterialsMerge());
            if (showMerge && clearPatternEncodingButton != null && clearPatternEncodingButton.visible) {
                processingMaterialsMergeButton.setX(clearPatternEncodingButton.getX() + 10);
                processingMaterialsMergeButton.setY(clearPatternEncodingButton.getY());
            }
        }

        boolean showItemSubstitutions = patternEncodingMode == EncodingMode.CRAFTING
                || patternEncodingMode == EncodingMode.SMITHING_TABLE;
        if (patternSubstitutionButton != null) {
            patternSubstitutionButton.visible = showItemSubstitutions;
            patternSubstitutionButton.active = showItemSubstitutions;
            patternSubstitutionButton.setState(menu.isPatternSubstitute());
            patternSubstitutionButton.setX(buttonX + (patternEncodingMode == EncodingMode.CRAFTING ? 72 : 16));
            patternSubstitutionButton.setY(buttonY + (patternEncodingMode == EncodingMode.CRAFTING ? 6 : 14));
        }

        boolean showFluidSubstitutions = patternEncodingMode == EncodingMode.CRAFTING;
        if (patternFluidSubstitutionButton != null) {
            patternFluidSubstitutionButton.visible = showFluidSubstitutions;
            patternFluidSubstitutionButton.active = showFluidSubstitutions;
            patternFluidSubstitutionButton.setState(menu.isPatternFluidSubstitute());
            patternFluidSubstitutionButton.setX(buttonX + 82);
            patternFluidSubstitutionButton.setY(buttonY + 6);
        }
    }

    private ExtendedPanelLayout.Rect getPatternEncodingBackgroundOrigin() {
        var inputBase = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(185, imageHeight - 213, 18, 18), imageWidth, imageHeight);
        return new ExtendedPanelLayout.Rect(
                inputBase.left() + PATTERN_ENCODING_BG_X_OFFSET_FROM_INPUT,
                inputBase.top() + PATTERN_ENCODING_BG_Y_OFFSET_FROM_INPUT,
                PATTERN_ENCODING_BG_WIDTH,
                PATTERN_ENCODING_BG_HEIGHT);
    }

    private void updateEncodedPatternSlot() {
        var encodedPatternRect = mainLayout.slot("ENCODED_PATTERN",
                new ExtendedPanelLayout.Rect(308, imageHeight - 197, 16, 16), imageWidth, imageHeight);
        for (Slot slot : menu.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setActive(true);
            }
            slot.x = encodedPatternRect.left();
            slot.y = encodedPatternRect.top();
        }
    }

    private void syncPatternManagementSearchFromEncodedPatternSlot() {
        Slot encodedPatternSlot = getEncodedPatternSlot();
        ItemStack encodedPattern = encodedPatternSlot != null ? encodedPatternSlot.getItem() : ItemStack.EMPTY;
        String uploadSearchText = PatternUploadMetadata.getProviderSearchText(encodedPattern);
        boolean changed = !ItemStack.isSameItemSameComponents(lastEncodedPatternForUploadSync, encodedPattern)
                || !Objects.equals(lastEncodedPatternUploadSearchText, uploadSearchText);
        if (!changed) {
            return;
        }

        lastEncodedPatternForUploadSync = encodedPattern.isEmpty() ? ItemStack.EMPTY : encodedPattern.copy();
        lastEncodedPatternUploadSearchText = uploadSearchText;
        if (uploadSearchText == null || uploadSearchText.isBlank() || patternManageSearchField == null) {
            return;
        }

        String currentSearch = patternManageSearchField.getValue();
        if (!uploadSearchText.equals(currentSearch)) {
            patternManageSearchField.setValue(uploadSearchText);
            patternProviderSlotLayoutDirty = true;
        }
        syncSelectedPatternProviderFromSearch(uploadSearchText);
    }

    @Nullable
    private Slot getEncodedPatternSlot() {
        for (Slot slot : menu.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            return slot;
        }
        return null;
    }

    private void syncSelectedPatternProviderFromSearch(String uploadSearchText) {
        String normalizedQuery = uploadSearchText.trim();
        if (normalizedQuery.isEmpty()) {
            return;
        }
        var matchingEntries = findPatternProvidersBySearch(normalizedQuery);
        if (matchingEntries.isEmpty()) {
            String resolved = resolveEaepProviderSearchKey(normalizedQuery);
            if (resolved != null && !resolved.equals(normalizedQuery)) {
                matchingEntries = findPatternProvidersBySearch(resolved);
            }
        }
        var distinctNames = matchingEntries.stream()
                .map(this::providerDisplayText)
                .distinct()
                .toList();
        if (distinctNames.size() != 1 || matchingEntries.isEmpty()) {
            return;
        }
        long providerId = matchingEntries.get(0).providerId();
        if (selectedPatternProviderId != providerId) {
            selectedPatternProviderId = providerId;
            selectedPatternProviderSlot = -1;
        }
    }

    private long preferredPatternProviderIdForUpload(@Nullable String searchKey) {
        long inferred = inferPatternProviderIdFromSearch(searchKey);
        if (inferred > 0) {
            logPatternUploadDebug("client inferred provider from search searchKey={}, providerId={}", searchKey, inferred);
            selectedPatternProviderId = inferred;
            selectedPatternProviderSlot = -1;
            return inferred;
        }

        if (containsPatternProvider(selectedPatternProviderId)) {
            logPatternUploadDebug("client using selected provider providerId={}, searchKey={}", selectedPatternProviderId, searchKey);
            return selectedPatternProviderId;
        }

        if (containsPatternProvider(focusedPatternProviderId)) {
            logPatternUploadDebug("client using focused provider providerId={}, searchKey={}", focusedPatternProviderId, searchKey);
            return focusedPatternProviderId;
        }

        logPatternUploadDebug("client no preferred provider searchKey={}", searchKey);
        return -1L;
    }

    private String uploadProviderNameForStatus(long providerId, @Nullable String searchKey) {
        if (providerId <= 0) {
            return "";
        }
        String resolvedSearch = searchKey == null ? "" : resolveEaepProviderSearchKey(searchKey);
        if (resolvedSearch != null && !resolvedSearch.isBlank()) {
            var matchingEntries = findPatternProvidersBySearch(resolvedSearch.trim());
            var distinctIds = matchingEntries.stream()
                    .map(PatternProviderListPacket.Entry::providerId)
                    .distinct()
                    .toList();
            var distinctNames = matchingEntries.stream()
                    .map(this::providerDisplayText)
                    .distinct()
                    .toList();
            if (distinctIds.size() == 1 && distinctIds.get(0) == providerId && distinctNames.size() == 1) {
                return resolvedSearch.trim();
            }
        }
        return patternProviders.stream()
                .filter(entry -> entry.providerId() == providerId)
                .map(this::providerDisplayText)
                .findFirst()
                .orElse("");
    }

    private long inferPatternProviderIdFromSearch(@Nullable String searchKey) {
        String[] candidates = {
                searchKey,
                searchKey == null ? null : resolveEaepProviderSearchKey(searchKey),
                currentPatternManagementSearchText(),
                resolvedPatternManagementSearchText
        };
        for (String candidate : candidates) {
            String normalized = candidate == null ? "" : candidate.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            var matchingEntries = findPatternProvidersBySearch(normalized);
            var distinctNames = matchingEntries.stream()
                    .map(this::providerDisplayText)
                    .distinct()
                    .toList();
            if (distinctNames.size() == 1 && !matchingEntries.isEmpty()) {
                return matchingEntries.get(0).providerId();
            }
        }
        return -1L;
    }

    private List<PatternProviderListPacket.Entry> findPatternProvidersBySearch(String query) {
        return patternProviders.stream()
                .filter(entry -> JecSearchCompat.contains(providerDisplayText(entry), query))
                .toList();
    }

    private static void logPatternUploadDebug(String message, Object... args) {
        if (DEBUG_PATTERN_UPLOAD) {
            WcwtMod.LOGGER.info("WCWT pattern upload debug: " + message, args);
        }
    }

    private void updatePatternCraftingSlots(List<Slot> slots, boolean visible) {
        var base = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(192, imageHeight - 216, 18, 18), imageWidth, imageHeight);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            setPatternEncodingSlotActive(slot, visible);
            if (visible) {
                slot.x = base.left() - 9 + (i % 3) * 18;
                slot.y = base.top() + (i / 3) * 18;
            } else {
                hidePatternEncodingSlot(slot);
            }
        }
    }

    private void updatePatternProcessingInputSlots(List<Slot> slots, boolean visible, int scroll) {
        var base = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(192, imageHeight - 216, 18, 18), imageWidth, imageHeight);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int effectiveRow = (i / 3) - scroll;
            boolean show = visible && effectiveRow >= 0 && effectiveRow < 3;
            setPatternEncodingSlotActive(slot, show);
            if (show) {
                slot.x = base.left() + (i % 3) * 18;
                slot.y = base.top() + effectiveRow * 18;
            } else {
                hidePatternEncodingSlot(slot);
            }
        }
    }

    private void updatePatternEncodingOutputSlots(List<Slot> slots, boolean visible, int scroll) {
        var base = mainLayout.slot("PROCESSING_OUTPUTS",
                new ExtendedPanelLayout.Rect(270, imageHeight - 216, 18, 18), imageWidth, imageHeight);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int effectiveRow = i - scroll;
            boolean slotVisible = visible && effectiveRow >= 0 && effectiveRow < 3;
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setActive(slotVisible);
            }
            if (slotVisible) {
                slot.x = base.left();
                slot.y = base.top() + effectiveRow * 18;
            } else {
                hidePatternEncodingSlot(slot);
            }
        }
    }

    private void updatePatternSmithingSlots(boolean visible) {
        var slots = List.of(
                menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_SMITHING_TEMPLATE),
                menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_SMITHING_BASE),
                menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_SMITHING_ADDITION));
        var base = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(192, imageHeight - 216, 18, 18), imageWidth, imageHeight);
        int[] xOffsets = { -9, 9, 27 };
        int y = base.top() + 18;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                continue;
            }
            var slot = slots.get(i).get(0);
            setPatternEncodingSlotActive(slot, visible);
            if (visible) {
                slot.x = base.left() + xOffsets[i];
                slot.y = y;
            } else {
                hidePatternEncodingSlot(slot);
            }
        }
    }

    private void updatePatternStonecuttingSlot(boolean visible) {
        var slots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_STONECUTTING_INPUT);
        if (slots.isEmpty()) {
            return;
        }
        var base = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(192, imageHeight - 216, 18, 18), imageWidth, imageHeight);
        var slot = slots.get(0);
        setPatternEncodingSlotActive(slot, visible);
        if (visible) {
            slot.x = base.left() - 9;
            slot.y = base.top() + 18;
        } else {
            hidePatternEncodingSlot(slot);
        }
    }

    private void updatePatternEncodingPreviewSlot(List<Slot> slots, boolean processingVisible) {
        if (slots.isEmpty()) {
            return;
        }

        var slot = slots.get(0);
        boolean visible = !processingVisible && patternEncodingMode != EncodingMode.STONECUTTING;
        if (slot instanceof AppEngSlot appEngSlot) {
            appEngSlot.setActive(visible);
        }
        if (visible) {
            var inputBase = mainLayout.slot("PROCESSING_INPUTS",
                    new ExtendedPanelLayout.Rect(192, imageHeight - 216, 18, 18), imageWidth, imageHeight);
            var outputBase = mainLayout.slot("PROCESSING_OUTPUTS",
                    new ExtendedPanelLayout.Rect(277, imageHeight - 216, 18, 18), imageWidth, imageHeight);
            if (patternEncodingMode == EncodingMode.CRAFTING) {
                var craftingResult = mainLayout.widget("pattern_crafting_result_slot",
                        new ExtendedPanelLayout.Rect(275, imageHeight - 195, 18, 18), imageWidth, imageHeight);
                slot.x = craftingResult.left();
                slot.y = craftingResult.top();
            } else {
                slot.x = outputBase.left();
                slot.y = inputBase.top() + 18;
            }
        } else {
            hidePatternEncodingSlot(slot);
        }
    }

    private void setPatternEncodingSlotActive(Slot slot, boolean active) {
        setSlotActive(slot, active);
    }

    private void hidePatternEncodingSlot(Slot slot) {
        hideSlot(slot);
    }

    private void setSlotActive(Slot slot, boolean active) {
        if (slot instanceof AppEngSlot appEngSlot) {
            appEngSlot.setActive(active);
        } else if (slot instanceof WcwtActivatableSlot activatableSlot) {
            activatableSlot.setWcwtActive(active);
        }
    }

    private void hideSlot(Slot slot) {
        setSlotActive(slot, false);
        slot.x = HIDDEN_SLOT_POS.getX();
        slot.y = HIDDEN_SLOT_POS.getY();
    }

    private void showSlot(Slot slot, int x, int y) {
        setSlotActive(slot, true);
        slot.x = x;
        slot.y = y;
    }

    @SuppressWarnings("unchecked")
    private @Nullable AbstractWidget resolveWidgetById(String id) {
        try {
            Field field = appeng.client.gui.WidgetContainer.class.getDeclaredField("widgets");
            field.setAccessible(true);
            Object value = field.get(widgets);
            if (value instanceof Map<?, ?> map) {
                Object widget = map.get(id);
                if (widget instanceof AbstractWidget abstractWidget) {
                    return abstractWidget;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private void setSemanticSlotsHidden(appeng.menu.SlotSemantic semantic, boolean hidden) {
        setSlotsHidden(semantic, hidden);
        for (var slot : menu.getSlots(semantic)) {
            setSlotActive(slot, !hidden);
            if (hidden) {
                slot.x = HIDDEN_SLOT_POS.getX();
                slot.y = HIDDEN_SLOT_POS.getY();
            }
        }
    }

    private void rebuildPatternManagementRows() {
        long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
        patternManagementRows.clear();
        patternManagementSearchHighlightSlots.clear();
        String rawFilter = patternManageSearchField != null ? patternManageSearchField.getValue().trim() : "";
        String resolvedFilter = resolvedPatternManagementSearchText.trim();
        String filter = (!resolvedFilter.isEmpty() ? resolvedFilter : rawFilter).toLowerCase(Locale.ROOT);
        Map<PatternContainerGroup, List<PatternProviderListPacket.Entry>> providersByGroup = new LinkedHashMap<>();
        patternProviders.stream()
                .filter(this::isPatternProviderVisibleInManagement)
                .filter(entry -> filter.isEmpty() || patternProviderMatches(entry, filter))
                .sorted(Comparator.comparing(entry -> providerDisplayText(entry).toLowerCase(Locale.ROOT)))
                .forEach(entry -> providersByGroup
                        .computeIfAbsent(entry.group(), ignored -> new ArrayList<>())
                        .add(entry));
        providersByGroup.forEach((group, entries) -> {
            patternManagementRows.add(new PatternManagementHeaderRow(group, entries));
            if (patternManagementShowSlots) {
                for (var entry : entries) {
                    for (int offset = 0; offset < entry.inventorySize(); offset += PATTERN_MANAGEMENT_COLS) {
                        patternManagementRows.add(new PatternManagementSlotsRow(entry, offset,
                                Math.min(PATTERN_MANAGEMENT_COLS, entry.inventorySize() - offset)));
                    }
                }
            }
        });
        if (DEBUG_PERF) {
            long totalNs = System.nanoTime() - startNs;
            if (totalNs >= PERF_LOG_THRESHOLD_NS || !filter.isEmpty()) {
                WcwtMod.LOGGER.info(
                        "WCWT perf: rebuildPatternManagementRows totalMs={}, providers={}, rows={}, filterEmpty={}, filterLen={}, showSlots={}, displayMode={}, searchMode={}",
                        String.format(java.util.Locale.ROOT, "%.3f", totalNs / 1_000_000.0D),
                        patternProviders.size(),
                        patternManagementRows.size(),
                        filter.isEmpty(),
                        filter.length(),
                        patternManagementShowSlots,
                        patternManagementDisplayMode,
                        patternManagementSearchMode);
            }
        }
    }

    private boolean isPatternProviderVisibleInManagement(PatternProviderListPacket.Entry entry) {
        return switch (patternManagementDisplayMode) {
            case ALL, VISIBLE -> true;
            case NOT_FULL -> entry.slots().size() < entry.inventorySize();
        };
    }

    private boolean isExtendedUiCardInstalledUncached(IExtendedUIHost.ExtendedUIType type) {
        var host = menu.getMenuHost();
        if (host == null) {
            return false;
        }
        var card = switch (type) {
            case ADVANCED_CODING -> com.lhy.wcwt.init.ModItems.ADVANCED_CODING_CARD.get();
            case COSMETIC_ARMOR -> com.lhy.wcwt.init.ModItems.COSMETIC_ARMOR_CARD.get();
            case CURIOS -> com.lhy.wcwt.init.ModItems.CURIOS_CARD.get();
            case TOOL_SLOTS_BOX -> com.lhy.wcwt.init.ModItems.TOOL_SLOTS_BOX_CARD.get();
            case TOOLKIT -> com.lhy.wcwt.init.ModItems.TOOLKIT_CARD.get();
            case RESONATING_LIGHTNING_PATTERN_CODING ->
                    com.lhy.wcwt.init.ModItems.RESONATING_LIGHTNING_PATTERN_CODING_CARD.get();
            case NONE -> null;
        };
        if (card == null) {
            return false;
        }
        return host.getUpgrades().isInstalled(card);
    }

    private String providerDisplayText(PatternProviderListPacket.Entry entry) {
        String mappedName = entry.mappedDisplayName();
        if (mappedName != null && !mappedName.isBlank()) {
            return mappedName;
        }
        return entry.group().name().getString();
    }

    private boolean patternProviderMatches(PatternProviderListPacket.Entry entry, String filter) {
        if (JecSearchCompat.contains(providerDisplayText(entry), filter)) {
            return true;
        }
        boolean matched = false;
        for (var slotEntry : entry.slots().entrySet()) {
            if (patternStackMatches(slotEntry.getValue(), filter)) {
                patternManagementSearchHighlightSlots.add(new PatternManagementSlotKey(entry.providerId(), slotEntry.getKey()));
                matched = true;
            }
        }
        return matched;
    }

    private boolean patternStackMatches(ItemStack stack, String filter) {
        var level = Minecraft.getInstance().level;
        var details = level != null ? appeng.api.crafting.PatternDetailsHelper.decodePattern(stack, level) : null;
        if (details == null) {
            return JecSearchCompat.contains(stack.getHoverName().getString(), filter);
        }
        if (patternManagementSearchMode.searchOutputs()) {
            for (var output : details.getOutputs()) {
                if (JecSearchCompat.contains(output.what().getDisplayName().getString(), filter)) {
                    return true;
                }
            }
        }
        if (patternManagementSearchMode.searchInputs()) {
            for (var input : details.getInputs()) {
                for (var candidate : input.getPossibleInputs()) {
                    if (JecSearchCompat.contains(candidate.what().getDisplayName().getString(), filter)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updatePatternManagement() {
        refreshPatternManagementLayout();
        boolean toolkitInManagementArea = isToolkitExpandedInManagementArea();
        keepPatternProviderSubscriptionAlive();
        int visibleRows = toolkitInManagementArea ? getManagementToolkitVisibleRows()
                : Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int maxScroll;
        if (toolkitInManagementArea) {
            int columns = getManagementToolkitColumns();
            int totalRows = (getToolkitSlots().size() + columns - 1) / columns;
            maxScroll = Math.max(0, totalRows - visibleRows);
        } else {
            maxScroll = Math.max(0, patternManagementRows.size() - visibleRows);
        }
        if (patternManagementScrollbar != null) {
            patternManagementScrollbar.setHeight(toolkitInManagementArea
                    ? managementToolkitScrollbarRect.height()
                    : patternManagementPage.height());
            patternManagementScrollbar.setRange(0, maxScroll, 1);
            patternManagementScrollbar.setVisible(maxScroll > 0);
            var activeScrollbarRect = toolkitInManagementArea ? managementToolkitScrollbarRect : patternManagementScrollbarRect;
            patternManagementScrollbar.setPosition(new Point(
                    activeScrollbarRect.left(),
                    activeScrollbarRect.top()));
        }
    }

    private static void prewarmKeyTypeSelectionScreenHeight() {
        try {
            var keyTypeSelectionStyle = StyleManager.loadStyleDoc("/screens/key_type_selection.json");
            var generatedBackground = keyTypeSelectionStyle.getGeneratedBackground();
            if (generatedBackground != null) {
                generatedBackground.setHeight(20 + appeng.api.stacks.AEKeyTypes.getAll().size() * 20 + 6);
            }
        } catch (RuntimeException ignored) {
            // AE2 will fall back to its own lazy sizing if this style is unavailable during early resource reload.
        }
    }

    private void updatePatternProviderSlots() {
        var slots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PROVIDER);
        if (slots.isEmpty()) {
            return;
        }

        List<PatternProviderSlotSyncPacket.Mapping> mappings = new ArrayList<>(slots.size());
        boolean toolkitInManagementArea = isToolkitExpandedInManagementArea();
        int scroll = patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0;
        boolean stateChanged = patternProviderSlotLayoutDirty
                || lastPatternProviderSlotsInToolkitArea != toolkitInManagementArea
                || lastPatternProviderSlotScroll != scroll
                || lastPatternProviderRowCount != patternManagementRows.size()
                || forcePatternProviderSlotSnapshot;
        if (!stateChanged) {
            return;
        }
        if (DEBUG_FRAME_SYNC) {
            frameSyncProviderSlotRebuilds++;
        }
        if (toolkitInManagementArea) {
            updatePatternProviderSlotsHidden(slots);
            syncPatternProviderSlotMappings(mappings);
            forcePatternProviderSlotSnapshot = false;
            patternProviderSlotLayoutDirty = false;
            lastPatternProviderSlotsInToolkitArea = true;
            lastPatternProviderSlotScroll = scroll;
            lastPatternProviderRowCount = patternManagementRows.size();
            return;
        }

        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int visibleSlotIndex = 0;
        int rowStart = patternManagementPage.top();

        for (int rowOffset = 0; rowOffset < visibleRows; rowOffset++) {
            int rowIndex = scroll + rowOffset;
            if (rowIndex < 0 || rowIndex >= patternManagementRows.size()) {
                break;
            }
            int rowY = rowStart + rowOffset * PATTERN_MANAGEMENT_ROW_H;
            var row = patternManagementRows.get(rowIndex);
            if (!(row instanceof PatternManagementSlotsRow slotsRow)) {
                continue;
            }
            for (int col = 0; col < slotsRow.slots(); col++) {
                if (visibleSlotIndex >= slots.size()) {
                    break;
                }
                int mappingIndex = visibleSlotIndex;
                int slotX = patternManagementPage.left() + col * PATTERN_MANAGEMENT_SLOT_STEP;
                int slotY = rowY + PATTERN_MANAGEMENT_SLOT_Y_OFFSET;
                int providerSlot = slotsRow.offset() + col;
                PatternProviderListPacket.Entry entry = slotsRow.entry();
                var stack = entry.slots().getOrDefault(providerSlot, ItemStack.EMPTY);
                var slot = slots.get(visibleSlotIndex++);
                if (slot instanceof WirelessComprehensiveWorkTerminalMenu.PatternProviderSlot providerSlotSlot) {
                    providerSlotSlot.setMapping(entry.providerId(), providerSlot);
                    if (forcePatternProviderSlotSnapshot) {
                        providerSlotSlot.forceClientDisplayStack(stack);
                    } else {
                        providerSlotSlot.setClientDisplayStack(stack);
                    }
                }
                showSlot(slot, slotX, slotY);
                mappings.add(new PatternProviderSlotSyncPacket.Mapping(mappingIndex, entry.providerId(), providerSlot));
            }
        }

        for (int i = visibleSlotIndex; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot instanceof WirelessComprehensiveWorkTerminalMenu.PatternProviderSlot providerSlot) {
                providerSlot.clearMapping();
            }
            hideSlot(slot);
        }
        syncPatternProviderSlotMappings(mappings);
        forcePatternProviderSlotSnapshot = false;
        patternProviderSlotLayoutDirty = false;
        lastPatternProviderSlotsInToolkitArea = false;
        lastPatternProviderSlotScroll = scroll;
        lastPatternProviderRowCount = patternManagementRows.size();
    }

    private void updatePatternProviderSlotsHidden(List<Slot> slots) {
        for (Slot slot : slots) {
            if (slot instanceof WirelessComprehensiveWorkTerminalMenu.PatternProviderSlot providerSlot) {
                providerSlot.clearMapping();
            }
            hideSlot(slot);
        }
    }

    private void syncPatternProviderSlotMappings(List<PatternProviderSlotSyncPacket.Mapping> mappings) {
        if (mappings.equals(lastPatternProviderSlotSync)) {
            return;
        }
        if (DEBUG_FRAME_SYNC) {
            frameSyncProviderSlotPacketSends++;
        }
        lastPatternProviderSlotSync = List.copyOf(mappings);
        PacketDistributor.sendToServer(new PatternProviderSlotSyncPacket(mappings));
    }

    private boolean isPatternManagementActive() {
        if (isToolkitExpandedInManagementArea()) {
            return true;
        }
        return patternManagementUploadEnabled
                || (patternManageSearchField != null && patternManageSearchField.isFocused())
                || selectedPatternProviderId >= 0
                || !patternProviders.isEmpty();
    }

    private void refreshPatternManagementLayout() {
        patternManagementPage = mainLayout.widget("management_page", patternManagementPage, imageWidth, imageHeight);
        patternManagementScrollbarRect = mainLayout.widget("manage_scrollbar", patternManagementScrollbarRect,
                imageWidth, imageHeight);
        pinnedRowOverlayRect = mainLayout.widget("pinned_row_overlay", pinnedRowOverlayRect, imageWidth, imageHeight);
        managementToolkitBackgroundRect = managementToolkitLayout.widget("management_toolkit_background",
                managementToolkitBackgroundRect, imageWidth, imageHeight);
        managementToolkitSlotRect = managementToolkitLayout.slot("management_toolkit_slots", managementToolkitSlotRect,
                imageWidth, imageHeight);
        managementToolkitScrollbarRect = managementToolkitLayout.widget("management_toolkit_scrollbar",
                managementToolkitScrollbarRect, imageWidth, imageHeight);
        patternManagementAddButton = mainLayout.widget("increase_mapping", patternManagementAddButton, imageWidth, imageHeight);
        patternManagementReloadButton = mainLayout.widget("heavy_load_mapping", patternManagementReloadButton, imageWidth, imageHeight);
        patternManagementDeleteButton = mainLayout.widget("delete_mapping", patternManagementDeleteButton, imageWidth, imageHeight);
        patternManagementCancelButton = mainLayout.widget("manage_cancel", patternManagementCancelButton, imageWidth, imageHeight);
        patternManagementUploadButton = mainLayout.widget("manage_upload_pattern", patternManagementUploadButton, imageWidth, imageHeight);
        patternManagementUiButton = mainLayout.widget("manage_open_ui", patternManagementUiButton, imageWidth, imageHeight);
        patternManagementHighlightButton = mainLayout.widget("manage_highlight_provider", patternManagementHighlightButton, imageWidth, imageHeight);
        patternManagementDisplayModeButton = mainLayout.widget("manage_display_mode", patternManagementDisplayModeButton, imageWidth, imageHeight);
        patternManagementDisplaySlotsButton = mainLayout.widget("manage_displays_slots", patternManagementDisplaySlotsButton, imageWidth, imageHeight);
        patternManagementAutoUploadButton = mainLayout.widget("manage_output_mode", patternManagementAutoUploadButton, imageWidth, imageHeight);
        patternManagementSearchModeButton = mainLayout.widget("automatic_upload", patternManagementSearchModeButton, imageWidth, imageHeight);
        batchItemReplacementButton = mainLayout.widget("pattern_Replace1", batchItemReplacementButton, imageWidth, imageHeight);
        batchFluidReplacementButton = mainLayout.widget("pattern_Replace2", batchFluidReplacementButton, imageWidth, imageHeight);
    }

    /**
     * 面板关闭时 setSlotsHidden(true) → 槽位移到 (-9999,-9999)，vanilla 不渲染也不交互；
     * 面板打开时 setSlotsHidden(false) → 恢复 JSON 坐标，由 AdvancedCodingPanel 负责手绘渲染。
     */
    private void updatePanelSlotActivity() {
        boolean panelOpen = advancedCodingPanel != null && advancedCodingPanel.isVisible();
        boolean rlpcOpen = resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible();
        boolean cosmeticOpen = cosmeticArmorPanel != null && cosmeticArmorPanel.isVisible();
        boolean curiosOpen = curiosPanel != null && curiosPanel.isVisible();
        boolean toolkitOpen = toolkitPanel != null && toolkitPanel.isVisible();
        boolean toolkitInManagementArea = isToolkitExpandedInManagementArea();
        boolean toolboxOpen = toolboxPanel != null && toolboxPanel.isVisible();
        int curiosScroll = curiosScrollbar != null ? curiosScrollbar.getCurrentScroll() : 0;
        int toolkitScroll = toolkitScrollbar != null ? toolkitScrollbar.getCurrentScroll() : 0;
        int managementToolkitScroll = patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0;
        boolean showCellUpgrades = panelOpen && hasVisibleCellUpgradeSlot();

        boolean stateChanged = panelSlotActivityDirty
                || lastAdvancedPanelOpen != panelOpen
                || lastResonatingPanelOpen != rlpcOpen
                || lastCosmeticPanelOpen != cosmeticOpen
                || lastCuriosPanelOpen != curiosOpen
                || lastToolkitPanelOpen != toolkitOpen
                || lastToolkitInManagementArea != toolkitInManagementArea
                || lastToolboxPanelOpen != toolboxOpen
                || lastCellUpgradesPanelVisible != showCellUpgrades
                || (curiosOpen && lastCuriosSlotScroll != curiosScroll)
                || ((toolkitOpen || toolkitInManagementArea)
                        && lastToolkitSlotScroll != toolkitScroll)
                || (toolkitInManagementArea && lastManagementToolkitSlotScroll != managementToolkitScroll);
        if (!stateChanged) {
            return;
        }

        if (lastAdvancedPanelOpen != panelOpen || panelSlotActivityDirty) {
            for (var semantic : PANEL_SLOT_SEMANTICS) {
                setSemanticSlotsHidden(semantic, !panelOpen);
            }
        }
        if (lastResonatingPanelOpen != rlpcOpen || panelSlotActivityDirty) {
            for (var semantic : RLPC_PANEL_SLOT_SEMANTICS) {
                setSemanticSlotsHidden(semantic, !rlpcOpen);
            }
        }

        if (lastCosmeticPanelOpen != cosmeticOpen || panelSlotActivityDirty) {
            for (var semantic : COSMETIC_ARMOR_SLOT_SEMANTICS) {
                setSemanticSlotsHidden(semantic, !cosmeticOpen);
            }
        }
        if (cosmeticOpen) {
            positionCosmeticArmorSlots();
        }

        if (lastCuriosPanelOpen != curiosOpen || panelSlotActivityDirty || (curiosOpen && lastCuriosSlotScroll != curiosScroll)) {
            setSemanticSlotsHidden(WcwtSlotSemantics.AE_CURIOS, !curiosOpen);
            if (curiosScrollbar != null) {
                curiosScrollbar.setVisible(curiosOpen);
            }
            updateCurioSlots();
        }

        if (lastToolkitPanelOpen != toolkitOpen
                || lastToolkitInManagementArea != toolkitInManagementArea
                || panelSlotActivityDirty
                || ((toolkitOpen || toolkitInManagementArea)
                        && (lastToolkitSlotScroll != toolkitScroll
                        || lastManagementToolkitSlotScroll != managementToolkitScroll))) {
            setSemanticSlotsHidden(WcwtSlotSemantics.WCWT_TOOLKIT, !(toolkitOpen || toolkitInManagementArea));
            if (toolkitScrollbar != null) {
                toolkitScrollbar.setVisible(toolkitOpen);
            }
            updateToolkitSlots();
            patternProviderSlotLayoutDirty = true;
        }

        if (lastToolboxPanelOpen != toolboxOpen || panelSlotActivityDirty) {
            if (toolboxOpen) {
                positionToolboxSlots();
            } else {
                hideToolboxSlots();
            }
        }

        if (lastResonatingPanelOpen != rlpcOpen || panelSlotActivityDirty) {
            updateResonatingStorageSlots();
        }

        // 元件升级卡槽：面板关闭→隐藏；面板打开→根据当前面板位置实时定位。
        // 元件没放入时 OptionalRestrictedInputSlot.isSlotEnabled() 自动返回 false，
        // vanilla 不渲染、不接点击，所以不需要再额外 hide。
        if (cellUpgradesPanel != null) {
            cellUpgradesPanel.setVisible(showCellUpgrades);
            if (showCellUpgrades && upgradesPanel != null && advancedCodingPanel != null) {
                int panelHeight = cellUpgradesPanel.getBounds().getHeight();
                // 仍然使用右侧 WTLib 升级槽列的 X；Y 改成底部贴住高级编码面板顶部。
                cellUpgradesPanel.setPosition(new Point(
                        upgradesPanel.getBounds().getX(),
                        advancedCodingPanel.getY() - topPos - panelHeight + 3));
            }
        } else if (!panelOpen) {
            setSemanticSlotsHidden(com.lhy.wcwt.menu.WcwtSlotSemantics.WCWT_CELL_UPGRADE, true);
        }
        lastAdvancedPanelOpen = panelOpen;
        lastResonatingPanelOpen = rlpcOpen;
        lastCosmeticPanelOpen = cosmeticOpen;
        lastCuriosPanelOpen = curiosOpen;
        lastToolkitPanelOpen = toolkitOpen;
        lastToolkitInManagementArea = toolkitInManagementArea;
        lastToolboxPanelOpen = toolboxOpen;
        lastCellUpgradesPanelVisible = showCellUpgrades;
        lastCuriosSlotScroll = curiosScroll;
        lastToolkitSlotScroll = toolkitScroll;
        lastManagementToolkitSlotScroll = managementToolkitScroll;
        panelSlotActivityDirty = false;
    }

    private void updateResonatingStorageSlots() {
        var slots = menu.getSlots(WcwtSlotSemantics.WCWT_RESONATING_STORAGE);
        boolean open = resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible();
        if (!open || resonatingLightningPatternCodingPanel == null) {
            for (var slot : slots) {
                hideSlot(slot);
            }
            return;
        }

        var bounds = resonatingLightningPatternCodingPanel.getBounds();
        int columns = resonatingLightningPatternCodingPanel.getResonatingColumns();
        int slotSpacingX = resonatingLightningPatternCodingPanel.getResonatingSlotSpacingX();
        int slotSpacingY = resonatingLightningPatternCodingPanel.getResonatingSlotSpacingY();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int x = bounds.getX() - leftPos
                    + resonatingLightningPatternCodingPanel.getResonatingSlotAnchorX()
                    + (i % columns) * slotSpacingX;
            int y = bounds.getY() - topPos
                    + resonatingLightningPatternCodingPanel.getResonatingSlotAnchorY()
                    + (i / columns) * slotSpacingY;
            showSlot(slot, x, y);
        }
    }

    private void hideToolboxSlots() {
        for (var slot : menu.getSlots(SlotSemantics.TOOLBOX)) {
            hideSlot(slot);
        }
    }

    private void positionToolboxSlots() {
        if (toolboxPanel == null) {
            return;
        }
        var bounds = toolboxPanel.getBounds();
        var slots = menu.getSlots(SlotSemantics.TOOLBOX);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int x = bounds.getX() - leftPos + toolboxPanel.getSlotRelativeX() + (i % 3) * 18;
            int y = bounds.getY() - topPos + toolboxPanel.getSlotRelativeY() + (i / 3) * 18;
            showSlot(slot, x, y);
        }
    }

    private void updateCurioSlots() {
        var slots = getCurioSlots();
        boolean curiosOpen = curiosPanel != null && curiosPanel.isVisible();
        if (!curiosOpen || curiosPanel == null) {
            for (var slot : slots) {
                hideSlot(slot);
            }
            return;
        }

        int columns = curiosPanel.getColumns();
        int visibleSlots = columns * CuriosPanel.VISIBLE_ROWS;
        int totalRows = (slots.size() + columns - 1) / columns;
        int maxScroll = Math.max(0, totalRows - CuriosPanel.VISIBLE_ROWS);
        if (curiosScrollbar != null) {
            curiosScrollbar.setHeight(curiosPanel.getScrollbarHeight());
            curiosScrollbar.setRange(0, maxScroll, 1);
            curiosScrollbar.setVisible(true);
            var bounds = curiosPanel.getBounds();
            curiosScrollbar.setPosition(new Point(
                    bounds.getX() - leftPos + curiosPanel.getScrollbarX(),
                    bounds.getY() - topPos + curiosPanel.getScrollbarY()));
        }

        int firstSlot = (curiosScrollbar != null ? curiosScrollbar.getCurrentScroll() : 0) * columns;
        var bounds = curiosPanel.getBounds();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            boolean visible = i >= firstSlot && i < firstSlot + visibleSlots;
            if (visible) {
                int visibleIndex = i - firstSlot;
                int x = bounds.getX() - leftPos
                        + curiosPanel.getSlotAnchorX()
                        + (visibleIndex % columns) * CuriosPanel.SLOT_SIZE;
                int y = bounds.getY() - topPos
                        + curiosPanel.getSlotAnchorY()
                        + (visibleIndex / columns) * CuriosPanel.SLOT_SIZE;
                showSlot(slot, x, y);
            } else {
                hideSlot(slot);
            }
        }
    }

    private void updateToolkitSlots() {
        var slots = getToolkitSlots();
        boolean toolkitOpen = toolkitPanel != null && toolkitPanel.isVisible();
        boolean toolkitInManagementArea = isToolkitExpandedInManagementArea();
        if (!toolkitOpen && !toolkitInManagementArea) {
            for (var slot : slots) {
                hideSlot(slot);
            }
            return;
        }

        int columns = toolkitInManagementArea ? getManagementToolkitColumns() : toolkitPanel.getColumns();
        int visibleRows = toolkitInManagementArea ? getManagementToolkitVisibleRows() : ToolkitPanel.VISIBLE_ROWS;
        // 嵌入样板管理区时，本方法在 updateBeforeRender 里先于 updatePatternManagement()
        // → refreshPatternManagementLayout() 执行，此时这两个 rect 可能仍是上一次/默认值，
        // 会导致首帧工具图标与滚动条定位到错误位置。这里先就地解析一次，保证基准坐标是当前布局的最新值。
        if (toolkitInManagementArea) {
            managementToolkitSlotRect = managementToolkitLayout.slot("management_toolkit_slots",
                    managementToolkitSlotRect, imageWidth, imageHeight);
            managementToolkitScrollbarRect = managementToolkitLayout.widget("management_toolkit_scrollbar",
                    managementToolkitScrollbarRect, imageWidth, imageHeight);
            managementToolkitMemoryButton = managementToolkitLayout.widget("management_toolkit_memory",
                    managementToolkitMemoryButton, imageWidth, imageHeight);
        }
        int visibleSlots = columns * visibleRows;
        int totalRows = (slots.size() + columns - 1) / columns;
        int maxScroll = Math.max(0, totalRows - visibleRows);
        Scrollbar activeScrollbar = toolkitInManagementArea ? patternManagementScrollbar : toolkitScrollbar;
        if (activeScrollbar != null) {
            int scrollbarHeight = toolkitInManagementArea ? managementToolkitScrollbarRect.height() : toolkitPanel.getScrollbarHeight();
            activeScrollbar.setHeight(scrollbarHeight);
            activeScrollbar.setRange(0, maxScroll, 1);
            activeScrollbar.setVisible(true);
            if (toolkitInManagementArea) {
                activeScrollbar.setPosition(new Point(
                        managementToolkitScrollbarRect.left(),
                        managementToolkitScrollbarRect.top()));
            }
            if (!toolkitInManagementArea) {
                var bounds = toolkitPanel.getBounds();
                activeScrollbar.setPosition(new Point(
                        bounds.getX() - leftPos + toolkitPanel.getScrollbarX(),
                        bounds.getY() - topPos + toolkitPanel.getScrollbarY()));
            }
        }
        if (!toolkitInManagementArea && toolkitScrollbar != null) {
            toolkitScrollbar.setHeight(toolkitPanel.getScrollbarHeight());
            toolkitScrollbar.setRange(0, maxScroll, 1);
            toolkitScrollbar.setVisible(true);
            var bounds = toolkitPanel.getBounds();
            toolkitScrollbar.setPosition(new Point(
                    bounds.getX() - leftPos + toolkitPanel.getScrollbarX(),
                    bounds.getY() - topPos + toolkitPanel.getScrollbarY()));
        }

        int firstSlot = (activeScrollbar != null ? activeScrollbar.getCurrentScroll() : 0) * columns;
        if (toolkitPanel != null) {
            toolkitPanel.setFirstVisibleSlot(firstSlot);
        }
        int baseX;
        int baseY;
        if (toolkitInManagementArea) {
            baseX = managementToolkitSlotRect.left();
            baseY = managementToolkitSlotRect.top();
        } else {
            var bounds = toolkitPanel.getBounds();
            baseX = bounds.getX() - leftPos + toolkitPanel.getSlotAnchorX();
            baseY = bounds.getY() - topPos + toolkitPanel.getSlotAnchorY();
        }
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            boolean visible = i >= firstSlot && i < firstSlot + visibleSlots;
            if (visible) {
                int visibleIndex = i - firstSlot;
                int x = baseX + (visibleIndex % columns) * ToolkitPanel.SLOT_SIZE;
                int y = baseY + (visibleIndex / columns) * ToolkitPanel.SLOT_SIZE;
                showSlot(slot, x, y);
            } else {
                hideSlot(slot);
            }
        }
    }

    private void positionCosmeticArmorSlots() {
        if (cosmeticArmorPanel == null) {
            return;
        }
        var bounds = cosmeticArmorPanel.getBounds();
        for (int i = 0; i < COSMETIC_ARMOR_SLOT_SEMANTICS.length; i++) {
            for (var slot : menu.getSlots(COSMETIC_ARMOR_SLOT_SEMANTICS[i])) {
                int x = bounds.getX() - leftPos + cosmeticArmorPanel.getSlotRelativeX(i);
                int y = bounds.getY() - topPos + cosmeticArmorPanel.getSlotRelativeY(i);
                showSlot(slot, x, y);
            }
        }
    }

    private net.minecraft.world.item.ItemStack getCurrentCellWorkbenchStack() {
        var slots = menu.getSlots(WcwtSlotSemantics.WCWT_STORAGE_CELL);
        return slots.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : slots.get(0).getItem();
    }

    private boolean hasVisibleCellUpgradeSlot() {
        for (int i = 0; i < WirelessComprehensiveWorkTerminalMenu.CELL_UPGRADE_SLOTS; i++) {
            if (menu.isSlotEnabled(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 覆写 renderSlot：当高级编码面板打开时，跳过面板专属槽位的 vanilla 渲染。
     * 这些槽位的物品和悬停高亮由 AdvancedCodingPanel.renderContent() 在面板背景层之后绘制，
     * 确保物品显示在面板纹理上方，且不会出现双重图标。
     */
    @Override
    public void renderSlot(GuiGraphics guiGraphics, net.minecraft.world.inventory.Slot slot) {
        var semantic = menu.getSlotSemantic(slot);
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible() && isPanelSlotSemantic(semantic)) {
            return;
        }
        if (resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible()
                && semantic == WcwtSlotSemantics.WCWT_RESONATING_STORAGE) {
            renderResonatingStorageSlot(guiGraphics, slot);
            return;
        }
        if (semantic == SlotSemantics.TOOLBOX
                && (toolboxPanel == null || !toolboxPanel.isVisible())) {
            return;
        }
        if (semantic == WcwtSlotSemantics.WCWT_TOOLKIT
                && ((toolkitPanel == null || !toolkitPanel.isVisible()) && !isToolkitExpandedInManagementArea())) {
            return;
        }
        if (semantic == WcwtSlotSemantics.WCWT_PATTERN_PROVIDER) {
            return;
        }
        int cacheIndex = getPatternCacheSlotIndex(slot);
        if (patternSelectionLockedMode && cacheIndex >= 0 && slot.hasItem() && cacheIndex == selectedPatternCacheIndex) {
            guiGraphics.blit(WCWT_STATES_TEXTURE,
                    slot.x - 1, slot.y - 1,
                    SELECTED_PATTERN_BG_U, SELECTED_PATTERN_BG_V,
                    SELECTED_PATTERN_BG_SIZE, SELECTED_PATTERN_BG_SIZE,
                    256, 256);
        }
        if (shouldRenderToolkitMemoryInsteadOfSlot(semantic, slot)) {
            renderToolkitMemoryGhost(guiGraphics, slot);
        } else {
            super.renderSlot(guiGraphics, slot);
            if (semantic == WcwtSlotSemantics.WCWT_TOOLKIT) {
                renderToolkitMemoryGhost(guiGraphics, slot);
            }
        }
        renderFavoritedIndicator(guiGraphics, slot);
        renderCraftablePatternIndicator(guiGraphics, slot);
        renderCurioToggle(guiGraphics, slot);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        var lines = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot != null && shouldShowCraftableIndicatorForSlot(hoveredSlot)) {
            lines = new ArrayList<>(lines);
            lines.add(ButtonToolTips.Craftable.text().withStyle(ChatFormatting.DARK_GRAY));
        }
        if (hoveredSlot instanceof RepoSlot repoSlot) {
            GridInventoryEntry entry = getDisplayedRepoEntry(repoSlot);
            if (entry != null && entry.getWhat() != null && WcwtFavorites.isFavorited(entry.getWhat())) {
                lines = new ArrayList<>(lines);
                lines.add(Component.translatable("gui.wcwt.favorite.marked").withStyle(ChatFormatting.GOLD));
            }
        }
        return lines;
    }

    private void renderFavoritedIndicator(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof RepoSlot repoSlot)) {
            return;
        }
        GridInventoryEntry entry = getDisplayedRepoEntry(repoSlot);
        if (entry == null || entry.getWhat() == null || !WcwtFavorites.isFavorited(entry.getWhat())) {
            return;
        }
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 300);
        guiGraphics.blit(WCWT_STATES_TEXTURE, slot.x, slot.y, 48, 32, 16, 16, 256, 256);
        poseStack.popPose();
    }

    private void renderCraftablePatternIndicator(GuiGraphics guiGraphics, Slot slot) {
        if (!shouldShowCraftableIndicatorForSlot(slot)) {
            return;
        }
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 100);
        StackSizeRenderer.renderSizeLabel(guiGraphics, this.font, slot.x - 11, slot.y - 11, "+", false);
        poseStack.popPose();
    }

    private boolean shouldShowCraftableIndicatorForSlot(Slot slot) {
        if (!slot.isActive() || craftableIndicatorKeys.isEmpty()) {
            return false;
        }

        var semantic = menu.getSlotSemantic(slot);
        if (semantic != WcwtSlotSemantics.WCWT_PATTERN_CRAFTING_GRID
                && semantic != WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_INPUTS
                && semantic != WcwtSlotSemantics.WCWT_PATTERN_SMITHING_ADDITION
                && semantic != WcwtSlotSemantics.WCWT_PATTERN_SMITHING_BASE
                && semantic != WcwtSlotSemantics.WCWT_PATTERN_SMITHING_TEMPLATE
                && semantic != WcwtSlotSemantics.WCWT_PATTERN_STONECUTTING_INPUT) {
            return false;
        }

        var slotContent = GenericStack.fromItemStack(slot.getItem());
        return slotContent != null && craftableIndicatorKeys.contains(slotContent.what());
    }

    private void renderResonatingStorageSlot(GuiGraphics guiGraphics, Slot slot) {
        if (!slot.getItem().isEmpty()) {
            guiGraphics.renderItem(slot.getItem(), slot.x + 1, slot.y + 1);
            guiGraphics.renderItemDecorations(font, slot.getItem(), slot.x + 1, slot.y + 1);
        }
    }

    private void renderCurioToggle(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof WirelessComprehensiveWorkTerminalMenu.WcwtCurioSlot curioSlot)
                || curiosPanel == null
                || !curiosPanel.isVisible()
                || !curioSlot.canToggleRendering()
                || slot.x == HIDDEN_SLOT_POS.getX()
                || slot.y == HIDDEN_SLOT_POS.getY()) {
            return;
        }

        int texX = curioSlot.getRenderStatus() ? 75 : 83;
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 300);
        guiGraphics.blit(CURIOS_INVENTORY_TEXTURE, slot.x + 12, slot.y - 1, texX, 0, 8, 8, 256, 256);
        poseStack.popPose();
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (menu.getSlotSemantic(slot) == WcwtSlotSemantics.WCWT_PATTERN_PROVIDER) {
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
        renderFavoritedIndicator(guiGraphics, slot);
        renderCurioToggle(guiGraphics, slot);
    }

    private boolean isCosmeticArmorVisible(int cosmeticIndex) {
        if (!ModList.get().isLoaded("cosmeticarmorreworked")) {
            return false;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        int slot = switch (cosmeticIndex) {
            case 0 -> 3;
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 0;
            default -> -1;
        };
        return slot >= 0 && com.lhy.wcwt.compat.CosmeticArmorReworkedBridge.isSkinArmor(player, slot);
    }

    private void renderCosmeticArmorToggleOverlay(GuiGraphics guiGraphics) {
        if (cosmeticArmorPanel == null || !cosmeticArmorPanel.isVisible()) {
            return;
        }
        var bounds = cosmeticArmorPanel.getBounds();
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 400);
        for (int cosmeticIndex = 0; cosmeticIndex < 4; cosmeticIndex++) {
            int iconX = bounds.getX() + 3 + cosmeticIndex * 18;
            int iconY = bounds.getY() + 15;
            int texX = isCosmeticArmorVisible(cosmeticIndex) ? 208 : 192;
            guiGraphics.blit(WCWT_STATES_TEXTURE, iconX, iconY, texX, 0, 5, 5, 256, 256);
        }
        poseStack.popPose();
    }

    private static boolean isPanelSlotSemantic(appeng.menu.SlotSemantic slotSemantic) {
        for (var semantic : PANEL_SLOT_SEMANTICS) {
            if (semantic == slotSemantic) {
                return true;
            }
        }
        return false;
    }

    private int getPatternCacheSlotIndex(Slot slot) {
        return getPatternCacheSlots().indexOf(slot);
    }

    private List<Slot> getPatternCacheSlots() {
        return menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_CACHE);
    }

    private List<Slot> getCurioSlots() {
        return menu.getSlots(WcwtSlotSemantics.AE_CURIOS);
    }

    private List<Slot> getToolkitSlots() {
        return menu.getSlots(WcwtSlotSemantics.WCWT_TOOLKIT);
    }

    private boolean isMouseOverPatternCache(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        int slotY = patternCacheSlotsOriginY();
        return relX >= PATTERN_CACHE_SLOT_X
                && relX < PATTERN_CACHE_SLOT_X + PATTERN_CACHE_COLS * 18 + 12
                && relY >= slotY
                && relY < slotY + PATTERN_CACHE_VISIBLE_ROWS * 18;
    }

    private boolean isMouseOverCuriosPanel(double mouseX, double mouseY) {
        if (curiosPanel == null || !curiosPanel.isVisible()) {
            return false;
        }
        var bounds = curiosPanel.getBounds();
        return mouseX >= bounds.getX()
                && mouseX < bounds.getX() + bounds.getWidth()
                && mouseY >= bounds.getY()
                && mouseY < bounds.getY() + bounds.getHeight();
    }

    private boolean isMouseOverToolkitPanel(double mouseX, double mouseY) {
        if (isToolkitExpandedInManagementArea()) {
            return isMouseOverPatternManagement(mouseX, mouseY);
        }
        if (toolkitPanel == null || !toolkitPanel.isVisible()) {
            return false;
        }
        var bounds = toolkitPanel.getBounds();
        return mouseX >= bounds.getX()
                && mouseX < bounds.getX() + bounds.getWidth()
                && mouseY >= bounds.getY()
                && mouseY < bounds.getY() + bounds.getHeight();
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        renderPinnedRowBackgroundOverlay(guiGraphics, offsetX, offsetY);
        renderManualWorkspaceBackground(guiGraphics, offsetX, offsetY);
        renderPatternEncodingBackground(guiGraphics, offsetX, offsetY);
        if (isToolkitExpandedInManagementArea()) {
            guiGraphics.blit(WCWT_TOOLS_TEXTURE,
                    offsetX + managementToolkitBackgroundRect.left(), offsetY + managementToolkitBackgroundRect.top(),
                    0, 0,
                    managementToolkitBackgroundRect.width(), managementToolkitBackgroundRect.height(),
                    256, 256);
        }
    }

    private void renderManualWorkspaceBackground(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        var mode = menu.getManualWorkspaceMode();
        if (mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING) {
            return;
        }

        var rect = mainLayout.widget("manual_workspace_background",
                new ExtendedPanelLayout.Rect(78, imageHeight - 213, 88, 58), imageWidth, imageHeight);
        int srcY = mode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.SMITHING ? 0 : 59;
        int renderHeight = Math.min(rect.height(), 58);
        guiGraphics.blit(WCWT_EXTENDED_CRAFTING_TEXTURE,
                offsetX + rect.left(), offsetY + rect.top(),
                0, srcY, rect.width(), renderHeight, 256, 256);
    }

    private void renderPatternEncodingBackground(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        var inputBase = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(185, imageHeight - 213, 18, 18), imageWidth, imageHeight);
        int srcX = patternEncodingMode == EncodingMode.SMITHING_TABLE ? 128 : 0;
        int srcY = switch (patternEncodingMode) {
            case CRAFTING -> 0;
            case PROCESSING -> 70;
            case SMITHING_TABLE -> 70;
            case STONECUTTING -> 140;
        };
        guiGraphics.blit(AE2_PATTERN_MODES_TEXTURE,
                offsetX + inputBase.left() + PATTERN_ENCODING_BG_X_OFFSET_FROM_INPUT,
                offsetY + inputBase.top() + PATTERN_ENCODING_BG_Y_OFFSET_FROM_INPUT,
                srcX, srcY, PATTERN_ENCODING_BG_WIDTH, PATTERN_ENCODING_BG_HEIGHT, 256, 256);
        if (patternEncodingMode == EncodingMode.STONECUTTING) {
            renderStonecuttingResults(guiGraphics, offsetX, offsetY, inputBase);
        }
    }

    private void renderStonecuttingResults(GuiGraphics guiGraphics, int offsetX, int offsetY,
                                           ExtendedPanelLayout.Rect inputBase) {
        var recipes = menu.getStonecuttingRecipes();
        int scroll = stonecuttingPatternScrollbar == null ? 0 : stonecuttingPatternScrollbar.getCurrentScroll();
        int startIndex = scroll * STONECUTTING_RESULT_COLS;
        int endIndex = Math.min(recipes.size(), startIndex + STONECUTTING_RESULT_COLS * STONECUTTING_RESULT_ROWS);
        ResourceLocation selectedRecipe = menu.getStonecuttingRecipeId();
        int relMouseX = (int) Math.round(this.minecraft.mouseHandler.xpos()
                * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth())
                - leftPos;
        int relMouseY = (int) Math.round(this.minecraft.mouseHandler.ypos()
                * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight())
                - topPos;

        for (int i = startIndex; i < endIndex; i++) {
            var bounds = getStonecuttingRecipeBounds(inputBase, i - startIndex);
            var recipe = recipes.get(i);
            boolean hover = inRect(relMouseX, relMouseY, bounds);
            boolean selected = selectedRecipe != null && selectedRecipe.equals(recipe.id());
            int srcY = STONECUTTING_RESULT_SRC_Y + (selected ? STONECUTTING_RESULT_SLOT_H
                    : hover ? STONECUTTING_RESULT_SLOT_H * 2 : 0);
            guiGraphics.blit(AE2_PATTERN_MODES_TEXTURE,
                    offsetX + bounds.left(), offsetY + bounds.top(),
                    STONECUTTING_RESULT_SRC_X, srcY, STONECUTTING_RESULT_SLOT_W, STONECUTTING_RESULT_SLOT_H,
                    256, 256);

            ItemStack result = recipe.value().getResultItem(Objects.requireNonNull(Minecraft.getInstance().level)
                    .registryAccess());
            int itemY = offsetY + bounds.top() + (selected || hover ? 3 : 2);
            guiGraphics.renderItem(result, offsetX + bounds.left() + 2, itemY);
            guiGraphics.renderItemDecorations(font, result, offsetX + bounds.left() + 2, itemY);
        }
    }

    private ExtendedPanelLayout.Rect getStonecuttingRecipeBounds(ExtendedPanelLayout.Rect inputBase, int index) {
        int bgLeft = inputBase.left() + PATTERN_ENCODING_BG_X_OFFSET_FROM_INPUT;
        int bgTop = inputBase.top() + PATTERN_ENCODING_BG_Y_OFFSET_FROM_INPUT;
        int col = index % STONECUTTING_RESULT_COLS;
        int row = index / STONECUTTING_RESULT_COLS;
        return new ExtendedPanelLayout.Rect(
                bgLeft + 26 + col * STONECUTTING_RESULT_SLOT_W,
                bgTop + 12 + row * STONECUTTING_RESULT_SLOT_H,
                STONECUTTING_RESULT_SLOT_W,
                STONECUTTING_RESULT_SLOT_H);
    }

    @Nullable
    private RecipeHolder<StonecutterRecipe> getStonecuttingRecipeAt(int relX, int relY) {
        if (patternEncodingMode != EncodingMode.STONECUTTING) {
            return null;
        }

        var recipes = menu.getStonecuttingRecipes();
        if (recipes.isEmpty()) {
            return null;
        }

        var inputBase = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(185, imageHeight - 213, 18, 18), imageWidth, imageHeight);
        int scroll = stonecuttingPatternScrollbar == null ? 0 : stonecuttingPatternScrollbar.getCurrentScroll();
        int startIndex = scroll * STONECUTTING_RESULT_COLS;
        int endIndex = Math.min(recipes.size(), startIndex + STONECUTTING_RESULT_COLS * STONECUTTING_RESULT_ROWS);
        for (int i = startIndex; i < endIndex; i++) {
            if (inRect(relX, relY, getStonecuttingRecipeBounds(inputBase, i - startIndex))) {
                return recipes.get(i);
            }
        }
        return null;
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        renderManualAnvilCost(guiGraphics);
        renderPatternManagement(guiGraphics, mouseX, mouseY);
    }

    private void renderManualAnvilCost(GuiGraphics guiGraphics) {
        if (menu.getManualWorkspaceMode() != WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL) {
            return;
        }
        int cost = menu.getManualAnvilCost();
        if (cost <= 0) {
            return;
        }

        boolean hasResult = hasManualAnvilResult();
        boolean mayPickup = mayPickupManualAnvilResult();
        boolean tooExpensive = !hasResult && cost >= ANVIL_TOO_EXPENSIVE_COST
                && (minecraft == null || minecraft.player == null || !minecraft.player.getAbilities().instabuild);
        if (!tooExpensive && !hasResult) {
            return;
        }
        Component text = tooExpensive
                ? Component.translatable("container.repair.expensive")
                : Component.translatable("container.repair.cost", cost);
        int color = (tooExpensive || !mayPickup) ? 0xFF6060 : 0x80FF20;
        var rect = mainLayout.widget("manual_anvil_cost",
                new ExtendedPanelLayout.Rect(79, imageHeight - 194, 0, 0), imageWidth, imageHeight);
        guiGraphics.drawString(font, text, rect.left(), rect.top(), color);
    }

    private boolean hasManualAnvilResult() {
        return menu.getSlots(WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RESULT).stream()
                .anyMatch(slot -> !slot.getItem().isEmpty());
    }

    private boolean mayPickupManualAnvilResult() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        return menu.getSlots(WcwtSlotSemantics.WCWT_MANUAL_ANVIL_RESULT).stream()
                .anyMatch(slot -> slot.mayPickup(minecraft.player));
    }

    private void renderPatternManagement(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int scroll = patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0;
        int textColor = 0x404040;

        renderPatternManagementBackground(guiGraphics);

        renderPatternManagementTextButton(guiGraphics, patternManagementAddButton, mouseX, mouseY);
        renderPatternManagementTextButton(guiGraphics, patternManagementReloadButton, mouseX, mouseY);
        renderPatternManagementTextButton(guiGraphics, patternManagementDeleteButton, mouseX, mouseY);
        renderPatternManagementTextButton(guiGraphics, patternManagementCancelButton, mouseX, mouseY);
        renderPatternManagementToggleButton(guiGraphics, patternManagementDisplayModeButton,
                patternManagementDisplayMode.iconU(), patternManagementDisplayMode.iconV(),
                AE2_STATES_TEXTURE, 256, 256, mouseX, mouseY, false);
        renderPatternManagementToggleButton(guiGraphics, patternManagementDisplaySlotsButton,
                patternManagementShowSlots ? 80 : 64, 80,
                AE2_STATES_TEXTURE, 256, 256, mouseX, mouseY, !patternManagementShowSlots);
        renderPatternManagementUploadToggleButton(guiGraphics, mouseX, mouseY);
        renderPatternManagementToggleButton(guiGraphics, patternManagementSearchModeButton,
                patternManagementSearchMode.iconU(), patternManagementSearchMode.iconV(),
                EAE_ICONS_TEXTURE, 64, 64, mouseX, mouseY, false);

        renderPatternManagementButtonText(guiGraphics, patternManagementAddButton,
                Component.translatable("gui.wcwt.pattern_management.add_mapping"), 0xFFFFFF, mouseX, mouseY);
        renderPatternManagementButtonText(guiGraphics, patternManagementReloadButton,
                Component.translatable("gui.wcwt.pattern_management.reload_mapping"), 0xFFFFFF, mouseX, mouseY);
        renderPatternManagementButtonText(guiGraphics, patternManagementDeleteButton,
                Component.translatable("gui.wcwt.pattern_management.delete_mapping"), 0xFFFFFF, mouseX, mouseY);
        renderPatternManagementButtonText(guiGraphics, patternManagementCancelButton,
                Component.translatable("gui.wcwt.pattern_management.cancel"), 0xFFFFFF, mouseX, mouseY);

        renderBatchProcessingLabels(guiGraphics, textColor);
        renderBatchPropertyButton(guiGraphics, batchItemReplacementButton, batchItemSubstitutions, mouseX, mouseY);
        renderBatchPropertyButton(guiGraphics, batchFluidReplacementButton, batchFluidSubstitutions, mouseX, mouseY);

        if (isToolkitExpandedInManagementArea()) {
            renderManagementToolkit(guiGraphics, mouseX, mouseY);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scroll + i;
            if (rowIndex >= patternManagementRows.size()) {
                break;
            }
            int rowY = patternManagementPage.top() + i * PATTERN_MANAGEMENT_ROW_H;
            var row = patternManagementRows.get(rowIndex);
            if (row instanceof PatternManagementHeaderRow header) {
                renderPatternManagementHeader(guiGraphics, header, rowY, textColor, mouseX, mouseY);
            } else if (row instanceof PatternManagementSlotsRow slotsRow) {
                renderPatternManagementSlots(guiGraphics, slotsRow, rowY, mouseX, mouseY);
            }
        }
    }

    private void renderPatternManagementBackground(GuiGraphics guiGraphics) {
        if (!patternManagementShowSlots) {
            guiGraphics.blit(WCWT_PATTERN_MANAGEMENT_HIDDEN_BG_TEXTURE,
                    patternManagementPage.left(), patternManagementPage.top(),
                    0, 0,
                    patternManagementPage.width(), patternManagementPage.height(),
                    patternManagementPage.width(), patternManagementPage.height());
            return;
        }
        // 供应器名称行不再铺底图；样板槽底图在 {@link #renderPatternManagementSlots} 按格绘制。
    }

    private void renderManagementToolkit(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int columns = getManagementToolkitColumns();
        int visibleRows = getManagementToolkitVisibleRows();
        int scroll = patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0;
        int firstSlot = scroll * columns;
        for (int visibleIndex = 0; visibleIndex < columns * visibleRows; visibleIndex++) {
            int slotIndex = firstSlot + visibleIndex;
            if (slotIndex >= getToolkitSlots().size()) {
                break;
            }
            int x = managementToolkitSlotRect.left() + (visibleIndex % columns) * ToolkitPanel.SLOT_SIZE;
            int y = managementToolkitSlotRect.top() + (visibleIndex / columns) * ToolkitPanel.SLOT_SIZE;
            Slot slot = getToolkitSlots().get(slotIndex);
            if (slotIndex < 11 && (slot == null || slot.getItem().isEmpty())) {
                guiGraphics.blit(WCWT_STATES_TEXTURE, x, y, 48 + slotIndex * 16, 16, 16, 16, 256, 256);
            }
        }
    }

    private void renderToolkitMemoryOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var rect = getActiveToolkitMemoryButtonRect();
        if (rect == null) {
            return;
        }
        renderToolkitMemoryButtonAbsolute(guiGraphics, rect, mouseX, mouseY);
    }

    private void renderToolkitMemoryButtonAbsolute(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                                   int mouseX, int mouseY) {
        int x = leftPos + rect.left();
        int y = topPos + rect.top();
        boolean hover = mouseX >= x && mouseX < x + rect.width()
                && mouseY >= y && mouseY < y + rect.height();
        int sourceU = toolkitMemoryMode || hover ? TOOLKIT_MEMORY_TOGGLE_ON_U : TOOLKIT_MEMORY_TOGGLE_OFF_U;
        guiGraphics.blit(WCWT_STATES_TEXTURE, x, y, rect.width(), rect.height(),
                sourceU, TOOLKIT_MEMORY_TOGGLE_V,
                TOOLKIT_MEMORY_TOGGLE_SOURCE_SIZE, TOOLKIT_MEMORY_TOGGLE_SOURCE_SIZE, 256, 256);
    }

    @Nullable
    private ExtendedPanelLayout.Rect getActiveToolkitMemoryButtonRect() {
        if (isToolkitExpandedInManagementArea()) {
            return managementToolkitMemoryButton;
        }
        if (toolkitPanel == null || !toolkitPanel.isVisible()) {
            return null;
        }
        var bounds = toolkitPanel.getBounds();
        var rect = toolkitPanel.getMemoryButton();
        toolkitMemoryButton = new ExtendedPanelLayout.Rect(
                bounds.getX() - leftPos + rect.left(),
                bounds.getY() - topPos + rect.top(),
                rect.width(),
                rect.height());
        return toolkitMemoryButton;
    }

    private void renderToolkitMemoryGhost(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof WirelessComprehensiveWorkTerminalMenu.ToolkitSlot toolkitSlot)
                || !menu.hasToolkitMemory(toolkitSlot.toolkitLogicalIndex())) {
            return;
        }
        ItemStack memory = menu.getToolkitMemoryStack(toolkitSlot.toolkitLogicalIndex());
        if (memory.isEmpty()) {
            return;
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, TOOLKIT_MEMORY_GHOST_ALPHA);
        guiGraphics.renderItem(memory, slot.x, slot.y);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean shouldRenderToolkitMemoryInsteadOfSlot(@Nullable appeng.menu.SlotSemantic semantic, Slot slot) {
        return toolkitMemoryMode
                && semantic == WcwtSlotSemantics.WCWT_TOOLKIT
                && menu.hasToolkitMemory(slot instanceof WirelessComprehensiveWorkTerminalMenu.ToolkitSlot toolkitSlot
                ? toolkitSlot.toolkitLogicalIndex()
                : -1);
    }

    private void renderPatternManagementHeader(GuiGraphics guiGraphics, PatternManagementHeaderRow row,
                                               int rowY, int textColor, int mouseX, int mouseY) {
        var entry = row.firstEntry();
        if (patternManagementShowSlots && row.containsProvider(selectedPatternProviderId)) {
            guiGraphics.fill(patternManagementPage.left(), rowY,
                    patternManagementPage.left() + patternManagementPage.width(),
                    rowY + PATTERN_MANAGEMENT_ROW_H, 0x44FFFFFF);
        }
        var icon = row.group().icon();
        if (icon != null) {
            guiGraphics.renderItem(icon.toStack(), patternManagementPage.left() + 2,
                    rowY + 1 + PATTERN_MANAGEMENT_HEADER_Y_OFFSET);
        }
        Component displayName = row.entries().size() > 1
                ? Component.empty().append(row.displayName()).append(Component.literal(" (" + row.entries().size() + ")"))
                : row.displayName();
        guiGraphics.drawString(font,
                font.substrByWidth(displayName, 94).getString(),
                patternManagementPage.left() + 20, rowY + 5 + PATTERN_MANAGEMENT_HEADER_Y_OFFSET,
                textColor, false);

        renderPatternManagementButton(guiGraphics, rowButton(patternManagementUploadButton, rowY),
                161, 0, 177, 0,
                AE2_STATES_TEXTURE, 0, 144, 256, 256, mouseX, mouseY);
        renderPatternManagementButtonIcon(guiGraphics, rowButton(patternManagementUiButton, rowY),
                161, 0, 177, 0,
                WCWT_STATES_TEXTURE, 52, 5, 8, 7, 256, 256, mouseX, mouseY);
        if (!patternManagementShowSlots) {
            renderPatternManagementButton(guiGraphics, rowButton(patternManagementHighlightButton, rowY),
                    192, 160, 224, 160,
                    EAE_ICONS_TEXTURE, 48, 32, 64, 64, mouseX, mouseY,
                    PATTERN_MANAGEMENT_HIGHLIGHT_ICON_X_OFFSET, 0);
        }
    }

    private void renderPatternManagementSlots(GuiGraphics guiGraphics, PatternManagementSlotsRow row, int rowY,
                                              int mouseX, int mouseY) {
        for (int col = 0; col < row.slots(); col++) {
            int slot = row.offset() + col;
            int x = patternManagementPage.left() + col * PATTERN_MANAGEMENT_SLOT_STEP;
            int y = rowY + PATTERN_MANAGEMENT_SLOT_Y_OFFSET;
            int panelRight = patternManagementPage.left() + patternManagementPage.width();
            int drawW = Math.min(PATTERN_MANAGEMENT_SLOT_BG_SIZE, panelRight - x);
            if (drawW <= 0) {
                continue;
            }
            guiGraphics.blit(WCWT_MANAGEMENT_TEXTURE, x, y, 0, 0,
                    drawW, PATTERN_MANAGEMENT_SLOT_BG_SIZE,
                    256, 256);
            if (isMouseOverPatternManagementSlot(mouseX, mouseY, x, y)) {
                renderPatternManagementSlotHighlight(guiGraphics, x, y);
            }
            if (shouldHighlightPatternManagementSearchResult(row.entry().providerId(), slot)) {
                renderPatternManagementSearchSlotHighlight(guiGraphics, x, y);
            }
            if (shouldHighlightFocusedPatternSlot(row.entry().providerId(), slot)) {
                renderFocusedPatternManagementSlotHighlight(guiGraphics, x, y);
            }
            ItemStack stack = row.entry().slots().getOrDefault(slot, ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                ItemStack displayStack = getPatternDisplayStack(stack);
                int ix = patternManagementSlotHitMinX(x);
                int iy = patternManagementSlotHitMinY(y);
                guiGraphics.renderItem(displayStack, ix, iy);
                guiGraphics.renderItemDecorations(font, displayStack, ix, iy);
            }
        }
        if (row.offset() == 0) {
            renderPatternManagementButton(guiGraphics, slotRowButton(patternManagementHighlightButton, rowY),
                    192, 160, 224, 160,
                    EAE_ICONS_TEXTURE, 48, 32, 64, 64, mouseX, mouseY,
                    PATTERN_MANAGEMENT_HIGHLIGHT_ICON_X_OFFSET, 0);
        }
    }

    private ItemStack getPatternDisplayStack(ItemStack patternStack) {
        if (patternStack.getItem() instanceof appeng.crafting.pattern.EncodedPatternItem<?> encodedPattern) {
            ItemStack output = encodedPattern.getOutput(patternStack);
            if (!output.isEmpty()) {
                return output;
            }
        }
        return patternStack;
    }

    private static int patternManagementSlotHitMinX(int bgX) {
        return bgX + PATTERN_MANAGEMENT_SLOT_HIT_INSET + PATTERN_MANAGEMENT_SLOT_HIT_X_OFFSET;
    }

    private static int patternManagementSlotHitMinY(int bgY) {
        return bgY + PATTERN_MANAGEMENT_SLOT_HIT_INSET;
    }

    private boolean isMouseOverPatternManagementSlot(int mouseX, int mouseY, int bgX, int bgY) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        int hx = patternManagementSlotHitMinX(bgX);
        int hy = patternManagementSlotHitMinY(bgY);
        return relX >= hx
                && relX < hx + PATTERN_MANAGEMENT_SLOT_HIT_SIZE
                && relY >= hy
                && relY < hy + PATTERN_MANAGEMENT_SLOT_HIT_SIZE;
    }

    private void renderPatternManagementSlotHighlight(GuiGraphics guiGraphics, int bgX, int bgY) {
        int x = patternManagementSlotHitMinX(bgX);
        int y = patternManagementSlotHitMinY(bgY);
        int s = PATTERN_MANAGEMENT_SLOT_HIT_SIZE;
        guiGraphics.hLine(x, x + s, y - 1, 0xFFDAFFFF);
        guiGraphics.hLine(x - 1, x + s, y + s, 0xFFDAFFFF);
        guiGraphics.vLine(x - 1, y - 2, y + s, 0xFFDAFFFF);
        guiGraphics.vLine(x + s, y - 2, y + s, 0xFFDAFFFF);
        guiGraphics.fillGradient(net.minecraft.client.renderer.RenderType.guiOverlay(),
                x, y, x + s, y + s,
                0x669CD3FF, 0x669CD3FF, 0);
    }

    private void renderFocusedPatternManagementSlotHighlight(GuiGraphics guiGraphics, int bgX, int bgY) {
        int x = patternManagementSlotHitMinX(bgX);
        int y = patternManagementSlotHitMinY(bgY);
        int s = PATTERN_MANAGEMENT_SLOT_HIT_SIZE;
        long phase = (System.currentTimeMillis() / FOCUSED_PATTERN_FLASH_PERIOD_MS) & 1L;
        int outer = phase == 0 ? 0xFFBFFFFF : 0xFF78DFFF;
        int inner = phase == 0 ? 0xFFFAFFFF : 0xFF4EC7FF;
        drawPatternManagementSlotOutline(guiGraphics, x, y, s, outer, inner, 1);
    }

    private void renderPatternManagementSearchSlotHighlight(GuiGraphics guiGraphics, int bgX, int bgY) {
        int x = patternManagementSlotHitMinX(bgX);
        int y = patternManagementSlotHitMinY(bgY);
        int s = PATTERN_MANAGEMENT_SLOT_HIT_SIZE;
        int rainbowRgb = getPatternManagementSearchRainbowRgb();
        drawPatternManagementSlotBox(guiGraphics, x, y, s,
                withAlpha(rainbowRgb, PATTERN_MANAGEMENT_SEARCH_MATCH_ALPHA),
                withAlpha(rainbowRgb, PATTERN_MANAGEMENT_SEARCH_MATCH_BG_ALPHA));
    }

    private static int getPatternManagementSearchRainbowRgb() {
        long now = System.currentTimeMillis();
        float hue = (now % PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT_PERIOD_MS)
                / (float) PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT_PERIOD_MS;
        return hsvToRgb(hue, 1.0F, 1.0F);
    }

    private static int withAlpha(int rgb, int alpha255) {
        return ((alpha255 & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int hsvToRgb(float h, float s, float v) {
        if (s <= 0.0F) {
            int g = Math.round(v * 255.0F);
            return (g << 16) | (g << 8) | g;
        }
        float hh = (h - (float) Math.floor(h)) * 6.0F;
        int sector = (int) Math.floor(hh);
        float f = hh - sector;
        float p = v * (1.0F - s);
        float q = v * (1.0F - s * f);
        float t = v * (1.0F - s * (1.0F - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = v;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = v;
                b = p;
            }
            case 2 -> {
                r = p;
                g = v;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = v;
            }
            case 4 -> {
                r = t;
                g = p;
                b = v;
            }
            default -> {
                r = v;
                g = p;
                b = q;
            }
        }
        return (Math.round(r * 255.0F) << 16)
                | (Math.round(g * 255.0F) << 8)
                | Math.round(b * 255.0F);
    }

    private void drawPatternManagementSlotBox(GuiGraphics guiGraphics, int x, int y, int size,
                                              int borderColor, int backgroundColor) {
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y, borderColor);
        guiGraphics.fill(x - 1, y + size, x + size + 1, y + size + 1, borderColor);
        guiGraphics.fill(x - 1, y, x, y + size, borderColor);
        guiGraphics.fill(x + size, y, x + size + 1, y + size, borderColor);
        guiGraphics.fill(x, y, x + size, y + size, backgroundColor);
    }

    private void drawPatternManagementSlotOutline(GuiGraphics guiGraphics, int x, int y, int size,
                                                  int primaryColor, int secondaryColor, int thickness) {
        if (size <= 1 || thickness <= 0) {
            return;
        }
        for (int i = 0; i < thickness; i++) {
            int left = x - i;
            int top = y - i;
            int right = x + size - 1 + i;
            int bottom = y + size - 1 + i;
            int colorA = (i & 1) == 0 ? primaryColor : secondaryColor;
            int colorB = (i & 1) == 0 ? secondaryColor : primaryColor;
            guiGraphics.hLine(left, right, top, colorA);
            guiGraphics.hLine(left, right, bottom, colorB);
            guiGraphics.vLine(left, top, bottom, colorA);
            guiGraphics.vLine(right, top, bottom, colorB);
        }
    }

    private boolean shouldHighlightFocusedPatternSlot(long providerId, int slot) {
        return focusedPatternProviderId == providerId
                && focusedPatternProviderSlot == slot
                && System.currentTimeMillis() <= focusedPatternProviderUntilMs;
    }

    private boolean shouldHighlightPatternManagementSearchResult(long providerId, int slot) {
        return WcwtClientConfig.patternManagementSearchHighlight()
                && patternManageSearchField != null
                && !patternManageSearchField.getValue().trim().isEmpty()
                && patternManagementSearchHighlightSlots.contains(new PatternManagementSlotKey(providerId, slot));
    }

    private void scrollPatternManagementToProvider(long providerId, int slot) {
        if (patternManagementScrollbar == null) {
            return;
        }
        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int targetRow = -1;
        int providerHeaderRow = -1;
        for (int i = 0; i < patternManagementRows.size(); i++) {
            var row = patternManagementRows.get(i);
            if (row instanceof PatternManagementHeaderRow header && header.containsProvider(providerId)) {
                providerHeaderRow = i;
                if (slot < 0) {
                    targetRow = i;
                    break;
                }
                continue;
            }
            if (row instanceof PatternManagementSlotsRow slotsRow
                    && slotsRow.entry().providerId() == providerId
                    && slot >= slotsRow.offset()
                    && slot < slotsRow.offset() + slotsRow.slots()) {
                targetRow = i;
                break;
            }
        }
        if (targetRow < 0) {
            targetRow = providerHeaderRow;
        }
        if (targetRow < 0) {
            return;
        }
        int current = patternManagementScrollbar.getCurrentScroll();
        int desired = current;
        if (targetRow < current) {
            desired = targetRow;
        } else if (targetRow >= current + visibleRows) {
            desired = targetRow - visibleRows + 1;
        }
        int clampedDesired = Math.max(0, desired);
        if (clampedDesired != current) {
            patternManagementScrollbar.setCurrentScroll(clampedDesired);
            patternProviderSlotLayoutDirty = true;
            panelSlotActivityDirty = true;
        }
    }

    private void renderPatternManagementToggleButton(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                                     int iconU, int iconV, ResourceLocation iconTexture,
                                                     int iconTextureWidth, int iconTextureHeight,
                                                     int mouseX, int mouseY, boolean active) {
        boolean hover = mouseX >= leftPos + rect.left() && mouseX < leftPos + rect.left() + rect.width()
                && mouseY >= topPos + rect.top() && mouseY < topPos + rect.top() + rect.height();
        int pressOffsetY = (hover || active) ? BUTTON_PRESS_OFFSET_Y : 0;
        int bgU = (hover || active) ? 42 : 28;
        int bgV = (hover || active) ? 0 : 0;
        guiGraphics.blit(AE2_CHECKBOX_TEXTURE, rect.left(), rect.top(), rect.width(), rect.height(),
                bgU, bgV, 14, 14, 64, 64);
        int iconW = rect.width();
        int iconH = rect.height();
        int iconX = rect.left() + (rect.width() - iconW) / 2;
        int iconY = rect.top() + (rect.height() - iconH) / 2 + pressOffsetY;
        int sourceW = iconTextureWidth == 64 ? 12 : 16;
        int sourceH = iconTextureWidth == 64 ? 12 : 16;
        guiGraphics.blit(iconTexture, iconX, iconY, iconW, iconH, iconU, iconV,
                sourceW, sourceH, iconTextureWidth, iconTextureHeight);
    }

    private void renderPatternManagementUploadToggleButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var rect = patternManagementAutoUploadButton;
        boolean hover = mouseX >= leftPos + rect.left() && mouseX < leftPos + rect.left() + rect.width()
                && mouseY >= topPos + rect.top() && mouseY < topPos + rect.top() + rect.height();
        int bgU = patternManagementUploadEnabled
                ? AE2_RADIO_CHECKED_FOCUS_U
                : hover ? AE2_RADIO_UNCHECKED_FOCUS_U : AE2_RADIO_UNCHECKED_U;
        int bgV = patternManagementUploadEnabled
                ? AE2_RADIO_CHECKED_FOCUS_V
                : hover ? AE2_RADIO_UNCHECKED_FOCUS_V : AE2_RADIO_UNCHECKED_V;
        guiGraphics.blit(AE2_CHECKBOX_TEXTURE, rect.left(), rect.top(), rect.width(), rect.height(),
                bgU, bgV, AE2_RADIO_SIZE, AE2_RADIO_SIZE, 64, 64);
    }

    private void renderPatternManagementTextButton(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                                   int mouseX, int mouseY) {
        boolean hover = mouseX >= leftPos + rect.left() && mouseX < leftPos + rect.left() + rect.width()
                && mouseY >= topPos + rect.top() && mouseY < topPos + rect.top() + rect.height();
        guiGraphics.blit(WCWT_STATES_TEXTURE, rect.left(), rect.top(), rect.width(), rect.height(),
                128, hover ? 193 : 160, 60, 22, 256, 256);
    }

    private void renderPatternManagementButtonText(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                                   Component text, int color, int mouseX, int mouseY) {
        String label = text.getString();
        float scale = 0.75F;
        int textWidth = font.width(label);
        boolean hover = mouseX >= leftPos + rect.left() && mouseX < leftPos + rect.left() + rect.width()
                && mouseY >= topPos + rect.top() && mouseY < topPos + rect.top() + rect.height();
        float x = rect.left() + (rect.width() - textWidth * scale) / 2.0F;
        float y = rect.top() + (rect.height() - font.lineHeight * scale) / 2.0F
                + (hover ? BUTTON_PRESS_OFFSET_Y : 0);
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, label, 0, 0, color, false);
        pose.popPose();
    }

    private void renderBatchProcessingLabels(GuiGraphics guiGraphics, int color) {
        drawScaledVerticalText(guiGraphics,
                Component.translatable("gui.wcwt.batch.pattern_multiplier").getString(),
                13, imageHeight - 136, color, 0.875F);
        guiGraphics.drawString(font, Component.translatable("gui.wcwt.batch.item_substitution"),
                108, imageHeight - 131, color, false);
        guiGraphics.drawString(font, Component.translatable("gui.wcwt.batch.fluid_substitution"),
                108, imageHeight - 113, color, false);
    }

    private void drawVerticalText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        for (int i = 0; i < text.length(); i++) {
            guiGraphics.drawString(font, text.substring(i, i + 1), x, y + i * 8, color, false);
        }
    }

    private void drawScaledVerticalText(GuiGraphics guiGraphics, String text, int x, int y, int color, float scale) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0F);
        // 缩小后也按缩小后的字高排布，避免视觉上字符间隙被放大。
        int step = 8;
        for (int i = 0; i < text.length(); i++) {
            guiGraphics.drawString(font, text.substring(i, i + 1), 0, i * step, color, false);
        }
        pose.popPose();
    }

    private void renderBatchPropertyButton(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                           boolean selected, int mouseX, int mouseY) {
        boolean hover = mouseX >= leftPos + rect.left() && mouseX < leftPos + rect.left() + rect.width()
                && mouseY >= topPos + rect.top() && mouseY < topPos + rect.top() + rect.height();
        int pressOffsetY = (hover || selected) ? BUTTON_PRESS_OFFSET_Y : 0;
        int bgU = selected ? AE2_RADIO_CHECKED_FOCUS_U
                : hover ? AE2_RADIO_UNCHECKED_FOCUS_U : AE2_RADIO_UNCHECKED_U;
        int bgV = selected ? AE2_RADIO_CHECKED_FOCUS_V
                : hover ? AE2_RADIO_UNCHECKED_FOCUS_V : AE2_RADIO_UNCHECKED_V;
        guiGraphics.blit(AE2_CHECKBOX_TEXTURE, rect.left(), rect.top() + pressOffsetY, rect.width(), rect.height(),
                bgU, bgV, AE2_RADIO_SIZE, AE2_RADIO_SIZE, 64, 64);
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, int x, int y,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               int w, int h, ResourceLocation iconTexture, int iconU, int iconV,
                                               int mouseX, int mouseY) {
        renderPatternManagementButton(guiGraphics, x, y, normalU, normalV, hoverU, hoverV, w, h,
                iconTexture, iconU, iconV, 64, 64, mouseX, mouseY, 0, 0);
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, int x, int y,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               int w, int h, ResourceLocation iconTexture, int iconU, int iconV,
                                               int iconTextureWidth, int iconTextureHeight,
                                               int mouseX, int mouseY) {
        renderPatternManagementButton(guiGraphics, x, y, normalU, normalV, hoverU, hoverV, w, h,
                iconTexture, iconU, iconV, iconTextureWidth, iconTextureHeight, mouseX, mouseY, 0, 0);
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, int x, int y,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               int w, int h, ResourceLocation iconTexture, int iconU, int iconV,
                                               int iconTextureWidth, int iconTextureHeight,
                                               int mouseX, int mouseY, int iconOffsetX, int iconOffsetY) {
        int iconSize = iconTextureWidth == 256 ? 16 : 12;
        renderPatternManagementButton(guiGraphics, x, y, normalU, normalV, hoverU, hoverV, w, h,
                iconTexture, iconU, iconV, iconSize, iconSize, iconTextureWidth, iconTextureHeight,
                mouseX, mouseY, iconOffsetX, iconOffsetY);
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, int x, int y,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               int w, int h, ResourceLocation iconTexture, int iconU, int iconV,
                                               int iconW, int iconH, int iconTextureWidth, int iconTextureHeight,
                                               int mouseX, int mouseY, int iconOffsetX, int iconOffsetY) {
        boolean hover = mouseX >= leftPos + x && mouseX < leftPos + x + w
                && mouseY >= topPos + y && mouseY < topPos + y + h;
        int pressOffsetY = hover ? BUTTON_PRESS_OFFSET_Y : 0;
        guiGraphics.blit(WCWT_STATES_TEXTURE, x, y, hover ? hoverU : normalU, hover ? hoverV : normalV,
                Math.min(w, 23), Math.min(h, 16), 256, 256);
        if (iconTexture != null) {
            int iconX = x + (w - iconW) / 2 + iconOffsetX;
            int iconY = y + (h - iconH) / 2 + iconOffsetY + pressOffsetY;
            guiGraphics.blit(iconTexture, iconX, iconY, iconU, iconV, iconW, iconH,
                    iconTextureWidth, iconTextureHeight);
        }
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               ResourceLocation iconTexture, int iconU, int iconV,
                                               int mouseX, int mouseY) {
        renderPatternManagementButton(guiGraphics, rect.left(), rect.top(), normalU, normalV, hoverU, hoverV,
                rect.width(), rect.height(), iconTexture, iconU, iconV, mouseX, mouseY);
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               ResourceLocation iconTexture, int iconU, int iconV,
                                               int iconTextureWidth, int iconTextureHeight,
                                               int mouseX, int mouseY) {
        renderPatternManagementButton(guiGraphics, rect.left(), rect.top(), normalU, normalV, hoverU, hoverV,
                rect.width(), rect.height(), iconTexture, iconU, iconV, iconTextureWidth, iconTextureHeight,
                mouseX, mouseY);
    }

    private void renderPatternManagementButton(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                               int normalU, int normalV, int hoverU, int hoverV,
                                               ResourceLocation iconTexture, int iconU, int iconV,
                                               int iconTextureWidth, int iconTextureHeight,
                                               int mouseX, int mouseY, int iconOffsetX, int iconOffsetY) {
        renderPatternManagementButton(guiGraphics, rect.left(), rect.top(), normalU, normalV, hoverU, hoverV,
                rect.width(), rect.height(), iconTexture, iconU, iconV, iconTextureWidth, iconTextureHeight,
                mouseX, mouseY, iconOffsetX, iconOffsetY);
    }

    private void renderPatternManagementButtonIcon(GuiGraphics guiGraphics, ExtendedPanelLayout.Rect rect,
                                                   int normalU, int normalV, int hoverU, int hoverV,
                                                   ResourceLocation iconTexture, int iconU, int iconV,
                                                   int iconW, int iconH, int iconTextureWidth, int iconTextureHeight,
                                                   int mouseX, int mouseY) {
        renderPatternManagementButton(guiGraphics, rect.left(), rect.top(), normalU, normalV, hoverU, hoverV,
                rect.width(), rect.height(), iconTexture, iconU, iconV, iconW, iconH,
                iconTextureWidth, iconTextureHeight, mouseX, mouseY, 0, 0);
    }

    private ExtendedPanelLayout.Rect rowButton(ExtendedPanelLayout.Rect rect, int rowY) {
        return new ExtendedPanelLayout.Rect(rect.left(), rowY + rect.top() + PATTERN_MANAGEMENT_HEADER_Y_OFFSET,
                rect.width(), rect.height());
    }

    private ExtendedPanelLayout.Rect slotRowButton(ExtendedPanelLayout.Rect rect, int rowY) {
        return new ExtendedPanelLayout.Rect(rect.left(), rowY + rect.top() + PATTERN_MANAGEMENT_SLOT_Y_OFFSET,
                rect.width(), rect.height());
    }

    @Override
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (patternSelectionLockedMode) {
            int cacheIndex = getPatternCacheSlotIndex(slot);
            if (cacheIndex >= 0) {
                if (mouseButton == 0 && clickType == ClickType.PICKUP && slot != null && slot.hasItem()) {
                    selectedPatternCacheIndex = cacheIndex;
                    PacketDistributor.sendToServer(new PatternSelectionPacket(cacheIndex));
                }
                return; // 高级编码 UI 打开时锁定缓存槽，禁止拿走/放入样板。
            }
        }
        if (slot instanceof WirelessComprehensiveWorkTerminalMenu.PatternProviderSlot patternProviderSlot) {
            handlePatternProviderSlotAction(patternProviderSlot, mouseButton, clickType);
            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    private void handlePatternProviderSlotAction(WirelessComprehensiveWorkTerminalMenu.PatternProviderSlot slot,
                                                 int mouseButton,
                                                 ClickType clickType) {
        if (slot.providerId() <= 0 || slot.providerSlot() < 0) {
            return;
        }
        switch (clickType) {
            case PICKUP -> sendPatternManagementAction(
                    PatternManagementActionPacket.Action.EXCHANGE_PROVIDER_SLOT,
                    slot.providerId(),
                    slot.providerSlot());
            case QUICK_MOVE -> sendPatternManagementAction(
                    PatternManagementActionPacket.Action.QUICK_EXTRACT_PROVIDER_SLOT,
                    slot.providerId(),
                    slot.providerSlot());
            default -> {
                return;
            }
        }
        playPatternManagementItemTransferSound(menu.getCarried().isEmpty());
    }
    
    private void renderExtendedUI(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染当前可见的扩展UI面板
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible()) {
            advancedCodingPanel.render(guiGraphics, mouseX, mouseY, 0);
        }
        if (cosmeticArmorPanel != null && cosmeticArmorPanel.isVisible()) {
            cosmeticArmorPanel.render(guiGraphics, mouseX, mouseY, 0);
        }
        if (curiosPanel != null && curiosPanel.isVisible()) {
            curiosPanel.render(guiGraphics, mouseX, mouseY, 0);
        }
        if (toolboxPanel != null && toolboxPanel.isVisible()) {
            toolboxPanel.render(guiGraphics, mouseX, mouseY, 0);
        }
        if (toolkitPanel != null && toolkitPanel.isVisible()) {
            toolkitPanel.render(guiGraphics, mouseX, mouseY, 0);
        }
        if (resonatingLightningPatternCodingPanel != null && resonatingLightningPatternCodingPanel.isVisible()) {
            resonatingLightningPatternCodingPanel.render(guiGraphics, mouseX, mouseY, 0);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (DEBUG_SLOT_HIT) {
            logSlotHitDebug(mouseX, mouseY, button, "before");
        }
        if (this.minecraft.options.keyPickItem.matchesMouse(button)) {
            var slot = getSlotAt(mouseX, mouseY);
            if (menu.canModifyAmountForSlot(slot)) {
                var currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    var screen = new WcwtSetProcessingPatternAmountSubScreen(
                            this,
                            currentStack,
                            newStack -> {
                                ServerboundPacket message = new InventoryActionPacket(
                                        InventoryAction.SET_FILTER,
                                        slot.index,
                                        GenericStack.wrapInItemStack(newStack));
                                PacketDistributor.sendToServer(message);
                            });
                    switchToScreen(screen);
                    return true;
                }
            }
        }

        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        if (button == 1) {
            // AETextField 使用屏幕绝对坐标，须与 MEStorageScreen 一致使用 isMouseOver（而非 GUI 相对坐标）
            if (patternManageSearchField != null && patternManageSearchField.isMouseOver(mouseX, mouseY)) {
                patternManageSearchField.setValue("");
                return true;
            }
            if (patternManageMappingField != null && patternManageMappingField.isMouseOver(mouseX, mouseY)) {
                patternManageMappingField.setValue("");
                return true;
            }
        }
        preparePatternProviderSlotClick(mouseX, mouseY);
        if (button == 0 && handleManualAnvilNameFieldClick(mouseX, mouseY)) {
            return true;
        }
        // 先检查扩展UI面板是否处理点击
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible()) {
            if (advancedCodingPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (cosmeticArmorPanel != null && cosmeticArmorPanel.isVisible()) {
            if (cosmeticArmorPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (curiosPanel != null && curiosPanel.isVisible()) {
            if (curiosPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button == 0) {
                var curioToggleSlot = getCurioToggleSlotAt(mouseX, mouseY);
                if (curioToggleSlot != null) {
                    curioToggleSlot.toggleRenderStatus();
                    CuriosBridge.toggleRender(curioToggleSlot.getIdentifier(), curioToggleSlot.getSlotIndex());
                    playPatternManagementClickSound();
                    return true;
                }
            }
        }
        if (toolboxPanel != null && toolboxPanel.isVisible()) {
            if (toolboxPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if ((toolkitPanel != null && toolkitPanel.isVisible()) || isToolkitExpandedInManagementArea()) {
            if (handleToolkitMemoryClick(mouseX, mouseY, button)) {
                return true;
            }
            if (toolkitPanel != null && toolkitPanel.isVisible()
                    && toolkitPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button == 0 && tryAeNetworkToolkitSlotDoubleDeposit(mouseX, mouseY)) {
                return true;
            }
        }
        if (resonatingLightningPatternCodingPanel != null && resonatingLightningPatternCodingPanel.isVisible()) {
            if (resonatingLightningPatternCodingPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0 && handlePatternManagementClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && handleStonecuttingRecipeClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0) {
            Slot clickedSlot = getPlayerInventorySlotAt(mouseX, mouseY);
            if (handleQuickPatternProviderInsert(clickedSlot)) {
                return true;
            }
        }
        if (button == 0 && patternSelectionLockedMode) {
            var cacheSlots = getPatternCacheSlots();
            for (int i = 0; i < cacheSlots.size(); i++) {
                var slot = cacheSlots.get(i);
                if (slot != null && slot.getItem() != null
                        && relX >= slot.x && relX < slot.x + 16
                        && relY >= slot.y && relY < slot.y + 16) {
                    if (menu.trySelectPatternForAdvancedCodingFromCacheSlot(i)) {
                        selectedPatternCacheIndex = i;
                        PacketDistributor.sendToServer(new PatternSelectionPacket(i));
                        return true;
                    }
                }
            }
        }
        
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (DEBUG_SLOT_HIT) {
            logSlotHitDebug(mouseX, mouseY, button, handled ? "after-handled" : "after-pass");
        }
        return handled;
    }

    @Nullable
    private Slot getSlotAt(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        for (int menuPos = 0; menuPos < menu.slots.size(); menuPos++) {
            Slot slot = menu.slots.get(menuPos);
            if (!slot.isActive()) {
                continue;
            }
            if (shouldIgnoreGhostSlot(menuPos, slot, relX, relY)) {
                continue;
            }
            if (relX >= slot.x && relX < slot.x + 16 && relY >= slot.y && relY < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    private boolean shouldIgnoreGhostSlot(int menuPos, Slot slot, int relX, int relY) {
        var semantic = menu.getSlotSemantic(slot);
        if (semantic != null) {
            return false;
        }
        if (menuPos == slot.index) {
            return false;
        }
        if (upgradesPanel == null) {
            return false;
        }
        var bounds = upgradesPanel.getBounds();
        int panelLeft = bounds.getX();
        int panelTop = bounds.getY();
        int panelRight = panelLeft + bounds.getWidth();
        int panelBottom = panelTop + bounds.getHeight();
        boolean insideUpgradePanel = relX >= panelLeft && relX < panelRight && relY >= panelTop && relY < panelBottom;
        return !insideUpgradePanel;
    }

    private void logSlotHitDebug(double mouseX, double mouseY, int button, String stage) {
        Slot slot = getSlotAt(mouseX, mouseY);
        int menuPos = slot == null ? -1 : menu.slots.indexOf(slot);
        var semantic = slot == null ? null : menu.getSlotSemantic(slot);
        String semanticName = semantic == null ? "<null>" : semantic.toString();
        String slotDesc = slot == null
                ? "<none>"
                : "menuPos=" + menuPos + ",slotIdx=" + slot.index + ",x=" + slot.x + ",y=" + slot.y + ",semantic=" + semanticName
                        + ",stack=" + describeSlotHitStack(slot.getItem());
        String upgradesBounds = upgradesPanel == null
                ? "<none>"
                : "x=" + upgradesPanel.getBounds().getX()
                        + ",y=" + upgradesPanel.getBounds().getY()
                        + ",w=" + upgradesPanel.getBounds().getWidth()
                        + ",h=" + upgradesPanel.getBounds().getHeight();
        WcwtMod.LOGGER.info(
                "WCWT slot hit debug: stage={}, mouseX={}, mouseY={}, relX={}, relY={}, button={}, slot={}, upgradesBounds={}",
                stage,
                String.format(java.util.Locale.ROOT, "%.2f", mouseX),
                String.format(java.util.Locale.ROOT, "%.2f", mouseY),
                String.format(java.util.Locale.ROOT, "%.2f", mouseX - leftPos),
                String.format(java.util.Locale.ROOT, "%.2f", mouseY - topPos),
                button,
                slotDesc,
                upgradesBounds);
    }

    private static String describeSlotHitStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getCount() + "x" + (key == null ? stack.getItem().toString() : key.toString());
    }

    private static class WcwtSetProcessingPatternAmountSubScreen extends
            appeng.client.gui.AESubScreen<WirelessComprehensiveWorkTerminalMenu, WirelessComprehensiveWorkTerminalScreen> {
        private final NumberEntryWidget amount;
        private final GenericStack currentStack;
        private final java.util.function.Consumer<GenericStack> setter;

        WcwtSetProcessingPatternAmountSubScreen(WirelessComprehensiveWorkTerminalScreen parent,
                                                GenericStack currentStack,
                                                java.util.function.Consumer<GenericStack> setter) {
            super(parent, "/screens/set_processing_pattern_amount.json");
            this.currentStack = currentStack;
            this.setter = setter;

            widgets.addButton("save", Component.translatable("gui.ae2.Set"), btn -> confirm());
            var icon = getMenu().getHost().getMainMenuIcon();
            widgets.add("back", new TabButton(Icon.BACK, icon.getHoverName(), btn -> returnToParent()));

            this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
            this.amount.setLongValue(currentStack.amount());
            this.amount.setMaxValue(getMaxAmount());
            this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
            this.amount.setMinValue(0);
            this.amount.setHideValidationIcon(true);
            this.amount.setOnConfirm(this::confirm);

            addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
        }

        @Override
        protected void init() {
            super.init();
            setSlotsHidden(SlotSemantics.TOOLBOX, true);
            removeInjectedSortButtons();
        }

        @Override
        protected void updateBeforeRender() {
            super.updateBeforeRender();
            removeInjectedSortButtons();
        }

        private void removeInjectedSortButtons() {
            var toRemove = new java.util.ArrayList<net.minecraft.client.gui.components.events.GuiEventListener>();
            for (var child : new java.util.ArrayList<>(children())) {
                if (child instanceof net.minecraft.client.gui.components.Button button) {
                    int x = button.getX() - leftPos;
                    int y = button.getY() - topPos;
                    boolean inDialog = x >= 0 && y >= 0 && x + button.getWidth() <= imageWidth && y + button.getHeight() <= imageHeight;
                    boolean isAeOwnButton = child instanceof appeng.client.gui.widgets.TabButton
                            || child instanceof appeng.client.gui.widgets.AE2Button;
                    if (inDialog && !isAeOwnButton) {
                        toRemove.add(child);
                    }
                }
            }
            toRemove.forEach(this::removeWidget);
        }

        private void confirm() {
            this.amount.getLongValue().ifPresent(newAmount -> {
                newAmount = Longs.constrainToRange(newAmount, 0, getMaxAmount());
                if (newAmount <= 0) {
                    setter.accept(null);
                } else {
                    setter.accept(new GenericStack(currentStack.what(), newAmount));
                }
                returnToParent();
            });
        }

        private long getMaxAmount() {
            return 999999L * (long) currentStack.what().getAmountPerUnit();
        }
    }

    private boolean handleManualAnvilNameFieldClick(double mouseX, double mouseY) {
        if (manualAnvilNameField == null || !manualAnvilNameField.isVisible()) {
            return false;
        }

        if (!manualAnvilNameField.isMouseOver(mouseX, mouseY)) {
            manualAnvilNameField.setFocused(false);
            if (getFocused() == manualAnvilNameField) {
                setFocused(null);
            }
            return false;
        }

        setFocused(manualAnvilNameField);
        return manualAnvilNameField.mouseClicked(mouseX, mouseY, 0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((patternManageSearchField != null && patternManageSearchField.isFocused()
                && patternManageSearchField.keyPressed(keyCode, scanCode, modifiers))
                || (patternManageMappingField != null && patternManageMappingField.isFocused()
                && patternManageMappingField.keyPressed(keyCode, scanCode, modifiers))) {
            return true;
        }
        if (manualAnvilNameField != null && manualAnvilNameField.isVisible() && manualAnvilNameField.isFocused()
                && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)) {
            manualAnvilNameField.setFocused(false);
            if (getFocused() == manualAnvilNameField) {
                setFocused(null);
            }
            return true;
        }
        if (WcwtKeybindings.TOGGLE_FAVORITE_ITEM.matches(keyCode, scanCode) && toggleFavoriteForHoveredRepoSlot()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean handleStonecuttingRecipeClick(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        var recipe = getStonecuttingRecipeAt(relX, relY);
        if (recipe == null) {
            return false;
        }

        PacketDistributor.sendToServer(new StonecuttingRecipeSelectionPacket(recipe.id()));
        menu.setStonecuttingRecipeId(recipe.id());
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
        return true;
    }

    private boolean handlePatternManagementClick(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);

        if (inRect(relX, relY, patternManagementAddButton)) {
            playPatternManagementClickSound();
            String searchText = patternManageSearchField != null ? patternManageSearchField.getValue() : "";
            String mappingText = patternManageMappingField != null ? patternManageMappingField.getValue() : "";
            boolean localMappingAdded = updateClientMappingAdd(searchText, mappingText);
            displayPatternManagementMessage(Component.translatable(localMappingAdded
                    ? "gui.wcwt.pattern_management.mapping_added"
                    : "gui.wcwt.pattern_management.mapping_failed"));
            if (localMappingAdded && patternManageSearchField != null) {
                patternManageSearchField.setValue(mappingText.trim());
                resolvedPatternManagementSearchText = resolveClientProviderSearchText(mappingText);
            }
            refreshPatternProviders();
            clearPatternManagementMappingField();
            return true;
        }
        if (inRect(relX, relY, patternManagementReloadButton)) {
            playPatternManagementClickSound();
            ExtendedAePlusUploadCompat.loadRecipeTypeNames();
            resolvedPatternManagementSearchText = resolveClientProviderSearchText(currentPatternManagementSearchText());
            displayPatternManagementMessage(Component.translatable("gui.wcwt.pattern_management.mapping_reloaded"));
            refreshPatternProviders();
            return true;
        }
        if (inRect(relX, relY, patternManagementDeleteButton)) {
            playPatternManagementClickSound();
            String mappingText = patternManageMappingField != null ? patternManageMappingField.getValue() : "";
            int removed = ExtendedAePlusUploadCompat.removeMappingsByCnValue(mappingText);
            resolvedPatternManagementSearchText = resolveClientProviderSearchText(currentPatternManagementSearchText());
            displayPatternManagementMessage(Component.translatable(
                    "gui.wcwt.pattern_management.mapping_deleted", removed));
            refreshPatternProviders();
            clearPatternManagementMappingField();
            return true;
        }
        if (inRect(relX, relY, patternManagementCancelButton)) {
            playPatternManagementClickSound();
            if (patternManageSearchField != null) {
                patternManageSearchField.setValue("");
            }
            if (patternManageMappingField != null) {
                patternManageMappingField.setValue("");
            }
            rebuildPatternManagementRows();
            patternProviderSlotLayoutDirty = true;
            return true;
        }
        if (inRect(relX, relY, patternManagementDisplayModeButton)) {
            playPatternManagementClickSound();
            patternManagementDisplayMode = patternManagementDisplayMode.next();
            rebuildPatternManagementRows();
            patternProviderSlotLayoutDirty = true;
            sendPatternManagementSettings();
            return true;
        }
        if (inRect(relX, relY, patternManagementDisplaySlotsButton)) {
            playPatternManagementClickSound();
            patternManagementShowSlots = !patternManagementShowSlots;
            rebuildPatternManagementRows();
            patternProviderSlotLayoutDirty = true;
            sendPatternManagementSettings();
            return true;
        }
        if (inRect(relX, relY, patternManagementAutoUploadButton)) {
            playPatternManagementClickSound();
            patternManagementUploadEnabled = !patternManagementUploadEnabled;
            sendPatternManagementSettings();
            return true;
        }
        if (inRect(relX, relY, patternManagementSearchModeButton)) {
            playPatternManagementClickSound();
            patternManagementSearchMode = patternManagementSearchMode.next();
            rebuildPatternManagementRows();
            patternProviderSlotLayoutDirty = true;
            refreshPatternProviders();
            sendPatternManagementSettings();
            return true;
        }
        if (inRect(relX, relY, batchItemReplacementButton)) {
            playPatternManagementClickSound();
            batchItemSubstitutions = !batchItemSubstitutions;
            PacketDistributor.sendToServer(new PatternModePacket(
                    PatternModePacket.MODE_PATTERN_ITEM_SUBSTITUTIONS, batchItemSubstitutions));
            return true;
        }
        if (inRect(relX, relY, batchFluidReplacementButton)) {
            playPatternManagementClickSound();
            batchFluidSubstitutions = !batchFluidSubstitutions;
            PacketDistributor.sendToServer(new PatternModePacket(
                    PatternModePacket.MODE_PATTERN_FLUID_SUBSTITUTIONS, batchFluidSubstitutions));
            return true;
        }

        if (handlePatternManagementHeaderButtonClick(relX, relY)) {
            return true;
        }

        var hit = getPatternManagementHeaderAt(relX, relY);
        if (hit != null) {
            selectedPatternProviderId = hit.header().firstEntry().providerId();
            selectedPatternProviderSlot = -1;
            return true;
        }

        return false;
    }

    private boolean handlePatternManagementHeaderButtonClick(int relX, int relY) {
        var hit = getPatternManagementHeaderButtonAt(relX, relY);
        if (hit == null) {
            return false;
        }

        selectedPatternProviderId = hit.entry().providerId();
        selectedPatternProviderSlot = -1;
        playPatternManagementClickSound();
        refreshPatternProviders();
        switch (hit.button()) {
            case UPLOAD -> sendPatternManagementAction(PatternManagementActionPacket.Action.UPLOAD_CACHE_SLOT,
                    hit.entry().providerId(), -1);
            case UI -> {
                clearPatternManagementMappingField();
                sendPatternManagementAction(PatternManagementActionPacket.Action.OPEN_PROVIDER_UI,
                        hit.entry().providerId(), -1);
            }
            case HIGHLIGHT -> highlightPatternProvider(hit.entry());
        }
        return true;
    }

    private void clearPatternManagementMappingField() {
        if (patternManageMappingField != null) {
            patternManageMappingField.setValue("");
        }
    }

    private boolean updateClientMappingAdd(String searchText, String mappingText) {
        String search = searchText == null ? "" : searchText.trim();
        String mapping = mappingText == null ? "" : mappingText.trim();
        if (search.isEmpty() || mapping.isEmpty()) {
            return false;
        }
        return ExtendedAePlusUploadCompat.addOrUpdateAliasMapping(search, mapping);
    }

    private void displayPatternManagementMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    private void playPatternManagementClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /**
     * Shift 在样板管理与背包间快移样板时的音效，贴近物品进出容器而非按钮点击。
     *
     * @param intoNetwork true：背包 → 样板供应器；false：供应器槽 → 背包
     */
    private void playPatternManagementItemTransferSound(boolean intoNetwork) {
        Minecraft mc = Minecraft.getInstance();
        float pitch = intoNetwork ? 1.06F : 0.92F;
        var sound = intoNetwork ? SoundEvents.BUNDLE_INSERT : SoundEvents.ITEM_PICKUP;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    /**
     * 与 {@link PatternManagementActionPacket} 中列表顺序一致的有效 1-based id。
     *
     * <p>优先使用当前选中的供应器；若未显式选中，则回退到当前聚焦/高亮的供应器；
     * 再回退到样板管理区当前滚动页里首个可见的供应器，避免服务端默认塞到排序后的第一个供应器。
     */
    private long quickInsertTargetProviderId() {
        if (containsPatternProvider(selectedPatternProviderId)) {
            return selectedPatternProviderId;
        }
        if (containsPatternProvider(focusedPatternProviderId)) {
            return focusedPatternProviderId;
        }
        return getFirstVisiblePatternProviderId();
    }

    private boolean containsPatternProvider(long providerId) {
        return providerId > 0 && patternProviders.stream().anyMatch(e -> e.providerId() == providerId);
    }

    private long getFirstVisiblePatternProviderId() {
        if (patternManagementRows.isEmpty() || patternManagementPage.height() <= 0) {
            return -1L;
        }

        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int scroll = patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0;
        int end = Math.min(patternManagementRows.size(), scroll + visibleRows);
        for (int rowIndex = scroll; rowIndex < end; rowIndex++) {
            var row = patternManagementRows.get(rowIndex);
            if (row instanceof PatternManagementHeaderRow header) {
                return header.firstEntry().providerId();
            }
            if (row instanceof PatternManagementSlotsRow slotsRow) {
                return slotsRow.entry().providerId();
            }
        }
        return -1L;
    }

    private void sendPatternManagementAction(PatternManagementActionPacket.Action action, long providerId, int cacheSlot) {
        sendPatternManagementAction(
                action,
                providerId,
                cacheSlot,
                resolvedPatternManagementSearchTextForRequest(),
                patternManageMappingField != null ? patternManageMappingField.getValue() : "");
    }

    private void sendPatternManagementAction(PatternManagementActionPacket.Action action, long providerId, int cacheSlot,
                                             String searchText, String mappingText) {
        String resolvedSearchText = resolveClientProviderSearchText(searchText);
        PacketDistributor.sendToServer(new PatternManagementActionPacket(
                action,
                providerId,
                cacheSlot,
                resolvedSearchText,
                mappingText == null ? "" : mappingText,
                WcwtClientConfig.patternManagementShiftQuickEnabled()));
    }

    private boolean patternManagementShortcutActive() {
        Minecraft mc = Minecraft.getInstance();
        long w = mc.getWindow().getWindow();
        return mc.options.keyShift.isDown()
                || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void preparePatternProviderSlotClick(double mouseX, double mouseY) {
        if (isToolkitExpandedInManagementArea()) {
            return;
        }
        if (patternProviders.isEmpty()) {
            return;
        }
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        PatternManagementSlotHit slotHit = getPatternManagementSlotAt(relX, relY);
        if (slotHit == null) {
            return;
        }
        selectedPatternProviderId = slotHit.entry().providerId();
        selectedPatternProviderSlot = slotHit.slot();
        updatePatternProviderSlots();
    }

    private boolean tryAeNetworkToolkitSlotDoubleDeposit(double mouseX, double mouseY) {
        var host = menu.getMenuHost();
        if (host == null || host.getCurrentExtendedUI() != IExtendedUIHost.ExtendedUIType.TOOLKIT) {
            return false;
        }
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        Slot hit = null;
        for (Slot slot : menu.getSlots(WcwtSlotSemantics.WCWT_TOOLKIT)) {
            if (!slot.isActive()) {
                continue;
            }
            if (!(slot instanceof WirelessComprehensiveWorkTerminalMenu.ToolkitSlot toolkitSlot)) {
                continue;
            }
            if (toolkitSlot.toolkitLogicalIndex() != ToolkitItemRules.NETWORK_TOOL_DEDICATED_INDEX) {
                continue;
            }
            if (relX >= slot.x && relX < slot.x + PLAYER_INVENTORY_SLOT_HIT_SIZE
                    && relY >= slot.y && relY < slot.y + PLAYER_INVENTORY_SLOT_HIT_SIZE) {
                hit = slot;
                break;
            }
        }
        if (hit == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (lastAeNetworkToolkitDoubleClickSlot != null && hit == lastAeNetworkToolkitDoubleClickSlot
                && now - lastAeNetworkToolkitDoubleClickMs <= 550L) {
            PacketDistributor.sendToServer(new ToolkitNetworkToolDepositPacket());
            playPatternManagementClickSound();
            lastAeNetworkToolkitDoubleClickSlot = null;
            lastAeNetworkToolkitDoubleClickMs = 0L;
            return true;
        }
        lastAeNetworkToolkitDoubleClickSlot = hit;
        lastAeNetworkToolkitDoubleClickMs = now;
        return false;
    }

    private boolean handleToolkitMemoryClick(double mouseX, double mouseY, int button) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        ExtendedPanelLayout.Rect buttonRect = getActiveToolkitMemoryButtonRect();
        if (button == 0 && buttonRect != null && inRect(relX, relY, buttonRect)) {
            toolkitMemoryMode = !toolkitMemoryMode;
            playPatternManagementClickSound();
            return true;
        }
        if (!toolkitMemoryMode || (button != 0 && button != 1)) {
            return false;
        }
        Slot toolkitSlot = getToolkitSlotAt(mouseX, mouseY);
        if (!(toolkitSlot instanceof WirelessComprehensiveWorkTerminalMenu.ToolkitSlot logicalSlot)) {
            return false;
        }
        int slotIndex = logicalSlot.toolkitLogicalIndex();
        if (slotIndex < ToolkitItemRules.DEDICATED_SLOT_COUNT) {
            return false;
        }
        if (button == 0 && toolkitSlot.getItem().isEmpty()) {
            return true;
        }
        PacketDistributor.sendToServer(new ToolkitMemorySlotPacket(slotIndex, button == 0));
        menu.setToolkitMemorySlot(slotIndex, button == 0);
        playPatternManagementClickSound();
        return true;
    }

    @Nullable
    private Slot getToolkitSlotAt(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        for (Slot slot : getToolkitSlots()) {
            if (!slot.isActive()) {
                continue;
            }
            if (relX >= slot.x && relX < slot.x + PLAYER_INVENTORY_SLOT_HIT_SIZE
                    && relY >= slot.y && relY < slot.y + PLAYER_INVENTORY_SLOT_HIT_SIZE) {
                return slot;
            }
        }
        return null;
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return slot.container == minecraft.player.getInventory();
    }

    @Nullable
    private Slot getPlayerInventorySlotAt(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        for (Slot slot : menu.slots) {
            if (!isPlayerInventorySlot(slot) || !slot.isActive()) {
                continue;
            }
            if (relX >= slot.x && relX < slot.x + PLAYER_INVENTORY_SLOT_HIT_SIZE
                    && relY >= slot.y && relY < slot.y + PLAYER_INVENTORY_SLOT_HIT_SIZE) {
                return slot;
            }
        }
        return null;
    }

    private boolean handleQuickPatternProviderInsert(@Nullable Slot slot) {
        if (slot == null
                || !patternManagementShortcutActive()
                || !isPlayerInventorySlot(slot)
                || !slot.hasItem()
                || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(slot.getItem())) {
            return false;
        }
        if (DEBUG_REPO) {
            WcwtMod.LOGGER.info(
                    "WCWT repo debug: quick insert trigger menuSlot={} rawSlotIndex={} item={}",
                    menu.slots.indexOf(slot), slot.index, slot.getItem());
        }
        String uploadSearchText = PatternUploadMetadata.getProviderSearchText(slot.getItem());
        if (uploadSearchText == null || uploadSearchText.isBlank()) {
            uploadSearchText = currentPatternManagementSearchText();
        }
        sendPatternManagementAction(
                PatternManagementActionPacket.Action.QUICK_INSERT_FIRST_PROVIDER,
                quickInsertTargetProviderId(),
                menu.slots.indexOf(slot),
                uploadSearchText,
                "");
        playPatternManagementItemTransferSound(true);
        return true;
    }

    private void logRepoViewState(String source, int computedColumns) {
        int repoSlots = 0;
        int visibleEntries = 0;
        int firstRowColumns = 0;
        int firstRepoRowY = Integer.MIN_VALUE;

        for (Slot slot : menu.slots) {
            if (!(slot instanceof RepoSlot)) {
                continue;
            }
            repoSlots++;
            if (slot.hasItem()) {
                visibleEntries++;
            }
            if (firstRepoRowY == Integer.MIN_VALUE) {
                firstRepoRowY = slot.y;
            }
            if (slot.y == firstRepoRowY) {
                firstRowColumns++;
            }
        }

        if (computedColumns < 0) {
            computedColumns = firstRowColumns;
        }

        boolean connected = menu.getLinkStatus().connected();
        if (computedColumns == lastDebugRepoColumns
                && repoSlots == lastDebugRepoSlots
                && visibleEntries == lastDebugRepoVisibleEntries
                && connected == lastDebugRepoConnected) {
            return;
        }

        lastDebugRepoColumns = computedColumns;
        lastDebugRepoSlots = repoSlots;
        lastDebugRepoVisibleEntries = visibleEntries;
        lastDebugRepoConnected = connected;

        WcwtMod.LOGGER.info(
                "WCWT repo debug: source={} connected={} linkStatus={} repoSlots={} firstRowColumns={} visibleEntries={}",
                source,
                connected,
                menu.getLinkStatus(),
                repoSlots,
                computedColumns,
                visibleEntries);
    }

    private PatternManagementHeaderHit getPatternManagementHeaderAt(int relX, int relY) {
        if (isToolkitExpandedInManagementArea()) {
            return null;
        }
        if (relX < patternManagementPage.left()
                || relX >= patternManagementPage.left() + patternManagementPage.width()
                || relY < patternManagementPage.top()
                || relY >= patternManagementPage.top() + patternManagementPage.height()) {
            return null;
        }
        int visibleRow = (relY - patternManagementPage.top()) / PATTERN_MANAGEMENT_ROW_H;
        int rowIndex = (patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0) + visibleRow;
        if (rowIndex < 0 || rowIndex >= patternManagementRows.size()) {
            return null;
        }
        if (patternManagementRows.get(rowIndex) instanceof PatternManagementHeaderRow header) {
            return new PatternManagementHeaderHit(header, visibleRow);
        }
        return null;
    }

    private PatternManagementHeaderButtonHit getPatternManagementHeaderButtonAt(int relX, int relY) {
        if (isToolkitExpandedInManagementArea()) {
            return null;
        }
        int scroll = patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0;
        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        for (int visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
            int rowIndex = scroll + visibleRow;
            if (rowIndex < 0 || rowIndex >= patternManagementRows.size()) {
                break;
            }
            var row = patternManagementRows.get(rowIndex);
            int rowY = patternManagementPage.top() + visibleRow * PATTERN_MANAGEMENT_ROW_H;
            if (row instanceof PatternManagementHeaderRow header) {
                if (inRect(relX, relY, rowButton(patternManagementUploadButton, rowY))) {
                    return new PatternManagementHeaderButtonHit(header.firstEntry(), PatternManagementHeaderButton.UPLOAD);
                }
                if (inRect(relX, relY, rowButton(patternManagementUiButton, rowY))) {
                    return new PatternManagementHeaderButtonHit(header.firstEntry(), PatternManagementHeaderButton.UI);
                }
                if (!patternManagementShowSlots
                        && inRect(relX, relY, rowButton(patternManagementHighlightButton, rowY))) {
                    return new PatternManagementHeaderButtonHit(header.firstEntry(), PatternManagementHeaderButton.HIGHLIGHT);
                }
            } else if (row instanceof PatternManagementSlotsRow slotsRow && slotsRow.offset() == 0
                    && inRect(relX, relY, slotRowButton(patternManagementHighlightButton, rowY))) {
                return new PatternManagementHeaderButtonHit(slotsRow.entry(), PatternManagementHeaderButton.HIGHLIGHT);
            }
        }
        return null;
    }

    private PatternManagementSlotHit getPatternManagementSlotAt(int relX, int relY) {
        var activeRect = isToolkitExpandedInManagementArea() ? managementToolkitBackgroundRect : patternManagementPage;
        if (relX < activeRect.left()
                || relX >= activeRect.left() + activeRect.width()
                || relY < activeRect.top()
                || relY >= activeRect.top() + activeRect.height()) {
            return null;
        }
        if (isToolkitExpandedInManagementArea()) {
            int columns = getManagementToolkitColumns();
            int visibleRows = getManagementToolkitVisibleRows();
            int visibleRow = (relY - managementToolkitSlotRect.top()) / ToolkitPanel.SLOT_SIZE;
            int visibleCol = (relX - managementToolkitSlotRect.left()) / ToolkitPanel.SLOT_SIZE;
            if (visibleRow < 0 || visibleRow >= visibleRows || visibleCol < 0 || visibleCol >= columns) {
                return null;
            }
            int slotX = managementToolkitSlotRect.left() + visibleCol * ToolkitPanel.SLOT_SIZE;
            int slotY = managementToolkitSlotRect.top() + visibleRow * ToolkitPanel.SLOT_SIZE;
            int hitX = patternManagementSlotHitMinX(slotX);
            int hitY = patternManagementSlotHitMinY(slotY);
            if (relX < hitX || relX >= hitX + PATTERN_MANAGEMENT_SLOT_HIT_SIZE
                    || relY < hitY || relY >= hitY + PATTERN_MANAGEMENT_SLOT_HIT_SIZE) {
                return null;
            }
            int firstSlot = (patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0) * columns;
            int slot = firstSlot + visibleRow * columns + visibleCol;
            return slot >= 0 && slot < getToolkitSlots().size()
                    ? new PatternManagementSlotHit(null, slot)
                    : null;
        }
        int visibleRow = (relY - patternManagementPage.top()) / PATTERN_MANAGEMENT_ROW_H;
        int rowIndex = (patternManagementScrollbar != null ? patternManagementScrollbar.getCurrentScroll() : 0) + visibleRow;
        if (rowIndex < 0 || rowIndex >= patternManagementRows.size()) {
            return null;
        }
        if (!(patternManagementRows.get(rowIndex) instanceof PatternManagementSlotsRow row)) {
            return null;
        }
        int relSlotX = relX - patternManagementPage.left();
        int col = relSlotX / PATTERN_MANAGEMENT_SLOT_STEP;
        int slotX = patternManagementPage.left() + col * PATTERN_MANAGEMENT_SLOT_STEP;
        int slotY = patternManagementPage.top() + visibleRow * PATTERN_MANAGEMENT_ROW_H
                + PATTERN_MANAGEMENT_SLOT_Y_OFFSET;
        int hitX = patternManagementSlotHitMinX(slotX);
        int hitY = patternManagementSlotHitMinY(slotY);
        if (col < 0 || col >= row.slots()
                || relX < hitX || relX >= hitX + PATTERN_MANAGEMENT_SLOT_HIT_SIZE
                || relY < hitY || relY >= hitY + PATTERN_MANAGEMENT_SLOT_HIT_SIZE) {
            return null;
        }
        int slot = row.offset() + col;
        return new PatternManagementSlotHit(row.entry(), slot);
    }

    private static boolean inRect(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static boolean inRect(int x, int y, ExtendedPanelLayout.Rect rect) {
        return inRect(x, y, rect.left(), rect.top(), rect.width(), rect.height());
    }

    private void highlightPatternProvider(PatternProviderListPacket.Entry entry) {
        if (entry.pos() == null || entry.dimension() == null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.wcwt.pattern_management.highlight_missing"), false);
            return;
        }
        try {
            var handler = Class.forName("com.glodblock.github.extendedae.client.render.EAEHighlightHandler");
            long until = System.currentTimeMillis() + 3000;
            if (entry.face() == null) {
                handler.getMethod("highlight", net.minecraft.core.BlockPos.class,
                                net.minecraft.resources.ResourceKey.class, long.class)
                        .invoke(null, entry.pos(), entry.dimension(), until);
            } else {
                handler.getMethod("highlight", net.minecraft.core.BlockPos.class, net.minecraft.core.Direction.class,
                                net.minecraft.resources.ResourceKey.class, long.class, AABB.class)
                        .invoke(null, entry.pos(), entry.face(), entry.dimension(), until, new AABB(entry.pos()));
            }
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("extendedae_plus.message.provider.selected", entry.providerId()), true);
            }
            displayPatternProviderHighlightMessage(entry);
        } catch (Throwable error) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.wcwt.pattern_management.highlight_failed"), false);
        }
    }

    private void displayPatternProviderHighlightMessage(PatternProviderListPacket.Entry entry) {
        var player = Minecraft.getInstance().player;
        if (player == null || entry.pos() == null || entry.dimension() == null) {
            return;
        }
        try {
            var messageUtil = Class.forName("com.glodblock.github.extendedae.util.MessageUtil");
            var message = messageUtil.getMethod("createEnhancedHighlightMessage",
                            net.minecraft.world.entity.player.Player.class,
                            net.minecraft.core.BlockPos.class,
                            net.minecraft.resources.ResourceKey.class,
                            String.class)
                    .invoke(null, player, entry.pos(), entry.dimension(), "chat.ex_pattern_access_terminal.pos");
            if (message instanceof Component component) {
                player.displayClientMessage(component, false);
                return;
            }
        } catch (Throwable ignored) {
        }
        player.displayClientMessage(Component.translatable("chat.ex_pattern_access_terminal.pos",
                Component.literal(entry.pos().toShortString()),
                Component.literal(entry.dimension().location().toString()),
                (int) Math.sqrt(player.blockPosition().distSqr(entry.pos()))), false);
    }

    private void openPatternProviderUi(PatternProviderListPacket.Entry entry) {
        if (entry.pos() == null || entry.dimension() == null) {
            return;
        }
        try {
            var packetClass = Class.forName("com.extendedae_plus.network.OpenProviderUiC2SPacket");
            var ctor = packetClass.getConstructor(long.class, ResourceLocation.class, int.class);
            int face = entry.face() != null ? entry.face().ordinal() : -1;
            Object packet = ctor.newInstance(entry.pos().asLong(), entry.dimension().location(), face);
            if (packet instanceof net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
                PacketDistributor.sendToServer(payload);
            }
        } catch (Throwable ignored) {
        }
    }

    private WirelessComprehensiveWorkTerminalMenu.WcwtCurioSlot getCurioToggleSlotAt(double mouseX, double mouseY) {
        if (curiosPanel == null || !curiosPanel.isVisible()) {
            return null;
        }
        for (var slot : getCurioSlots()) {
            if (slot instanceof WirelessComprehensiveWorkTerminalMenu.WcwtCurioSlot curioSlot
                    && curioSlot.canToggleRendering()
                    && slot.x != HIDDEN_SLOT_POS.getX()
                    && slot.y != HIDDEN_SLOT_POS.getY()) {
                int toggleX = leftPos + slot.x + 12;
                int toggleY = topPos + slot.y - 1;
                if (mouseX >= toggleX && mouseX < toggleX + 8 && mouseY >= toggleY && mouseY < toggleY + 8) {
                    return curioSlot;
                }
            }
        }
        return null;
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderCosmeticArmorToggleOverlay(guiGraphics);
        ItemStack stonecuttingStack = getStonecuttingResultUnderMouse(mouseX, mouseY);
        if (!stonecuttingStack.isEmpty()) {
            guiGraphics.renderTooltip(font, stonecuttingStack, mouseX, mouseY);
            return;
        }

        var toolkitMemoryTooltip = getToolkitMemoryTooltip(mouseX, mouseY);
        if (toolkitMemoryTooltip != null) {
            guiGraphics.renderTooltip(font, toolkitMemoryTooltip, mouseX, mouseY);
            return;
        }

        ItemStack patternManagementStack = getPatternManagementStackUnderMouse(mouseX, mouseY);
        if (!patternManagementStack.isEmpty()) {
            guiGraphics.renderTooltip(font, patternManagementStack, mouseX, mouseY);
            return;
        }

        var patternTooltip = getPatternManagementTooltip(mouseX, mouseY);
        if (patternTooltip != null) {
            guiGraphics.renderTooltip(font, patternTooltip, mouseX, mouseY);
            return;
        }

        var curioToggleSlot = getCurioToggleSlotAt(mouseX, mouseY);
        if (curioToggleSlot != null) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.curios.toggle"), mouseX, mouseY);
            return;
        }

        if (curiosPanel != null
                && curiosPanel.isVisible()
                && hoveredSlot instanceof WirelessComprehensiveWorkTerminalMenu.WcwtCurioSlot curioSlot
                && getMenu().getCarried().isEmpty()
                && hoveredSlot.getItem().isEmpty()) {
            guiGraphics.renderTooltip(font, Component.literal(getCurioSlotName(curioSlot)), mouseX, mouseY);
            return;
        }

        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Nullable
    private Component getToolkitMemoryTooltip(int mouseX, int mouseY) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        var buttonRect = getActiveToolkitMemoryButtonRect();
        if (buttonRect != null && inRect(relX, relY, buttonRect)) {
            return Component.translatable(toolkitMemoryMode
                    ? "gui.wcwt.toolkit.memory_mode.enabled"
                    : "gui.wcwt.toolkit.memory_mode.disabled");
        }
        if (!toolkitMemoryMode) {
            return null;
        }
        Slot toolkitSlot = getToolkitSlotAt(mouseX, mouseY);
        if (!(toolkitSlot instanceof WirelessComprehensiveWorkTerminalMenu.ToolkitSlot logicalSlot)
                || logicalSlot.toolkitLogicalIndex() < ToolkitItemRules.DEDICATED_SLOT_COUNT) {
            return null;
        }
        return toolkitSlot.getItem().isEmpty()
                ? Component.translatable("gui.wcwt.toolkit.memory_slot.empty")
                : Component.translatable("gui.wcwt.toolkit.memory_slot.set");
    }

    private ItemStack getStonecuttingResultUnderMouse(int mouseX, int mouseY) {
        var recipe = getStonecuttingRecipeAt(mouseX - leftPos, mouseY - topPos);
        if (recipe == null || Minecraft.getInstance().level == null) {
            return ItemStack.EMPTY;
        }
        return recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess()).copy();
    }

    private ItemStack getPatternManagementStackUnderMouse(int mouseX, int mouseY) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        var hit = getPatternManagementSlotAt(relX, relY);
        if (hit == null) {
            return ItemStack.EMPTY;
        }
        if (isToolkitExpandedInManagementArea()) {
            var toolkitSlots = getToolkitSlots();
            return hit.slot() >= 0 && hit.slot() < toolkitSlots.size()
                    ? toolkitSlots.get(hit.slot()).getItem()
                    : ItemStack.EMPTY;
        }
        return hit.entry().slots().getOrDefault(hit.slot(), ItemStack.EMPTY);
    }

    private Component getPatternManagementTooltip(int mouseX, int mouseY) {
        if (isToolkitExpandedInManagementArea()) {
            return null;
        }
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        if (inRect(relX, relY, patternManagementAddButton)) {
            return Component.translatable("gui.wcwt.pattern_management.add_mapping.tooltip");
        }
        if (inRect(relX, relY, patternManagementReloadButton)) {
            return Component.translatable("gui.wcwt.pattern_management.reload_mapping.tooltip");
        }
        if (inRect(relX, relY, patternManagementDeleteButton)) {
            return Component.translatable("gui.wcwt.pattern_management.delete_mapping.tooltip");
        }
        if (inRect(relX, relY, patternManagementCancelButton)) {
            return Component.translatable("gui.wcwt.pattern_management.cancel.tooltip");
        }
        if (inRect(relX, relY, patternManagementDisplayModeButton)) {
            return Component.translatable("gui.wcwt.pattern_management.display_mode.tooltip",
                    Component.translatable(patternManagementDisplayMode.translationKey()));
        }
        if (inRect(relX, relY, patternManagementDisplaySlotsButton)) {
            return Component.translatable(patternManagementShowSlots
                    ? "gui.expatternprovider.hide_slots"
                    : "gui.expatternprovider.show_slots");
        }
        if (inRect(relX, relY, patternManagementAutoUploadButton)) {
            return Component.translatable(patternManagementUploadEnabled
                    ? "gui.wcwt.pattern_management.auto_upload.enabled"
                    : "gui.wcwt.pattern_management.auto_upload.disabled");
        }
        if (inRect(relX, relY, patternManagementSearchModeButton)) {
            return Component.translatable("gui.wcwt.pattern_management.search_mode.tooltip",
                    Component.translatable(patternManagementSearchMode.translationKey()));
        }
        if (inRect(relX, relY, batchItemReplacementButton)) {
            return Component.translatable(batchItemSubstitutions
                    ? "gui.tooltips.ae2.SubstitutionsOn"
                    : "gui.tooltips.ae2.SubstitutionsOff");
        }
        if (inRect(relX, relY, batchFluidReplacementButton)) {
            return Component.translatable(batchFluidSubstitutions
                    ? "gui.tooltips.ae2.SubstitutionsOn"
                    : "gui.tooltips.ae2.SubstitutionsOff");
        }
        var buttonHit = getPatternManagementHeaderButtonAt(relX, relY);
        if (buttonHit != null) {
            return switch (buttonHit.button()) {
                case UPLOAD -> Component.translatable("gui.wcwt.pattern_management.upload.tooltip");
                case UI -> Component.translatable("extendedae_plus.tooltip.provider.open_ui");
                case HIGHLIGHT -> Component.translatable("gui.extendedae.ex_pattern_access_terminal.tooltip.03");
            };
        }
        var hit = getPatternManagementHeaderAt(relX, relY);
        if (hit == null) {
            return null;
        }
        return hit.header().displayName();
    }

    private boolean isToolkitExpandedInManagementArea() {
        return WcwtClientConfig.expandToolkitInManagementArea()
                && menu.getMenuHost() != null
                && menu.getMenuHost().getCurrentExtendedUI() == IExtendedUIHost.ExtendedUIType.TOOLKIT;
    }

    private void rememberManagementToolkitOpenState(boolean open) {
        if (!WcwtClientConfig.expandToolkitInManagementArea()) {
            return;
        }
        WcwtClientConfig.setLastManagementToolkitOpen(open);
    }

    private void restoreManagementToolkitOpenStateIfNeeded() {
        if (attemptedRestoreManagementToolkitOpenState || !WcwtClientConfig.expandToolkitInManagementArea()) {
            return;
        }
        attemptedRestoreManagementToolkitOpenState = true;
        var host = menu.getMenuHost();
        if (host == null || !WcwtClientConfig.lastManagementToolkitOpen()) {
            return;
        }
        if (menu.getSyncedExtendedUIType() != IExtendedUIHost.ExtendedUIType.NONE) {
            return;
        }
        host.setCurrentExtendedUI(IExtendedUIHost.ExtendedUIType.TOOLKIT);
        PacketDistributor.sendToServer(new ExtendedUIPacket(IExtendedUIHost.ExtendedUIType.TOOLKIT));
        updateExtendedUIVisibility();
    }

    private int getManagementToolkitColumns() {
        return 9;
    }

    private int getManagementToolkitVisibleRows() {
        return Math.max(1, managementToolkitBackgroundRect.height() / ToolkitPanel.SLOT_SIZE);
    }

    private static String getCurioSlotName(WirelessComprehensiveWorkTerminalMenu.WcwtCurioSlot slot) {
        String key = "curios.identifier." + slot.getIdentifier();
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        String identifier = slot.getIdentifier();
        return identifier.isEmpty()
                ? ""
                : Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1).toLowerCase();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible()) {
            if (advancedCodingPanel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible()
                && resonatingLightningPatternCodingPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible()
                && advancedCodingPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible()
                && resonatingLightningPatternCodingPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible()
                && advancedCodingPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible()
                && resonatingLightningPatternCodingPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (scrollY != 0 && patternCacheScrollbar != null && patternCacheScrollbar.isVisible()
                && isMouseOverPatternCache(mouseX, mouseY)
                && patternCacheScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            return true;
        }
        if (scrollY != 0 && patternEncodingScrollbar != null && patternEncodingScrollbar.isVisible()
                && isMouseOverPatternEncoding(mouseX, mouseY)
                && patternEncodingScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            return true;
        }
        if (scrollY != 0 && stonecuttingPatternScrollbar != null && stonecuttingPatternScrollbar.isVisible()
                && isMouseOverPatternEncoding(mouseX, mouseY)
                && stonecuttingPatternScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            return true;
        }
        if (scrollY != 0 && patternManagementScrollbar != null && patternManagementScrollbar.isVisible()
                && isMouseOverPatternManagement(mouseX, mouseY)
                && patternManagementScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            patternProviderSlotLayoutDirty = true;
            panelSlotActivityDirty = true;
            return true;
        }
        if (scrollY != 0 && curiosScrollbar != null && curiosScrollbar.isVisible()
                && isMouseOverCuriosPanel(mouseX, mouseY)
                && curiosScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            panelSlotActivityDirty = true;
            return true;
        }
        if (scrollY != 0 && toolkitScrollbar != null && toolkitScrollbar.isVisible()
                && isMouseOverToolkitPanel(mouseX, mouseY)
                && toolkitScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            panelSlotActivityDirty = true;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseOverPatternEncoding(double mouseX, double mouseY) {
        var inputBase = mainLayout.slot("PROCESSING_INPUTS",
                new ExtendedPanelLayout.Rect(185, imageHeight - 213, 18, 18), imageWidth, imageHeight);
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        int x = inputBase.left() + PATTERN_ENCODING_BG_X_OFFSET_FROM_INPUT;
        int y = inputBase.top() + PATTERN_ENCODING_BG_Y_OFFSET_FROM_INPUT;
        return relX >= x && relX < x + PATTERN_ENCODING_BG_WIDTH
                && relY >= y && relY < y + PATTERN_ENCODING_BG_HEIGHT;
    }

    private boolean isMouseOverPatternManagement(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        var activeRect = isToolkitExpandedInManagementArea() ? managementToolkitBackgroundRect : patternManagementPage;
        int extraRight = isToolkitExpandedInManagementArea() ? 0 : 16;
        return relX >= activeRect.left()
                && relX < activeRect.left() + activeRect.width() + extraRight
                && relY >= activeRect.top()
                && relY < activeRect.top() + activeRect.height();
    }
    
    @Override
    public void onClose() {
        super.onClose();
    }

    private sealed interface PatternManagementRow permits PatternManagementHeaderRow, PatternManagementSlotsRow {
    }

    private record PatternManagementHeaderRow(PatternContainerGroup group,
                                              List<PatternProviderListPacket.Entry> entries)
            implements PatternManagementRow {
        private PatternProviderListPacket.Entry firstEntry() {
            return entries.get(0);
        }

        private Component displayName() {
            String mappedName = firstEntry().mappedDisplayName();
            if (mappedName != null && !mappedName.isBlank()) {
                return Component.literal(mappedName);
            }
            return group.name();
        }

        private boolean containsProvider(long providerId) {
            return entries.stream().anyMatch(entry -> entry.providerId() == providerId);
        }
    }

    private record PatternManagementSlotsRow(PatternProviderListPacket.Entry entry, int offset, int slots)
            implements PatternManagementRow {
    }

    private record PatternManagementHeaderHit(PatternManagementHeaderRow header, int visibleRow) {
    }

    private enum PatternManagementHeaderButton {
        UPLOAD,
        UI,
        HIGHLIGHT
    }

    private record PatternManagementHeaderButtonHit(PatternProviderListPacket.Entry entry,
                                                    PatternManagementHeaderButton button) {
    }

    private record PatternManagementSlotHit(PatternProviderListPacket.Entry entry, int slot) {
    }

    private record PatternManagementSlotKey(long providerId, int slot) {
    }

    private enum PatternManagementDisplayMode {
        ALL(112, 80, "gui.wcwt.pattern_management.display_mode.all"),
        VISIBLE(96, 80, "gui.wcwt.pattern_management.display_mode.visible"),
        NOT_FULL(128, 80, "gui.wcwt.pattern_management.display_mode.not_full");

        private final int iconU;
        private final int iconV;
        private final String translationKey;

        PatternManagementDisplayMode(int iconU, int iconV, String translationKey) {
            this.iconU = iconU;
            this.iconV = iconV;
            this.translationKey = translationKey;
        }

        int iconU() {
            return iconU;
        }

        int iconV() {
            return iconV;
        }

        String translationKey() {
            return translationKey;
        }

        PatternManagementDisplayMode next() {
            var values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum PatternManagementSearchMode {
        OUT(16, 48, "gui.extendedae.ex_pattern_access_terminal.search_mode.01"),
        IN(0, 48, "gui.extendedae.ex_pattern_access_terminal.search_mode.02"),
        IN_OUT(32, 48, "gui.extendedae.ex_pattern_access_terminal.search_mode.03");

        private final int iconU;
        private final int iconV;
        private final String translationKey;

        PatternManagementSearchMode(int iconU, int iconV, String translationKey) {
            this.iconU = iconU;
            this.iconV = iconV;
            this.translationKey = translationKey;
        }

        int iconU() {
            return iconU;
        }

        int iconV() {
            return iconV;
        }

        String translationKey() {
            return translationKey;
        }

        boolean searchInputs() {
            return this == IN || this == IN_OUT;
        }

        boolean searchOutputs() {
            return this == OUT || this == IN_OUT;
        }

        PatternManagementSearchMode next() {
            var values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
