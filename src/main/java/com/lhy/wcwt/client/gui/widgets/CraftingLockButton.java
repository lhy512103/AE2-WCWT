package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import com.lhy.wcwt.api.ICraftingLockHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 合成网格锁定按钮
 * 控制JEI配方拉取目标（合成网格 vs 样板编码网格）
 * 
 * 状态：
 * - 未锁定（默认）：JEI 工作台类配方将传输到样板编码网格
 * - 锁定：JEI 将使用 AE2 标准流程填充手动合成 3×3（从 ME / 背包取料）
 */
public class CraftingLockButton extends Button implements ITooltip {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");
    
    private final ICraftingLockHost host;
    
    public CraftingLockButton(ICraftingLockHost host, OnPress onPress) {
        super(0, 0, 16, 16, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.host = host;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        
        boolean pressedLook = host.isCraftingGridLocked() || isHoveredOrFocused();
        int srcX = host.isCraftingGridLocked() ? 32 : isHovered() ? 16 : 0;
        guiGraphics.blit(TEXTURE, getX(), getY() + (pressedLook ? 1 : 0), srcX, 0, 16, 16, 256, 256);
    }
    
    @Override
    public List<Component> getTooltipMessage() {
        if (host.isCraftingGridLocked()) {
            return List.of(
                Component.translatable("gui.wcwt.crafting_lock.locked"),
                Component.translatable("gui.wcwt.crafting_lock.locked.desc")
            );
        } else {
            return List.of(
                Component.translatable("gui.wcwt.crafting_lock.unlocked"),
                Component.translatable("gui.wcwt.crafting_lock.unlocked.desc")
            );
        }
    }
    
    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), 16, 16);
    }
    
    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}
