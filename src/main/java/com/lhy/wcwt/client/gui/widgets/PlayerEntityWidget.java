package com.lhy.wcwt.client.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * 渲染玩家实体的 Widget，用于在 WCWT 界面黑色框内展示玩家模型。
 * 渲染区域：从 (getX(), getY()) 到 (getX()+46, getY()+70)。
 */
public class PlayerEntityWidget extends AbstractWidget {
    private final LivingEntity entity;

    public PlayerEntityWidget(LivingEntity entity) {
        super(0, 0, 46, 70, Component.empty());
        this.entity = entity;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                getX(), getY(),
                getX() + 46,
                getY() + 70,
                30, 0.0625F,
                mouseX,
                mouseY,
                entity);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
