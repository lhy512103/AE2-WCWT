package com.lhy.wcwt.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEKey;

/**
 * 方向输入按钮 - 用于样板编码器的方向选择
 * 支持7个方向：ANY, NORTH, EAST, SOUTH, WEST, UP, DOWN
 */
public class DirectionInputButton extends Button {
    
    private final Pair<ResourceLocation, ResourceLocation> textures;
    private AEKey key;
    private int index;
    private boolean highlighted;
    
    public DirectionInputButton(
            int x, int y, int width, int height, 
            Pair<ResourceLocation, ResourceLocation> textures, 
            OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.textures = textures;
    }
    
    public void setHighlighted(boolean isHighlighted) {
        this.highlighted = isHighlighted;
    }
    
    public void setKey(AEKey key) {
        this.key = key;
    }
    
    public AEKey getKey() {
        return this.key;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    @Nullable
    public Direction getDirection() {
        return switch (index) {
            case 1 -> Direction.NORTH;
            case 2 -> Direction.EAST;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.UP;
            case 6 -> Direction.DOWN;
            default -> null; // 0 = ANY
        };
    }
    
    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        
        if (highlighted) {
            RenderSystem.setShaderTexture(0, textures.getSecond());
            guiGraphics.blit(textures.getSecond(), this.getX(), this.getY(), 0, 0, width, height, 16, 16);
        } else {
            RenderSystem.setShaderTexture(0, textures.getFirst());
            guiGraphics.blit(textures.getFirst(), this.getX(), this.getY(), 0, 0, width, height, 16, 16);
        }
    }
}
