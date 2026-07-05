package com.lhy.wcwt.client;

import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.config.WcwtServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WcwtConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 74;
    private static final int LABEL_X = 36;
    private static final int CONTENT_TOP = 28;
    private static final int CONTENT_BOTTOM_MARGIN = 40;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLL_STEP = ROW_HEIGHT;

    private final @Nullable Screen parent;
    private final List<ConfigRow> rows = new ArrayList<>();
    private int serverPathY = 28 + ROW_HEIGHT * 8;
    private int contentHeight;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private Button doneButton;

    public WcwtConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("wcwt.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();
        int y = 42;

        if (WcwtClientConfig.SPEC.isLoaded()) {
            addBooleanRow(y, "wcwt.config.patternUploadFailFallbackToEditor",
                    () -> WcwtClientConfig.PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR.get(),
                    value -> {
                        WcwtClientConfig.PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.enableRecipePullTransfer",
                    () -> WcwtClientConfig.ENABLE_RECIPE_PULL_TRANSFER.get(),
                    value -> {
                        WcwtClientConfig.ENABLE_RECIPE_PULL_TRANSFER.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.filterGtceuNonConsumablePatternInputs",
                    () -> WcwtClientConfig.FILTER_GTCEU_NON_CONSUMABLE_PATTERN_INPUTS.get(),
                    value -> {
                        WcwtClientConfig.FILTER_GTCEU_NON_CONSUMABLE_PATTERN_INPUTS.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.keepGtceuProgrammedCircuitWhenFilteringNonConsumables",
                    () -> WcwtClientConfig.KEEP_GTCEU_PROGRAMMED_CIRCUIT_WHEN_FILTERING_NON_CONSUMABLES.get(),
                    value -> {
                        WcwtClientConfig.KEEP_GTCEU_PROGRAMMED_CIRCUIT_WHEN_FILTERING_NON_CONSUMABLES.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.autoSwitchManualWorkspaceOnRecipeTransfer",
                    () -> WcwtClientConfig.AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER.get(),
                    value -> {
                        WcwtClientConfig.AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.patternManagementShiftQuick",
                    () -> WcwtClientConfig.PATTERN_MANAGEMENT_SHIFT_QUICK.get(),
                    value -> {
                        WcwtClientConfig.PATTERN_MANAGEMENT_SHIFT_QUICK.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.patternMultiplierApplyToEditorProcessing",
                    () -> WcwtClientConfig.PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING.get(),
                    value -> {
                        WcwtClientConfig.PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.preferJeiBookmarksForPatternEncoding",
                    () -> WcwtClientConfig.PREFER_JEI_BOOKMARKS_FOR_PATTERN_ENCODING.get(),
                    value -> {
                        WcwtClientConfig.PREFER_JEI_BOOKMARKS_FOR_PATTERN_ENCODING.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.preferFavoritesForPatternEncoding",
                    () -> WcwtClientConfig.PREFER_FAVORITES_FOR_PATTERN_ENCODING.get(),
                    value -> {
                        WcwtClientConfig.PREFER_FAVORITES_FOR_PATTERN_ENCODING.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.fillProviderSearchFromJeiBookmark",
                    () -> WcwtClientConfig.FILL_PROVIDER_SEARCH_FROM_JEI_BOOKMARK.get(),
                    value -> {
                        WcwtClientConfig.FILL_PROVIDER_SEARCH_FROM_JEI_BOOKMARK.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.expandToolkitInManagementArea",
                    () -> WcwtClientConfig.EXPAND_TOOLKIT_IN_MANAGEMENT_AREA.get(),
                    value -> {
                        WcwtClientConfig.EXPAND_TOOLKIT_IN_MANAGEMENT_AREA.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.priorityShiftMoveToCosmeticArmor",
                    () -> WcwtClientConfig.PRIORITY_SHIFT_MOVE_TO_COSMETIC_ARMOR.get(),
                    value -> {
                        WcwtClientConfig.PRIORITY_SHIFT_MOVE_TO_COSMETIC_ARMOR.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.priorityShiftMoveToCardBox",
                    () -> WcwtClientConfig.PRIORITY_SHIFT_MOVE_TO_CARD_BOX.get(),
                    value -> {
                        WcwtClientConfig.PRIORITY_SHIFT_MOVE_TO_CARD_BOX.set(value);
                        saveClientConfig();
                    });
            y += ROW_HEIGHT;
            addBooleanRow(y, "wcwt.config.priorityShiftMoveToToolkit",
                    () -> WcwtClientConfig.PRIORITY_SHIFT_MOVE_TO_TOOLKIT.get(),
                    value -> {
                        WcwtClientConfig.PRIORITY_SHIFT_MOVE_TO_TOOLKIT.set(value);
                        saveClientConfig();
                    });
        } else {
            addDisabledRow(y, "wcwt.config.clientNotLoaded");
        }

        y += ROW_HEIGHT + 8;
        serverPathY = y - 14;
        if (WcwtServerConfig.SPEC.isLoaded()) {
            addBooleanRow(y, "wcwt.config.patternProviderActiveRefresh",
                    () -> WcwtServerConfig.PATTERN_PROVIDER_ACTIVE_REFRESH.get(),
                    value -> {
                        WcwtServerConfig.PATTERN_PROVIDER_ACTIVE_REFRESH.set(value);
                        saveServerConfig();
                    });
            y += ROW_HEIGHT;
            addIntegerRow(y, "wcwt.config.toolkitSlotCount",
                    () -> {
                        int next = Math.max(WcwtServerConfig.MIN_TOOLKIT_SLOTS,
                                WcwtServerConfig.TOOLKIT_SLOT_COUNT.get() - 8);
                        WcwtServerConfig.TOOLKIT_SLOT_COUNT.set(next);
                        saveServerConfig();
                    },
                    () -> {
                        int next = Math.min(WcwtServerConfig.MAX_TOOLKIT_SLOTS,
                                WcwtServerConfig.TOOLKIT_SLOT_COUNT.get() + 8);
                        WcwtServerConfig.TOOLKIT_SLOT_COUNT.set(next);
                        saveServerConfig();
                    });
            y += ROW_HEIGHT;
        } else {
            addDisabledRow(y, "wcwt.config.serverNotLoaded");
            y += ROW_HEIGHT;
        }

        contentHeight = y + 8;
        updateScrollableWidgets();
        doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 75, height - 32, 150, 20)
                .build());
    }

    private void addBooleanRow(int y, String translationKey, BooleanSupplier getter, BooleanSetter setter) {
        Button button = addRenderableWidget(Button.builder(booleanButtonText(getter.getAsBoolean()), btn -> {
            setter.set(!getter.getAsBoolean());
            btn.setMessage(booleanButtonText(getter.getAsBoolean()));
        }).bounds(width - LABEL_X - BUTTON_WIDTH, y - 4, BUTTON_WIDTH, 20).build());
        rows.add(new ConfigRow(translationKey, y, List.of(button)));
    }

    private void addIntegerRow(int y, String translationKey, Runnable decreaseAction, Runnable increaseAction) {
        int currentValue = WcwtServerConfig.TOOLKIT_SLOT_COUNT.get();
        Button decrease = addRenderableWidget(Button.builder(Component.literal("-8 (" + currentValue + ")"),
                btn -> {
                    decreaseAction.run();
                    rebuildWidgets();
                }).bounds(width - LABEL_X - BUTTON_WIDTH * 2 - 4, y - 4, BUTTON_WIDTH, 20).build());
        Button increase = addRenderableWidget(Button.builder(Component.literal("+8 (" + currentValue + ")"),
                btn -> {
                    increaseAction.run();
                    rebuildWidgets();
                }).bounds(width - LABEL_X - BUTTON_WIDTH, y - 4, BUTTON_WIDTH, 20).build());
        rows.add(new ConfigRow(translationKey, y, List.of(decrease, increase)));
    }

    private void addDisabledRow(int y, String translationKey) {
        Button button = addRenderableWidget(Button.builder(Component.translatable("wcwt.config.unavailable"), btn -> {
        }).bounds(width - LABEL_X - BUTTON_WIDTH, y - 4, BUTTON_WIDTH, 20).build());
        button.active = false;
        rows.add(new ConfigRow(translationKey, y, List.of(button)));
    }

    private static void saveClientConfig() {
        if (WcwtClientConfig.SPEC.isLoaded()) {
            WcwtClientConfig.SPEC.save();
        }
    }

    private static void saveServerConfig() {
        if (WcwtServerConfig.SPEC.isLoaded()) {
            WcwtServerConfig.SPEC.save();
        }
    }

    private static Component booleanButtonText(boolean value) {
        return value ? Component.translatable("options.on") : Component.translatable("options.off");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);

        guiGraphics.enableScissor(0, CONTENT_TOP, width, scrollAreaBottom());
        drawContentLine(guiGraphics, Component.literal("Client: config/wcwt-client.toml"), 28, 0xA0A0A0);
        drawContentLine(guiGraphics, Component.literal("Server: config/wcwt-server.toml")
                .withStyle(ChatFormatting.GRAY), serverPathY, 0xA0A0A0);

        for (ConfigRow row : rows) {
            int rowY = contentToScreenY(row.y());
            if (isTextVisible(rowY)) {
                guiGraphics.drawString(font, Component.translatable(row.translationKey()), LABEL_X, rowY, 0xE0E0E0);
            }
        }

        for (ConfigRow row : rows) {
            for (Button button : row.buttons()) {
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics);
        doneButton.render(guiGraphics, mouseX, mouseY, partialTick);

        for (ConfigRow row : rows) {
            for (Button button : row.buttons()) {
                if (button.visible && button.isHoveredOrFocused()) {
                    Component tooltip = Component.translatable(row.translationKey() + ".tooltip");
                    guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
                    return;
                }
            }
        }
    }

    private void drawContentLine(GuiGraphics guiGraphics, Component text, int contentY, int color) {
        int screenY = contentToScreenY(contentY);
        if (isTextVisible(screenY)) {
            guiGraphics.drawString(font, text, LABEL_X, screenY, color);
        }
    }

    private void updateScrollableWidgets() {
        scrollOffset = Mth.clamp(scrollOffset, 0, getMaxScroll());
        for (ConfigRow row : rows) {
            int buttonY = contentToScreenY(row.y() - 4);
            boolean visible = buttonY + 20 > CONTENT_TOP && buttonY < scrollAreaBottom();
            for (Button button : row.buttons()) {
                button.setY(buttonY);
                button.visible = visible;
            }
        }
    }

    private int contentToScreenY(int contentY) {
        return contentY - scrollOffset;
    }

    private boolean isTextVisible(int y) {
        return y + font.lineHeight > CONTENT_TOP && y < scrollAreaBottom();
    }

    private int scrollAreaBottom() {
        return height - CONTENT_BOTTOM_MARGIN;
    }

    private int getMaxScroll() {
        return Math.max(0, contentHeight - scrollAreaBottom());
    }

    private void scrollBy(int delta) {
        int next = Mth.clamp(scrollOffset + delta, 0, getMaxScroll());
        if (next != scrollOffset) {
            scrollOffset = next;
            updateScrollableWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= CONTENT_TOP && mouseY <= scrollAreaBottom() && getMaxScroll() > 0) {
            scrollBy((int) Math.round(-delta * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (doneButton.isMouseOver(mouseX, mouseY)) {
            return doneButton.mouseClicked(mouseX, mouseY, button);
        }
        if (mouseY < CONTENT_TOP || mouseY > scrollAreaBottom()) {
            return false;
        }
        if (button == 0 && isMouseOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        return getMaxScroll() > 0
                && mouseX >= scrollbarX()
                && mouseX <= scrollbarX() + SCROLLBAR_WIDTH
                && mouseY >= CONTENT_TOP
                && mouseY <= scrollAreaBottom();
    }

    private void setScrollFromMouse(double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int top = CONTENT_TOP;
        int trackHeight = scrollAreaBottom() - top;
        int thumbHeight = scrollbarThumbHeight();
        int travel = Math.max(1, trackHeight - thumbHeight);
        double progress = (mouseY - top - thumbHeight / 2.0) / travel;
        scrollOffset = Mth.clamp((int) Math.round(progress * maxScroll), 0, maxScroll);
        updateScrollableWidgets();
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int x = scrollbarX();
        int top = CONTENT_TOP;
        int bottom = scrollAreaBottom();
        int thumbHeight = scrollbarThumbHeight();
        int thumbTravel = Math.max(1, bottom - top - thumbHeight);
        int thumbY = top + Math.round(thumbTravel * (scrollOffset / (float) maxScroll));
        guiGraphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, 0x66000000);
        guiGraphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF9A9A9A);
    }

    private int scrollbarX() {
        return width - 16;
    }

    private int scrollbarThumbHeight() {
        int viewportHeight = scrollAreaBottom() - CONTENT_TOP;
        int scrollableHeight = Math.max(viewportHeight, contentHeight - CONTENT_TOP);
        return Mth.clamp(viewportHeight * viewportHeight / scrollableHeight, 16, viewportHeight);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private record ConfigRow(String translationKey, int y, List<Button> buttons) {
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }
}
