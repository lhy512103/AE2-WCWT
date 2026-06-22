package com.lhy.wcwt.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.menu.AEBaseMenu;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.SwitchUniversalTerminalPacket;
import com.lhy.wcwt.network.SwitchUniversalTerminalPacket.Action;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import com.lhy.wcwt.util.ResourceLocationCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WcwtUniversalTerminalButton extends appeng.client.gui.widgets.IconButton {
    private static final ResourceLocation TEXTURE =
            ResourceLocationCompat.id(WcwtMod.MOD_ID, "textures/guis/wcwt_terminal_switch_button.png");

    private final AEBaseMenu menu;
    private WcwtUniversalTerminals.SwitchTarget nextTarget;
    private WcwtUniversalTerminals.SwitchTarget previousTarget;

    public WcwtUniversalTerminalButton(AEBaseMenu menu) {
        super(button -> ModNetworking.sendToServer(new SwitchUniversalTerminalPacket(Action.NEXT)));
        this.menu = menu;
    }

    public void refresh() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            setVisibility(false);
            nextTarget = null;
            previousTarget = null;
            return;
        }
        var locator = WcwtUniversalTerminals.currentLocatorOf(menu);
        if (locator == null) {
            setVisibility(false);
            nextTarget = null;
            previousTarget = null;
            return;
        }
        var next = WcwtUniversalTerminals.getNextSwitchTarget(player, locator);
        if (next.isEmpty()) {
            setVisibility(false);
            nextTarget = null;
            previousTarget = null;
            return;
        }
        setVisibility(true);
        nextTarget = next.get();
        previousTarget = WcwtUniversalTerminals.getPreviousSwitchTarget(player, locator).orElse(null);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        refresh();
        if (!visible) {
            return;
        }
        Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).blit(guiGraphics);
        if (nextTarget != null && nextTarget.displayStack().isEmpty()) {
            guiGraphics.blit(TEXTURE, getX(), getY(), 0, 0, 16, 16, 16, 16);
        } else if (nextTarget != null) {
            ItemStack stack = nextTarget.displayStack().copy();
            stack.setCount(1);
            guiGraphics.renderItem(stack, getX(), getY());
        } else {
            guiGraphics.blit(TEXTURE, getX(), getY(), 0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    protected Icon getIcon() {
        return Icon.TOOLBAR_BUTTON_BACKGROUND;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Action action = clickAction(button);
        if (action != null && active && visible && clicked(mouseX, mouseY)) {
            ModNetworking.sendToServer(new SwitchUniversalTerminalPacket(action));
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        return false;
    }

    private static Action clickAction(int button) {
        if (button == 2) {
            return Action.BASE;
        }
        if (button == 1) {
            return Action.PREVIOUS;
        }
        if (button == 0) {
            return Minecraft.getInstance().screen instanceof appeng.client.gui.AEBaseScreen<?> screen
                            && screen.isHandlingRightClick()
                    ? Action.PREVIOUS
                    : Action.NEXT;
        }
        return null;
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (nextTarget == null) {
            return List.of(Component.translatable("gui.wcwt.universal_terminal.empty"));
        }
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.wcwt.cycle_terminal.desc", terminalName(nextTarget)));
        if (previousTarget != null) {
            tooltip.add(Component.translatable("gui.wcwt.cycle_terminal.desc1", terminalName(previousTarget)));
        }
        tooltip.add(Component.translatable("gui.wcwt.cycle_terminal.middle"));
        return tooltip;
    }

    private Component terminalName(WcwtUniversalTerminals.SwitchTarget target) {
        if (target.isBaseTerminal()) {
            return Component.translatable("item.wcwt.wireless_universal_comprehensive_work_terminal");
        }
        return target.displayStack().isEmpty()
                ? Component.translatable("gui.wcwt.universal_terminal.empty")
                : target.displayStack().getHoverName();
    }
}
