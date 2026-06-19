package com.lhy.wcwt.mixin.guideme;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * GuideME 在开发环境下，当 Modern UI 等模组触发提前资源重载时，
 * 其 reload listener 会在配置加载前读取配置值，导致
 * {@code IllegalStateException: Cannot get config value before config is loaded}。
 * 此 Mixin 将该方法替换为安全的默认值（false），避免崩溃。
 * 生产环境下此方法通常不会在配置加载前被调用，因此无副作用。
 */
@Mixin(targets = "guideme.internal.GuideMEClient", remap = false)
public class GuideMEClientMixin {
    /**
     * @author WCWT
     * @reason Prevent crash when config is not yet loaded during early resource reload
     */
    @Overwrite
    public boolean isIgnoreTranslatedGuides() {
        return false;
    }
}
