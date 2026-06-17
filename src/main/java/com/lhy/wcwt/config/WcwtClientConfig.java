package com.lhy.wcwt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/wcwt-client.toml} 客户端个人配置。
 * 联机时也只影响当前玩家自己的界面与发包行为。
 */
public final class WcwtClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR;
    public static final ModConfigSpec.BooleanValue ENABLE_RECIPE_PULL_TRANSFER;
    public static final ModConfigSpec.BooleanValue AUTO_SWITCH_MANUAL_WORKSPACE_ON_RECIPE_TRANSFER;
    public static final ModConfigSpec.BooleanValue PATTERN_MANAGEMENT_SHIFT_QUICK;
    public static final ModConfigSpec.BooleanValue PATTERN_MANAGEMENT_SEARCH_HIGHLIGHT;
    public static final ModConfigSpec.BooleanValue PATTERN_MULTIPLIER_APPLY_TO_EDITOR_PROCESSING;
    public static final ModConfigSpec.BooleanValue PREFER_JEI_BOOKMARKS_FOR_PATTERN_ENCODING;
    public static final ModConfigSpec.BooleanValue PREFER_WCWT_FAVORITES_FOR_RECIPE_TRANSFER;
    public static final ModConfigSpec.BooleanValue EXPAND_TOOLKIT_IN_MANAGEMENT_AREA;
    public static final ModConfigSpec.BooleanValue LAST_MANAGEMENT_TOOLKIT_OPEN;
    public static final ModConfigSpec.BooleanValue LAST_VIEW_CELLS_PANEL_VISIBLE;
    public static final ModConfigSpec.BooleanValue FAVORITED_ITEMS_FIRST;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> FAVORITED_KEYS;

    static {
        PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR = BUILDER
                .comment("If true: failed pattern uploads fall back to the pattern edit slot first. If false: fall back to the pattern cache first.")
                .translation("wcwt.config.patternUploadFailFallbackToEditor")
                .define("patternUploadFailFallbackToEditor", false);
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
                .comment("If true: opening the toolkit expands it in the pattern management area instead of the right-side panel. Saving wcwt-client.toml usually reloads without restart.")
                .translation("wcwt.config.expandToolkitInManagementArea")
                .define("expandToolkitInManagementArea", false);
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
                .defineList("favoritedKeys", java.util.List.of(), entry -> entry instanceof String);
        SPEC = BUILDER.build();
    }

    private WcwtClientConfig() {
    }

    public static boolean patternUploadFailFallbackToEditor() {
        return PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR.get();
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

    public static boolean expandToolkitInManagementArea() {
        return EXPAND_TOOLKIT_IN_MANAGEMENT_AREA.get();
    }

    public static boolean lastManagementToolkitOpen() {
        return LAST_MANAGEMENT_TOOLKIT_OPEN.get();
    }

    public static void setLastManagementToolkitOpen(boolean open) {
        LAST_MANAGEMENT_TOOLKIT_OPEN.set(open);
        SPEC.save();
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
