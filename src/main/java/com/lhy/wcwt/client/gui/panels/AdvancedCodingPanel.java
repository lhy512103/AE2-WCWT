package com.lhy.wcwt.client.gui.panels;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.gui.widgets.BulkCompressionCutoffButton;
import com.lhy.wcwt.client.gui.widgets.DirectionInputButton;
import com.lhy.wcwt.client.gui.widgets.IconButton;
import com.lhy.wcwt.compat.WcwtMegaCellsCompat;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.network.CellConfigSetPacket;
import com.lhy.wcwt.network.CellWorkbenchActionPacket;
import com.lhy.wcwt.network.CopyPatternPacket;
import com.lhy.wcwt.network.DirectionChangePacket;
import com.lhy.wcwt.network.ReplacePatternPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import appeng.api.config.CopyMode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.Scrollbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * 高级编码面板
 * 包含：
 * - 样板方向选择器
 * - 复制样板功能
 * - 替换物品功能
 * - 元件工作台
 */
public class AdvancedCodingPanel extends ExtendedUIPanel implements ITooltip {
    private static final ResourceLocation PANEL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_advanced_coding.png");
    /** 覆盖父类 STATES_TEXTURE，指向实际路径（ae2 命名空间下的 wcwt 子目录）。 */
    private static final ResourceLocation WCWT_STATES =
        ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");
    // ─── 样板输入方向编辑区（上半部分） ──────────────────────────
    // 数据来自 assets/ae2/screens/terminals/encoding/wcwt_advanced_coding.json
    // manage_scrollbar: left=6, top=20, height=56  → 滚动条在列表左侧
    private static final int MANAGE_SCROLLBAR_X = 6;
    private static final int MANAGE_SCROLLBAR_Y = 20;
    private static final int MANAGE_SCROLLBAR_HEIGHT = 56;

    private static final int ROW_HEIGHT = 18;
    private static final int SLOT_SIZE = ROW_HEIGHT;
    private static final int ROW_SPACING = 2;
    private static final int VISIBLE_ROWS = 3;
    /** 列表锚点 = 滚动条右侧 + 12px（滚动条宽度）+ 留白 */
    // 视觉上整体偏右下 1px，所以这里都 -1 以便槽底/物品/方向按钮一起向左上挪 1px。
    private static final int LIST_ANCHOR_X = 16;
    private static final int LIST_ANCHOR_Y = 19;
    private static final int DIRECTION_BUTTONS_OFFSET_X = 1;
    private static final int DIRECTION_BUTTONS_WIDTH = 12;
    private static final int DIRECTION_BUTTONS_HEIGHT = 14;

    // ─── 复制样板 / 替换功能区（中间一行，y=86） ──────────────────
    // 槽位与按钮坐标均来自 JSON 设计稿
    private static final int COPY_PATTERN_SLOT_X = 4,   COPY_PATTERN_SLOT_Y = 86;
    private static final int COPY_PATTERN_BTN_X  = 24,  COPY_PATTERN_BTN_Y  = 86;
    private static final int COPY_PATTERN_BTN_W  = 17,  COPY_PATTERN_BTN_H  = 16;
    private static final int REPLACE_INPUT_X     = 51,  REPLACE_INPUT_Y     = 86;
    private static final int REPLACE_OUTPUT_X    = 86,  REPLACE_OUTPUT_Y    = 86;
    private static final int REPLACE_BTN_X       = 106, REPLACE_BTN_Y       = 86;
    private static final int REPLACE_BTN_W       = 23,  REPLACE_BTN_H       = 16;
    /** 替换区两个 ghost 框里的物品视觉上偏右下，整体向左上挪 1px。 */
    private static final int REPLACE_GHOST_RENDER_OFFSET_X = 0;
    private static final int REPLACE_GHOST_RENDER_OFFSET_Y = 0;

    // ─── 元件编辑区（下半部分） ────────────────────────────────
    private static final int CELL_EDIT_TITLE_X   = 4,  CELL_EDIT_TITLE_Y   = 112;
    private static final int STORAGE_CELL_SLOT_X = 112, STORAGE_CELL_SLOT_Y = 107;
    private static final int CELL_CONFIG_GRID_X  = 4,  CELL_CONFIG_GRID_Y  = 126;
    /** config 网格默认显示的行 / 列数。JSON 注释里说"6 列换行"，3 行可放 18 格，
     *  下边界 ≈ 126 + 3×18 = 180，符合 cell_scrollbar 高 52 → 底端 178 的设计。 */
    private static final int CELL_CONFIG_ROWS    = 3;
    private static final int CELL_CONFIG_COLS    = 6;
    private static final int CELL_SCROLLBAR_X    = 118, CELL_SCROLLBAR_Y   = 126;
    private static final int CELL_SCROLLBAR_HEIGHT = 52;
    /** JEI 绿色目标高亮区域：比 18x18 槽位略收一圈，并整体向左上挪 1px。 */
    private static final int GHOST_TARGET_OFFSET_X = -1;
    private static final int GHOST_TARGET_OFFSET_Y = -1;
    private static final int GHOST_TARGET_INSET = 1;
    private static final int GHOST_TARGET_SIZE = 16;
    /** 元件工作台底部四个按钮：12×12，底图使用 AE2 Toolbar 按钮底图（Icon.TOOLBAR_BUTTON_BACKGROUND） */
    private static final int CELL_BTN_Y          = 112;
    private static final int CELL_BTN_W          = 12, CELL_BTN_H = 12;
    private static final int PARTITIONED_STORAGE_BTN_X = 58;
    private static final int CELL_CLEAR_BTN_X          = 71;
    private static final int CELL_COPY_MODE_BTN_X      = 84;
    private static final int CELL_COMPRESSION_CUTOFF_BTN_X = CELL_COPY_MODE_BTN_X + 14;
    /** 元件编辑区拖拽/悬停高亮。原 0x80，降低 20% → 0x66。 */
    private static final int CELL_CONFIG_HOVER_COLOR = 0x66FFFFFF;

    public static final int PANEL_WIDTH   = 134;
    public static final int PANEL_HEIGHT  = 186;
    /** PNG 内有效宽度。*/
    private static final int BACKGROUND_BLIT_WIDTH = 134;
    private final ExtendedPanelLayout layout = ExtendedPanelLayout.load("wcwt_advanced_coding.json");
    private ExtendedPanelLayout.Rect copyPatternSlotRect =
            new ExtendedPanelLayout.Rect(COPY_PATTERN_SLOT_X, COPY_PATTERN_SLOT_Y, 16, 16);
    private ExtendedPanelLayout.Rect replaceInputSlotRect =
            new ExtendedPanelLayout.Rect(REPLACE_INPUT_X, REPLACE_INPUT_Y, 16, 16);
    private ExtendedPanelLayout.Rect replaceOutputSlotRect =
            new ExtendedPanelLayout.Rect(REPLACE_OUTPUT_X, REPLACE_OUTPUT_Y, 16, 16);
    private ExtendedPanelLayout.Rect storageCellSlotRect =
            new ExtendedPanelLayout.Rect(STORAGE_CELL_SLOT_X, STORAGE_CELL_SLOT_Y, 16, 16);

    // 输入列表：物品->方向映射
    private LinkedHashMap<AEKey, Direction> inputList = new LinkedHashMap<>();

    // 方向按钮：每个输入物品有7个按钮（ANY, N, E, S, W, UP, DOWN）
    private final HashMap<AEKey, DirectionInputButton[]> directionButtons = new HashMap<>();

    // 输入行列表
    private final ArrayList<InputRow> rows = new ArrayList<>();

    // ─── 功能按钮（仅 UI 骨架，点击事件 TODO） ──────────────────────
    private IconButton copyPatternBtn;
    private IconButton replaceBtn;
    private IconButton partitionedStorageBtn;
    private IconButton cellClearBtn;
    private IconButton cellCopyModeBtn;
    private BulkCompressionCutoffButton bulkCompressionCutoffBtn;

    /** Menu 引用（由 Screen 注入），用于读取槽位物品渲染和获取 GuiSync 状态。 */
    private Supplier<WirelessComprehensiveWorkTerminalMenu> menuSupplier;

    private List<Component> currentTooltip = List.of();
    private Rect2i currentTooltipArea = new Rect2i(0, 0, 0, 0);

    /** 上次解码过的当前选中缓存样板，用于检测变化并避免每帧重解码。 */
    private net.minecraft.world.item.ItemStack lastDecodedPattern = net.minecraft.world.item.ItemStack.EMPTY;
    /**
     * 元件 config 的客户端乐观覆盖层。
     *
     * <p>AE2 原元件工作台使用真实 CellPartitionSlot，因此菜单会直接同步每个 fake slot 内容。
     * WCWT 这里是 3×6 滚动自绘伪槽，只发 CellConfigSetPacket 写服务端；服务端确实写进元件了，
     * 但当前槽里的 ItemStack 组件不一定马上广播回来，导致 UI 会“闪一下又消失”。这里在客户端先记一层
     * 覆盖值：containsKey(idx)==true 且 value==null 表示该格刚被清空。</p>
     */
    private final HashMap<Integer, AEKey> localCellConfigOverrides = new HashMap<>();
    private net.minecraft.world.item.ItemStack lastCellStackForOverrides = net.minecraft.world.item.ItemStack.EMPTY;

    /** 样板替换区两个 ghost 标记：左侧为被替换对象，右侧为空时表示删除匹配项。 */
    private @Nullable AEKey replaceWhatKey;
    private @Nullable AEKey replaceWithKey;

    // ─── 滑块（参考 AAE AdvPatternEncoderScreen.resetScrollbar）──
    // 通过 panel 自身的 mouse 事件转发：仅当鼠标命中滑块矩形 / 范围>0 / 滚轮时才生效，
    // 因此不会出现"range==0 时左键被吞"的问题。
    private Scrollbar manageScrollbar;
    private Scrollbar cellScrollbar;

    @Override
    public void setPosition(int newX, int newY) {
        super.setPosition(newX, newY);
        // Screen 流程：先 init() 再 setPosition() —— init() 时 x=y=0 创建的按钮坐标失效，
        // 这里按面板新坐标重新定位。
        repositionButtons();
    }

    private void repositionButtons() {
        if (copyPatternBtn != null) {
            copyPatternBtn.setPosition(x + COPY_PATTERN_BTN_X, y + COPY_PATTERN_BTN_Y);
        }
        if (replaceBtn != null) {
            replaceBtn.setPosition(x + REPLACE_BTN_X, y + REPLACE_BTN_Y);
        }
        if (partitionedStorageBtn != null) {
            partitionedStorageBtn.setPosition(x + PARTITIONED_STORAGE_BTN_X, y + CELL_BTN_Y);
        }
        if (cellClearBtn != null) {
            cellClearBtn.setPosition(x + CELL_CLEAR_BTN_X, y + CELL_BTN_Y);
        }
        if (cellCopyModeBtn != null) {
            cellCopyModeBtn.setPosition(x + CELL_COPY_MODE_BTN_X, y + CELL_BTN_Y);
        }
        if (bulkCompressionCutoffBtn != null) {
            bulkCompressionCutoffBtn.setPosition(x + CELL_COMPRESSION_CUTOFF_BTN_X, y + CELL_BTN_Y);
        }
        // 滑块只渲染、不接事件，但渲染前必须有正确的 displayX/Y。
        if (manageScrollbar != null) {
            manageScrollbar.setPosition(new appeng.client.Point(
                    x + MANAGE_SCROLLBAR_X, y + MANAGE_SCROLLBAR_Y));
        }
        if (cellScrollbar != null) {
            cellScrollbar.setPosition(new appeng.client.Point(
                    x + CELL_SCROLLBAR_X, y + CELL_SCROLLBAR_Y));
        }
    }
    
    public AdvancedCodingPanel(int x, int y) {
        super(x, y, PANEL_WIDTH, PANEL_HEIGHT);
    }

    /** 面板左上角绝对 X（Screen 用来定位升级槽实际坐标）。 */
    public int getX() { return x; }
    public int getY() { return y; }

    public void setMenuSupplier(Supplier<WirelessComprehensiveWorkTerminalMenu> menuSupplier) {
        this.menuSupplier = menuSupplier;
    }

    public int getCellConfigScroll() {
        return cellScrollbar != null ? cellScrollbar.getCurrentScroll() : 0;
    }

    public int getCellConfigGridLeft() {
        return x + CELL_CONFIG_GRID_X;
    }

    public int getCellConfigGridTop() {
        return y + CELL_CONFIG_GRID_Y;
    }

    public int getCellConfigRows() {
        return CELL_CONFIG_ROWS;
    }

    public int getCellConfigCols() {
        return CELL_CONFIG_COLS;
    }

    public Rect2i getCellConfigSlotArea(int slotIdx) {
        int row = slotIdx / CELL_CONFIG_COLS - getCellConfigScroll();
        int col = slotIdx % CELL_CONFIG_COLS;
        return new Rect2i(
                getCellConfigGridLeft() + col * 18 + GHOST_TARGET_INSET + GHOST_TARGET_OFFSET_X,
                getCellConfigGridTop() + row * 18 + GHOST_TARGET_INSET + GHOST_TARGET_OFFSET_Y,
                GHOST_TARGET_SIZE, GHOST_TARGET_SIZE);
    }

    public Rect2i getReplaceInputArea() {
        return toGhostArea(replaceInputSlotRect);
    }

    public Rect2i getReplaceOutputArea() {
        return toGhostArea(replaceOutputSlotRect);
    }

    private Rect2i toGhostArea(ExtendedPanelLayout.Rect slotRect) {
        return new Rect2i(
                x + slotRect.left() + GHOST_TARGET_INSET + GHOST_TARGET_OFFSET_X,
                y + slotRect.top() + GHOST_TARGET_INSET + GHOST_TARGET_OFFSET_Y,
                GHOST_TARGET_SIZE,
                GHOST_TARGET_SIZE);
    }

    public void hideBulkCompressionCutoffButton() {
        if (bulkCompressionCutoffBtn != null) {
            bulkCompressionCutoffBtn.setItem(net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    private Rect2i toAbsoluteRect(ExtendedPanelLayout.Rect rect) {
        return new Rect2i(x + rect.left(), y + rect.top(), rect.width(), rect.height());
    }

    public boolean isCellConfigSlotVisible(int slotIdx) {
        int row = slotIdx / CELL_CONFIG_COLS - getCellConfigScroll();
        return row >= 0 && row < CELL_CONFIG_ROWS;
    }

    public void setCellConfigSlotFromItem(int slotIdx, net.minecraft.world.item.ItemStack stack) {
        if (slotIdx < 0 || stack.isEmpty()) {
            return;
        }
        setCellConfigSlotFromKey(slotIdx, AEItemKey.of(stack));
    }

    /**
     * 通用入口：把任意 AEKey（物品或流体）写入元件 config 第 slotIdx 格。
     * JEI ghost handler 同时支持物品/流体两种 ITypedIngredient，统一走这里。
     */
    public void setCellConfigSlotFromKey(int slotIdx, @Nullable AEKey key) {
        if (slotIdx < 0 || key == null) return;
        var menu = menuSupplier != null ? menuSupplier.get() : null;
        if (menu != null && !menu.isCellConfigKeyAllowed(slotIdx, key)) {
            return;
        }
        rememberLocalCellConfig(slotIdx, key);
        PacketDistributor.sendToServer(new CellConfigSetPacket(slotIdx, key));
    }

    public void setReplaceInputFromKey(@Nullable AEKey key) {
        replaceWhatKey = key;
    }

    public void setReplaceOutputFromKey(@Nullable AEKey key) {
        replaceWithKey = key;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            currentTooltip = List.of();
            return;
        }

        renderBackground(guiGraphics);
        renderContent(guiGraphics, mouseX, mouseY, partialTick);

        if (returnButton != null) {
            returnButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (var child : children) {
            if (child instanceof Renderable renderable) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        // 必须在 children 之后画，否则 IconButton 会把文字盖住。
        renderCopyReplaceLabels(guiGraphics, mouseX, mouseY);
        updatePanelTooltip(mouseX, mouseY);
    }

    /** 从 GuiSync 字段读取当前复制模式（由服务端同步），用于按钮图标切换。 */
    private boolean isCopyModeOn() {
        if (menuSupplier == null) return false;
        var menu = menuSupplier.get();
        return menu != null && menu.cellCopyMode == CopyMode.KEEP_ON_REMOVE;
    }
    
    @Override
    public void init() {
        children.clear();
        copyPatternSlotRect = layout.slot("copy_pattern", copyPatternSlotRect);
        replaceInputSlotRect = layout.slot("replace_input", replaceInputSlotRect);
        replaceOutputSlotRect = layout.slot("replace_output", replaceOutputSlotRect);
        storageCellSlotRect = layout.slot("STORAGE_CELL", storageCellSlotRect);

        // 来自 wcwt_advanced_coding.json -> widgets.expandui_return
        configureReturnButton(23, -7, 20, 20);
        createReturnButton();

        // ─── 复制样板按钮（17×16）────────────────────────────────────
        copyPatternBtn = new IconButton(
                x + COPY_PATTERN_BTN_X, y + COPY_PATTERN_BTN_Y,
                COPY_PATTERN_BTN_W, COPY_PATTERN_BTN_H,
                192, 160, 224, 160, 17, 16,
                WCWT_STATES,
                Component.translatable("gui.wcwt.advanced_coding.copy_pattern"),
                btn -> PacketDistributor.sendToServer(new CopyPatternPacket()));
        children.add(copyPatternBtn);

        // ─── 替换物品按钮（23×16）────────────────────────────────────
        replaceBtn = new IconButton(
                x + REPLACE_BTN_X, y + REPLACE_BTN_Y,
                REPLACE_BTN_W, REPLACE_BTN_H,
                0, 192, 32, 192, 23, 16,
                WCWT_STATES,
                Component.translatable("gui.wcwt.advanced_coding.replace"),
                btn -> PacketDistributor.sendToServer(new ReplacePatternPacket(replaceWhatKey, replaceWithKey)));
        children.add(replaceBtn);

        // ─── 元件编辑区三按钮：12×12，底图用 AE2 Toolbar 按钮底图 ───
        partitionedStorageBtn = new IconButton(
                x + PARTITIONED_STORAGE_BTN_X, y + CELL_BTN_Y,
                CELL_BTN_W, CELL_BTN_H,
                0, 0, 0, 0, 1, 1,
                WCWT_STATES,
                Component.translatable("gui.wcwt.advanced_coding.partitioned_storage"),
                btn -> {
                    localCellConfigOverrides.clear();
                    PacketDistributor.sendToServer(new CellWorkbenchActionPacket(CellWorkbenchActionPacket.Action.PARTITION));
                })
                .useAE2ToolbarBackground()
                .setOverlayOffsetY(-1)
                .setOverlayIcon(() -> Icon.COG)
                .setTooltipLines(List.of(
                        Component.translatable("gui.wcwt.advanced_coding.partitioned_storage"),
                        Component.translatable("gui.wcwt.advanced_coding.partitioned_storage.desc")
                                .withStyle(ChatFormatting.GRAY)));
        children.add(partitionedStorageBtn);

        cellClearBtn = new IconButton(
                x + CELL_CLEAR_BTN_X, y + CELL_BTN_Y,
                CELL_BTN_W, CELL_BTN_H,
                0, 0, 0, 0, 1, 1,
                WCWT_STATES,
                Component.translatable("gui.wcwt.advanced_coding.cell_clear"),
                btn -> {
                    for (int i = 0; i < getCellConfigSize(); i++) {
                        rememberLocalCellConfig(i, null);
                    }
                    PacketDistributor.sendToServer(new CellWorkbenchActionPacket(CellWorkbenchActionPacket.Action.CLEAR));
                })
                .useAE2ToolbarBackground()
                .setOverlayOffsetY(-1)
                .setOverlayIcon(() -> Icon.CLEAR)
                .setTooltipLines(List.of(
                        Component.translatable("gui.wcwt.advanced_coding.cell_clear"),
                        Component.translatable("gui.wcwt.advanced_coding.cell_clear.desc")
                                .withStyle(ChatFormatting.GRAY)));
        children.add(cellClearBtn);

        cellCopyModeBtn = new IconButton(
                x + CELL_COPY_MODE_BTN_X, y + CELL_BTN_Y,
                CELL_BTN_W, CELL_BTN_H,
                0, 0, 0, 0, 1, 1,
                WCWT_STATES,
                Component.translatable("gui.wcwt.advanced_coding.cell_copy_mode"),
                btn -> PacketDistributor.sendToServer(new CellWorkbenchActionPacket(CellWorkbenchActionPacket.Action.COPY_MODE)))
                .useAE2ToolbarBackground()
                .setOverlayOffsetY(-1)
                .setOverlayIcon(() -> isCopyModeOn() ? Icon.COPY_MODE_ON : Icon.COPY_MODE_OFF)
                .setTooltipLines(List.of(
                        Component.translatable("gui.wcwt.advanced_coding.cell_copy_mode"),
                        Component.translatable("gui.wcwt.advanced_coding.cell_copy_mode.desc")
                                .withStyle(ChatFormatting.GRAY)));
        children.add(cellCopyModeBtn);

        bulkCompressionCutoffBtn = new BulkCompressionCutoffButton(towardMoreCompressed ->
                PacketDistributor.sendToServer(new CellWorkbenchActionPacket(towardMoreCompressed
                        ? CellWorkbenchActionPacket.Action.COMPRESSION_CUTOFF_NEXT
                        : CellWorkbenchActionPacket.Action.COMPRESSION_CUTOFF_PREVIOUS)));
        children.add(bulkCompressionCutoffBtn);

        // ─── 滑块（Scrollbar.SMALL = 与 ScrollingUpgradesPanel 升级槽完全相同的风格） ───
        // 当前没真实滚动需求，range 保持 0 → 自动渲染为 disabled 灰图标，不接事件。
        manageScrollbar = new Scrollbar(Scrollbar.SMALL);
        manageScrollbar.setHeight(MANAGE_SCROLLBAR_HEIGHT);
        manageScrollbar.setCaptureMouseWheel(false); // 仅在面板范围内响应滚轮

        cellScrollbar = new Scrollbar(Scrollbar.SMALL);
        cellScrollbar.setHeight(CELL_SCROLLBAR_HEIGHT);
        cellScrollbar.setCaptureMouseWheel(false);

        repositionButtons();  // 同步两个 scrollbar 的 displayX/Y
        refreshList();
    }
    
    /**
     * 更新输入列表
     * @param inputList 新的输入列表
     */
    public void updateInputList(LinkedHashMap<AEKey, Direction> inputList) {
        this.inputList.clear();
        
        // 清除旧的方向按钮
        directionButtons.forEach((k, v) -> {
            for (var btn : v) {
                children.remove(btn);
            }
        });
        directionButtons.clear();
        rows.clear();
        
        this.inputList = inputList;
        refreshList();
    }
    
    /**
     * 刷新输入列表和方向按钮
     */
    private void refreshList() {
        for (var key : inputList.keySet()) {
            rows.add(new InputRow(key, inputList.get(key)));
            
            // 为每个输入创建7个方向按钮
            DirectionInputButton[] buttons = new DirectionInputButton[7];
            for (var i = 0; i < 7; i++) {
                var button = new DirectionInputButton(
                    0, 0,
                    DIRECTION_BUTTONS_WIDTH,
                    DIRECTION_BUTTONS_HEIGHT,
                    getDirButtonTextures(i),
                    this::directionButtonPressed
                );
                button.setTooltip(Tooltip.create(getDirButtonText(i)));
                button.setKey(key);
                button.setIndex(i);
                button.visible = false;
                buttons[i] = button;
                children.add(button);
            }
            
            directionButtons.put(key, buttons);
        }
        resetScrollbarRange();
    }

    /** 根据当前输入条数刷新管理区滑块的可滚动范围；元件区暂无滚动需求，保持 0。*/
    private void resetScrollbarRange() {
        if (manageScrollbar != null) {
            int max = Math.max(0, inputList.size() - VISIBLE_ROWS);
            manageScrollbar.setRange(0, max, 1);
        }
        if (cellScrollbar != null) {
            int cellSize = getCellConfigSize();
            // 网格 6 列，可视 3 行 → 总行数 = ceil(cellSize / 6)，可滚动行数 = max(0, totalRows - CELL_CONFIG_ROWS)
            int totalRows = (cellSize + CELL_CONFIG_COLS - 1) / CELL_CONFIG_COLS;
            int max = Math.max(0, totalRows - CELL_CONFIG_ROWS);
            cellScrollbar.setRange(0, max, 1);
        }
    }

    /** 当前放入的元件 ItemStack（可能为 EMPTY）。 */
    private net.minecraft.world.item.ItemStack getCellStack() {
        if (menuSupplier == null) return net.minecraft.world.item.ItemStack.EMPTY;
        var menu = menuSupplier.get();
        if (menu == null) return net.minecraft.world.item.ItemStack.EMPTY;
        var slots = menu.getSlots(WcwtSlotSemantics.WCWT_STORAGE_CELL);
        return slots.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : slots.get(0).getItem();
    }

    /** 当前元件的 config 槽数（无元件则 0）。 */
    private int getCellConfigSize() {
        var menu = menuSupplier != null ? menuSupplier.get() : null;
        return menu != null ? menu.getCellConfigInventorySize() : 0;
    }

    /**
     * 读取元件编辑区第 idx 格内容（双端可调用）。直接走 menu 里的统一入口，避免依赖客户端不存在的真实 CONFIG 槽。
     */
    private @Nullable GenericStack getCellConfigStack(int idx) {
        if (localCellConfigOverrides.containsKey(idx)) {
            var key = localCellConfigOverrides.get(idx);
            return key == null ? null : new GenericStack(key, 0);
        }
        var menu = menuSupplier != null ? menuSupplier.get() : null;
        return menu != null ? menu.getCellConfigStack(idx) : null;
    }

    private void syncLocalCellConfigOverridesWithCellStack() {
        var stack = getCellStack();
        if (!net.minecraft.world.item.ItemStack.matches(stack, lastCellStackForOverrides)) {
            localCellConfigOverrides.clear();
            lastCellStackForOverrides = stack.copy();
        }
    }

    private void rememberLocalCellConfig(int slotIdx, @Nullable AEKey key) {
        localCellConfigOverrides.put(slotIdx, key);
    }

    /**
     * 方向按钮点击处理。
     * 立即在客户端更新 inputList 和 rows（使高亮即时变化），同时发包告知服务端。
     */
    private void directionButtonPressed(Button b) {
        DirectionInputButton button = (DirectionInputButton) b;
        AEKey key = button.getKey();
        Direction dir = button.getDirection(); // null = ANY (index 0)

        // 立即更新本地状态，无需等服务端回包
        if (inputList.containsKey(key)) {
            inputList.put(key, dir);
            // 仅重建 rows（方向按钮实例保持不动，不触发 updateInputList 全量重建）
            rows.clear();
            inputList.forEach((k, v) -> rows.add(new InputRow(k, v)));
        }

        PacketDistributor.sendToServer(new DirectionChangePacket(key, dir));
    }
    
    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        // 面板背景纹理（wcwt_advanced_coding.png）只覆盖左侧 134×186，
        // 右侧 22×186 用作"元件升级卡列"，背景由 renderCellUpgradeColumn() 处理。
        guiGraphics.blit(PANEL_TEXTURE, x, y, 0, 0, BACKGROUND_BLIT_WIDTH, height, 256, 256);
    }
    
    /**
     * 客户端每帧检查当前选中的缓存样板，若样板变化则解码其输入物品并刷新输入列表。
     * 优先级：AAE 高级加工样板（含方向）→ 普通加工样板（方向初始 null）→ 其他通用样板。
     */
    private void syncInputListFromSelectedPattern() {
        if (menuSupplier == null) return;
        var menu = menuSupplier.get();
        if (menu == null) return;

        var host = menu.getMenuHost();
        var cacheSlots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_CACHE);
        if (host == null) {
            if (!lastDecodedPattern.isEmpty()) {
                lastDecodedPattern = net.minecraft.world.item.ItemStack.EMPTY;
                updateInputList(new LinkedHashMap<>());
            }
            return;
        }
        int selectedIndex = host.getSelectedPatternIndex();
        var stack = selectedIndex >= 0 && selectedIndex < cacheSlots.size()
                ? cacheSlots.get(selectedIndex).getItem()
                : net.minecraft.world.item.ItemStack.EMPTY;
        // 仅当槽位物品发生实际变化时才重新解码（避免每帧重建按钮）
        if (net.minecraft.world.item.ItemStack.matches(stack, lastDecodedPattern)) return;
        lastDecodedPattern = stack.copy();

        var newList = new LinkedHashMap<AEKey, Direction>();
        if (!stack.isEmpty()) {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                var detail = appeng.api.crafting.PatternDetailsHelper.decodePattern(stack, level);
                LinkedHashMap<AEKey, Direction> advMap = readAdvDirectionMap(detail);
                if (advMap != null) {
                    // AAE 高级加工样板：直接用其内置的 directionMap，保留方向状态
                    newList.putAll(advMap);
                } else if (detail instanceof appeng.crafting.pattern.AEProcessingPattern proc) {
                    for (var input : proc.getSparseInputs()) {
                        if (input != null && input.what() != null) {
                            newList.putIfAbsent(input.what(), null);  // null = ANY
                        }
                    }
                } else if (detail != null) {
                    // 合成样板等：使用通用 IPatternDetails 接口
                    for (var in : detail.getInputs()) {
                        var possible = in.getPossibleInputs();
                        if (possible != null && possible.length > 0 && possible[0] != null) {
                            newList.putIfAbsent(possible[0].what(), null);
                        }
                    }
                }
            }
        }
        updateInputList(newList);
    }

    // 反射缓存：AAE 的 AdvProcessingPattern 类与 getDirectionMap 方法（避免硬依赖）
    private static volatile boolean advReflectInited = false;
    private static Class<?> advPatternClass;
    private static java.lang.reflect.Method advGetDirectionMap;

    private static synchronized void initAdvReflect() {
        if (advReflectInited) return;
        advReflectInited = true;
        try {
            advPatternClass = Class.forName("net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern");
            advGetDirectionMap = advPatternClass.getMethod("getDirectionMap");
        } catch (Throwable ignored) {
            advPatternClass = null;
            advGetDirectionMap = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static LinkedHashMap<AEKey, Direction> readAdvDirectionMap(Object detail) {
        if (detail == null) return null;
        initAdvReflect();
        if (advPatternClass == null || !advPatternClass.isInstance(detail)) return null;
        try {
            return (LinkedHashMap<AEKey, Direction>) advGetDirectionMap.invoke(detail);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 调试日志限频：避免每帧刷屏。每隔 60 帧（约 1 秒）打印一次关键状态。 */
    private static int debugTick = 0;
    private static final boolean DEBUG = false;

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 同步输入列表（基于当前选中的缓存样板）
        syncInputListFromSelectedPattern();
        syncLocalCellConfigOverridesWithCellStack();
        updateBulkCompressionCutoffButton();

        // ⚠️ 关键：cell 容量是动态变化的（玩家可以随时放/取元件），
        // 仅在 init() 里调一次 resetScrollbarRange() 会让元件区滑块永远停在 range=0，
        // 表现为"明明有 18+ 行配置，鼠标拖滑块/滚轮都不动"。每帧刷新一次代价极小。
        resetScrollbarRange();

        if (DEBUG && (debugTick++ % 60) == 0) {
            WcwtMod.LOGGER.info("[WCWT-DEBUG] renderContent panel=({},{}) mouse=({},{}) inputList.size={} scrollLevel={}",
                    x, y, mouseX, mouseY, inputList.size(),
                    manageScrollbar != null ? manageScrollbar.getCurrentScroll() : -1);
        }

        // 渲染标题
        var font = Minecraft.getInstance().font;
        
        // 样板输入方向编辑区标题（JSON: text.input_direction_title left=4 top=3）
        guiGraphics.drawString(font,
                Component.translatable("gui.wcwt.advanced_coding.input_direction_title"),
                x + 4, y + 3, 0x404040, false);

        // 元件编辑区标题（JSON: text.cell_edit_title）
        guiGraphics.drawString(font,
                Component.translatable("gui.wcwt.advanced_coding.cell_edit_title"),
                x + CELL_EDIT_TITLE_X, y + CELL_EDIT_TITLE_Y, 0x404040, false);
        
        // 隐藏所有方向按钮
        directionButtons.forEach((key, buttons) -> {
            for (int i = 0; i < 7; i++) {
                buttons[i].visible = false;
            }
        });
        
        // 渲染可见的输入行（参考 AAE AdvPatternEncoderScreen.drawFG / drawBG）
        final int scrollLevel = manageScrollbar != null ? manageScrollbar.getCurrentScroll() : 0;
        int visibleRows = Math.min(VISIBLE_ROWS, inputList.size());
        
        for (int i = 0; i < visibleRows; i++) {
            int currentRow = scrollLevel + i;
            if (currentRow >= inputList.size()) {
                break;
            }
            
            InputRow row = rows.get(currentRow);
            int rowAbsX = x + LIST_ANCHOR_X;
            int rowAbsY = y + LIST_ANCHOR_Y + i * (ROW_HEIGHT + ROW_SPACING);

            // 先画 AE2 标准 18×18 槽底，再渲染物品。
            drawStandardAeSlotBackground(guiGraphics, rowAbsX, rowAbsY);
            guiGraphics.renderItem(
                row.key().wrapForDisplayOrFilter(),
                rowAbsX + 1,
                rowAbsY + 1
            );
            
            // 更新并显示方向按钮
            DirectionInputButton[] buttons = directionButtons.get(row.key());
            if (buttons == null) {
                continue;
            }
            var highlight = getSelectedDirButton(row.dir());
            
            for (var col = 0; col < 7; col++) {
                var button = buttons[col];
                button.setPosition(
                    x + LIST_ANCHOR_X + 2 + SLOT_SIZE + 
                    (col + 1) * DIRECTION_BUTTONS_OFFSET_X + col * DIRECTION_BUTTONS_WIDTH,
                    y + LIST_ANCHOR_Y + 1 + i * (ROW_HEIGHT + ROW_SPACING)
                );
                button.setHighlighted(col == highlight);
                button.visible = true;
            }
        }

        // ─── 面板槽悬停高亮（用 fill 代替 fillGradient，避免羽化问题）─────
        drawSlotHoverHighlight(guiGraphics, mouseX, mouseY, toAbsoluteRect(copyPatternSlotRect));
        drawSlotHoverHighlight(guiGraphics, mouseX, mouseY, getReplaceInputArea());
        drawSlotHoverHighlight(guiGraphics, mouseX, mouseY, getReplaceOutputArea());
        drawSlotHoverHighlight(guiGraphics, mouseX, mouseY, toAbsoluteRect(storageCellSlotRect));

        // ─── 渲染面板槽物品（Screen.renderSlot 已跳过这些槽的 vanilla 渲染，此处补回） ─────
        if (menuSupplier != null) {
            var menu = menuSupplier.get();
            if (menu != null) {
                renderSlotItem(guiGraphics, menu, WcwtSlotSemantics.COPY_PATTERN,
                        copyPatternSlotRect.left(), copyPatternSlotRect.top());
                renderSlotItem(guiGraphics, menu, WcwtSlotSemantics.WCWT_STORAGE_CELL,
                        storageCellSlotRect.left(), storageCellSlotRect.top());
            }
        }
        renderGhostKey(guiGraphics, replaceWhatKey,
                replaceInputSlotRect.left() + REPLACE_GHOST_RENDER_OFFSET_X,
                replaceInputSlotRect.top() + REPLACE_GHOST_RENDER_OFFSET_Y);
        renderGhostKey(guiGraphics, replaceWithKey,
                replaceOutputSlotRect.left() + REPLACE_GHOST_RENDER_OFFSET_X,
                replaceOutputSlotRect.top() + REPLACE_GHOST_RENDER_OFFSET_Y);

        // ─── 元件编辑区：3×6 槽格 + 物品渲染 ─────────────────────────
        renderCellConfigGrid(guiGraphics, mouseX, mouseY);

        // ─── 滑块手柄（仅渲染；range==0 → 自动渲染 disabled 灰底色样式） ─────
        var mousePoint = new appeng.client.Point(mouseX, mouseY);
        if (manageScrollbar != null) {
            manageScrollbar.drawForegroundLayer(guiGraphics, manageScrollbar.getBounds(), mousePoint);
        }
        if (cellScrollbar != null) {
            cellScrollbar.drawForegroundLayer(guiGraphics, cellScrollbar.getBounds(), mousePoint);
        }

    }

    private void updateBulkCompressionCutoffButton() {
        if (bulkCompressionCutoffBtn == null) {
            return;
        }
        bulkCompressionCutoffBtn.setItem(WcwtMegaCellsCompat.getCompressionCutoffItem(getCellStack()));
    }

    private void renderCopyReplaceLabels(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        var copyLabel    = Component.translatable("gui.wcwt.advanced_coding.copy_label");
        var replaceLabel = Component.translatable("gui.wcwt.advanced_coding.replace_label");
        int copyTextW    = font.width(copyLabel);
        int replaceTextW = font.width(replaceLabel);
        int copyLabelX    = x + COPY_PATTERN_BTN_X + Math.round((COPY_PATTERN_BTN_W - copyTextW)    / 2f);
        int replaceLabelX = x + REPLACE_BTN_X      + Math.round((REPLACE_BTN_W      - replaceTextW) / 2f);
        int labelBaseY = y + COPY_PATTERN_BTN_Y + (COPY_PATTERN_BTN_H - 7) / 2 - 1;
        boolean copyHovered = copyPatternBtn != null
                ? copyPatternBtn.isHoveredOrFocused()
                : mouseX >= x + COPY_PATTERN_BTN_X && mouseX < x + COPY_PATTERN_BTN_X + COPY_PATTERN_BTN_W
                && mouseY >= y + COPY_PATTERN_BTN_Y && mouseY < y + COPY_PATTERN_BTN_Y + COPY_PATTERN_BTN_H;
        boolean replaceHovered = replaceBtn != null
                ? replaceBtn.isHoveredOrFocused()
                : mouseX >= x + REPLACE_BTN_X && mouseX < x + REPLACE_BTN_X + REPLACE_BTN_W
                && mouseY >= y + REPLACE_BTN_Y && mouseY < y + REPLACE_BTN_Y + REPLACE_BTN_H;
        int copyLabelY = labelBaseY + (copyHovered ? 1 : 0);
        int replaceLabelY = labelBaseY + (replaceHovered ? 1 : 0);

        var pose = g.pose();
        pose.pushPose();
        float copyScale = 0.80f;
        float copyCenterX = x + COPY_PATTERN_BTN_X + COPY_PATTERN_BTN_W / 2f;
        float copyCenterY = copyLabelY + 3.5f;
        pose.translate(copyCenterX, copyCenterY, 0);
        pose.scale(copyScale, copyScale, 1.0f);
        g.drawString(font, copyLabel, Math.round(-copyTextW / 2f), Math.round(-3.5f), 0xFFFFFF, false);
        pose.popPose();
        g.drawString(font, replaceLabel, replaceLabelX, replaceLabelY, 0xFFFFFF, false);

        if (DEBUG && (debugTick % 60) == 0) {
            WcwtMod.LOGGER.info("[WCWT-DEBUG] copy label: textW={} btnX={} btnW={} -> labelX={} (btnCenter={}, textCenter={})",
                    copyTextW, COPY_PATTERN_BTN_X, COPY_PATTERN_BTN_W, copyLabelX - x,
                    COPY_PATTERN_BTN_X + COPY_PATTERN_BTN_W / 2f,
                    (copyLabelX - x) + copyTextW / 2f);
            WcwtMod.LOGGER.info("[WCWT-DEBUG] replace label: textW={} btnX={} btnW={} -> labelX={} (btnCenter={}, textCenter={})",
                    replaceTextW, REPLACE_BTN_X, REPLACE_BTN_W, replaceLabelX - x,
                    REPLACE_BTN_X + REPLACE_BTN_W / 2f,
                    (replaceLabelX - x) + replaceTextW / 2f);
        }
    }

    private void updatePanelTooltip(int mouseX, int mouseY) {
        currentTooltip = List.of();
        currentTooltipArea = new Rect2i(mouseX, mouseY, 1, 1);

        if (isInRect(mouseX, mouseY, getReplaceInputArea()) && replaceWhatKey != null) {
            currentTooltip = tooltipForKey(replaceWhatKey);
            currentTooltipArea = getReplaceInputArea();
            return;
        }
        if (isInRect(mouseX, mouseY, getReplaceOutputArea()) && replaceWithKey != null) {
            currentTooltip = tooltipForKey(replaceWithKey);
            currentTooltipArea = getReplaceOutputArea();
            return;
        }

        if (hoveredCellConfigSlot >= 0) {
            var stack = getCellConfigStack(hoveredCellConfigSlot);
            if (stack != null) {
                currentTooltip = tooltipForKey(stack.what());
                return;
            }
        }

        for (var child : children) {
            if (!(child instanceof ITooltip tt)) continue;
            if (!tt.isTooltipAreaVisible()) continue;
            var area = tt.getTooltipArea();
            if (mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                    && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight()) {
                currentTooltip = tt.getTooltipMessage();
                currentTooltipArea = area;
                return;
            }
        }
    }

    private static boolean isInRect(int mouseX, int mouseY, Rect2i rect) {
        return mouseX >= rect.getX() && mouseX < rect.getX() + rect.getWidth()
                && mouseY >= rect.getY() && mouseY < rect.getY() + rect.getHeight();
    }

    private static List<Component> tooltipForKey(AEKey key) {
        return key.wrapForDisplayOrFilter().getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.EMPTY,
                Minecraft.getInstance().player,
                Minecraft.getInstance().options.advancedItemTooltips
                        ? net.minecraft.world.item.TooltipFlag.ADVANCED
                        : net.minecraft.world.item.TooltipFlag.NORMAL);
    }

    /** 当前悬停的 cell config 槽格索引（-1 = 无）。在 renderContent 里更新，结尾统一输出 tooltip。 */
    private int hoveredCellConfigSlot = -1;

    /**
     * 元件编辑区 3×6 槽位网格：
     *  - 背景纹理已画好所有槽底，不需手动绘制边框
     *  - 物品来源：当前 Cell 的 config inventory（{@link ICellWorkbenchItem#getConfigInventory}）
     *  - 滚动条 cellScrollbar 控制起始行；超出 cell 实际容量的格不绘制
     */
    private void renderCellConfigGrid(GuiGraphics g, int mouseX, int mouseY) {
        final int scroll = cellScrollbar != null ? cellScrollbar.getCurrentScroll() : 0;
        final int cellSize = getCellConfigSize();

        hoveredCellConfigSlot = -1;
        for (int row = 0; row < CELL_CONFIG_ROWS; row++) {
            for (int col = 0; col < CELL_CONFIG_COLS; col++) {
                int slotIdx = (scroll + row) * CELL_CONFIG_COLS + col;
                int sx = x + CELL_CONFIG_GRID_X + col * 18;
                int sy = y + CELL_CONFIG_GRID_Y + row * 18;
                if (slotIdx < cellSize) {
                    // 有元件：画 AE2 标准槽底（与本 mod 内不存在的 adv_pattern_encoder.png 无关）。
                    drawStandardAeSlotBackground(g, sx, sy);
                }

                // 悬停检测（18×18 格子）
                boolean hovered = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
                if (hovered) {
                    hoveredCellConfigSlot = slotIdx;
                    // 悬停高亮（非 fillGradient）
                    if (slotIdx < cellSize) {
                        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, CELL_CONFIG_HOVER_COLOR);
                    }
                }

                if (slotIdx >= cellSize) continue; // 超出元件容量：仅空槽（背景纹理已画）
                var stack = getCellConfigStack(slotIdx);
                if (stack == null) continue;

                g.renderItem(stack.what().wrapForDisplayOrFilter(), sx + 1, sy + 1);
            }
        }
    }

    /** 使用 AE2 自带 {@code ae2:textures/guis/states.png} 中的标准槽底，避免引用本包未自带的 adv_pattern_encoder 贴图。 */
    private void drawStandardAeSlotBackground(GuiGraphics g, int absX, int absY) {
        Icon.SLOT_BACKGROUND.getBlitter().dest(absX, absY).blit(g);
    }

    /**
     * 获取选中的方向按钮索引
     */
    private int getSelectedDirButton(@Nullable Direction dir) {
        if (dir == null) return 0;
        
        return switch (dir) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case UP -> 5;
            case DOWN -> 6;
        };
    }
    
    /**
     * 获取方向按钮纹理
     */
    private Pair<ResourceLocation, ResourceLocation> getDirButtonTextures(int index) {
        return switch (index) {
            case 1 -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/north_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/north_button_selected.png"));
            case 2 -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/east_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/east_button_selected.png"));
            case 3 -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/south_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/south_button_selected.png"));
            case 4 -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/west_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/west_button_selected.png"));
            case 5 -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/up_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/up_button_selected.png"));
            case 6 -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/down_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/down_button_selected.png"));
            default -> new Pair<>(
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/any_button.png"),
                ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "textures/guis/any_button_selected.png"));
        };
    }
    
    /**
     * 获取方向按钮文本
     */
    private Component getDirButtonText(int index) {
        return switch (index) {
            case 1 -> Component.translatable("gui.wcwt.direction.north");
            case 2 -> Component.translatable("gui.wcwt.direction.east");
            case 3 -> Component.translatable("gui.wcwt.direction.south");
            case 4 -> Component.translatable("gui.wcwt.direction.west");
            case 5 -> Component.translatable("gui.wcwt.direction.up");
            case 6 -> Component.translatable("gui.wcwt.direction.down");
            default -> Component.translatable("gui.wcwt.direction.any");
        };
    }
    
    /**
     * 渲染某个槽位语义对应的第一个槽的物品（含数量/耐久装饰）。
     * 坐标相对面板左上角，与 JSON 中 left/top 一致（无需 +1 偏移）。
     */
    private void renderSlotItem(GuiGraphics g, WirelessComprehensiveWorkTerminalMenu menu,
                                appeng.menu.SlotSemantic semantic, int relX, int relY) {
        var slots = menu.getSlots(semantic);
        if (slots.isEmpty()) return;
        var item = slots.get(0).getItem();
        if (!item.isEmpty()) {
            int ax = x + relX, ay = y + relY;
            g.renderItem(item, ax, ay);
            g.renderItemDecorations(Minecraft.getInstance().font, item, ax, ay);
        }
    }

    private void renderGhostKey(GuiGraphics g, @Nullable AEKey key, int relX, int relY) {
        if (key == null) {
            return;
        }
        g.renderItem(key.wrapForDisplayOrFilter(), x + relX, y + relY);
    }

    // ─── 滑块鼠标事件转发（先于 super，避免被子控件吞掉） ──────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (manageScrollbar != null && hits(manageScrollbar, mouseX, mouseY)
                && manageScrollbar.onMouseDown(new appeng.client.Point((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        if (cellScrollbar != null && hits(cellScrollbar, mouseX, mouseY)
                && cellScrollbar.onMouseDown(new appeng.client.Point((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        if (button == 0 || button == 1) {
            if (handleReplaceGhostClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        // 元件编辑区是面板内部"伪槽"：左键 + 手持物 = 写入 ghost 标记；
        // 左键 + 空手 / 右键 = 清空该格。点击仅在面板矩形内有效，避免吞掉外部物品交互。
        if (button == 0 || button == 1) {
            if (handleCellGridClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleReplaceGhostClick(double mouseX, double mouseY, int button) {
        boolean input = contains(getReplaceInputArea(), mouseX, mouseY);
        boolean output = contains(getReplaceOutputArea(), mouseX, mouseY);
        if (!input && !output) {
            return false;
        }

        AEKey key = null;
        if (button == 0 && menuSupplier != null) {
            var menu = menuSupplier.get();
            if (menu != null) {
                var carried = menu.getCarried();
                if (!carried.isEmpty()) {
                    key = AEItemKey.of(carried);
                }
            }
        }

        if (input) {
            replaceWhatKey = key;
        } else {
            replaceWithKey = key;
        }
        return true;
    }

    /**
     * 处理元件 config 槽格点击：
     *  - 左键 + 手持物品：把该物品的 AEKey 写入对应格
     *  - 左键 + 空手 / 右键：清除该格
     */
    private boolean handleCellGridClick(double mouseX, double mouseY, int button) {
        int gridLeft = x + CELL_CONFIG_GRID_X;
        int gridTop  = y + CELL_CONFIG_GRID_Y;
        int gridW    = CELL_CONFIG_COLS * 18;
        int gridH    = CELL_CONFIG_ROWS * 18;
        if (mouseX < gridLeft || mouseX >= gridLeft + gridW || mouseY < gridTop || mouseY >= gridTop + gridH) {
            return false;
        }
        int col = ((int) mouseX - gridLeft) / 18;
        int row = ((int) mouseY - gridTop)  / 18;
        int scroll = cellScrollbar != null ? cellScrollbar.getCurrentScroll() : 0;
        int slotIdx = (scroll + row) * CELL_CONFIG_COLS + col;
        int cellSize = getCellConfigSize();
        if (slotIdx < 0 || slotIdx >= cellSize) return true; // 吃掉点击但不发包

        AEKey key = null;
        if (button == 0 && menuSupplier != null) {
            var menu = menuSupplier.get();
            if (menu != null) {
                var carried = menu.getCarried();
                if (!carried.isEmpty()) {
                    key = AEItemKey.of(carried);
                    if (!menu.isCellConfigKeyAllowed(slotIdx, key)) {
                        return true;
                    }
                }
            }
        }
        if (DEBUG) {
            WcwtMod.LOGGER.info("[WCWT-DEBUG] cell config click slotIdx={} button={} key={}",
                    slotIdx, button, key);
        }
        rememberLocalCellConfig(slotIdx, key);
        PacketDistributor.sendToServer(new CellConfigSetPacket(slotIdx, key));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        var p = new appeng.client.Point((int) mouseX, (int) mouseY);
        if (manageScrollbar != null) manageScrollbar.onMouseUp(p, button);
        if (cellScrollbar != null) cellScrollbar.onMouseUp(p, button);
        // 关键：玩家可以从背包拿着物品拖到元件伪槽再松开。
        // 如果这里不吃掉 release，vanilla 会把面板外释放当作"丢弃物品"处理；
        // 同时 release 也需要写入 ghost 标记，覆盖"按下发生在背包槽，松开发生在元件格"的拖拽路径。
        if ((button == 0 || button == 1) && handleReplaceGhostClick(mouseX, mouseY, button)) {
            return true;
        }
        if ((button == 0 || button == 1) && handleCellGridClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible) return false;
        var p = new appeng.client.Point((int) mouseX, (int) mouseY);
        if (manageScrollbar != null && manageScrollbar.onMouseDrag(p, button)) return true;
        if (cellScrollbar != null && cellScrollbar.onMouseDrag(p, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** 鼠标滚轮：只有当指针落在面板矩形内才转发（避免抢全屏滚轮）。 */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible) return false;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false;
        var p = new appeng.client.Point((int) mouseX, (int) mouseY);
        // 先按鼠标所在区域分发。Scrollbar.onMouseWheel 本身不区分上下区域，
        // 所以不能无条件先给 manageScrollbar，否则元件区滚轮会带动上半区。
        if (isInCellScrollArea(mouseX, mouseY)) {
            if (cellScrollbar != null) cellScrollbar.onMouseWheel(p, delta);
            return true;
        }
        if (isInManageScrollArea(mouseX, mouseY)) {
            if (manageScrollbar != null) manageScrollbar.onMouseWheel(p, delta);
            return true;
        }
        return false;
    }

    private boolean isInManageScrollArea(double mouseX, double mouseY) {
        return mouseY >= y + MANAGE_SCROLLBAR_Y - 2
                && mouseY < y + MANAGE_SCROLLBAR_Y + MANAGE_SCROLLBAR_HEIGHT + 2;
    }

    private boolean isInCellScrollArea(double mouseX, double mouseY) {
        return mouseY >= y + CELL_CONFIG_GRID_Y - 4
                && mouseY < y + CELL_CONFIG_GRID_Y + CELL_CONFIG_ROWS * 18 + 4;
    }

    private static boolean hits(Scrollbar sb, double mouseX, double mouseY) {
        var b = sb.getBounds();
        return mouseX >= b.getX() && mouseX < b.getX() + b.getWidth()
                && mouseY >= b.getY() && mouseY < b.getY() + b.getHeight();
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX < rect.getX() + rect.getWidth()
                && mouseY >= rect.getY() && mouseY < rect.getY() + rect.getHeight();
    }

    /** 当鼠标悬停在槽位区域时，绘制半透明白色高亮（16×16）。 */
    private void drawSlotHoverHighlight(GuiGraphics g, int mouseX, int mouseY, int slotAbsX, int slotAbsY) {
        if (mouseX >= slotAbsX && mouseX < slotAbsX + 16
                && mouseY >= slotAbsY && mouseY < slotAbsY + 16) {
            g.fill(slotAbsX, slotAbsY, slotAbsX + 16, slotAbsY + 16, 0x80FFFFFF);
        }
    }

    private void drawSlotHoverHighlight(GuiGraphics g, int mouseX, int mouseY, Rect2i area) {
        if (mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight()) {
            g.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(),
                    0x80FFFFFF);
        }
    }

    @Override
    public List<Component> getTooltipMessage() {
        return currentTooltip;
    }

    @Override
    public Rect2i getTooltipArea() {
        return currentTooltipArea;
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible && !currentTooltip.isEmpty();
    }

    /**
     * 输入行记录
     */
    public record InputRow(AEKey key, @Nullable Direction dir) {}
}
