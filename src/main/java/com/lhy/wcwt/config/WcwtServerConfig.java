package com.lhy.wcwt.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code config/wcwt-server.toml} 全局服务端配置。
 * 保存文件后 Forge 会重载，这里的 getter 会读取最新值。
 */
public final class WcwtServerConfig {
    public static final int MIN_TOOLKIT_SLOTS = 11;
    public static final int MAX_TOOLKIT_SLOTS = 640;
    private static final int DEFAULT_TOOLKIT_SLOTS = 64;

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue TOOLKIT_SLOT_COUNT;
    public static final ForgeConfigSpec.BooleanValue PATTERN_PROVIDER_ACTIVE_REFRESH;

    static {
        TOOLKIT_SLOT_COUNT = BUILDER
                .comment("Toolkit slot count. Minimum 11 keeps the dedicated tool slots available.")
                .translation("wcwt.config.toolkitSlotCount")
                .defineInRange("toolkitSlotCount", DEFAULT_TOOLKIT_SLOTS, MIN_TOOLKIT_SLOTS, MAX_TOOLKIT_SLOTS);
        PATTERN_PROVIDER_ACTIVE_REFRESH = BUILDER
                .comment("Actively refresh the pattern management provider list while it is visible. Disabled keeps one-shot refreshes only.")
                .translation("wcwt.config.patternProviderActiveRefresh")
                .define("patternProviderActiveRefresh", false);
        SPEC = BUILDER.build();
    }

    private WcwtServerConfig() {
    }

    public static int toolkitSlotCount() {
        return TOOLKIT_SLOT_COUNT.get();
    }

    public static boolean patternProviderActiveRefresh() {
        return PATTERN_PROVIDER_ACTIVE_REFRESH.get();
    }
}
