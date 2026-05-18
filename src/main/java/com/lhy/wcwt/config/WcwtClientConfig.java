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
    public static final ModConfigSpec.BooleanValue PATTERN_MANAGEMENT_SHIFT_QUICK;

    static {
        PATTERN_UPLOAD_FAIL_FALLBACK_TO_EDITOR = BUILDER
                .comment("If true: failed pattern uploads fall back to the pattern edit slot first. If false: fall back to the pattern cache first.")
                .translation("wcwt.config.patternUploadFailFallbackToEditor")
                .define("patternUploadFailFallbackToEditor", false);
        PATTERN_MANAGEMENT_SHIFT_QUICK = BUILDER
                .comment("If false: pattern management shift quick moves use normal clicks only. Saving wcwt-client.toml usually reloads without restart.")
                .translation("wcwt.config.patternManagementShiftQuick")
                .define("patternManagementShiftQuick", true);
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
}
