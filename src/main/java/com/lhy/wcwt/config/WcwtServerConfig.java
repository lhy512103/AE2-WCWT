package com.lhy.wcwt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/wcwt-server.toml} 服务端配置。
 * 保存文件后 NeoForge 会重载，这里的 getter 会读取最新值。
 */
public final class WcwtServerConfig {
    public static final int MIN_TOOLKIT_SLOTS = 11;
    public static final int MAX_TOOLKIT_SLOTS = 640;
    private static final int DEFAULT_TOOLKIT_SLOTS = 64;

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TOOLKIT_SLOT_COUNT;
    public static final ModConfigSpec.BooleanValue PATTERN_PROVIDER_ACTIVE_REFRESH;

    static {
        TOOLKIT_SLOT_COUNT = BUILDER
                .comment("Toolkit slot count. Minimum 11 keeps the dedicated tool slots available.")
                .translation("wcwt.config.toolkitSlotCount")
                .defineInRange("toolkitSlotCount", DEFAULT_TOOLKIT_SLOTS, MIN_TOOLKIT_SLOTS, MAX_TOOLKIT_SLOTS);
        PATTERN_PROVIDER_ACTIVE_REFRESH = BUILDER
                .comment("Whether the pattern management area actively refreshes provider lists while it is open.")
                .translation("wcwt.config.patternProviderActiveRefresh")
                .define("patternProviderActiveRefresh", true);
        SPEC = BUILDER.build();
    }

    private WcwtServerConfig() {
    }

    public static int toolkitSlotCount() {
        return TOOLKIT_SLOT_COUNT.get();
    }

    public static void setToolkitSlotCount(int value) {
        TOOLKIT_SLOT_COUNT.set(value);
        SPEC.save();
    }

    public static boolean patternProviderActiveRefresh() {
        return PATTERN_PROVIDER_ACTIVE_REFRESH.get();
    }

    public static void setPatternProviderActiveRefresh(boolean value) {
        PATTERN_PROVIDER_ACTIVE_REFRESH.set(value);
        SPEC.save();
    }
}
