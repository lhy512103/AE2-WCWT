package com.lhy.wcwt.mixin;

import com.lhy.wcwt.client.WcwtRestockState;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.util.ReadableNumberConverter;

@Mixin(Gui.class)
public class GuiMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"),
            cancellable = true)
    public void wcwt$restockOverlay(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player,
                                    ItemStack stack, int seed, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.isCreative()
                || !WcwtRestockState.shouldRender(stack)) {
            return;
        }
        String number = ReadableNumberConverter.format(WcwtRestockState.getAccessibleAmount(stack), 3);
        if (number.startsWith(",")) {
            number = 0 + number;
        }
        guiGraphics.renderItemDecorations(minecraft.font, stack, x, y, number);
        ci.cancel();
    }

    @WrapWithCondition(method = "renderItemHotbar", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private boolean wcwt$skipVanillaHotbarSelection(GuiGraphics graphics, ResourceLocation sprite, int x, int y,
                                                    int width, int height) {
        if (!ResourceLocation.withDefaultNamespace("hud/hotbar_selection").equals(sprite)) {
            return true;
        }
        Player player = minecraft.player;
        return player == null || !WcwtToolkitHotbarState.isToolkitSelected(player);
    }
}
