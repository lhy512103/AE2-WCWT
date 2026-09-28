package com.lhy.wcwt.mixin.client;

import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When a toolkit extra bar is selected, hide the vanilla hotbar highlight so only the extra-bar
 * selection box is shown.
 */
@Mixin(Gui.class)
public abstract class WcwtGuiHotbarMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Redirect(method = "renderHotbar", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
            ordinal = 1))
    private void wcwt$skipVanillaHotbarSelection(GuiGraphics graphics, ResourceLocation texture, int x, int y,
            int u, int v, int width, int height) {
        Player player = minecraft.player;
        if (player != null && WcwtToolkitHotbarState.isToolkitSelected(player)) {
            return;
        }
        graphics.blit(texture, x, y, u, v, width, height);
    }
}
