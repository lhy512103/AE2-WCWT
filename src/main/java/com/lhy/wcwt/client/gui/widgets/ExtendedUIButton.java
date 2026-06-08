package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.widgets.ITooltip;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.client.gui.WcwtAe2Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 扩展UI按钮，用于切换不同的扩展UI面板（高级编码、装饰盔甲、饰品、卡槽箱、工具包）。
 *
 * 精灵图 wcwt_states.png 坐标说明（均为 x:y 格式，图像尺寸 256x256）：
 *   按钮面（默认 20×17）: (128, 128)
 *   按钮面（悬停 20×17）: (160, 128)
 *   按钮面（激活 20×17）: (192, 128)  — 当前扩展UI正在展示时高亮
 *
 * 各按钮图标（14×14，居中绘制在按钮面上）：
 *   ADVANCED_CODING  : (0,  16)  — 电路/编码图标
 *   CURIOS           : (16, 16)  — 饰品图标
 *   COSMETIC_ARMOR   : (32, 16)  — 装饰盔甲图标
 *   TOOL_SLOTS_BOX   : (48, 16)  — 卡槽箱图标
 *   TOOLKIT          : (32, 16)  — 工具包图标
 *   RESONATING_LIGHTNING_PATTERN_CODING : (240, 0) — 谐振过载编码器图标
 *
 * ⚠ 上面图标的 UV 坐标是根据精灵图布局推测的，如果图标显示不正确请对照
 *   综合工作终端/GUI/ae2/textures/guis/wcwt/wcwt_states.png 实际像素核对并修改此处常量。
 */
public class ExtendedUIButton extends Button implements ITooltip {

    private static final int THEMED_Z_OFFSET = 40;

    private static final ResourceLocation TEXTURE =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_states.png");

    // ---- 按钮面（三种状态）----
    private static final int BTN_V = 128;
    private static final int BTN_U_NORMAL  = 128;
    private static final int BTN_U_HOVERED = 160;
    private static final int BTN_U_ACTIVE  = 192;   // 当前扩展UI激活时使用
    private static final int BTN_W = 20, BTN_H = 17;

    // ---- 各类型图标（默认 14×14）----
    private static final int ICON_W = 14, ICON_H = 14;
    /** 图标在按钮面内的像素偏移（居中）: offsetX = (20-14)/2 = 3, offsetY = (17-14)/2 = 1 */
    private static final int ICON_OFFSET_X = 3, ICON_OFFSET_Y = 1;

    private final IExtendedUIHost host;
    private final IExtendedUIHost.ExtendedUIType uiType;
    private final BooleanSupplier activeOverride;

    public ExtendedUIButton(IExtendedUIHost host, IExtendedUIHost.ExtendedUIType uiType, OnPress onPress) {
        this(host, uiType, onPress, null);
    }

    public ExtendedUIButton(IExtendedUIHost host, IExtendedUIHost.ExtendedUIType uiType, OnPress onPress,
                            BooleanSupplier activeOverride) {
        super(0, 0, BTN_W, BTN_H, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.host = host;
        this.uiType = uiType;
        this.activeOverride = activeOverride;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) return;

        var pose = guiGraphics.pose();
        pose.pushPose();
        if (WcwtAe2Textures.usingThemedExtraPanels()) {
            pose.translate(0, 0, THEMED_Z_OFFSET);
        }

        // 1. 渲染按钮面（精灵图实际只有两种状态：普通 / 高亮）。
        //    点击后 isActive()==true 一直保持高亮，直到对应面板关闭；
        //    鼠标悬停也使用同一张高亮纹理。
        boolean highlighted = isActive() || isHovered();
        int btnU = highlighted ? BTN_U_HOVERED : BTN_U_NORMAL;
        if (isFocused()) {
            guiGraphics.fill(getX() - 2, getY() - 1, getX() + BTN_W, getY(), -1);
            guiGraphics.fill(getX() - 2, getY(), getX() - 1, getY() + BTN_H, -1);
            guiGraphics.fill(getX() + BTN_W - 1, getY(), getX() + BTN_W, getY() + BTN_H, -1);
            guiGraphics.fill(getX() - 2, getY() + BTN_H, getX() + BTN_W, getY() + BTN_H + 1, -1);
        }
        guiGraphics.blit(TEXTURE, getX()-1, getY(), btnU, BTN_V, BTN_W, BTN_H, 256, 256);

        // 2. 渲染各类型图标（居中叠加在按钮面上）。
        //    高亮时部分按钮（如装饰盔甲）使用彩色图标，UV 在 getIconU(true) 中给出。
        int iconU = getIconU(highlighted);
        int iconV = getIconV(highlighted);
        int iconW = getIconWidth();
        int iconH = getIconHeight();
        int iconOffsetX = (BTN_W - iconW) / 2 + getIconRenderOffsetX();
        int iconOffsetY = (BTN_H - iconH) / 2 + getIconRenderOffsetY();
        guiGraphics.blit(TEXTURE, getX() - 1 + iconOffsetX, getY() + iconOffsetY,
                iconU, iconV, iconW, iconH, 256, 256);
        pose.popPose();
    }

    /**
     * 返回当前按钮类型图标在精灵图中的 U（X）坐标。
     * @param highlighted 是否高亮（点击激活或悬停）
     * 如果图标错误，请对照 wcwt_states.png 像素修改此处。
     */
    private int getIconU(boolean highlighted) {
        return switch (uiType) {
            case ADVANCED_CODING -> 112;                    // 电路/编码图标
            case CURIOS          -> 96;                     // 饰品图标
            case COSMETIC_ARMOR  -> highlighted ? 144 : 128;// 装饰盔甲：高亮时换彩色图标
            case TOOL_SLOTS_BOX  -> 224;                    // 卡槽箱图标
            case TOOLKIT         -> 32;                     // 工具包图标
            case RESONATING_LIGHTNING_PATTERN_CODING -> 240;
            default              -> 0;
        };
    }

    /** 大多数图标在第 0 行，工具包图标在 wcwt_states.png 的第 16px 行。 */
    private int getIconV(boolean highlighted) {
        return switch (uiType) {
            case TOOLKIT -> 16;
            case RESONATING_LIGHTNING_PATTERN_CODING -> 0;
            default -> 0;
        };
    }

    private int getIconWidth() {
        return uiType == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING ? 16 : ICON_W;
    }

    private int getIconHeight() {
        return uiType == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING ? 16 : ICON_H;
    }

    private int getIconRenderOffsetX() {
        return uiType == IExtendedUIHost.ExtendedUIType.RESONATING_LIGHTNING_PATTERN_CODING ? 0 : -1;
    }

    private int getIconRenderOffsetY() {
        return 0;
    }

    @Override
    public List<Component> getTooltipMessage() {
        String key = switch (uiType) {
            case ADVANCED_CODING -> "gui.wcwt.extended_ui.advanced_coding";
            case COSMETIC_ARMOR  -> "gui.wcwt.extended_ui.cosmetic_armor";
            case CURIOS          -> "gui.wcwt.extended_ui.curios";
            case TOOL_SLOTS_BOX  -> "gui.wcwt.extended_ui.tool_slots_box";
            case TOOLKIT         -> "gui.wcwt.extended_ui.toolkit";
            case RESONATING_LIGHTNING_PATTERN_CODING -> "gui.wcwt.extended_ui.resonating_lightning_pattern_coding";
            default              -> "gui.wcwt.extended_ui.none";
        };
        return List.of(Component.translatable(key));
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), BTN_W, BTN_H);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    /** 返回当前按钮对应的扩展UI是否正在展示。 */
    public boolean isActive() {
        if (activeOverride != null) {
            return activeOverride.getAsBoolean();
        }
        return host.getCurrentExtendedUI() == uiType;
    }
}

