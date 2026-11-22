package com.lhy.wcwt.client.gui.widgets;

import appeng.api.stacks.AEKey;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DirectionInputButton extends Button {

    private final Pair<Identifier, Identifier> textures;
    private AEKey key;
    private int index;
    private boolean highlighted;

    public DirectionInputButton(
            int x, int y, int width, int height,
            Pair<Identifier, Identifier> textures,
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
    protected void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Identifier tex = highlighted ? textures.getSecond() : textures.getFirst();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0, 0, width, height, 16, 16);
    }
}
