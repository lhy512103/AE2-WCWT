package com.lhy.wcwt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.me.items.CraftingTermScreen;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.NumberEntryType;
import appeng.core.sync.BasePacket;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.slot.AppEngSlot;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.Point;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import com.lhy.wcwt.init.ModComponents;
import appeng.integration.abstraction.ItemListMod;
import appeng.menu.SlotSemantics;
import appeng.api.stacks.AEKeyType;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.compat.JecSearchCompat;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.client.WcwtKeybindings;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.helpers.ToolkitItemRules;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import com.lhy.wcwt.client.gui.panels.*;
import com.lhy.wcwt.client.gui.widgets.*;
import com.lhy.wcwt.client.gui.widgets.IconButton;
import com.lhy.wcwt.client.gui.widgets.WcwtScrollingUpgradesPanel;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.CraftingLockPacket;
import com.lhy.wcwt.network.EncodePatternPacket;
import com.lhy.wcwt.network.ExtendedUIPacket;
import com.lhy.wcwt.network.ManualAnvilNamePacket;
import com.lhy.wcwt.network.ManualWorkspaceModePacket;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.OpenToolkitHotkeyPacket;
import com.lhy.wcwt.network.PatternMultiplierPacket;
import com.lhy.wcwt.network.PatternModePacket;
import com.lhy.wcwt.network.PatternManagementActionPacket;
import com.lhy.wcwt.network.PatternManagementUploadSettingPacket;
import com.lhy.wcwt.network.PatternProviderListPacket;
import com.lhy.wcwt.network.PatternSelectionPacket;
import com.lhy.wcwt.network.PatternEncodingOptionPacket;
import com.lhy.wcwt.network.StonecuttingRecipeSelectionPacket;
import com.lhy.wcwt.network.ToolkitNetworkToolDepositPacket;
import com.lhy.wcwt.network.CycleProcessingOutputPacket;
import com.lhy.wcwt.network.WirelessSettingsPacket;
import com.lhy.wcwt.util.PatternUploadMetadata;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu.WcwtActivatableSlot;
import com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import com.lhy.wcwt.compat.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.fml.ModList;
import appeng.parts.encoding.EncodingMode;
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
import com.google.common.primitives.Longs;

/**
 * 无线综合工作终端界面
 * 集成了多个AE2附属模组的功能
 */
public class WirelessComprehensiveWorkTerminalScreen extends CraftingTermScreen<WirelessComprehensiveWorkTerminalMenu> {
    private static final String STYLE_PATH = "/screens/wcwt/wireless_comprehensive_work_terminal.json";
    private static final ResourceLocation MAIN_LAYOUT_ID = com.lhy.wcwt.util.ResourceLocationCompat.id("ae2",
            "screens/wcwt/wireless_comprehensive_work_terminal.json");
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");
    private static final long PERF_LOG_THRESHOLD_NS = 1_000_000L;
    private static final long PATTERN_PROVIDER_REFRESH_DEBOUNCE_MS = 180L;
    private static final long PATTERN_PROVIDER_SUBSCRIPTION_KEEPALIVE_MS = 3_000L;
    private boolean debugLoggedFirstUpdateBeforeRender;
    private int wcwtStepLogFrames;

    private void wcwtStep(String name, Runnable step) {
        boolean verbose = wcwtStepLogFrames < 3;
        if (verbose) {
            WcwtMod.LOGGER.info("WCWT debug: step {} begin", name);
        }
        long t0 = System.nanoTime();
        step.run();
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        if (verbose || ms >= 20) {
            WcwtMod.LOGGER.info("WCWT debug: step {} end took {} ms", name, ms);
        }
    }
    
    // 扩展UI按钮
    private ExtendedUIButton advancedCodingButton;
    private ExtendedUIButton cosmeticArmorButton;
    private ExtendedUIButton curiosButton;
    private ExtendedUIButton toolboxButton;
    private ExtendedUIButton toolkitButton;
    private ExtendedUIButton resonatingLightningPatternCodingButton;

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
    private WcwtScrollingUpgradesPanel upgradesPanel;
    /** AE2 原版 VIEW_CELL 面板：替换为 WCWT 自定义皮肤。 */
    private WcwtViewCellsPanel viewCellsPanel;
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
    private final List<PatternProviderListPacket.Entry> patternProviders = new ArrayList<>();
    private final List<PatternManagementRow> patternManagementRows = new ArrayList<>();
    private long selectedPatternProviderId = -1;
    private int selectedPatternProviderSlot = -1;
    private long focusedPatternProviderId = -1;
    private int focusedPatternProviderSlot = -1;
    private long focusedPatternProviderUntilMs;
    private boolean requestedPatternProviders;
    private long lastPatternProviderRefreshRequestMs;
    private long lastPatternProviderSubscriptionRequestMs;
    private ExtendedPanelLayout mainLayout = ExtendedPanelLayout.load(MAIN_LAYOUT_ID);
    private ExtendedPanelLayout.Rect patternManagementPage =
            new ExtendedPanelLayout.Rect(176, 210, 160, 71);
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

    private static final int PATTERN_CACHE_COLS = 9;
    private static final int PATTERN_CACHE_VISIBLE_ROWS = 2;
    private static final int PATTERN_CACHE_SLOT_X = 176;
    private static final int PATTERN_CACHE_SLOT_BOTTOM = 142;
    /** 相对样板缓存区背景的纵向微调（负数 = 整体上移）；槽位绘制与 {@link #isMouseOverPatternCache} 共用。 */
    private static final int PATTERN_CACHE_SLOT_Y_OFFSET = -1;
    private static final int PATTERN_CACHE_SCROLLBAR_HEIGHT = 34;
    private static final int PATTERN_MANAGEMENT_ROW_H = 17;
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
    private static final int BUTTON_PRESS_OFFSET_Y = 1;
    private static final int ANVIL_TOO_EXPENSIVE_COST = 40;
    private static final long FOCUSED_PATTERN_FLASH_PERIOD_MS = 480L;
    private static final boolean DEBUG_REPO = Boolean.getBoolean("wcwt.debug.repo");
    private static final int PLAYER_INVENTORY_SLOT_HIT_SIZE = 18;
    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);
    private static final ResourceLocation CURIOS_INVENTORY_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("curios", "textures/gui/inventory.png");
    private static final ResourceLocation EAE_ICONS_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("extendedae", "textures/guis/nicons.png");
    private static final ResourceLocation AE2_STATES_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/states.png");
    private static final ResourceLocation AE2_CHECKBOX_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/checkbox.png");
    private static final ResourceLocation AAE_STATES_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("advanced_ae", "textures/guis/states.png");
    private static final ResourceLocation WCWT_GUI_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wireless_comprehensive_work_terminal_gui.png");
    private static final ResourceLocation WCWT_EXTENDED_CRAFTING_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_extended_crafting.png");
    private static final ResourceLocation WCWT_MANAGEMENT_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_management.png");
    private static final ResourceLocation WCWT_STATES_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_states.png");
    private static final int SELECTED_PATTERN_BG_U = 32;
    private static final int SELECTED_PATTERN_BG_V = 128;
    private static final int SELECTED_PATTERN_BG_SIZE = 18;
    private static final ResourceLocation AE2_PATTERN_MODES_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/pattern_modes.png");
    private static final ResourceLocation WTLIB_ICONS_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/wtlib/guis/icons.png");
    private static final int AE2_RADIO_UNCHECKED_U = 28;
    private static final int AE2_RADIO_UNCHECKED_V = 0;
    private static final int AE2_RADIO_UNCHECKED_FOCUS_U = 42;
    private static final int AE2_RADIO_UNCHECKED_FOCUS_V = 0;
    private static final int AE2_RADIO_CHECKED_FOCUS_U = 42;
    private static final int AE2_RADIO_CHECKED_FOCUS_V = 14;
    private static final int AE2_RADIO_SIZE = 14;
    private static final int PATTERN_ENCODING_BG_WIDTH = 124;
    private static final int PATTERN_ENCODING_BG_HEIGHT = 66;
    private static final int STONECUTTING_RESULT_COLS = 4;
    private static final int STONECUTTING_RESULT_ROWS = 3;
    private static final int STONECUTTING_RESULT_SLOT_W = 16;
    private static final int STONECUTTING_RESULT_SLOT_H = 18;
    private static final int STONECUTTING_RESULT_SRC_X = 126;
    private static final int STONECUTTING_RESULT_SRC_Y = 141;
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
    private ItemStack lastEncodedPatternForUploadSync = ItemStack.EMPTY;
    private @Nullable String lastEncodedPatternUploadSearchText;
    
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
        ensureVerticalToolbarPosition();
        WcwtMod.LOGGER.info("WCWT debug: screen ctor begin player={}, hostPresent={}, title={}",
                playerInventory.player.getScoreboardName(),
                menu.getMenuHost() != null,
                title.getString());
        hookRepoUpdateListener();
        widgets.add("player", new PlayerEntityWidget(Objects.requireNonNull(Minecraft.getInstance().player)));
        var viewCellSlots = new ArrayList<>(menu.getSlots(SlotSemantics.VIEW_CELL));
        if (!viewCellSlots.isEmpty()) {
            viewCellsPanel = new WcwtViewCellsPanel(viewCellSlots, widgets,
                    () -> List.of(GuiText.TerminalViewCellsTooltip.text()));
            replaceCompositeWidget("viewCells", viewCellsPanel, style);
        }

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
            upgradesPanel = new WcwtScrollingUpgradesPanel(allUpgradeSlots, host, widgets, host::getUpgrades);
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
        patternManageSearchField.setResponder(str -> {
            rebuildPatternManagementRows();
            refreshPatternProviders();
        });
        patternManageSearchField.setMaxLength(64);

        patternManageMappingField = widgets.addTextField("manage_mapping");
        patternManageMappingField.setPlaceholder(Component.translatable("gui.wcwt.pattern_management.mapping_input"));
        patternManageMappingField.setMaxLength(64);
        patternManagementPage = mainLayout.widget("management_page", patternManagementPage, imageWidth, imageHeight);
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
        rebuildCraftableIndicatorCache();
        WcwtMod.LOGGER.info("WCWT debug: screen ctor end player={}, hostPresent={}, repoEntriesKnown={}",
                playerInventory.player.getScoreboardName(),
                host != null,
                craftableIndicatorKeys.size());

        // 以下所有 widgets.add() 必须在构造函数中完成，确保 populateScreen() 在 init() 中运行时
        // compositeWidgets 已有内容，才能被正确定位显示。（AE2 的约定：composite widget 在构造函数注册）
        if (host != null) {
            craftingLockButton = new CraftingLockButton(host, btn -> {
                host.toggleCraftingGridLock();
                ModNetworking.sendToServer(new CraftingLockPacket(host.isCraftingGridLocked()));
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
                ModNetworking.sendToServer(new ManualAnvilNamePacket(value));
            }
        });
        manualAnvilNameField.setFocused(false);
        widgets.add("manual_anvil_name", manualAnvilNameField);

        encodePatternButton = new ActionButton(appeng.api.config.ActionItems.ENCODE, this::handleEncodePatternButton);
        encodePatternButton.setMessage(Component.translatable("gui.tooltips.ae2.Encode"));
        widgets.add("wcwtEncodePattern", encodePatternButton);

        clearPatternEncodingButton = new ActionButton(appeng.api.config.ActionItems.CLOSE, () -> {
            menu.clearPatternEncoding();
            ModNetworking.sendToServer(new PatternEncodingOptionPacket(
                    PatternEncodingOptionPacket.ACTION_CLEAR, false));
        });
        clearPatternEncodingButton.setHalfSize(true);
        widgets.add("wcwtPatternClearPattern", clearPatternEncodingButton);

        processingMaterialsMergeButton = new WcwtProcessingMaterialsMergeButton(
                Component.translatable("gui.wcwt.processing_merge.on"),
                Component.translatable("gui.wcwt.processing_merge.desc.on"),
                Component.translatable("gui.wcwt.processing_merge.off"),
                Component.translatable("gui.wcwt.processing_merge.desc.off"),
                btn -> ModNetworking.sendToServer(new PatternEncodingOptionPacket(
                        PatternEncodingOptionPacket.ACTION_PROCESSING_MERGE_MATERIALS,
                        !menu.isProcessingMaterialsMerge())));
        widgets.add("wcwtPatternMergeMaterials", processingMaterialsMergeButton);

        patternSubstitutionButton = new ToggleButton(
                Icon.SUBSTITUTION_ENABLED,
                Icon.SUBSTITUTION_DISABLED,
                state -> {
                    menu.setPatternSubstitute(state);
                    ModNetworking.sendToServer(new PatternEncodingOptionPacket(
                            PatternEncodingOptionPacket.ACTION_SUBSTITUTE, state));
                });
        patternSubstitutionButton.setHalfSize(true);
        patternSubstitutionButton.setTooltipOn(List.of(
                ButtonToolTips.SubstitutionsOn.text(),
                ButtonToolTips.SubstitutionsDescEnabled.text()));
        patternSubstitutionButton.setTooltipOff(List.of(
                ButtonToolTips.SubstitutionsOff.text(),
                ButtonToolTips.SubstitutionsDescDisabled.text()));
        widgets.add("wcwtPatternSubstitutions", patternSubstitutionButton);

        patternFluidSubstitutionButton = new ToggleButton(
                Icon.FLUID_SUBSTITUTION_ENABLED,
                Icon.FLUID_SUBSTITUTION_DISABLED,
                state -> {
                    menu.setPatternFluidSubstitute(state);
                    ModNetworking.sendToServer(new PatternEncodingOptionPacket(
                            PatternEncodingOptionPacket.ACTION_FLUID_SUBSTITUTE, state));
                });
        patternFluidSubstitutionButton.setHalfSize(true);
        patternFluidSubstitutionButton.setTooltipOn(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescEnabled.text()));
        patternFluidSubstitutionButton.setTooltipOff(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescDisabled.text()));
        widgets.add("wcwtPatternFluidSubstitutions", patternFluidSubstitutionButton);

        cycleProcessingOutputButton = new ActionButton(appeng.api.config.ActionItems.CYCLE_PROCESSING_OUTPUT,
                () -> {
                    menu.cycleProcessingOutput();
                    ModNetworking.sendToServer(new CycleProcessingOutputPacket());
                });
        cycleProcessingOutputButton.setHalfSize(true);
        widgets.add("processingCycleOutput", cycleProcessingOutputButton);

        // 样板模式 tab 按钮（合成 / 处理 / 锻造台 / 切石机）
        tabCrafting = new TabButton(
                Items.CRAFTING_TABLE.getDefaultInstance(),
                Component.translatable("gui.ae2.CraftingPattern"),
                btn -> setPatternEncodingMode(EncodingMode.CRAFTING));
        tabCrafting.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton0", tabCrafting);
        // JSON: modeTabButton0 bottom:231（最大）→ 距底最远 → **最顶**的样板按钮
        topModeTabButton = tabCrafting;

        tabProcessing = new TabButton(
                Items.FURNACE.getDefaultInstance(),
                Component.translatable("gui.ae2.ProcessingPattern"),
                btn -> setPatternEncodingMode(EncodingMode.PROCESSING));
        tabProcessing.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton1", tabProcessing);

        tabSmithing = new TabButton(
                Items.SMITHING_TABLE.getDefaultInstance(),
                Component.translatable("gui.ae2.SmithingTablePattern"),
                btn -> setPatternEncodingMode(EncodingMode.SMITHING_TABLE));
        tabSmithing.setStyle(appeng.client.gui.widgets.TabButton.Style.HORIZONTAL);
        widgets.add("modeTabButton2", tabSmithing);

        tabStonecutting = new TabButton(
                Items.STONECUTTER.getDefaultInstance(),
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

        WcwtWirelessTerminalSettingsSubScreen(WirelessComprehensiveWorkTerminalScreen parent) {
            super(parent, "/screens/wcwt/wireless_terminal_settings.json");
            widgets.add("back", new TabButton(Icon.ARROW_LEFT, getMenu().getHost().getMainMenuIcon().getHoverName(),
                    btn -> returnToParent()));

            ItemStack stack = stack();
            pickBlock.setSelected(readTerminalBoolean(stack, "pick_block"));
            craftIfMissing.setSelected(readTerminalBoolean(stack, "craft_if_missing"));
            craftIfMissing.active = pickBlock.isSelected();
            restock.setSelected(readTerminalBoolean(stack, "restock"));
            magnet.setSelected(readMagnetSetting(stack, "magnet"));
            pickupToME.setSelected(readMagnetSetting(stack, "pickupToME"));
            patternUploadFailFallbackToEditor.setSelected(WcwtClientConfig.patternUploadFailFallbackToEditor());
            patternMultiplierApplyToEditorProcessing
                    .setSelected(WcwtClientConfig.patternMultiplierApplyToEditorProcessing());
            autoSwitchManualWorkspaceOnRecipeTransfer
                    .setSelected(WcwtClientConfig.autoSwitchManualWorkspaceOnRecipeTransfer());
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
            ModNetworking.sendToServer(new WirelessSettingsPacket(
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
            WcwtClientConfig.SPEC.save();
        }

        private static boolean readMagnetSetting(ItemStack stack, String methodName) {
            return switch (methodName) {
                case "magnet" -> readTerminalBoolean(stack, "magnet");
                case "pickupToME" -> readTerminalBoolean(stack, "pickup_to_me");
                default -> false;
            };
        }

        private static boolean readTerminalBoolean(ItemStack stack, String key) {
            var tag = stack.getTag();
            if (tag == null || !tag.contains(ModComponents.ROOT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                return false;
            }
            return tag.getCompound(ModComponents.ROOT_TAG).getBoolean(key);
        }
    }

    private void ensureVerticalToolbarPosition() {
        try {
            Field field = appeng.client.gui.AEBaseScreen.class.getDeclaredField("verticalToolbar");
            field.setAccessible(true);
            Object toolbar = field.get(this);
            if (toolbar instanceof appeng.client.gui.widgets.VerticalButtonBar verticalToolbar) {
                verticalToolbar.setPosition(new Point(3, 1));
            }
        } catch (RuntimeException | ReflectiveOperationException e) {
            WcwtMod.LOGGER.warn("WCWT: failed to apply vertical toolbar fallback position", e);
        }
    }

    @Override
    public void init() {
        super.init();
        WcwtMod.LOGGER.info("WCWT debug: screen init begin player={}, hostPresent={}, image={}x{}",
                menu.getPlayerInventory().player.getScoreboardName(),
                menu.getMenuHost() != null,
                imageWidth,
                imageHeight);
        rebuildCraftableIndicatorCache();
        syncRepoRowSize();
        clearCraftingGridButton = resolveWidgetById("clearCraftingGrid");
        clearToPlayerInvButton = resolveWidgetById("clearToPlayerInv");
        updatePlayerWidgetPosition();
        // 升级槽 maxRows 与终端风格联动（参考 WTLib WCT/WAT/WET 的标准做法）。
        // 切换"小/中/大终端"会让 ME 网格行数变化，从而影响升级槽行数。
        if (upgradesPanel != null) {
            upgradesPanel.setMaxRows(Math.max(2, getRepoRowCount()));
            refreshUpgradePanelLayout();
            refreshUpgradeScrollbarLayout();
        }
        if (viewCellsPanel != null) {
            viewCellsPanel.setMaxRows(Math.max(2, getRepoRowCount()));
            refreshViewCellsPanelLayout();
            refreshViewCellsScrollbarLayout();
        }
        if (cellUpgradesPanel != null) {
            cellUpgradesPanel.setMaxRows(2);
        }
        // 扩展按钮位置依赖升级槽实际高度，必须在 setMaxRows 之后调用。
        initExtendedUIButtons();
        initializePanels();
        updateManualWorkspaceUi();
        WcwtMod.LOGGER.info("WCWT debug: screen init end player={}, repoRows={}, craftableKeys={}, currentUi={}",
                menu.getPlayerInventory().player.getScoreboardName(),
                getRepoRowCount(),
                craftableIndicatorKeys.size(),
                menu.getSyncedExtendedUIType());
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

    private int getRepoRowCount() {
        // ME 网格可见行数 = RepoSlot 中不同 y 坐标的数量。
        // 升级槽面板 maxRows 与之对齐（参考 WTLib 标准做法），超出的升级槽通过滑块滚动。
        java.util.Set<Integer> rowYs = new java.util.HashSet<>();
        for (Slot slot : menu.slots) {
            if (slot instanceof RepoSlot) {
                rowYs.add(slot.y);
            }
        }
        return Math.max(2, rowYs.isEmpty() ? 2 : rowYs.size());
    }

    private void hookRepoUpdateListener() {
        repo.setUpdateViewListener(() -> {
            invokeMeStorageUpdateScrollbar();
            rebuildCraftableIndicatorCache();
        });
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
        if (!repo.hasPower()) {
            return;
        }

        for (var entry : repo.getAllEntries()) {
            if (entry.isCraftable()) {
                craftableIndicatorKeys.add(entry.getWhat());
            }
        }
    }

    /** 扩展UI按钮每次 init() 重新定位并注册（因为 addRenderableWidget 会被 clearWidgets() 清空）。*/
    private void initExtendedUIButtons() {
        var host = menu.getMenuHost();
        if (host == null) return;

        hideExistingExtendedButtons();

        // 扩展 UI 按钮位置优先读取 JSON 的 extended_functions_0..3。
        // 按钮不可用时会折叠，后面的按钮自动补位到前一个 JSON 槽位。
        final int EXT_BTN_GAP_LEFT = 1;
        int fallbackX;
        if (topModeTabButton != null && topModeTabButton.getWidth() > 0) {
            int tabRight = topModeTabButton.getX() + topModeTabButton.getWidth();
            fallbackX = tabRight + EXT_BTN_GAP_LEFT + 4; // setX = 外框左 + 4
        } else {
            fallbackX = leftPos + 331 + 22 + EXT_BTN_GAP_LEFT + 4;
        }
        int extBtnSpacing = 21;

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

        int visibleIndex = 0;
        visibleIndex = placeExtendedButton(advancedCodingButton, IExtendedUIHost.ExtendedUIType.ADVANCED_CODING,
                visibleIndex, fallbackX, extBtnSpacing);
        visibleIndex = placeExtendedButton(cosmeticArmorButton, IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR,
                visibleIndex, fallbackX, extBtnSpacing);
        visibleIndex = placeExtendedButton(curiosButton, IExtendedUIHost.ExtendedUIType.CURIOS,
                visibleIndex, fallbackX, extBtnSpacing);
        visibleIndex = placeExtendedButton(toolboxButton, IExtendedUIHost.ExtendedUIType.TOOL_SLOTS_BOX,
                visibleIndex, fallbackX, extBtnSpacing);
        visibleIndex = placeExtendedButton(toolkitButton, IExtendedUIHost.ExtendedUIType.TOOLKIT,
                visibleIndex, fallbackX, extBtnSpacing);
        placeExtendedButton(resonatingLightningPatternCodingButton,
                IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING,
                visibleIndex, fallbackX, extBtnSpacing);

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

    private void hideExistingExtendedButtons() {
        hideExtendedButtonInstance(advancedCodingButton);
        hideExtendedButtonInstance(cosmeticArmorButton);
        hideExtendedButtonInstance(curiosButton);
        hideExtendedButtonInstance(toolboxButton);
        hideExtendedButtonInstance(toolkitButton);
        hideExtendedButtonInstance(resonatingLightningPatternCodingButton);
    }

    private static void hideExtendedButtonInstance(@Nullable ExtendedUIButton button) {
        if (button == null) {
            return;
        }
        button.visible = false;
        button.active = false;
        button.setX(-10000);
        button.setY(-10000);
    }

    private int placeExtendedButton(ExtendedUIButton button, IExtendedUIHost.ExtendedUIType type, int visibleIndex,
                                    int fallbackX, int spacing) {
        boolean available = isExtendedUIAvailable(type);
        button.visible = available;
        button.active = available;
        if (!available) {
            // 未安装的扩展：保持不可见，且勿依赖默认 (0,0)；否则 updateExtendedUIVisibility 曾一度把 visible 设回 true 会画到界面左上角
            button.setX(-10000);
            button.setY(-10000);
            return visibleIndex;
        }

        int minTopGui = getExtendedButtonsMinTop(visibleIndex, spacing);
        var fallback = new ExtendedPanelLayout.Rect(fallbackX - leftPos,
                minTopGui, button.getWidth(), button.getHeight());
        var rect = mainLayout.widget("extended_functions_" + visibleIndex, fallback, imageWidth, imageHeight);
        if (rect.left() < -1000 || rect.top() < -1000) {
            rect = fallback;
        }
        // JSON 可软编码按钮位置；顶边最低值仍受升级面板底部安全间距约束，
        // 但这段安全间距本身已经改为 JSON 可配置。
        rect = new ExtendedPanelLayout.Rect(rect.left(), Math.max(rect.top(), minTopGui), rect.width(),
                rect.height());
        button.setX(leftPos + rect.left());
        button.setY(topPos + rect.top());
        addRenderableWidget(button);
        return visibleIndex + 1;
    }

    private int getExtendedButtonsMinTop(int visibleIndex, int spacing) {
        int defaultGap = 3;
        int minTopGap = mainLayout.widgetInt("extendedButtonsLayout", "minTopGap", defaultGap);
        int firstButtonTop;
        if (upgradesPanel != null) {
            var bounds = upgradesPanel.getBounds();
            firstButtonTop = bounds.getY() + bounds.getHeight() + minTopGap;
        } else {
            firstButtonTop = 46 + minTopGap;
        }
        return firstButtonTop + visibleIndex * spacing;
    }

    private boolean isExtendedUIAvailable(IExtendedUIHost.ExtendedUIType type) {
        return switch (type) {
            case ADVANCED_CODING -> true;
            case COSMETIC_ARMOR -> ModList.get().isLoaded("cosmeticarmorreworked");
            case CURIOS -> ModList.get().isLoaded("curios");
            case TOOL_SLOTS_BOX -> menu.getToolbox().isPresent();
            case TOOLKIT -> true;
            case RESONATING_LIGHTNING_PATTERN_CODING ->
                    ModList.get().isLoaded("ae2cs") || ModList.get().isLoaded("ae2lt");
            case NONE -> false;
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
        ModNetworking.sendToServer(new ExtendedUIPacket(IExtendedUIHost.ExtendedUIType.NONE));
        updateExtendedUIVisibility();
    }

    @Override
    public List<Rect2i> getExclusionZones() {
        var zones = super.getExclusionZones();

        addExtendedButtonsBackgroundExclusion(zones);
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

    private void addExtendedButtonsBackgroundExclusion(List<Rect2i> zones) {
        var bounds = getExtendedButtonsBackgroundBounds();
        if (bounds != null) {
            zones.add(bounds);
        }
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
        
        // 发送网络数据包同步状态
        ModNetworking.sendToServer(new ExtendedUIPacket(newType));
        updateExtendedUIVisibility();
    }
    
    private void updateExtendedUIVisibility() {
        var host = menu.getMenuHost();
        if (host == null) return;

        host.setCurrentExtendedUI(menu.getSyncedExtendedUIType());
        
        var currentUI = host.getCurrentExtendedUI();
        boolean hideExtendedButtons = currentUI == IExtendedUIHost.ExtendedUIType.ADVANCED_CODING
                || currentUI == IExtendedUIHost.ExtendedUIType.COSMETIC_ARMOR
                || currentUI == IExtendedUIHost.ExtendedUIType.CURIOS
                || currentUI == IExtendedUIHost.ExtendedUIType.TOOLKIT
                || currentUI == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING;

        // 更新高级编码模式状态
        advancedCodingMode = (currentUI == IExtendedUIHost.ExtendedUIType.ADVANCED_CODING);
        patternSelectionLockedMode = advancedCodingMode
                || currentUI == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING;
        if (!patternSelectionLockedMode) {
            selectedPatternCacheIndex = -1;
        }
        if (patternSelectionLockedMode && host.getSelectedPatternIndex() >= 0) {
            selectedPatternCacheIndex = host.getSelectedPatternIndex();
        }

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
                if (toolkitPanel != null) toolkitPanel.setVisible(true);
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
                || (patternManageMappingField != null && patternManageMappingField.isFocused());
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
        ModNetworking.sendToServer(new PatternMultiplierPacket(
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
            searchKey = resolveEaepProviderSearchKey("crafting");
            generatedFallbackSearchKey = searchKey != null && !searchKey.isBlank();
        }
        if (searchKey != null && !searchKey.isBlank() && patternManageSearchField != null && !generatedFallbackSearchKey) {
            patternManageSearchField.setValue(searchKey);
            rebuildPatternManagementRows();
        }
        if (encodePatternButton != null) {
            encodePatternButton.setFocused(false);
        }
        setFocused(null);
        PreferredUploadProvider preferredProvider = resolvePreferredUploadProvider(searchKey);
        ModNetworking.sendToServer(new EncodePatternPacket(patternEncodingMode, patternManagementUploadEnabled,
                searchKey == null ? "" : searchKey,
                WcwtClientConfig.patternUploadFailFallbackToEditor(),
                preferredProvider.providerId(),
                preferredProvider.providerName()));
    }

    private PreferredUploadProvider resolvePreferredUploadProvider(@Nullable String searchKey) {
        if (!patternManagementUploadEnabled
                || searchKey == null
                || searchKey.isBlank()
                || !isEaepAutoUploadUniqueMatchEnabled()) {
            return PreferredUploadProvider.NONE;
        }

        String query = searchKey.trim();
        Map<String, PreferredUploadProviderGroup> groups = new LinkedHashMap<>();
        for (var entry : patternProviders) {
            String name = entry.group().name().getString();
            if (name == null || name.isBlank()) {
                continue;
            }
            int emptySlots = Math.max(0, entry.inventorySize() - entry.slots().size());
            if (emptySlots <= 0) {
                continue;
            }
            groups.compute(name, (ignored, group) -> {
                if (group == null) {
                    return new PreferredUploadProviderGroup(entry.providerId(), name, emptySlots);
                }
                group.merge(entry.providerId(), emptySlots);
                return group;
            });
        }

        List<PreferredUploadProviderGroup> matchingGroups = groups.values().stream()
                .filter(group -> JecSearchCompat.contains(group.name(), query))
                .toList();
        if (matchingGroups.size() != 1) {
            return PreferredUploadProvider.NONE;
        }
        var group = matchingGroups.get(0);
        return new PreferredUploadProvider(group.bestProviderId(), group.name());
    }

    private static boolean isEaepAutoUploadUniqueMatchEnabled() {
        if (!ModList.get().isLoaded("extendedae_plus")) {
            return true;
        }
        try {
            var cfgPath = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                    .resolve("extendedae_plus/pinned_providers.json");
            if (!java.nio.file.Files.exists(cfgPath)) {
                return true;
            }
            com.google.gson.JsonObject obj = new com.google.gson.Gson()
                    .fromJson(java.nio.file.Files.readString(cfgPath), com.google.gson.JsonObject.class);
            if (obj == null) {
                return true;
            }
            com.google.gson.JsonElement element = obj.get("auto_upload_unique_match");
            return element == null || !element.isJsonPrimitive() || element.getAsBoolean();
        } catch (Exception ignored) {
            return true;
        }
    }

    @Nullable
    private String consumeEaepProviderSearchKey() {
        return ModList.get().isLoaded("extendedae_plus")
                ? RecipeTypeNameConfig.consumeLastProviderSearchKey()
                : null;
    }

    @Nullable
    private String resolveEaepProviderSearchKey(String rawKey) {
        return ModList.get().isLoaded("extendedae_plus")
                ? RecipeTypeNameConfig.resolveSearchKeyAlias(rawKey)
                : rawKey;
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
            rebuildPatternManagementRows();
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
        if (!ModList.get().isLoaded("extendedae_plus")) {
            return recipeBookmarkFallbackHoverName(recipeBookmark);
        }
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
                String mapped = RecipeTypeNameConfig.mapRecipeTypeToSearchKey(recipe);
                if (mapped != null && !mapped.isBlank()) {
                    return mapped;
                }
            }
            String derived = RecipeTypeNameConfig.deriveSearchKeyFromUnknownRecipe(recipeBookmark);
            if (derived != null && !derived.isBlank()) {
                return derived;
            }
        } catch (NoClassDefFoundError e) {
            return recipeBookmarkFallbackHoverName(recipeBookmark);
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

    private void updatePlayerWidgetPosition() {
        AbstractWidget playerWidget = resolveWidgetById("player");
        if (playerWidget == null) {
            return;
        }
        var rect = mainLayout.widget("player",
                new ExtendedPanelLayout.Rect(25, imageHeight - 222, playerWidget.getWidth(), playerWidget.getHeight()),
                imageWidth, imageHeight);
        playerWidget.setX(leftPos + rect.left());
        playerWidget.setY(topPos + rect.top());
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
        ModNetworking.sendToServer(new ManualWorkspaceModePacket(mode.ordinal()));
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
    }

    private void updateManualCraftingSlots(boolean visible) {
        var base = mainLayout.slot("CRAFTING_GRID",
                new ExtendedPanelLayout.Rect(79, imageHeight - 214, 18, 18), imageWidth, imageHeight);
        var result = mainLayout.slot("CRAFTING_RESULT",
                new ExtendedPanelLayout.Rect(140, imageHeight - 212, 18, 18), imageWidth, imageHeight);

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
                new ExtendedPanelLayout.Rect(140, imageHeight - 212, 18, 18), imageWidth, imageHeight);
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
                new ExtendedPanelLayout.Rect(140, imageHeight - 212, 18, 18), imageWidth, imageHeight);
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
        refreshUpgradePanelLayout();
        refreshUpgradeScrollbarLayout();
        refreshViewCellsPanelLayout();
        refreshViewCellsScrollbarLayout();
        if (!debugLoggedFirstUpdateBeforeRender) {
            debugLoggedFirstUpdateBeforeRender = true;
            WcwtMod.LOGGER.info("WCWT debug: first updateBeforeRender player={}, hostPresent={}, repoPower={}, currentUi={}",
                    menu.getPlayerInventory().player.getScoreboardName(),
                    menu.getMenuHost() != null,
                    repo.hasPower(),
                    menu.getSyncedExtendedUIType());
        }
        wcwtStep("refreshRepoViewAfterTransientReconnect", this::refreshRepoViewAfterTransientReconnect);
        boolean syncedPatternManagementUploadEnabled = menu.isPatternManagementUploadEnabled();
        var syncedPatternManagementDisplayMode = patternManagementDisplayModeFromOrdinal(menu.getPatternManagementDisplayMode());
        boolean syncedPatternManagementShowSlots = menu.isPatternManagementShowSlots();
        var syncedPatternManagementSearchMode = patternManagementSearchModeFromOrdinal(menu.getPatternManagementSearchMode());
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
        if (magnetCardMenuButton != null) {
            var host = menu.getMenuHost();
            boolean hasMagnet = host != null && WcwtWirelessFeatures.hasMagnetCardInstalled(host.getItemStack());
            magnetCardMenuButton.visible = hasMagnet;
            magnetCardMenuButton.active = hasMagnet;
        }
        wcwtStep("updateKeyTypeFilterButtons", this::updateKeyTypeFilterButtons);
        wcwtStep("syncPatternEncodingModeFromMenu", this::syncPatternEncodingModeFromMenu);
        wcwtStep("updateExtendedUIVisibility", this::updateExtendedUIVisibility);
        wcwtStep("updatePanelSlotActivity", this::updatePanelSlotActivity);
        wcwtStep("updateManualWorkspaceUi", this::updateManualWorkspaceUi);
        wcwtStep("updatePatternEncodingModeButtons", this::updatePatternEncodingModeButtons);
        wcwtStep("updatePatternEncodingSlots", this::updatePatternEncodingSlots);
        wcwtStep("syncPatternManagementSearchFromEncodedPatternSlot", this::syncPatternManagementSearchFromEncodedPatternSlot);
        wcwtStep("updatePatternCacheSlots", this::updatePatternCacheSlots);
        wcwtStep("updatePatternManagement", this::updatePatternManagement);
        wcwtStep("updateCurioSlots", this::updateCurioSlots);
        wcwtStepLogFrames++;
        if (DEBUG_REPO) {
            logRepoViewState("updateBeforeRender", -1);
        }
    }

    private void refreshRepoViewAfterTransientReconnect() {
        boolean connected = menu.getLinkStatus().connected();
        if (!observedLinkConnectionOnce) {
            observedLinkConnectionOnce = true;
            lastObservedLinkConnected = connected;
            if (connected && !repo.hasPower()) {
                repo.updateView();
            }
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
        ModNetworking.sendToServer(new PatternManagementUploadSettingPacket(
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
            ModNetworking.sendToServer(new PatternProviderListPacket.Request(true));
        }
    }

    private void refreshPatternProviders() {
        long now = System.currentTimeMillis();
        if (now - lastPatternProviderRefreshRequestMs < PATTERN_PROVIDER_REFRESH_DEBOUNCE_MS) {
            return;
        }
        requestedPatternProviders = true;
        lastPatternProviderRefreshRequestMs = now;
        lastPatternProviderSubscriptionRequestMs = now;
        ModNetworking.sendToServer(new PatternProviderListPacket.Request(true));
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
        ModNetworking.sendToServer(new PatternProviderListPacket.Request(true));
    }

    public void updatePatternProviders(List<PatternProviderListPacket.Entry> entries) {
        long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
        patternProviders.clear();
        patternProviders.addAll(entries);
        selectedPatternProviderId = patternProviders.stream().anyMatch(entry -> entry.providerId() == selectedPatternProviderId)
                ? selectedPatternProviderId : -1;
        selectedPatternProviderSlot = selectedPatternProviderId >= 0 ? selectedPatternProviderSlot : -1;
        focusedPatternProviderId = patternProviders.stream().anyMatch(entry -> entry.providerId() == focusedPatternProviderId)
                ? focusedPatternProviderId : -1;
        focusedPatternProviderSlot = focusedPatternProviderId >= 0 ? focusedPatternProviderSlot : -1;
        rebuildPatternManagementRows();
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
                showSlot(slot,
                        PATTERN_CACHE_SLOT_X + (visibleIndex % PATTERN_CACHE_COLS) * 18,
                        slotY + (visibleIndex / PATTERN_CACHE_COLS) * 18);
            } else {
                hideSlot(slot);
            }
        }
    }

    private void updatePatternEncodingSlots() {
        boolean processingVisible = patternEncodingMode == EncodingMode.PROCESSING;
        var processingInputSlots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_INPUTS);
        var processingOutputSlots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_PROCESSING_OUTPUTS);
        int processingColumns = mainLayout.widgetInt("pattern_processing_inputs", "columns", 3);
        int processingVisibleRows = mainLayout.widgetInt("pattern_processing_inputs", "visibleRows", 3);
        int maxScroll = Math.max(0, processingInputSlots.size() / processingColumns - processingVisibleRows);
        if (patternEncodingScrollbar != null) {
            var scrollbar = mainLayout.widget("processingPatternModeScrollbar",
                    new ExtendedPanelLayout.Rect(183, imageHeight - 216, 0, 52), imageWidth, imageHeight);
            patternEncodingScrollbar.setPosition(new Point(scrollbar.left(), scrollbar.top()));
            patternEncodingScrollbar.setHeight(scrollbar.height());
            patternEncodingScrollbar.setRange(0, maxScroll, processingVisibleRows);
            patternEncodingScrollbar.setVisible(processingVisible && maxScroll > 0);
        }
        if (stonecuttingPatternScrollbar != null) {
            int rows = (menu.getStonecuttingRecipes().size() + STONECUTTING_RESULT_COLS - 1) / STONECUTTING_RESULT_COLS;
            stonecuttingPatternScrollbar.setRange(0, Math.max(0, rows - STONECUTTING_RESULT_ROWS), STONECUTTING_RESULT_ROWS);
            var scrollbar = mainLayout.widget("stonecuttingPatternModeScrollbar",
                    new ExtendedPanelLayout.Rect(294, imageHeight - 202, 0, 44), imageWidth, imageHeight);
            stonecuttingPatternScrollbar.setPosition(new Point(scrollbar.left(), scrollbar.top()));
            stonecuttingPatternScrollbar.setHeight(scrollbar.height());
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
                case SMITHING_TABLE -> 10;
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
            patternSubstitutionButton.setState(menu.isPatternSubstitute());
            patternSubstitutionButton.setX(buttonX + (patternEncodingMode == EncodingMode.CRAFTING ? 72 : 20));
            patternSubstitutionButton.setY(buttonY + (patternEncodingMode == EncodingMode.CRAFTING ? 6 : 14));
        }

        boolean showFluidSubstitutions = patternEncodingMode == EncodingMode.CRAFTING;
        if (patternFluidSubstitutionButton != null) {
            patternFluidSubstitutionButton.visible = showFluidSubstitutions;
            patternFluidSubstitutionButton.setState(menu.isPatternFluidSubstitute());
            patternFluidSubstitutionButton.setX(buttonX + 82);
            patternFluidSubstitutionButton.setY(buttonY + 6);
        }
    }

    private ExtendedPanelLayout.Rect getPatternEncodingBackgroundOrigin() {
        return mainLayout.widget("pattern_encoding_background",
                new ExtendedPanelLayout.Rect(176, imageHeight - 223,
                        PATTERN_ENCODING_BG_WIDTH, PATTERN_ENCODING_BG_HEIGHT),
                imageWidth, imageHeight);
    }

    private ExtendedPanelLayout.Rect patternEncodingSlot(String id, int left, int top, int width, int height) {
        return patternEncodingSlot(id, new ExtendedPanelLayout.Rect(left, top, width, height));
    }

    private ExtendedPanelLayout.Rect patternEncodingSlot(String id, ExtendedPanelLayout.Rect fallback) {
        if (fallback == null) {
            fallback = defaultPatternEncodingSlotFallback(id);
        }
        return mainLayout.widget(id, fallback, imageWidth, imageHeight);
    }

    private ExtendedPanelLayout.Rect defaultPatternEncodingSlotFallback(String id) {
        return switch (id) {
            case "pattern_crafting_grid" -> new ExtendedPanelLayout.Rect(183, imageHeight - 216, 18, 18);
            case "pattern_processing_inputs" -> new ExtendedPanelLayout.Rect(192, imageHeight - 216, 18, 18);
            case "pattern_processing_outputs" -> new ExtendedPanelLayout.Rect(277, imageHeight - 216, 18, 18);
            case "pattern_smithing_template" -> new ExtendedPanelLayout.Rect(183, imageHeight - 198, 18, 18);
            case "pattern_smithing_base" -> new ExtendedPanelLayout.Rect(201, imageHeight - 198, 18, 18);
            case "pattern_smithing_addition" -> new ExtendedPanelLayout.Rect(219, imageHeight - 198, 18, 18);
            case "pattern_smithing_preview" -> new ExtendedPanelLayout.Rect(277, imageHeight - 198, 18, 18);
            case "pattern_crafting_preview" -> new ExtendedPanelLayout.Rect(273, imageHeight - 198, 18, 18);
            case "pattern_stonecutting_input" -> new ExtendedPanelLayout.Rect(183, imageHeight - 198, 18, 18);
            case "pattern_stonecutting_results" -> new ExtendedPanelLayout.Rect(202, imageHeight - 211,
                    STONECUTTING_RESULT_SLOT_W, STONECUTTING_RESULT_SLOT_H);
            default -> new ExtendedPanelLayout.Rect(0, 0, 18, 18);
        };
    }

    private void updateEncodedPatternSlot() {
        var encodedPatternRect = mainLayout.slot("ENCODED_PATTERN",
                new ExtendedPanelLayout.Rect(308, imageHeight - 197, 16, 16), imageWidth, imageHeight);
        for (Slot slot : menu.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setActive(true);
            }
            showSlot(slot, encodedPatternRect.left(), encodedPatternRect.top());
        }
    }

    private void syncPatternManagementSearchFromEncodedPatternSlot() {
        Slot encodedPatternSlot = getEncodedPatternSlot();
        ItemStack encodedPattern = encodedPatternSlot != null ? encodedPatternSlot.getItem() : ItemStack.EMPTY;
        String uploadSearchText = PatternUploadMetadata.getProviderSearchText(encodedPattern);
        boolean changed = !ItemStack.isSameItem(lastEncodedPatternForUploadSync, encodedPattern)
                || !Objects.equals(lastEncodedPatternForUploadSync.getTag(), encodedPattern.getTag())
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
            rebuildPatternManagementRows();
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
        var matchingEntries = patternProviders.stream()
                .filter(entry -> JecSearchCompat.contains(entry.group().name().getString(), normalizedQuery))
                .toList();
        var distinctNames = matchingEntries.stream()
                .map(entry -> entry.group().name().getString())
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

    private void updatePatternCraftingSlots(List<Slot> slots, boolean visible) {
        var base = patternEncodingSlot("pattern_crafting_grid",
                183, imageHeight - 216, 18, 18);
        int columns = mainLayout.widgetInt("pattern_crafting_grid", "columns", 3);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            setPatternEncodingSlotActive(slot, visible);
            if (visible) {
                showSlot(slot, base.left() + (i % columns) * base.width(),
                        base.top() + (i / columns) * base.height());
            } else {
                hidePatternEncodingSlot(slot);
            }
        }
    }

    private void updatePatternProcessingInputSlots(List<Slot> slots, boolean visible, int scroll) {
        var base = patternEncodingSlot("pattern_processing_inputs",
                192, imageHeight - 216, 18, 18);
        int columns = mainLayout.widgetInt("pattern_processing_inputs", "columns", 3);
        int visibleRows = mainLayout.widgetInt("pattern_processing_inputs", "visibleRows", 3);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int effectiveRow = (i / columns) - scroll;
            boolean show = visible && effectiveRow >= 0 && effectiveRow < visibleRows;
            setPatternEncodingSlotActive(slot, show);
            if (show) {
                showSlot(slot, base.left() + (i % columns) * base.width(),
                        base.top() + effectiveRow * base.height());
            } else {
                hidePatternEncodingSlot(slot);
            }
        }
    }

    private void updatePatternEncodingOutputSlots(List<Slot> slots, boolean visible, int scroll) {
        var base = patternEncodingSlot("pattern_processing_outputs",
                277, imageHeight - 216, 18, 18);
        int visibleRows = mainLayout.widgetInt("pattern_processing_outputs", "visibleRows", 3);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int effectiveRow = i - scroll;
            boolean slotVisible = visible && effectiveRow >= 0 && effectiveRow < visibleRows;
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setActive(slotVisible);
            }
            if (slotVisible) {
                showSlot(slot, base.left(), base.top() + effectiveRow * base.height());
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
        String[] slotIds = {
                "pattern_smithing_template",
                "pattern_smithing_base",
                "pattern_smithing_addition"
        };
        ExtendedPanelLayout.Rect[] fallbacks = {
                new ExtendedPanelLayout.Rect(183, imageHeight - 198, 18, 18),
                new ExtendedPanelLayout.Rect(201, imageHeight - 198, 18, 18),
                new ExtendedPanelLayout.Rect(219, imageHeight - 198, 18, 18)
        };
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                continue;
            }
            var slot = slots.get(i).get(0);
            setPatternEncodingSlotActive(slot, visible);
            if (visible) {
                var rect = patternEncodingSlot(slotIds[i], fallbacks[i]);
                showSlot(slot, rect.left(), rect.top());
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
        var slot = slots.get(0);
        setPatternEncodingSlotActive(slot, visible);
        if (visible) {
            var rect = patternEncodingSlot("pattern_stonecutting_input",
                    183, imageHeight - 198, 18, 18);
            showSlot(slot, rect.left(), rect.top());
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
            String slotId = patternEncodingMode == EncodingMode.CRAFTING
                    ? "pattern_crafting_preview"
                    : "pattern_smithing_preview";
            var rect = patternEncodingSlot(slotId, patternEncodingMode == EncodingMode.CRAFTING
                    ? new ExtendedPanelLayout.Rect(273, imageHeight - 198, 18, 18)
                    : new ExtendedPanelLayout.Rect(277, imageHeight - 198, 18, 18));
            showSlot(slot, rect.left(), rect.top());
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
        setSlotPosition(slot, HIDDEN_SLOT_POS.getX(), HIDDEN_SLOT_POS.getY());
    }

    private void showSlot(Slot slot, int x, int y) {
        setSlotActive(slot, true);
        setSlotPosition(slot, x, y);
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
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

    @SuppressWarnings("unchecked")
    private void replaceCompositeWidget(String id, ICompositeWidget widget, ScreenStyle style) {
        try {
            Field field = appeng.client.gui.WidgetContainer.class.getDeclaredField("compositeWidgets");
            field.setAccessible(true);
            Object value = field.get(widgets);
            if (value instanceof Map<?, ?> map && map.containsKey(id)) {
                var compositeWidgets = (Map<String, ICompositeWidget>) map;
                var widgetStyle = style.getWidget(id);
                widget.setSize(widgetStyle.getWidth(), widgetStyle.getHeight());
                compositeWidgets.put(id, widget);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("WCWT: failed to replace composite widget " + id, e);
        }
    }

    private void setSemanticSlotsHidden(appeng.menu.SlotSemantic semantic, boolean hidden) {
        setSlotsHidden(semantic, hidden);
        for (var slot : menu.getSlots(semantic)) {
            setSlotActive(slot, !hidden);
            if (hidden) {
                hideSlot(slot);
            }
        }
    }

    private void rebuildPatternManagementRows() {
        long startNs = DEBUG_PERF ? System.nanoTime() : 0L;
        patternManagementRows.clear();
        String filter = patternManageSearchField != null ? patternManageSearchField.getValue().trim().toLowerCase() : "";
        patternProviders.stream()
                .filter(this::isPatternProviderVisibleInManagement)
                .filter(entry -> filter.isEmpty() || patternProviderMatches(entry, filter))
                .sorted(Comparator.comparing(entry -> entry.group().name().getString().toLowerCase()))
                .forEach(entry -> {
                    patternManagementRows.add(new PatternManagementHeaderRow(entry));
                    if (patternManagementShowSlots) {
                        for (int offset = 0; offset < entry.inventorySize(); offset += PATTERN_MANAGEMENT_COLS) {
                            patternManagementRows.add(new PatternManagementSlotsRow(entry, offset,
                            Math.min(PATTERN_MANAGEMENT_COLS, entry.inventorySize() - offset)));
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

    private boolean patternProviderMatches(PatternProviderListPacket.Entry entry, String filter) {
        if (JecSearchCompat.contains(entry.group().name().getString(), filter)) {
            return true;
        }
        for (var stack : entry.slots().values()) {
            if (patternStackMatches(stack, filter)) {
                return true;
            }
        }
        return false;
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
        keepPatternProviderSubscriptionAlive();
        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int maxScroll = Math.max(0, patternManagementRows.size() - visibleRows);
        if (patternManagementScrollbar != null) {
            patternManagementScrollbar.setHeight(patternManagementPage.height());
            patternManagementScrollbar.setRange(0, maxScroll, 1);
            patternManagementScrollbar.setVisible(maxScroll > 0);
        }
    }

    private boolean isPatternManagementActive() {
        return patternManagementUploadEnabled
                || (patternManageSearchField != null && patternManageSearchField.isFocused())
                || selectedPatternProviderId >= 0
                || !patternProviders.isEmpty();
    }

    private void refreshPatternManagementLayout() {
        patternManagementPage = mainLayout.widget("management_page", patternManagementPage, imageWidth, imageHeight);
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
        wcwtStep("panel.PANEL_SLOT_SEMANTICS", () -> {
            for (var semantic : PANEL_SLOT_SEMANTICS) {
                setSemanticSlotsHidden(semantic, !panelOpen);
            }
        });
        boolean rlpcOpen = resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible();
        wcwtStep("panel.RLPC_PANEL_SLOT_SEMANTICS", () -> {
            for (var semantic : RLPC_PANEL_SLOT_SEMANTICS) {
                setSemanticSlotsHidden(semantic, !rlpcOpen);
            }
        });

        boolean cosmeticOpen = cosmeticArmorPanel != null && cosmeticArmorPanel.isVisible();
        wcwtStep("panel.COSMETIC_ARMOR_SLOT_SEMANTICS", () -> {
            for (var semantic : COSMETIC_ARMOR_SLOT_SEMANTICS) {
                setSemanticSlotsHidden(semantic, !cosmeticOpen);
            }
            if (cosmeticOpen) {
                positionCosmeticArmorSlots();
            }
        });

        boolean curiosOpen = curiosPanel != null && curiosPanel.isVisible();
        wcwtStep("panel.AE_CURIOS", () -> setSemanticSlotsHidden(WcwtSlotSemantics.AE_CURIOS, !curiosOpen));
        if (curiosScrollbar != null) {
            curiosScrollbar.setVisible(curiosOpen);
        }

        boolean toolkitOpen = toolkitPanel != null && toolkitPanel.isVisible();
        wcwtStep("panel.WCWT_TOOLKIT", () -> setSemanticSlotsHidden(WcwtSlotSemantics.WCWT_TOOLKIT, !toolkitOpen));
        if (toolkitScrollbar != null) {
            toolkitScrollbar.setVisible(toolkitOpen);
        }

        boolean toolboxOpen = toolboxPanel != null && toolboxPanel.isVisible();
        wcwtStep("panel.TOOLBOX", () -> {
            if (toolboxOpen) {
                positionToolboxSlots();
            } else {
                hideToolboxSlots();
            }
        });

        wcwtStep("panel.updateToolkitSlots", this::updateToolkitSlots);
        wcwtStep("panel.updateResonatingStorageSlots", this::updateResonatingStorageSlots);

        // 元件升级卡槽：面板关闭→隐藏；面板打开→根据主升级面板与 layout 配置实时定位。
        // 元件没放入时 OptionalRestrictedInputSlot.isSlotEnabled() 自动返回 false，
        // vanilla 不渲染、不接点击，所以不需要再额外 hide。
        wcwtStep("panel.cellUpgrades", () -> {
            if (cellUpgradesPanel != null) {
                boolean showCellUpgrades = panelOpen && hasVisibleCellUpgradeSlot();
                cellUpgradesPanel.setVisible(showCellUpgrades);
                if (showCellUpgrades && upgradesPanel != null) {
                    refreshCellUpgradePanelLayout();
                    refreshCellUpgradeScrollbarLayout();
                }
            } else if (!panelOpen) {
                setSemanticSlotsHidden(com.lhy.wcwt.menu.WcwtSlotSemantics.WCWT_CELL_UPGRADE, true);
            }
        });
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
            int x = bounds.getX() - leftPos + toolboxPanel.getSlotRelativeX() + (i % 3) * 17;
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
        if (!toolkitOpen || toolkitPanel == null) {
            for (var slot : slots) {
                hideSlot(slot);
            }
            return;
        }

        int columns = toolkitPanel.getColumns();
        int visibleSlots = columns * ToolkitPanel.VISIBLE_ROWS;
        int totalRows = (slots.size() + columns - 1) / columns;
        int maxScroll = Math.max(0, totalRows - ToolkitPanel.VISIBLE_ROWS);
        if (toolkitScrollbar != null) {
            toolkitScrollbar.setHeight(toolkitPanel.getScrollbarHeight());
            toolkitScrollbar.setRange(0, maxScroll, 1);
            toolkitScrollbar.setVisible(true);
            var bounds = toolkitPanel.getBounds();
            toolkitScrollbar.setPosition(new Point(
                    bounds.getX() - leftPos + toolkitPanel.getScrollbarX(),
                    bounds.getY() - topPos + toolkitPanel.getScrollbarY()));
        }

        int firstSlot = (toolkitScrollbar != null ? toolkitScrollbar.getCurrentScroll() : 0) * columns;
        toolkitPanel.setFirstVisibleSlot(firstSlot);
        var bounds = toolkitPanel.getBounds();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            boolean visible = i >= firstSlot && i < firstSlot + visibleSlots;
            if (visible) {
                int visibleIndex = i - firstSlot;
                int x = bounds.getX() - leftPos
                        + toolkitPanel.getSlotAnchorX()
                        + (visibleIndex % columns) * ToolkitPanel.SLOT_SIZE;
                int y = bounds.getY() - topPos
                        + toolkitPanel.getSlotAnchorY()
                        + (visibleIndex / columns) * ToolkitPanel.SLOT_SIZE;
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
        if (advancedCodingPanel != null && advancedCodingPanel.isVisible() && isPanelSlot(slot)) {
            return;
        }
        if (resonatingLightningPatternCodingPanel != null
                && resonatingLightningPatternCodingPanel.isVisible()
                && menu.getSlots(WcwtSlotSemantics.WCWT_RESONATING_STORAGE).contains(slot)) {
            renderResonatingStorageSlot(guiGraphics, slot);
            return;
        }
        if (menu.getSlots(SlotSemantics.TOOLBOX).contains(slot)
                && (toolboxPanel == null || !toolboxPanel.isVisible())) {
            return;
        }
        if (menu.getSlots(WcwtSlotSemantics.WCWT_TOOLKIT).contains(slot)
                && (toolkitPanel == null || !toolkitPanel.isVisible())) {
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
        super.renderSlot(guiGraphics, slot);
        renderCraftablePatternIndicator(guiGraphics, slot);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        var lines = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot != null && shouldShowCraftableIndicatorForSlot(hoveredSlot)) {
            lines = new ArrayList<>(lines);
            lines.add(ButtonToolTips.Craftable.text().withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
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
        RenderSystem.disableDepthTest();
        guiGraphics.blit(CURIOS_INVENTORY_TEXTURE, slot.x + 11, slot.y - 3, texX, 0, 8, 8, 256, 256);
        RenderSystem.enableDepthTest();
    }

    private void renderCurioToggleOverlays(GuiGraphics guiGraphics) {
        if (curiosPanel == null || !curiosPanel.isVisible()) {
            return;
        }
        for (var slot : getCurioSlots()) {
            renderCurioToggle(guiGraphics, slot);
        }
    }

    private void renderCosmeticArmorToggleOverlays(GuiGraphics guiGraphics) {
        if (cosmeticArmorPanel == null || !cosmeticArmorPanel.isVisible()) {
            return;
        }
        for (int i = 0; i < COSMETIC_ARMOR_SLOT_SEMANTICS.length; i++) {
            for (var slot : menu.getSlots(COSMETIC_ARMOR_SLOT_SEMANTICS[i])) {
                if (slot.x != HIDDEN_SLOT_POS.getX() && slot.y != HIDDEN_SLOT_POS.getY()) {
                    cosmeticArmorPanel.renderToggleOverlay(guiGraphics, i, slot.x, slot.y);
                }
            }
        }
    }

    private boolean isPanelSlot(net.minecraft.world.inventory.Slot slot) {
        for (var semantic : PANEL_SLOT_SEMANTICS) {
            if (menu.getSlots(semantic).contains(slot)) {
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
        renderExtendedButtonsBackground(guiGraphics);
        renderManualWorkspaceBackground(guiGraphics, offsetX, offsetY);
        renderPatternEncodingBackground(guiGraphics, offsetX, offsetY);
    }

    private void renderExtendedButtonsBackground(GuiGraphics guiGraphics) {
        var bounds = getExtendedButtonsBackgroundBounds();
        if (bounds == null) {
            return;
        }
        WcwtExtendedButtonsBackground.draw(guiGraphics, bounds.getX(), bounds.getY(),
                getVisibleExtendedButtons().size(), getExtendedButtonsBackgroundStyle());
    }

    @Nullable
    private Rect2i getExtendedButtonsBackgroundBounds() {
        var visibleButtons = getVisibleExtendedButtons();
        if (visibleButtons.isEmpty()) {
            return null;
        }

        var first = visibleButtons.get(0);
        var style = getExtendedButtonsBackgroundStyle();
        int x = first.getX() - (style.firstButtonX() + 1);
        int y = first.getY() - style.firstButtonY();
        return new Rect2i(x, y, style.width(),
                WcwtExtendedButtonsBackground.height(visibleButtons.size(), style));
    }

    private List<ExtendedUIButton> getVisibleExtendedButtons() {
        var visibleButtons = new ArrayList<ExtendedUIButton>(6);
        addVisibleExtendedButton(visibleButtons, advancedCodingButton);
        addVisibleExtendedButton(visibleButtons, cosmeticArmorButton);
        addVisibleExtendedButton(visibleButtons, curiosButton);
        addVisibleExtendedButton(visibleButtons, toolboxButton);
        addVisibleExtendedButton(visibleButtons, toolkitButton);
        addVisibleExtendedButton(visibleButtons, resonatingLightningPatternCodingButton);
        return visibleButtons;
    }

    private static void addVisibleExtendedButton(List<ExtendedUIButton> visibleButtons, @Nullable ExtendedUIButton button) {
        if (button != null && button.visible) {
            visibleButtons.add(button);
        }
    }

    private WcwtExtendedButtonsBackground.Style getExtendedButtonsBackgroundStyle() {
        var defaults = WcwtExtendedButtonsBackground.DEFAULT_STYLE;
        String widgetId = "extendedButtonsBackground";
        return new WcwtExtendedButtonsBackground.Style(
                mainLayout.widgetInt(widgetId, "width", defaults.width()),
                mainLayout.widgetInt(widgetId, "topU", defaults.topU()),
                mainLayout.widgetInt(widgetId, "topV", defaults.topV()),
                mainLayout.widgetInt(widgetId, "topHeight", defaults.topHeight()),
                mainLayout.widgetInt(widgetId, "middleU", defaults.middleU()),
                mainLayout.widgetInt(widgetId, "middleV", defaults.middleV()),
                mainLayout.widgetInt(widgetId, "middleHeight", defaults.middleHeight()),
                mainLayout.widgetInt(widgetId, "bottomU", defaults.bottomU()),
                mainLayout.widgetInt(widgetId, "bottomV", defaults.bottomV()),
                mainLayout.widgetInt(widgetId, "bottomHeight", defaults.bottomHeight()),
                mainLayout.widgetInt(widgetId, "firstButtonX", defaults.firstButtonX()),
                mainLayout.widgetInt(widgetId, "firstButtonY", defaults.firstButtonY()),
                mainLayout.widgetInt(widgetId, "middleButtonX", defaults.middleButtonX()),
                mainLayout.widgetInt(widgetId, "middleButtonY", defaults.middleButtonY()),
                mainLayout.widgetInt(widgetId, "lastButtonX", defaults.lastButtonX()),
                mainLayout.widgetInt(widgetId, "lastButtonY", defaults.lastButtonY()));
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
        var bg = getPatternEncodingBackgroundOrigin();
        int srcX = patternEncodingMode == EncodingMode.SMITHING_TABLE ? 128 : 0;
        int srcY = switch (patternEncodingMode) {
            case CRAFTING -> 0;
            case PROCESSING -> 70;
            case SMITHING_TABLE -> 70;
            case STONECUTTING -> 140;
        };
        guiGraphics.blit(AE2_PATTERN_MODES_TEXTURE,
                offsetX + bg.left(),
                offsetY + bg.top(),
                srcX, srcY, bg.width(), bg.height(), 256, 256);
        if (patternEncodingMode == EncodingMode.STONECUTTING) {
            renderStonecuttingResults(guiGraphics, offsetX, offsetY);
        }
    }

    private void renderStonecuttingResults(GuiGraphics guiGraphics, int offsetX, int offsetY) {
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
            var bounds = getStonecuttingRecipeBounds(i - startIndex);
            var recipe = recipes.get(i);
            boolean hover = inRect(relMouseX, relMouseY, bounds);
            boolean selected = selectedRecipe != null && selectedRecipe.equals(recipe.id());
            int srcY = STONECUTTING_RESULT_SRC_Y + (selected ? STONECUTTING_RESULT_SLOT_H
                    : hover ? STONECUTTING_RESULT_SLOT_H * 2 : 0);
            guiGraphics.blit(AE2_PATTERN_MODES_TEXTURE,
                    offsetX + bounds.left(), offsetY + bounds.top() - 1,
                    STONECUTTING_RESULT_SRC_X, srcY, STONECUTTING_RESULT_SLOT_W, STONECUTTING_RESULT_SLOT_H,
                    256, 256);

            ItemStack result = recipe.value().getResultItem(Objects.requireNonNull(Minecraft.getInstance().level)
                    .registryAccess());
            guiGraphics.renderItem(result, offsetX + bounds.left(), offsetY + bounds.top());
            guiGraphics.renderItemDecorations(font, result, offsetX + bounds.left(), offsetY + bounds.top());
        }
    }

    private ExtendedPanelLayout.Rect getStonecuttingRecipeBounds(int index) {
        var origin = patternEncodingSlot("pattern_stonecutting_results",
                202, imageHeight - 211, STONECUTTING_RESULT_SLOT_W, STONECUTTING_RESULT_SLOT_H);
        int columns = mainLayout.widgetInt("pattern_stonecutting_results", "columns", STONECUTTING_RESULT_COLS);
        int col = index % columns;
        int row = index / columns;
        return new ExtendedPanelLayout.Rect(
                origin.left() + col * origin.width(),
                origin.top() + row * origin.height(),
                origin.width(),
                origin.height());
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

        int scroll = stonecuttingPatternScrollbar == null ? 0 : stonecuttingPatternScrollbar.getCurrentScroll();
        int startIndex = scroll * STONECUTTING_RESULT_COLS;
        int endIndex = Math.min(recipes.size(), startIndex + STONECUTTING_RESULT_COLS * STONECUTTING_RESULT_ROWS);
        for (int i = startIndex; i < endIndex; i++) {
            if (inRect(relX, relY, getStonecuttingRecipeBounds(i - startIndex))) {
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
        renderCurioToggleOverlays(guiGraphics);
    }

    private void renderManualAnvilCost(GuiGraphics guiGraphics) {
        if (menu.getManualWorkspaceMode() != WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.ANVIL) {
            return;
        }
        int cost = menu.getManualAnvilCost();
        if (cost <= 0 || !hasManualAnvilResult()) {
            return;
        }

        boolean tooExpensive = cost >= ANVIL_TOO_EXPENSIVE_COST
                && (minecraft == null || minecraft.player == null || !minecraft.player.getAbilities().instabuild);
        Component text = tooExpensive
                ? Component.translatable("container.repair.expensive")
                : Component.translatable("container.repair.cost", cost);
        int color = tooExpensive || !mayPickupManualAnvilResult() ? 0xFF6060 : 0x80FF20;
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
                WCWT_STATES_TEXTURE, 256, 256, mouseX, mouseY, false);

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

        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scroll + i;
            if (rowIndex >= patternManagementRows.size()) {
                break;
            }
            int rowY = patternManagementPage.top() + i * PATTERN_MANAGEMENT_ROW_H;
            var row = patternManagementRows.get(rowIndex);
            if (row instanceof PatternManagementHeaderRow header) {
                renderPatternManagementHeader(guiGraphics, header.entry(), rowY, textColor, mouseX, mouseY);
            } else if (row instanceof PatternManagementSlotsRow slotsRow) {
                renderPatternManagementSlots(guiGraphics, slotsRow, rowY, mouseX, mouseY);
            }
        }
    }

    private void renderPatternManagementHeader(GuiGraphics guiGraphics, PatternProviderListPacket.Entry entry,
                                               int rowY, int textColor, int mouseX, int mouseY) {
        if (patternManagementShowSlots && entry.providerId() == selectedPatternProviderId) {
            guiGraphics.fill(patternManagementPage.left(), rowY,
                    patternManagementPage.left() + patternManagementPage.width(),
                    rowY + PATTERN_MANAGEMENT_ROW_H, 0x44FFFFFF);
        }
        var icon = entry.group().icon();
        if (icon != null) {
            guiGraphics.renderItem(icon.toStack(), patternManagementPage.left() + 2,
                    rowY + 1 + PATTERN_MANAGEMENT_HEADER_Y_OFFSET);
        }
        guiGraphics.drawString(font,
                font.substrByWidth(entry.group().name(), 94).getString(),
                patternManagementPage.left() + 20, rowY + 5 + PATTERN_MANAGEMENT_HEADER_Y_OFFSET,
                textColor, false);

        renderPatternManagementButton(guiGraphics, rowButton(patternManagementUploadButton, rowY),
                161, 0, 177, 0,
                WCWT_STATES_TEXTURE, 80, 32, 256, 256, mouseX, mouseY);
        renderPatternManagementButtonIcon(guiGraphics, rowButton(patternManagementUiButton, rowY),
                161, 0, 177, 0,
                WCWT_STATES_TEXTURE, 48, 0, 16, 16, 256, 256, mouseX, mouseY);
        renderPatternManagementButton(guiGraphics, rowButton(patternManagementHighlightButton, rowY),
                161, 0, 177, 0,
                WCWT_STATES_TEXTURE, 64, 32, 256, 256, mouseX, mouseY);
    }

    private void renderPatternManagementSlots(GuiGraphics guiGraphics, PatternManagementSlotsRow row, int rowY,
                                              int mouseX, int mouseY) {
        int relMouseX = mouseX - leftPos;
        int relMouseY = mouseY - topPos;
        for (int col = 0; col < row.slots(); col++) {
            int slot = row.offset() + col;
            ItemStack stack = row.entry().slots().getOrDefault(slot, ItemStack.EMPTY);
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
            if (shouldHighlightFocusedPatternSlot(row.entry().providerId(), slot)) {
                renderFocusedPatternManagementSlotHighlight(guiGraphics, x, y);
            }
            if (!stack.isEmpty()) {
                ItemStack displayStack = getPatternDisplayStack(stack);
                int ix = patternManagementSlotHitMinX(x);
                int iy = patternManagementSlotHitMinY(y);
                guiGraphics.renderItem(displayStack, ix, iy);
                guiGraphics.renderItemDecorations(font, displayStack, ix, iy);
            }
            if (isMouseOverPatternManagementSlot(relMouseX, relMouseY, x, y)) {
                renderSlotHighlight(guiGraphics, patternManagementSlotHitMinX(x), patternManagementSlotHitMinY(y), 0);
            }
        }
    }

    private ItemStack getPatternDisplayStack(ItemStack patternStack) {
        if (patternStack.getItem() instanceof appeng.crafting.pattern.EncodedPatternItem encodedPattern) {
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

    private static boolean isMouseOverPatternManagementSlot(int relMouseX, int relMouseY, int bgX, int bgY) {
        return inRect(relMouseX, relMouseY,
                patternManagementSlotHitMinX(bgX),
                patternManagementSlotHitMinY(bgY),
                PATTERN_MANAGEMENT_SLOT_HIT_SIZE,
                PATTERN_MANAGEMENT_SLOT_HIT_SIZE);
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

    private void scrollPatternManagementToProvider(long providerId, int slot) {
        if (patternManagementScrollbar == null) {
            return;
        }
        int visibleRows = Math.max(1, patternManagementPage.height() / PATTERN_MANAGEMENT_ROW_H);
        int targetRow = -1;
        for (int i = 0; i < patternManagementRows.size(); i++) {
            var row = patternManagementRows.get(i);
            if (row instanceof PatternManagementHeaderRow header && header.entry().providerId() == providerId) {
                targetRow = i;
                break;
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
            return;
        }
        int current = patternManagementScrollbar.getCurrentScroll();
        int desired = current;
        if (targetRow < current) {
            desired = targetRow;
        } else if (targetRow >= current + visibleRows) {
            desired = targetRow - visibleRows + 1;
        }
        patternManagementScrollbar.setCurrentScroll(Math.max(0, desired));
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
        int cacheIndex = getPatternCacheSlotIndex(slot);
        if (patternSelectionLockedMode) {
            if (cacheIndex >= 0) {
                if (mouseButton == 0 && clickType == ClickType.PICKUP && slot != null && slot.hasItem()) {
                    selectedPatternCacheIndex = cacheIndex;
                    ModNetworking.sendToServer(new PatternSelectionPacket(cacheIndex));
                }
                return; // 高级编码 UI 打开时锁定缓存槽，禁止拿走/放入样板。
            }
        }
        boolean predictCacheQuickMove = cacheIndex >= 0
                && clickType == ClickType.QUICK_MOVE
                && slot != null
                && slot.hasItem();
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
        if (predictCacheQuickMove) {
            slot.set(ItemStack.EMPTY);
            if (selectedPatternCacheIndex == cacheIndex) {
                selectedPatternCacheIndex = -1;
            }
        }
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
        if (this.minecraft.options.keyPickItem.matchesMouse(button)) {
            var slot = getSlotAt(mouseX, mouseY);
            if (menu.canModifyAmountForSlot(slot)) {
                var currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    var screen = new WcwtSetProcessingPatternAmountSubScreen(
                            this,
                            currentStack,
                            newStack -> {
                                InventoryActionPacket message = new InventoryActionPacket(
                                        InventoryAction.SET_FILTER,
                                        slot.index,
                                        GenericStack.wrapInItemStack(newStack));
                                appeng.core.sync.network.NetworkHandler.instance().sendToServer(message);
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
                rebuildPatternManagementRows();
                return true;
            }
            if (patternManageMappingField != null && patternManageMappingField.isMouseOver(mouseX, mouseY)) {
                patternManageMappingField.setValue("");
                return true;
            }
        }
        // 样板管理列表里的样板槽是纯客户端绘制命中区，须在 AE2 widgets.onMouseDown 之前拦截，
        // 否则可能被滚动条区等控件或下层 Repo 槽位吞掉。
        if ((button == 0 || button == 1) && handlePatternManagementProviderSlotInteract(mouseX, mouseY)) {
            return true;
        }
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
        if (toolkitPanel != null && toolkitPanel.isVisible()) {
            if (toolkitPanel.mouseClicked(mouseX, mouseY, button)) {
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
                        ModNetworking.sendToServer(new PatternSelectionPacket(i));
                        return true;
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    private Slot getSlotAt(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            if (relX >= slot.x && relX < slot.x + 16 && relY >= slot.y && relY < slot.y + 16) {
                return slot;
            }
        }
        return null;
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
            widgets.add("back", new TabButton(Icon.ARROW_LEFT, icon.getHoverName(), btn -> returnToParent()));

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
        if (keyCode == GLFW.GLFW_KEY_R
                && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0
                && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            reloadMainLayout();
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void reloadMainLayout() {
        mainLayout = ExtendedPanelLayout.load(MAIN_LAYOUT_ID);
        init();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("WCWT layout reloaded"), true);
        }
        WcwtMod.LOGGER.info("WCWT debug: reloaded main layout {}", MAIN_LAYOUT_ID);
    }

    private void refreshUpgradePanelLayout() {
        if (upgradesPanel == null) {
            return;
        }

        var bounds = upgradesPanel.getBounds();
        var fallback = new ExtendedPanelLayout.Rect(
                bounds.getX(),
                bounds.getY(),
                Math.max(0, bounds.getWidth()),
                Math.max(0, bounds.getHeight()));
        var rect = mainLayout.widget("scrollingUpgrades", fallback, imageWidth, imageHeight);
        upgradesPanel.setPosition(new Point(rect.left(), rect.top()));
    }

    private void refreshViewCellsPanelLayout() {
        if (viewCellsPanel == null) {
            return;
        }

        var viewBounds = viewCellsPanel.getBounds();
        var rect = resolvePanelRelativeToUpgrades("viewCells", viewBounds, 2, 0);
        viewCellsPanel.setPosition(new Point(rect.left(), rect.top()));
    }

    private void refreshViewCellsScrollbarLayout() {
        if (viewCellsPanel == null) {
            return;
        }

        var panelBounds = viewCellsPanel.getBounds();
        viewCellsPanel.setScrollbarOffset(new Point(
                mainLayout.widgetInt("viewCellsScrollbar", "left", WcwtUpgradeSlotBackground.SCROLLBAR_X),
                mainLayout.widgetInt("viewCellsScrollbar", "top", WcwtUpgradeSlotBackground.SCROLLBAR_Y)));
    }

    private void refreshCellUpgradePanelLayout() {
        if (cellUpgradesPanel == null || upgradesPanel == null) {
            return;
        }

        var cellBounds = cellUpgradesPanel.getBounds();
        var rect = resolvePanelRelativeToUpgrades("cellScrollingUpgrades", cellBounds, 2, 0);
        cellUpgradesPanel.setPosition(new Point(rect.left(), rect.top()));
    }

    private ExtendedPanelLayout.Rect resolvePanelRelativeToUpgrades(String widgetId,
            Rect2i panelBounds, int defaultOffsetX, int defaultOffsetY) {
        if (upgradesPanel == null) {
            return new ExtendedPanelLayout.Rect(
                    panelBounds.getX(),
                    panelBounds.getY(),
                    Math.max(0, panelBounds.getWidth()),
                    Math.max(0, panelBounds.getHeight()));
        }

        var parentBounds = upgradesPanel.getBounds();
        int offsetX = mainLayout.widgetInt(widgetId, "left", defaultOffsetX);
        int offsetY = mainLayout.widgetInt(widgetId, "top", defaultOffsetY);
        return new ExtendedPanelLayout.Rect(
                parentBounds.getX() + parentBounds.getWidth() + offsetX,
                parentBounds.getY() + offsetY,
                Math.max(0, panelBounds.getWidth()),
                Math.max(0, panelBounds.getHeight()));
    }

    private void refreshUpgradeScrollbarLayout() {
        if (upgradesPanel == null) {
            return;
        }

        var panelBounds = upgradesPanel.getBounds();
        var fallback = new ExtendedPanelLayout.Rect(
                panelBounds.getX() + WcwtUpgradeSlotBackground.SCROLLBAR_X,
                panelBounds.getY() + WcwtUpgradeSlotBackground.SCROLLBAR_Y,
                0,
                0);
        var rect = mainLayout.widget("upgradeScrollbar", fallback, imageWidth, imageHeight);
        upgradesPanel.setScrollbarOffset(new Point(
                rect.left() - panelBounds.getX(),
                rect.top() - panelBounds.getY()));
    }

    private void refreshCellUpgradeScrollbarLayout() {
        if (cellUpgradesPanel == null) {
            return;
        }

        var panelBounds = cellUpgradesPanel.getBounds();
        var fallback = new ExtendedPanelLayout.Rect(
                panelBounds.getX() + WcwtUpgradeSlotBackground.SCROLLBAR_X,
                panelBounds.getY() + WcwtUpgradeSlotBackground.SCROLLBAR_Y,
                0,
                0);
        var rect = mainLayout.widget("cellUpgradeScrollbar", fallback, imageWidth, imageHeight);
        cellUpgradesPanel.setScrollbarOffset(new Point(
                rect.left() - panelBounds.getX(),
                rect.top() - panelBounds.getY()));
    }

    private boolean handleStonecuttingRecipeClick(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        var recipe = getStonecuttingRecipeAt(relX, relY);
        if (recipe == null) {
            return false;
        }

        ModNetworking.sendToServer(new StonecuttingRecipeSelectionPacket(recipe.id()));
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
            refreshPatternProviders();
            sendPatternManagementAction(PatternManagementActionPacket.Action.ADD_MAPPING, -1, -1);
            if (patternManageSearchField != null && patternManageMappingField != null
                    && !patternManageMappingField.getValue().trim().isEmpty()) {
                patternManageSearchField.setValue(patternManageMappingField.getValue().trim());
                rebuildPatternManagementRows();
            }
            clearPatternManagementMappingField();
            return true;
        }
        if (inRect(relX, relY, patternManagementReloadButton)) {
            playPatternManagementClickSound();
            refreshPatternProviders();
            sendPatternManagementAction(PatternManagementActionPacket.Action.RELOAD_MAPPING, -1, -1);
            return true;
        }
        if (inRect(relX, relY, patternManagementDeleteButton)) {
            playPatternManagementClickSound();
            refreshPatternProviders();
            sendPatternManagementAction(PatternManagementActionPacket.Action.DELETE_MAPPING, -1, -1);
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
            return true;
        }
        if (inRect(relX, relY, patternManagementDisplayModeButton)) {
            playPatternManagementClickSound();
            patternManagementDisplayMode = patternManagementDisplayMode.next();
            rebuildPatternManagementRows();
            sendPatternManagementSettings();
            return true;
        }
        if (inRect(relX, relY, patternManagementDisplaySlotsButton)) {
            playPatternManagementClickSound();
            patternManagementShowSlots = !patternManagementShowSlots;
            rebuildPatternManagementRows();
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
            refreshPatternProviders();
            sendPatternManagementSettings();
            return true;
        }
        if (inRect(relX, relY, batchItemReplacementButton)) {
            playPatternManagementClickSound();
            batchItemSubstitutions = !batchItemSubstitutions;
            ModNetworking.sendToServer(new PatternModePacket(0, batchItemSubstitutions));
            return true;
        }
        if (inRect(relX, relY, batchFluidReplacementButton)) {
            playPatternManagementClickSound();
            batchFluidSubstitutions = !batchFluidSubstitutions;
            ModNetworking.sendToServer(new PatternModePacket(1, batchFluidSubstitutions));
            return true;
        }

        if (handlePatternManagementHeaderButtonClick(relX, relY)) {
            return true;
        }

        var hit = getPatternManagementHeaderAt(relX, relY);
        if (hit != null) {
            selectedPatternProviderId = hit.entry().providerId();
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
                sendPatternManagementOpenUi(hit.entry());
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
                return header.entry().providerId();
            }
            if (row instanceof PatternManagementSlotsRow slotsRow) {
                return slotsRow.entry().providerId();
            }
        }
        return -1L;
    }

    private void sendPatternManagementAction(PatternManagementActionPacket.Action action, long providerId, int cacheSlot) {
        ModNetworking.sendToServer(new PatternManagementActionPacket(
                action,
                providerId,
                cacheSlot,
                patternManageSearchField != null ? patternManageSearchField.getValue() : "",
                patternManageMappingField != null ? patternManageMappingField.getValue() : "",
                WcwtClientConfig.patternManagementShiftQuickEnabled(),
                false,
                0L,
                "",
                -1));
    }

    private void sendPatternManagementOpenUi(PatternProviderListPacket.Entry entry) {
        boolean hasLocation = entry.pos() != null && entry.dimension() != null;
        ModNetworking.sendToServer(new PatternManagementActionPacket(
                PatternManagementActionPacket.Action.OPEN_PROVIDER_UI,
                entry.providerId(),
                -1,
                patternManageSearchField != null ? patternManageSearchField.getValue() : "",
                patternManageMappingField != null ? patternManageMappingField.getValue() : "",
                WcwtClientConfig.patternManagementShiftQuickEnabled(),
                hasLocation,
                hasLocation ? entry.pos().asLong() : 0L,
                hasLocation ? entry.dimension().location().toString() : "",
                entry.face() != null ? entry.face().ordinal() : -1));
    }

    private boolean patternManagementShortcutActive() {
        Minecraft mc = Minecraft.getInstance();
        long w = mc.getWindow().getWindow();
        return mc.options.keyShift.isDown()
                || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** 仅在命中样板管理列表中的「虚拟样板槽」时触发；需在 AE widgets 拦截之前调用。 */
    private boolean handlePatternManagementProviderSlotInteract(double mouseX, double mouseY) {
        if (patternProviders.isEmpty()) {
            return false;
        }
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        PatternManagementSlotHit slotHit = getPatternManagementSlotAt(relX, relY);
        if (slotHit == null) {
            return false;
        }
        ItemStack carried = menu.getCarried();
        ItemStack slotStack = slotHit.entry().slots().getOrDefault(slotHit.slot(), ItemStack.EMPTY);
        if (patternManagementShortcutActive()) {
            if (slotStack.isEmpty()) {
                return false;
            }
        } else if (!canPredictPatternManagementProviderExchange(carried, slotStack)) {
            return false;
        }
        selectedPatternProviderId = slotHit.entry().providerId();
        selectedPatternProviderSlot = slotHit.slot();
        boolean predicted = patternManagementShortcutActive()
                ? predictQuickExtractPatternManagementProviderSlot(slotHit, slotStack)
                : predictExchangePatternManagementProviderSlot(slotHit, carried, slotStack);
        refreshPatternProviders();
        sendPatternManagementAction(patternManagementShortcutActive()
                        ? PatternManagementActionPacket.Action.QUICK_EXTRACT_PROVIDER_SLOT
                        : PatternManagementActionPacket.Action.EXCHANGE_PROVIDER_SLOT,
                slotHit.entry().providerId(), slotHit.slot());
        if (predicted) {
            playPatternManagementItemTransferSound(patternManagementShortcutActive()
                    ? false
                    : shouldPlayPatternManagementInsertSound(carried, slotStack));
        } else if (patternManagementShortcutActive()) {
            playPatternManagementItemTransferSound(false);
        } else {
            playPatternManagementClickSound();
        }
        return true;
    }

    private boolean canPredictPatternManagementProviderExchange(ItemStack carried, ItemStack slotStack) {
        if (carried.isEmpty()) {
            return !slotStack.isEmpty();
        }
        if (!appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(carried)) {
            return false;
        }
        return slotStack.isEmpty() || carried.getCount() == 1;
    }

    private boolean shouldPlayPatternManagementInsertSound(ItemStack carried, ItemStack slotStack) {
        return !carried.isEmpty() && (slotStack.isEmpty() || carried.getCount() == 1);
    }

    private boolean predictQuickExtractPatternManagementProviderSlot(PatternManagementSlotHit slotHit, ItemStack slotStack) {
        if (slotStack.isEmpty()) {
            return false;
        }
        mutatePatternManagementProviderSlot(slotHit.entry(), slotHit.slot(), ItemStack.EMPTY);
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.getInventory().placeItemBackInInventory(slotStack.copy(), false);
        }
        rebuildPatternManagementRows();
        return true;
    }

    private boolean predictExchangePatternManagementProviderSlot(PatternManagementSlotHit slotHit,
                                                                 ItemStack carried,
                                                                 ItemStack slotStack) {
        if (carried.isEmpty()) {
            if (slotStack.isEmpty()) {
                return false;
            }
            mutatePatternManagementProviderSlot(slotHit.entry(), slotHit.slot(), ItemStack.EMPTY);
            menu.setCarried(slotStack.copy());
            rebuildPatternManagementRows();
            return true;
        }
        if (!appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(carried)) {
            return false;
        }

        ItemStack toPlace = carried.copy();
        toPlace.setCount(1);
        if (slotStack.isEmpty()) {
            mutatePatternManagementProviderSlot(slotHit.entry(), slotHit.slot(), toPlace);
            ItemStack updatedCarried = carried.copy();
            updatedCarried.shrink(1);
            menu.setCarried(updatedCarried.isEmpty() ? ItemStack.EMPTY : updatedCarried);
            rebuildPatternManagementRows();
            return true;
        }
        if (carried.getCount() != 1) {
            return false;
        }

        mutatePatternManagementProviderSlot(slotHit.entry(), slotHit.slot(), carried.copy());
        menu.setCarried(slotStack.copy());
        rebuildPatternManagementRows();
        return true;
    }

    private void mutatePatternManagementProviderSlot(PatternProviderListPacket.Entry entry, int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            entry.slots().remove(slot);
        } else {
            entry.slots().put(slot, stack.copy());
        }
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
            ModNetworking.sendToServer(new ToolkitNetworkToolDepositPacket());
            playPatternManagementClickSound();
            lastAeNetworkToolkitDoubleClickSlot = null;
            lastAeNetworkToolkitDoubleClickMs = 0L;
            return true;
        }
        lastAeNetworkToolkitDoubleClickSlot = hit;
        lastAeNetworkToolkitDoubleClickMs = now;
        return false;
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
        refreshPatternProviders();
        sendPatternManagementAction(
                PatternManagementActionPacket.Action.QUICK_INSERT_FIRST_PROVIDER,
                quickInsertTargetProviderId(),
                menu.slots.indexOf(slot));
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
            return new PatternManagementHeaderHit(header.entry(), visibleRow);
        }
        return null;
    }

    private PatternManagementHeaderButtonHit getPatternManagementHeaderButtonAt(int relX, int relY) {
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
                    return new PatternManagementHeaderButtonHit(header.entry(), PatternManagementHeaderButton.UPLOAD);
                }
                if (inRect(relX, relY, rowButton(patternManagementUiButton, rowY))) {
                    return new PatternManagementHeaderButtonHit(header.entry(), PatternManagementHeaderButton.UI);
                }
                if (inRect(relX, relY, rowButton(patternManagementHighlightButton, rowY))) {
                    return new PatternManagementHeaderButtonHit(header.entry(), PatternManagementHeaderButton.HIGHLIGHT);
                }
            }
        }
        return null;
    }

    private PatternManagementSlotHit getPatternManagementSlotAt(int relX, int relY) {
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
            if (packet instanceof com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
                ModNetworking.sendToServer(payload);
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
                int toggleX = leftPos + slot.x + 11;
                int toggleY = topPos + slot.y - 3;
                if (mouseX >= toggleX && mouseX < toggleX + 8 && mouseY >= toggleY && mouseY < toggleY + 8) {
                    return curioSlot;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (getCurioToggleSlotAt(mouseX, mouseY) != null) {
            return false;
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderCosmeticArmorToggleOverlays(guiGraphics);

        ItemStack stonecuttingStack = getStonecuttingResultUnderMouse(mouseX, mouseY);
        if (!stonecuttingStack.isEmpty()) {
            guiGraphics.renderTooltip(font, stonecuttingStack, mouseX, mouseY);
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
        return hit.entry().slots().getOrDefault(hit.slot(), ItemStack.EMPTY);
    }

    private Component getPatternManagementTooltip(int mouseX, int mouseY) {
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
        return hit.entry().group().name();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
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
            return true;
        }
        if (scrollY != 0 && curiosScrollbar != null && curiosScrollbar.isVisible()
                && isMouseOverCuriosPanel(mouseX, mouseY)
                && curiosScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            return true;
        }
        if (scrollY != 0 && toolkitScrollbar != null && toolkitScrollbar.isVisible()
                && isMouseOverToolkitPanel(mouseX, mouseY)
                && toolkitScrollbar.onMouseWheel(
                        new Point((int) Math.round(mouseX - leftPos), (int) Math.round(mouseY - topPos)), scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    private boolean isMouseOverPatternEncoding(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        var bg = getPatternEncodingBackgroundOrigin();
        return relX >= bg.left() && relX < bg.left() + bg.width()
                && relY >= bg.top() && relY < bg.top() + bg.height();
    }

    private boolean isMouseOverPatternManagement(double mouseX, double mouseY) {
        int relX = (int) Math.round(mouseX - leftPos);
        int relY = (int) Math.round(mouseY - topPos);
        return relX >= patternManagementPage.left()
                && relX < patternManagementPage.left() + patternManagementPage.width() + 16
                && relY >= patternManagementPage.top()
                && relY < patternManagementPage.top() + patternManagementPage.height();
    }
    
    @Override
    public void onClose() {
        super.onClose();
    }

    private sealed interface PatternManagementRow permits PatternManagementHeaderRow, PatternManagementSlotsRow {
    }

    private record PatternManagementHeaderRow(PatternProviderListPacket.Entry entry) implements PatternManagementRow {
    }

    private record PatternManagementSlotsRow(PatternProviderListPacket.Entry entry, int offset, int slots)
            implements PatternManagementRow {
    }

    private record PatternManagementHeaderHit(PatternProviderListPacket.Entry entry, int visibleRow) {
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

    private record PreferredUploadProvider(long providerId, String providerName) {
        private static final PreferredUploadProvider NONE = new PreferredUploadProvider(-1L, "");
    }

    private static final class PreferredUploadProviderGroup {
        private long bestProviderId;
        private final String name;
        private int bestEmptySlots;

        private PreferredUploadProviderGroup(long bestProviderId, String name, int bestEmptySlots) {
            this.bestProviderId = bestProviderId;
            this.name = name;
            this.bestEmptySlots = bestEmptySlots;
        }

        private long bestProviderId() {
            return bestProviderId;
        }

        private String name() {
            return name;
        }

        private void merge(long providerId, int emptySlots) {
            if (emptySlots > bestEmptySlots) {
                bestProviderId = providerId;
                bestEmptySlots = emptySlots;
            }
        }
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
        OUT(112, 32, "gui.extendedae.ex_pattern_access_terminal.search_mode.01"),
        IN(96, 32, "gui.extendedae.ex_pattern_access_terminal.search_mode.02"),
        IN_OUT(128, 32, "gui.extendedae.ex_pattern_access_terminal.search_mode.03");

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

