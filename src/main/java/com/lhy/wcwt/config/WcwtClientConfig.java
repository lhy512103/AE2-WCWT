package com.lhy.wcwt.config;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code config/wcwt-client.toml} 客户端个人配置。
 * 联机时也只影响当前玩家自己的界面与发包行为。
 */
public final class WcwtClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR;
    public static final ModConfigSpec.BooleanValue AUTO_FILL_PATTERN_PROVIDER_SEARCH_WHEN_UPLOAD_DISABLED;
    public static final ModConfigSpec.BooleanValue FILL_SEARCH_HOTKEY_SYNC_PATTERN_MANAGEMENT;
    public static final ModConfigSpec.BooleanValue ENABLE_RECIPE_PULL_TRANSFER;
    public static final ModConfigSpec.BooleanValue AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER;
    public static final ModConfigSpec.BooleanValue PATTERN_MANAGEMENT_SHIFT_QUICK;
    public static final ModConfigSpec.BooleanValue PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT;
    public static final ModConfigSpec.BooleanValue PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING;
    public static final ModConfigSpec.BooleanValue PREFER_JEI_BOOKMARKS_FOR_PATTERN_ENCODING;
    public static final ModConfigSpec.BooleanValue PREFER_WCWT_FAVORITES_FOR_RECIPE_TRANSFER;
    public static final ModConfigSpec.BooleanValue EXPAND_TOOLKIT_IN_MANAGEMENT_AREA;
    public static final ModConfigSpec.BooleanValue SHOW_TOOLKIT_HOTBARS;
    public static final ModConfigSpec.BooleanValue PATTERN_MANAGEMENT_AUTO_COMPACT_EMPTY_SLOTS;
    public static final ModConfigSpec.BooleanValue EMI_PREVIEW_RECIPE_FILL;
    public static final ModConfigSpec.BooleanValue LAST_MANAGEMENT_TOOLKIT_OPEN;
    public static final ModConfigSpec.BooleanValue LAST_VIEW_CELLS_PANEL_VISIBLE;
    public static final ModConfigSpec.BooleanValue FAVORITED_ITEMS_FIRST;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> FAVORITED_KEYS;

    static {
        PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR = BUILDER
                .comment("If true: failed pattern uploads fall back to the pattern edit slot first. If false: fall back to the pattern cache first.")
                .translation("wcwt.config.patternUploadFailFallbackToEditor")
                .define("patternUploadFailFallbackToEditor", false);
        AUTO_FILL_PATTERN_PROVIDER_SEARCH_WHEN_UPLOAD_DISABLED = BUILDER
                .comment("If true: encoding a pattern also fills the pattern-provider search field when pattern upload is disabled. If false: the field is filled only when pattern upload is enabled.")
                .translation("wcwt.config.autoFillPatternProviderSearchWhenUploadDisabled")
                .define("autoFillPatternProviderSearchWhenUploadDisabled", true);
        FILL_SEARCH_HOTKEY_SYNC_PATTERN_MANAGEMENT = BUILDER
                .comment("If true: the EAEP fill-search hotkey also fills the WCWT pattern-management search field. The terminal search field is always filled.")
                .translation("wcwt.config.fillSearchHotkeySyncPatternManagement")
                .define("fillSearchHotkeySyncPatternManagement", true);
        ENABLE_RECIPE_PULL_TRANSFER = BUILDER
                .comment("If false: disable WCWT JEI/EMI recipe pull and encoding transfer handling, including preview highlights.")
                .translation("wcwt.config.enableRecipePullTransfer")
                .define("enableRecipePullTransfer", true);
        AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER = BUILDER
                .comment("If true: JEI/EMI recipe transfers switch the manual workspace to crafting or smithing when the recipe type is known.")
                .translation("wcwt.config.autoSwitchManualWorkspaceOnRecipeTransfer")
                .define("autoSwitchManualWorkspaceOnRecipeTransfer", true);
        PATTERN_MANAGEMENT_SHIFT_QUICK = BUILDER
                .comment("If false: pattern management shift quick moves use normal clicks only. Saving wcwt-client.toml usually reloads without restart.")
                .translation("wcwt.config.patternManagementShiftQuick")
                .define("patternManagementShiftQuick", true);
        PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT = BUILDER
                .comment("If true: when the pattern management search filters by a pattern's inputs or outputs, matching pattern slots are highlighted.")
                .translation("wcwt.config.patternManagementSearchHighlight")
                .define("patternManagementSearchHighlight", true);
        PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING = BUILDER
                .comment("If true: the batch pattern multiplier also applies to the current processing pattern in the pattern editor. Saving wcwt-client.toml usually reloads without restart.")
                .translation("wcwt.config.patternMultiplierApplyToEditorProcessing")
                .define("patternMultiplierApplyToEditorProcessing", true);
        PREFER_JEI_BOOKMARKS_FOR_PATTERN_ENCODING = BUILDER
                .comment("If true: when JEI transfers ingredients into the WCWT pattern encoding area, matching items from the JEI bookmark list are preferred first, in bookmark order. If false: use the existing WCWT/AE2 selection logic only.")
                .translation("wcwt.config.preferJeiBookmarksForPatternEncoding")
                .define("preferJeiBookmarksForPatternEncoding", true);
        PREFER_WCWT_FAVORITES_FOR_RECIPE_TRANSFER = BUILDER
                .comment("If true: when JEI/EMI recipe transfer has multiple matching item candidates, WCWT favorited ME terminal items are preferred before the existing selection logic. Disabled by default to preserve AE2-like behavior.")
                .translation("wcwt.config.preferWcwtFavoritesForRecipeTransfer")
                .define("preferWcwtFavoritesForRecipeTransfer", false);
        EXPAND_TOOLKIT_IN_MANAGEMENT_AREA = BUILDER
                .comment("If true: opening the toolkit expands it in the pattern management area instead of the right-side panel.")
                .translation("wcwt.config.expandToolkitInManagementArea")
                .define("expandToolkitInManagementArea", false);
        SHOW_TOOLKIT_HOTBARS = BUILDER
                .comment("If true: show the first two toolkit rows as left and right HUD hotbars while in the world.")
                .translation("wcwt.config.showToolkitHotbars")
                .define("showToolkitHotbars", true);
        PATTERN_MANAGEMENT_AUTO_COMPACT_EMPTY_SLOTS = BUILDER
                .comment("If true: empty pattern-provider slots are collapsed into one summary slot in the management area.")
                .translation("wcwt.config.patternManagementAutoCompactEmptySlots")
                .define("patternManagementAutoCompactEmptySlots", true);
        EMI_PREVIEW_RECIPE_FILL = BUILDER
                .comment("If true: EMI craftable actions (left-click / Shift+left-click on a bookmarked or recipe-context stack) fill the full recipe into WCWT, same as the recipe-page + button.")
                .translation("wcwt.config.emiPreviewRecipeFill")
                .define("emiPreviewRecipeFill", true);
        LAST_MANAGEMENT_TOOLKIT_OPEN = BUILDER
                .comment("Remembers whether the management-area toolkit was open the last time this client closed the terminal.")
                .translation("wcwt.config.lastManagementToolkitOpen")
                .define("lastManagementToolkitOpen", false);
        LAST_VIEW_CELLS_PANEL_VISIBLE = BUILDER
                .comment("Remembers whether the AE2 view cells panel was visible the last time this client toggled it in WCWT.")
                .translation("wcwt.config.lastViewCellsPanelVisible")
                .define("lastViewCellsPanelVisible", true);
        FAVORITED_ITEMS_FIRST = BUILDER
                .comment("If true: favorited ME terminal entries are displayed before non-favorited entries in WCWT.")
                .translation("wcwt.config.favoritedItemsFirst")
                .define("favoritedItemsFirst", false);
        FAVORITED_KEYS = BUILDER
                .comment("Serialized client-side favorite AE keys for WCWT terminal sorting and overlays.")
                .translation("wcwt.config.favoritedKeys")
                .defineList("favoritedKeys", java.util.List.of(), () -> "", entry -> entry instanceof String);
        SPEC = BUILDER.build();
    }

    private WcwtClientConfig() {
    }

    public static boolean patternUploadFailFallbackToEditor() {
        return PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR.get();
    }

    public static boolean autoFillPatternProviderSearchWhenUploadDisabled() {
        return AUTO_FILL_PATTERN_PROVIDER_SEARCH_WHEN_UPLOAD_DISABLED.get();
    }

    public static boolean fillSearchHotkeySyncPatternManagement() {
        return FILL_SEARCH_HOTKEY_SYNC_PATTERN_MANAGEMENT.get();
    }

    public static boolean patternManagementShiftQuickEnabled() {
        return PATTERN_MANAGEMENT_SHIFT_QUICK.get();
    }

    public static boolean patternManagementSearchHighlight() {
        return PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT.get();
    }

    public static boolean autoSwitchManualWorkspaceOnRecipeTransfer() {
        return AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER.get();
    }

    public static boolean enableRecipePullTransfer() {
        return ENABLE_RECIPE_PULL_TRANSFER.get();
    }

    public static boolean patternMultiplierApplyToEditorProcessing() {
        return PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING.get();
    }

    public static boolean preferJeiBookmarksForPatternEncoding() {
        return PREFER_JEI_BOOKMARKS_FOR_PATTERN_ENCODING.get();
    }

    public static boolean preferWcwtFavoritesForRecipeTransfer() {
        return PREFER_WCWT_FAVORITES_FOR_RECIPE_TRANSFER.get();
    }

    public static boolean showToolkitHotbars() {
        migrateLegacyToolkitSettings();
        return SHOW_TOOLKIT_HOTBARS.get();
    }

    public static void setShowToolkitHotbars(boolean enabled) {
        SHOW_TOOLKIT_HOTBARS.set(enabled);
        SPEC.save();
    }

    public static boolean expandToolkitInManagementArea() {
        migrateLegacyToolkitSettings();
        return EXPAND_TOOLKIT_IN_MANAGEMENT_AREA.get();
    }

    public static boolean patternManagementAutoCompactEmptySlots() {
        return PATTERN_MANAGEMENT_AUTO_COMPACT_EMPTY_SLOTS.get();
    }

    public static boolean emiPreviewRecipeFill() {
        return EMI_PREVIEW_RECIPE_FILL.get();
    }

    public static boolean lastManagementToolkitOpen() {
        return LAST_MANAGEMENT_TOOLKIT_OPEN.get();
    }

    public static void setLastManagementToolkitOpen(boolean open) {
        LAST_MANAGEMENT_TOOLKIT_OPEN.set(open);
        SPEC.save();
    }

    private static boolean toolkitSettingsMigrationChecked;
    private static final Pattern CONFIG_KEY = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(.+?)\\s*$");

    /** Convert the temporary display-mode setting used during development once. */
    private static void migrateLegacyToolkitSettings() {
        if (toolkitSettingsMigrationChecked) {
            return;
        }
        toolkitSettingsMigrationChecked = true;
        var path = FMLPaths.CONFIGDIR.get().resolve("wcwt-client.toml");
        if (!Files.isRegularFile(path)) {
            return;
        }
        boolean hasNew = false;
        Integer temporaryMode = null;
        try {
            for (String line : Files.readAllLines(path)) {
                Matcher matcher = CONFIG_KEY.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                if ("showToolkitHotbars".equals(matcher.group(1))) {
                    hasNew = true;
                } else if ("toolkitDisplayMode".equals(matcher.group(1))) {
                    try {
                        temporaryMode = Integer.parseInt(matcher.group(2).split("#", 2)[0].trim());
                    } catch (NumberFormatException ignored) {
                        temporaryMode = null;
                    }
                }
            }
        } catch (IOException ignored) {
            return;
        }
        if (temporaryMode != null && !hasNew) {
            EXPAND_TOOLKIT_IN_MANAGEMENT_AREA.set(temporaryMode == 1);
            SHOW_TOOLKIT_HOTBARS.set(temporaryMode == 2);
            SPEC.save();
        }
    }

    public static boolean lastViewCellsPanelVisible() {
        return LAST_VIEW_CELLS_PANEL_VISIBLE.get();
    }

    public static void setLastViewCellsPanelVisible(boolean visible) {
        LAST_VIEW_CELLS_PANEL_VISIBLE.set(visible);
        SPEC.save();
    }

    public static boolean favoritedItemsFirst() {
        return FAVORITED_ITEMS_FIRST.get();
    }

    public static void setFavoritedItemsFirst(boolean enabled) {
        FAVORITED_ITEMS_FIRST.set(enabled);
        SPEC.save();
    }

    public static java.util.List<String> favoritedKeys() {
        return new java.util.ArrayList<>(FAVORITED_KEYS.get()
                .stream()
                .map(String::valueOf)
                .toList());
    }

    public static void setFavoritedKeys(java.util.Collection<String> serializedKeys) {
        FAVORITED_KEYS.set(new java.util.ArrayList<>(serializedKeys));
        SPEC.save();
    }
}
