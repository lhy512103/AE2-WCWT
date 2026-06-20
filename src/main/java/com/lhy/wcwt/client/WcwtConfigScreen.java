package com.lhy.wcwt.client;

import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.config.WcwtServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WcwtConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 74;
    private static final int LABEL_X = 36;

    private final @Nullable Screen parent;
    private final List<ConfigRow> rows = new ArrayList<>();
    private int serverPathY = 28 + ROW_HEIGHT * 8;

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
                    WcwtServerConfig.TOOLKIT_SLOT_COUNT.get(),
                    -8,
                    () -> {
                        int next = Math.max(11, WcwtServerConfig.TOOLKIT_SLOT_COUNT.get() - 8);
                        WcwtServerConfig.TOOLKIT_SLOT_COUNT.set(next);
                        saveServerConfig();
                    });
            addIntegerRow(y, "wcwt.config.toolkitSlotCount",
                    WcwtServerConfig.TOOLKIT_SLOT_COUNT.get(),
                    8,
                    () -> {
                        int next = Math.min(640, WcwtServerConfig.TOOLKIT_SLOT_COUNT.get() + 8);
                        WcwtServerConfig.TOOLKIT_SLOT_COUNT.set(next);
                        saveServerConfig();
                    });
        } else {
            addDisabledRow(y, "wcwt.config.serverNotLoaded");
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 75, height - 32, 150, 20)
                .build());
    }

    private void addBooleanRow(int y, String translationKey, BooleanSupplier getter, BooleanSetter setter) {
        Button button = addRenderableWidget(Button.builder(booleanButtonText(getter.getAsBoolean()), btn -> {
            setter.set(!getter.getAsBoolean());
            btn.setMessage(booleanButtonText(getter.getAsBoolean()));
        }).bounds(width - LABEL_X - BUTTON_WIDTH, y - 4, BUTTON_WIDTH, 20).build());
        rows.add(new ConfigRow(translationKey, y, button));
    }

    private void addIntegerRow(int y, String translationKey, int currentValue, int delta, Runnable action) {
        int x = delta < 0 ? width - LABEL_X - BUTTON_WIDTH * 2 - 4 : width - LABEL_X - BUTTON_WIDTH;
        Button button = addRenderableWidget(Button.builder(Component.literal((delta < 0 ? "-8" : "+8") + " (" + currentValue + ")"),
                btn -> {
                    action.run();
                    rebuildWidgets();
                }).bounds(x, y - 4, BUTTON_WIDTH, 20).build());
        if (delta < 0) {
            rows.add(new ConfigRow(translationKey, y, button));
        }
    }

    private void addDisabledRow(int y, String translationKey) {
        Button button = addRenderableWidget(Button.builder(Component.translatable("wcwt.config.unavailable"), btn -> {
        }).bounds(width - LABEL_X - BUTTON_WIDTH, y - 4, BUTTON_WIDTH, 20).build());
        button.active = false;
        rows.add(new ConfigRow(translationKey, y, button));
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
        guiGraphics.drawString(font, Component.literal("Client: config/wcwt-client.toml"), LABEL_X, 28, 0xA0A0A0);
        guiGraphics.drawString(font, Component.literal("Server: saves/<world>/serverconfig/wcwt-server.toml")
                .withStyle(ChatFormatting.GRAY), LABEL_X, serverPathY, 0xA0A0A0);

        for (ConfigRow row : rows) {
            guiGraphics.drawString(font, Component.translatable(row.translationKey()), LABEL_X, row.y(), 0xE0E0E0);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (ConfigRow row : rows) {
            if (row.button().isHoveredOrFocused()) {
                Component tooltip = Component.translatable(row.translationKey() + ".tooltip");
                guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private record ConfigRow(String translationKey, int y, Button button) {
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
