package com.lhy.wcwt.mixin;

import com.lhy.wcwt.client.WcwtToolkitMemoryIcon;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
public abstract class WcwtGuiRendererMixin {
    @ModifyExpressionValue(method = "submitBlitFromItemAtlas", at = @At(value = "CONSTANT", args = "intValue=-1"))
    private int wcwt$applyMemoryIconColor(int color, GuiItemRenderState itemState) {
        return itemState.itemStackRenderState() instanceof WcwtToolkitMemoryIcon ? WcwtToolkitMemoryIcon.COLOR : color;
    }
}
