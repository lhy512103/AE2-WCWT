package com.lhy.wcwt.mixin;

import com.lhy.wcwt.client.WcwtToolkitMemoryIcon;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PictureInPictureRenderer.class)
public abstract class WcwtPictureInPictureRendererMixin {
    @ModifyExpressionValue(method = "blitTexture", at = @At(value = "CONSTANT", args = "intValue=-1"))
    private int wcwt$applyMemoryIconColor(int color, PictureInPictureRenderState state) {
        return state instanceof OversizedItemRenderState item
                && item.guiItemRenderState().itemStackRenderState() instanceof WcwtToolkitMemoryIcon
                ? WcwtToolkitMemoryIcon.COLOR : color;
    }
}
