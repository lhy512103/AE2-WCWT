package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import com.lhy.wcwt.client.WcwtKeybindings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 使用 AE 原版左侧工具栏按钮底图，只覆盖 WCWT 自定义收藏图标。
 */
public class FavoriteItemsButton extends IconButton {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/wcwt/wcwt_states.png");

    private static final int ICON_U_NORMAL = 240;
    private static final int ICON_U_ACTIVE = 224;
    private static final int ICON_V = 16;
    private static final int ICON_W = 16;
    private static final int ICON_H = 16;

    private final BooleanSupplier activeState;

    public FavoriteItemsButton(BooleanSupplier activeState, OnPress onPress) {
        super(onPress);
        this.activeState = activeState;
        setMessage(Component.translatable("gui.wcwt.favorite_items_first.disabled"));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }

        boolean active = activeState.getAsBoolean();
        boolean pressed = isHovered() || isFocused();
        boolean highlighted = active || pressed;
        int yOffset = pressed ? 1 : 0;
        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
        bgIcon.getBlitter()
                .dest(getX() - 1, getY() + yOffset, 18, 20)
                .zOffset(2)
                .blit(guiGraphics);

        int iconU = highlighted ? ICON_U_ACTIVE : ICON_U_NORMAL;
        int iconYOffset = pressed ? 1 : 0;
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 3);
        guiGraphics.blit(TEXTURE, getX(), getY() + 1 + iconYOffset, iconU, ICON_V, ICON_W, ICON_H, 256, 256);
        pose.popPose();
    }

    @Override
    protected Icon getIcon() {
        return Icon.TOOLBAR_BUTTON_BACKGROUND;
    }

    @Override
    public List<Component> getTooltipMessage() {
        var keyName = WcwtKeybindings.TOGGLE_FAVORITE_ITEM.getTranslatedKeyMessage()
                .copy()
                .withStyle(ChatFormatting.GOLD);
        return List.of(
                Component.translatable(activeState.getAsBoolean()
                        ? "gui.wcwt.favorite_items_first.enabled"
                        : "gui.wcwt.favorite_items_first.disabled"),
                Component.translatable("gui.wcwt.favorite_items_first.hint.prefix")
                        .append(keyName)
                        .append(Component.translatable("gui.wcwt.favorite_items_first.hint.suffix")));
    }
}
