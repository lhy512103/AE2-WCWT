package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.Icon;
import com.lhy.wcwt.client.WcwtKeybindings;
import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Uses AE2's terminal toolbar button background and WCWT's favorite icon.
 */
public class FavoriteItemsButton extends appeng.client.gui.widgets.IconButton {
    private static final ResourceLocation TEXTURE =
            ResourceLocationCompat.id("ae2", "textures/guis/wcwt/wcwt_states.png");

    private static final int ICON_U_NORMAL = 240;
    private static final int ICON_U_ACTIVE = 224;
    private static final int ICON_V = 16;
    private static final int ICON_W = 16;
    private static final int ICON_H = 16;

    private final BooleanSupplier activeState;

    public FavoriteItemsButton(BooleanSupplier activeState, OnPress onPress) {
        super(onPress);
        this.activeState = activeState;
        refreshMessage();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }

        refreshMessage();
        boolean modeEnabled = activeState.getAsBoolean();
        if (isFocused()) {
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), -1);
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + height, -1);
            guiGraphics.fill(getX() + width, getY(), getX() + width + 1, getY() + height, -1);
            guiGraphics.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, -1);
        }
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 2);
        Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter()
                .dest(getX(), getY())
                .blit(guiGraphics);

        int iconU = modeEnabled ? ICON_U_ACTIVE : ICON_U_NORMAL;
        pose.translate(0, 0, 1);
        guiGraphics.blit(TEXTURE, getX(), getY(), iconU, ICON_V, ICON_W, ICON_H, 256, 256);
        pose.popPose();
    }

    @Override
    protected Icon getIcon() {
        return Icon.TOOLBAR_BUTTON_BACKGROUND;
    }

    public void refreshMessage() {
        setMessage(Component.translatable(activeState.getAsBoolean()
                ? "gui.wcwt.favorite_items_first.enabled"
                : "gui.wcwt.favorite_items_first.disabled"));
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
