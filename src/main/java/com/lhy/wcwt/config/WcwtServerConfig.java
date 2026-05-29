package com.lhy.wcwt.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code config/wcwt-server.toml} 服务端配置。
 * 保存文件后 NeoForge 会重载，这里的 getter 会读取最新值。
 */
public final class WcwtServerConfig {
    private static final int MIN_TOOLKIT_SLOTS = 11;
    private static final int MAX_TOOLKIT_SLOTS = 640;
    private static final int DEFAULT_TOOLKIT_SLOTS = 64;

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue TOOLKIT_SLOT_COUNT;

    static {
        TOOLKIT_SLOT_COUNT = BUILDER
                .comment("Toolkit slot count. Minimum 11 keeps the dedicated tool slots available.")
                .translation("wcwt.config.toolkitSlotCount")
                .defineInRange("toolkitSlotCount", DEFAULT_TOOLKIT_SLOTS, MIN_TOOLKIT_SLOTS, MAX_TOOLKIT_SLOTS);
        SPEC = BUILDER.build();
    }

    private WcwtServerConfig() {
    }

    public static int toolkitSlotCount() {
        return TOOLKIT_SLOT_COUNT.get();
    }
}
