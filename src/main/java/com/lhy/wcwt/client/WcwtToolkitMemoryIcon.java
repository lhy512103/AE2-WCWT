package com.lhy.wcwt.client;

import com.lhy.wcwt.mixin.WcwtGuiGraphicsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

public final class WcwtToolkitMemoryIcon extends TrackingItemStackRenderState {
    // The item atlas uses premultiplied alpha, so scale RGB and alpha together to 38%.
    public static final int COLOR = 0x61616161;

    private WcwtToolkitMemoryIcon() {
    }

    public static void extract(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        var icon = new WcwtToolkitMemoryIcon();
        minecraft.getItemModelResolver().updateForTopItem(icon, stack, ItemDisplayContext.GUI,
                minecraft.level, minecraft.player, seed);
        ((WcwtGuiGraphicsAccessor) graphics).wcwt$getGuiRenderState().addItem(new GuiItemRenderState(
                new Matrix3x2f(graphics.pose()), icon, x, y, graphics.peekScissorStack()));
    }
}
