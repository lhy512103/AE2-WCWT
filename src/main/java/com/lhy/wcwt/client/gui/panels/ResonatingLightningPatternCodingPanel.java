package com.lhy.wcwt.client.gui.panels;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.Scrollbar;
import com.lhy.wcwt.client.gui.widgets.IconButton;
import com.lhy.wcwt.menu.WcwtSlotSemantics;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.ResonatingLightningPatternActionPacket;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ResonatingLightningPatternCodingPanel extends ExtendedUIPanel implements ITooltip {
    private static final boolean HAS_AE2LT = ModList.get().isLoaded("ae2lt");
    private static final boolean HAS_AE2CS = ModList.get().isLoaded("ae2cs");
    private static final ResourceLocation PANEL_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_resonating_lightning_pattern_coding.png");
    private static final ResourceLocation AE2LT_ENCODER_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2lt", "textures/gui/ae2lt_pattern_encoder.png");
    private static final ResourceLocation AE2_CHECKBOX_TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/checkbox.png");

    private static final int PANEL_WIDTH = 134;
    private static final int PANEL_HEIGHT = 186;
    private static final int AE2LT_TEXTURE_SIZE = 256;
    private static final int AE2_CHECKBOX_TEXTURE_SIZE = 64;
    private static final int LIGHTNING_VISIBLE_ROWS = 3;
    private static final int LIGHTNING_ROW_HEIGHT = 22;
    private static final int LIGHTNING_SLOT_X = 19;
    private static final int LIGHTNING_TEXT_X = 44;
    private static final int LIGHTNING_SWITCH_X = 102;
    private static final int LIGHTNING_SWITCH_W = 22;
    private static final int LIGHTNING_SWITCH_H = 12;
    private static final int LIGHTNING_SLOT_SIZE = 18;
    private static final int LIGHTNING_ENTRY_CONTENT_OFFSET_Y = 3;
    private static final int LIGHTNING_SLOT_U = 177;
    private static final int LIGHTNING_SLOT_V = 0;
    private static final int LIGHTNING_SLIDER_U = 177;
    private static final int LIGHTNING_SLIDER_V = 29;
    private static final int LIGHTNING_SLIDER_W = 7;
    private static final int LIGHTNING_SLIDER_H = 15;
    private static final int AE2_CHECKBOX_U = 0;
    private static final int AE2_CHECKBOX_V_OFF = 28;
    private static final int AE2_CHECKBOX_V_ON = 40;
    private static final int RESONATING_COLUMNS = 7;
    private static final int RESONATING_VISIBLE_ROWS = 3;
    private static final int RESONATING_SLOT_SIZE = 18;
    private static final int DEFAULT_RESONATING_SLOT_SPACING = 16;
    private final ExtendedPanelLayout layout = ExtendedPanelLayout.load("wcwt_resonating_lightning_pattern_coding.json");
    private final Scrollbar lightningScrollbar = new Scrollbar(Scrollbar.SMALL);
    private ExtendedPanelLayout.Rect lightningArea = new ExtendedPanelLayout.Rect(13, 16, 116, 73);
    private ExtendedPanelLayout.Rect lightningScrollbarRect = new ExtendedPanelLayout.Rect(5, 19, 0, 67);
    private ExtendedPanelLayout.Rect resonatingConvertRect = new ExtendedPanelLayout.Rect(56, 98, 16, 16);
    private ExtendedPanelLayout.Rect resonatingStorageAnchor = new ExtendedPanelLayout.Rect(4, 124, 0, 0);
    private Supplier<WirelessComprehensiveWorkTerminalMenu> menuSupplier;
    private IconButton resonatingConvertButton;
    private List<Component> currentTooltip = List.of();
    private Rect2i currentTooltipArea = new Rect2i(0, 0, 0, 0);
    private List<OverloadViewEntry> cachedEntries = List.of();
    private int cachedSelectedIndex = -1;
    private ItemStack cachedSelectedStack = ItemStack.EMPTY;
    private final Set<Integer> inputIdOnlySlots = new HashSet<>();
    private final Set<Integer> outputIdOnlySlots = new HashSet<>();
    private boolean draggingScrollbar;

    public ResonatingLightningPatternCodingPanel(int x, int y) {
        super(x, y, PANEL_WIDTH, PANEL_HEIGHT);
    }

    public void setMenuSupplier(Supplier<WirelessComprehensiveWorkTerminalMenu> menuSupplier) {
        this.menuSupplier = menuSupplier;
    }

    @Override
    public void init() {
        children.clear();
        var returnButton = layout.widget("return_button", new ExtendedPanelLayout.Rect(111, -8, 20, 20));
        configureReturnButton(width - returnButton.left(), returnButton.top(), returnButton.width(), returnButton.height());
        lightningArea = layout.widget("lightning_area", lightningArea);
        lightningScrollbarRect = layout.widget("rlpc_scrollbar", lightningScrollbarRect);
        resonatingConvertRect = layout.widget("resonating_convert", resonatingConvertRect);
        resonatingStorageAnchor = layout.slot("resonating_storage", resonatingStorageAnchor);
        lightningScrollbar.setHeight(lightningScrollbarRect.height());
        lightningScrollbar.setCaptureMouseWheel(false);
        resonatingConvertButton = null;
        if (HAS_AE2CS) {
            resonatingConvertButton = new IconButton(
                    x + resonatingConvertRect.left(), y + resonatingConvertRect.top(),
                    16, 16,
                    0, 0, 0, 0, 1, 1,
                    STATES_TEXTURE,
                    Component.translatable("gui.wcwt.rlpc.resonating_convert"),
                    btn -> PacketDistributor.sendToServer(new ResonatingLightningPatternActionPacket(
                            ResonatingLightningPatternActionPacket.Action.CONVERT_TO_RESONATING,
                            new int[0], new int[0])))
                    .useAE2ToolbarBackground()
                    .setOverlayIcon(() -> Icon.SCHEDULING_RANDOM)
                    .setTooltipLines(List.of(Component.translatable("gui.wcwt.rlpc.resonating_convert")));
            children.add(resonatingConvertButton);
        }
        repositionControls();
        createReturnButton();
    }

    @Override
    public void setPosition(int newX, int newY) {
        super.setPosition(newX, newY);
        repositionControls();
    }

    private void repositionControls() {
        lightningScrollbar.setPosition(new appeng.client.Point(
                x + lightningScrollbarRect.left(), y + lightningScrollbarRect.top()));
        if (resonatingConvertButton != null) {
            resonatingConvertButton.setX(x + resonatingConvertRect.left());
            resonatingConvertButton.setY(y + resonatingConvertRect.top());
        }
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.blit(PANEL_TEXTURE, x, y, 0, 0, width, height, 256, 256);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var menu = getMenu();
        if (menu == null) {
            return;
        }
        if (HAS_AE2LT) {
            syncEntries(menu);
            updateScrollbar(menu);
        } else {
            cachedEntries = List.of();
            lightningScrollbar.setRange(0, 0, 1);
            lightningScrollbar.setCurrentScroll(0);
        }

        var font = Minecraft.getInstance().font;
        if (HAS_AE2LT) {
            guiGraphics.drawString(font,
                    Component.translatable("gui.wcwt_rlpattern_coding.lightningtitle"),
                    x + 3, y + 3, 0x404040, false);
            renderLightningEntries(guiGraphics, mouseX, mouseY);
        }
        if (HAS_AE2CS) {
            guiGraphics.drawString(font,
                    Component.translatable("gui.wcwt_rlpattern_coding.resonatingtitle"),
                    x + 3, y + 95, 0x404040, false);
        }

        updateTooltip(mouseX, mouseY);
    }

    private void updateScrollbar(WirelessComprehensiveWorkTerminalMenu menu) {
        int totalRows = Math.max(0, cachedEntries.size());
        int max = Math.max(0, totalRows - LIGHTNING_VISIBLE_ROWS);
        lightningScrollbar.setRange(0, max, 1);
    }

    private void renderLightningEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var entries = cachedEntries;
        int start = Math.min(lightningScrollbar.getCurrentScroll(), Math.max(0, entries.size() - LIGHTNING_VISIBLE_ROWS));
        int end = Math.min(entries.size(), start + LIGHTNING_VISIBLE_ROWS);

        for (int visibleRow = 0; visibleRow < end - start; visibleRow++) {
            var entry = entries.get(start + visibleRow);
            int rowY = y + lightningArea.top() + visibleRow * LIGHTNING_ROW_HEIGHT + LIGHTNING_ENTRY_CONTENT_OFFSET_Y;
            int slotX = x + LIGHTNING_SLOT_X;
            ItemStack stack = entry.stack();
            guiGraphics.blit(AE2LT_ENCODER_TEXTURE, slotX, rowY,
                    LIGHTNING_SLOT_U, LIGHTNING_SLOT_V,
                    LIGHTNING_SLOT_SIZE, LIGHTNING_SLOT_SIZE,
                    AE2LT_TEXTURE_SIZE, AE2LT_TEXTURE_SIZE);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, slotX + 1, rowY + 1);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, slotX + 1, rowY + 1);
            }
            guiGraphics.drawString(Minecraft.getInstance().font,
                    entry.label(), x + LIGHTNING_TEXT_X, rowY + 5, 0x404040, false);
            renderModeSwitch(guiGraphics, entry, rowY);
        }
        renderLightningScrollbar(guiGraphics);
    }

    private void renderModeSwitch(GuiGraphics guiGraphics, OverloadViewEntry entry, int rowY) {
        int switchX = x + LIGHTNING_SWITCH_X;
        guiGraphics.blit(AE2_CHECKBOX_TEXTURE,
                switchX, rowY + 3,
                AE2_CHECKBOX_U, entry.idOnly() ? AE2_CHECKBOX_V_ON : AE2_CHECKBOX_V_OFF,
                LIGHTNING_SWITCH_W, LIGHTNING_SWITCH_H,
                AE2_CHECKBOX_TEXTURE_SIZE, AE2_CHECKBOX_TEXTURE_SIZE);
    }

    private void renderLightningScrollbar(GuiGraphics guiGraphics) {
        int trackX = x + lightningScrollbarRect.left();
        int trackY = y + lightningScrollbarRect.top() - 1;
        int maxScroll = Math.max(0, cachedEntries.size() - LIGHTNING_VISIBLE_ROWS);
        if (maxScroll <= 0) {
            guiGraphics.blit(AE2LT_ENCODER_TEXTURE, trackX, trackY,
                    LIGHTNING_SLIDER_U, LIGHTNING_SLIDER_V,
                    LIGHTNING_SLIDER_W, LIGHTNING_SLIDER_H,
                    AE2LT_TEXTURE_SIZE, AE2LT_TEXTURE_SIZE);
            return;
        }

        int travel = lightningScrollbarRect.height() - LIGHTNING_SLIDER_H + 1;
        int sliderY = trackY + Math.round((lightningScrollbar.getCurrentScroll() / (float) maxScroll) * travel);
        guiGraphics.blit(AE2LT_ENCODER_TEXTURE, trackX, sliderY,
                LIGHTNING_SLIDER_U, LIGHTNING_SLIDER_V,
                LIGHTNING_SLIDER_W, LIGHTNING_SLIDER_H,
                AE2LT_TEXTURE_SIZE, AE2LT_TEXTURE_SIZE);
    }

    public int getLightningScrollbarX() {
        return lightningScrollbarRect.left();
    }

    public int getLightningScrollbarY() {
        return lightningScrollbarRect.top();
    }

    public int getLightningScrollbarHeight() {
        return lightningScrollbarRect.height();
    }

    public int getResonatingSlotAnchorX() {
        return resonatingStorageAnchor.left();
    }

    public int getResonatingSlotAnchorY() {
        return resonatingStorageAnchor.top();
    }

    public int getResonatingColumns() {
        return layout.slotColumns("resonating_storage", RESONATING_COLUMNS);
    }

    public int getResonatingSlotSpacingX() {
        return layout.slotInt("resonating_storage", "slotSpacingX", DEFAULT_RESONATING_SLOT_SPACING);
    }

    public int getResonatingSlotSpacingY() {
        return layout.slotInt("resonating_storage", "slotSpacingY", DEFAULT_RESONATING_SLOT_SPACING);
    }

    public void setFirstVisibleResonatingSlot(int firstVisibleSlot) {
        // 当前 21 格固定全部可见，预留接口给 Screen 定位逻辑复用。
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !HAS_AE2LT) {
            return false;
        }
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        int maxScroll = Math.max(0, cachedEntries.size() - LIGHTNING_VISIBLE_ROWS);
        if (maxScroll > 0) {
            lightningScrollbar.setCurrentScroll(
                    Mth.clamp(lightningScrollbar.getCurrentScroll() - (int) Math.signum(delta), 0, maxScroll));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (HAS_AE2LT && button == 0 && isWithinScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollbarFromMouse(mouseY);
            return true;
        }

        var menu = getMenu();
        if (HAS_AE2LT && menu != null && button == 0) {
            var entry = getEntryAt(menu, mouseX, mouseY);
            if (entry != null && isWithinEntrySwitch(mouseX, mouseY, entry.visibleRow, entry.rowY)) {
                toggleEntry(entry.entry);
                sendOverloadConvert();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (HAS_AE2LT && draggingScrollbar) {
            updateScrollbarFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isWithinScrollbar(double mouseX, double mouseY) {
        return mouseX >= x + lightningScrollbarRect.left()
                && mouseX < x + lightningScrollbarRect.left() + LIGHTNING_SLIDER_W
                && mouseY >= y + lightningScrollbarRect.top() - 1
                && mouseY < y + lightningScrollbarRect.top() - 1 + lightningScrollbarRect.height();
    }

    private boolean isWithinEntrySwitch(double mouseX, double mouseY, int visibleRow, int rowY) {
        int switchX = x + LIGHTNING_SWITCH_X;
        return mouseX >= switchX && mouseX < switchX + LIGHTNING_SWITCH_W
                && mouseY >= rowY + 3 && mouseY < rowY + 3 + LIGHTNING_SWITCH_H;
    }

    @Nullable
    private EntryHit getEntryAt(WirelessComprehensiveWorkTerminalMenu menu, double mouseX, double mouseY) {
        if (!HAS_AE2LT) {
            return null;
        }
        if (mouseX < x + lightningArea.left() || mouseX >= x + lightningArea.left() + lightningArea.width()
                || mouseY < y + lightningArea.top() || mouseY >= y + lightningArea.top() + lightningArea.height()) {
            return null;
        }

        int visibleRow = (int) ((mouseY - (y + lightningArea.top())) / LIGHTNING_ROW_HEIGHT);
        if (visibleRow < 0 || visibleRow >= LIGHTNING_VISIBLE_ROWS) {
            return null;
        }

        var entries = cachedEntries;
        int index = lightningScrollbar.getCurrentScroll() + visibleRow;
        if (index < 0 || index >= entries.size()) {
            return null;
        }

        int rowY = y + lightningArea.top() + visibleRow * LIGHTNING_ROW_HEIGHT + LIGHTNING_ENTRY_CONTENT_OFFSET_Y;
        return new EntryHit(entries.get(index), visibleRow, rowY);
    }

    @Nullable
    private WirelessComprehensiveWorkTerminalMenu getMenu() {
        return menuSupplier != null ? menuSupplier.get() : null;
    }

    private void syncEntries(WirelessComprehensiveWorkTerminalMenu menu) {
        if (!HAS_AE2LT) {
            cachedSelectedIndex = -1;
            cachedSelectedStack = ItemStack.EMPTY;
            inputIdOnlySlots.clear();
            outputIdOnlySlots.clear();
            cachedEntries = List.of();
            return;
        }
        int selectedIndex = menu.getMenuHost() != null ? menu.getMenuHost().getSelectedPatternIndex() : -1;
        ItemStack selectedStack = ItemStack.EMPTY;
        List<Slot> cacheSlots = menu.getSlots(WcwtSlotSemantics.WCWT_PATTERN_CACHE);
        if (selectedIndex >= 0 && selectedIndex < cacheSlots.size()) {
            selectedStack = cacheSlots.get(selectedIndex).getItem();
        }

        if (selectedIndex != cachedSelectedIndex
                || !ItemStack.isSameItem(selectedStack, cachedSelectedStack)
                || !java.util.Objects.equals(selectedStack.getTag(), cachedSelectedStack.getTag())) {
            cachedSelectedIndex = selectedIndex;
            cachedSelectedStack = selectedStack.copy();
            inputIdOnlySlots.clear();
            outputIdOnlySlots.clear();
            cachedEntries = buildEntries(selectedStack, menu);
            for (var entry : cachedEntries) {
                if (entry.idOnly()) {
                    if (entry.input()) {
                        inputIdOnlySlots.add(entry.slotIndex());
                    } else {
                        outputIdOnlySlots.add(entry.slotIndex());
                    }
                }
            }
        }
    }

    private List<OverloadViewEntry> buildEntries(ItemStack selectedStack, WirelessComprehensiveWorkTerminalMenu menu) {
        if (selectedStack.isEmpty()) {
            return List.of();
        }

        var details = PatternDetailsHelper.decodePattern(selectedStack, Minecraft.getInstance().level);
        if (details == null) {
            return List.of();
        }

        boolean overloadPattern = isOverloadPattern(selectedStack);
        OverloadModeReader overloadModeReader = overloadPattern ? OverloadModeReader.read(selectedStack) : null;

        List<OverloadViewEntry> entries = new ArrayList<>();
        var inputs = details.getInputs();
        for (int slot = 0; slot < inputs.length; slot++) {
            ItemStack first = firstItemTemplate(inputs[slot].getPossibleInputs());
            if (!first.isEmpty()) {
                entries.add(new OverloadViewEntry(true, slot,
                        overloadModeReader != null && overloadModeReader.inputIdOnly(slot),
                        first,
                        Component.translatable("gui.wcwt.rlpc.entry.input")));
            }
        }

        var outputs = details.getOutputs();
        for (int slot = 0; slot < outputs.length; slot++) {
            ItemStack first = toItemStack(outputs[slot]);
            if (!first.isEmpty()) {
                entries.add(new OverloadViewEntry(false, slot,
                        overloadModeReader != null && overloadModeReader.outputIdOnly(slot),
                        first,
                        Component.translatable("gui.wcwt.rlpc.entry.output")));
            }
        }
        return entries;
    }

    private void toggleEntry(OverloadViewEntry entry) {
        if (entry.input()) {
            if (!inputIdOnlySlots.add(entry.slotIndex())) {
                inputIdOnlySlots.remove(entry.slotIndex());
            }
        } else {
            if (!outputIdOnlySlots.add(entry.slotIndex())) {
                outputIdOnlySlots.remove(entry.slotIndex());
            }
        }
        cachedEntries = rebuildEntryModes(cachedEntries);
    }

    private List<OverloadViewEntry> rebuildEntryModes(List<OverloadViewEntry> entries) {
        List<OverloadViewEntry> updated = new ArrayList<>(entries.size());
        for (var entry : entries) {
            boolean idOnly = entry.input()
                    ? inputIdOnlySlots.contains(entry.slotIndex())
                    : outputIdOnlySlots.contains(entry.slotIndex());
            updated.add(new OverloadViewEntry(entry.input(), entry.slotIndex(), idOnly, entry.stack(), entry.label()));
        }
        return updated;
    }

    private void sendOverloadConvert() {
        if (!HAS_AE2LT) {
            return;
        }
        PacketDistributor.sendToServer(new ResonatingLightningPatternActionPacket(
                ResonatingLightningPatternActionPacket.Action.CONVERT_TO_OVERLOAD,
                inputIdOnlySlots.stream().mapToInt(Integer::intValue).sorted().toArray(),
                outputIdOnlySlots.stream().mapToInt(Integer::intValue).sorted().toArray()));
    }

    private void updateScrollbarFromMouse(double mouseY) {
        int maxScroll = Math.max(0, cachedEntries.size() - LIGHTNING_VISIBLE_ROWS);
        if (maxScroll <= 0) {
            lightningScrollbar.setCurrentScroll(0);
            return;
        }
        double progress = ((mouseY - (y + lightningScrollbarRect.top()) - LIGHTNING_SLIDER_H / 2.0) + 1.0)
                / Math.max(1.0, lightningScrollbarRect.height() - LIGHTNING_SLIDER_H + 1.0);
        int scroll = Math.round((float) (Mth.clamp(progress, 0.0, 1.0) * maxScroll));
        lightningScrollbar.setCurrentScroll(scroll);
    }

    private void updateTooltip(int mouseX, int mouseY) {
        currentTooltip = List.of();
        currentTooltipArea = new Rect2i(mouseX, mouseY, 1, 1);

        if (!HAS_AE2LT) {
            return;
        }

        var menu = getMenu();
        if (menu == null) {
            return;
        }

        var hit = getEntryAt(menu, mouseX, mouseY);
        if (hit == null) {
            return;
        }

        int slotX = x + LIGHTNING_SLOT_X;
        int slotY = hit.rowY();
        if (mouseX >= slotX && mouseX < slotX + LIGHTNING_SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + LIGHTNING_SLOT_SIZE) {
            currentTooltip = itemTooltip(hit.entry().stack());
            currentTooltipArea = new Rect2i(slotX, slotY, LIGHTNING_SLOT_SIZE, LIGHTNING_SLOT_SIZE);
            return;
        }

        if (isWithinEntrySwitch(mouseX, mouseY, hit.visibleRow(), hit.rowY())) {
            currentTooltip = List.of(
                    hit.entry().idOnly()
                            ? Component.translatable("ae2lt.gui.overload_pattern_encoder.mode.id_only")
                            : Component.translatable("ae2lt.gui.overload_pattern_encoder.mode.strict"));
            currentTooltipArea = new Rect2i(x + LIGHTNING_SWITCH_X, hit.rowY() + 3,
                    LIGHTNING_SWITCH_W, LIGHTNING_SWITCH_H);
        }
    }

    private static List<Component> itemTooltip(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return List.of(stack.getHoverName());
        }
        return stack.getTooltipLines(minecraft.player,
                minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
    }

    private static boolean isOverloadPattern(ItemStack stack) {
        try {
            return Class.forName("com.moakiee.ae2lt.item.OverloadPatternItem").isInstance(stack.getItem());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ItemStack firstItemTemplate(GenericStack[] possibleInputs) {
        for (var possible : possibleInputs) {
            if (possible.what() instanceof AEItemKey itemKey) {
                return itemKey.toStack((int) possible.amount());
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack toItemStack(GenericStack stack) {
        if (stack.what() instanceof AEItemKey itemKey) {
            return itemKey.toStack((int) stack.amount());
        }
        return ItemStack.EMPTY;
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

    private record EntryHit(OverloadViewEntry entry, int visibleRow, int rowY) {
    }

    private record OverloadViewEntry(boolean input, int slotIndex, boolean idOnly, ItemStack stack, Component label) {
    }

    private record OverloadModeReader(Set<Integer> inputIdOnlySlots, Set<Integer> outputIdOnlySlots) {
        boolean inputIdOnly(int slot) {
            return inputIdOnlySlots.contains(slot);
        }

        boolean outputIdOnly(int slot) {
            return outputIdOnlySlots.contains(slot);
        }

        @Nullable
        static OverloadModeReader read(ItemStack stack) {
            try {
                Class<?> overloadItemClass = Class.forName("com.moakiee.ae2lt.item.OverloadPatternItem");
                if (!overloadItemClass.isInstance(stack.getItem())) {
                    return null;
                }
                Object optional = overloadItemClass.getMethod("readEncodedPattern", ItemStack.class)
                        .invoke(stack.getItem(), stack);
                Object encodedPattern = optional.getClass().getMethod("orElse", Object.class).invoke(optional, new Object[]{null});
                if (encodedPattern == null) {
                    return null;
                }

                Set<Integer> inputs = new HashSet<>();
                for (Object slot : (Iterable<?>) encodedPattern.getClass().getMethod("inputSlots").invoke(encodedPattern)) {
                    Object matchMode = slot.getClass().getMethod("matchMode").invoke(slot);
                    boolean idOnly = (boolean) matchMode.getClass().getMethod("ignoresComponents").invoke(matchMode);
                    if (idOnly) {
                        inputs.add((Integer) slot.getClass().getMethod("slotIndex").invoke(slot));
                    }
                }

                Set<Integer> outputs = new HashSet<>();
                for (Object slot : (Iterable<?>) encodedPattern.getClass().getMethod("outputSlots").invoke(encodedPattern)) {
                    Object matchMode = slot.getClass().getMethod("matchMode").invoke(slot);
                    boolean idOnly = (boolean) matchMode.getClass().getMethod("ignoresComponents").invoke(matchMode);
                    if (idOnly) {
                        outputs.add((Integer) slot.getClass().getMethod("slotIndex").invoke(slot));
                    }
                }
                return new OverloadModeReader(inputs, outputs);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}

