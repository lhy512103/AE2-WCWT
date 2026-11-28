package com.lhy.wcwt.mixin;

import appeng.items.contents.NetworkToolMenuHost;
import appeng.menu.ToolboxMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ToolboxMenu.class)
public interface WcwtToolboxMenuAccessor {
    @Accessor("inv")
    @Nullable NetworkToolMenuHost<?> wcwt$getHost();
}
