package com.lhy.wcwt.client.gui.widgets;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.widgets.ITooltip;

public final class WcwtProcessingMaterialsMergeButton extends Button implements ITooltip {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");
    private static final int TEX_SIZE = 256;
    /** 合并开启：u=0,v=16 */
    private static final int ON_U = 0;
    private static final int ON_V = 16;
    /** 合并关闭：u=16,v=16 */
    private static final int OFF_U = 16;
    private static final int OFF_V = 16;
    private static final int SPRITE = 8;

    private boolean mergeEnabled;

    private final Component tooltipTitleOn;
    private final Component tooltipTitleOff;
    @Nullable
    private final Component tooltipDescOn;
    @Nullable
    private final Component tooltipDescOff;

    public WcwtProcessingMaterialsMergeButton(Component tooltipTitleOn,
                                              @Nullable Component tooltipDescOn,
                                              Component tooltipTitleOff,
                                              @Nullable Component tooltipDescOff,
                                              OnPress onPress) {
        super(0, 0, SPRITE, SPRITE, tooltipTitleOn, onPress, Button.DEFAULT_NARRATION);
        this.tooltipTitleOn = tooltipTitleOn;
        this.tooltipTitleOff = tooltipTitleOff;
        this.tooltipDescOn = tooltipDescOn;
        this.tooltipDescOff = tooltipDescOff;
    }

    public void setMergeEnabled(boolean mergeEnabled) {
        this.mergeEnabled = mergeEnabled;
    }

    public boolean isMergeEnabled() {
        return mergeEnabled;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        int u = mergeEnabled ? ON_U : OFF_U;
        int v = mergeEnabled ? ON_V : OFF_V;
        guiGraphics.blit(TEX, getX(), getY(), u, v, SPRITE, SPRITE, TEX_SIZE, TEX_SIZE);
    }

    @Override
    public List<Component> getTooltipMessage() {
        var title = mergeEnabled ? tooltipTitleOn : tooltipTitleOff;
        var desc = mergeEnabled ? tooltipDescOn : tooltipDescOff;
        if (desc != null) {
            return List.of(title, desc);
        }
        return List.of(title);
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}
