package com.lhy.wcwt.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface WcwtGuiGraphicsAccessor {
    @Accessor("guiRenderState")
    GuiRenderState wcwt$getGuiRenderState();
}
