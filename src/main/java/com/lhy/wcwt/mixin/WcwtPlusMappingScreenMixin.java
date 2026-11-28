package com.lhy.wcwt.mixin;

import com.extendedae_plus.client.screen.RecipeTypeMappingScreen;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.extendedae_plus.client.screen.RecipeTypeMappingScreen", remap = false)
public abstract class WcwtPlusMappingScreenMixin {
    @WrapWithCondition(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(value = "INVOKE", target = "Lcom/extendedae_plus/client/screen/RecipeTypeMappingScreen;"
                    + "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"),
            require = 0)
    private boolean wcwt$skipDuplicateBackground(RecipeTypeMappingScreen screen, GuiGraphicsExtractor graphics,
                                                int mouseX, int mouseY, float partialTick) {
        // Screen extracts the background before calling extractRenderState.
        return false;
    }
}
