package com.lhy.wcwt.client.gui.widgets;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.client.gui.widgets.ITooltip;
import appeng.menu.AEBaseMenu;
import appeng.menu.locator.MenuHostLocator;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.network.SwitchUniversalTerminalPacket;
import com.lhy.wcwt.network.SwitchUniversalTerminalPacket.Action;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import de.mari_023.ae2wtlib.api.TextConstants;
import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WcwtUniversalTerminalButton extends Button implements ITooltip {
    private final AEBaseMenu autoRefreshMenu;
    private WcwtUniversalTerminals.SwitchTarget nextTarget;
    private WcwtUniversalTerminals.SwitchTarget previousTarget;

    public WcwtUniversalTerminalButton() {
        this(null);
    }

    public WcwtUniversalTerminalButton(AEBaseMenu autoRefreshMenu) {
        super(0, 0, 16, 16, Component.translatable("gui.wcwt.universal_terminal.empty"),
                button -> PacketDistributor.sendToServer(new SwitchUniversalTerminalPacket(Action.NEXT)),
                Button.DEFAULT_NARRATION);
        this.autoRefreshMenu = autoRefreshMenu;
    }

    public void refresh(AEBaseMenu menu) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            setVisibleState(false);
            nextTarget = null;
            previousTarget = null;
            return;
        }
        MenuHostLocator locator = resolveLocator(menu);
        if (locator == null) {
            setVisibleState(false);
            nextTarget = null;
            previousTarget = null;
            return;
        }
        var next = WcwtUniversalTerminals.getNextSwitchTarget(player, locator);
        if (next.isEmpty()) {
            setVisibleState(false);
            nextTarget = null;
            previousTarget = null;
            return;
        }
        setVisibleState(true);
        nextTarget = next.get();
        previousTarget = WcwtUniversalTerminals.getPreviousSwitchTarget(player, locator).orElse(null);
    }

    public static MenuHostLocator resolveLocator(AEBaseMenu menu) {
        if (menu.getTarget() instanceof ItemMenuHost<?> itemMenuHost) {
            MenuHostLocator locator = itemMenuHost.getLocator();
            if (locator != null) {
                return locator;
            }
        }
        return menu.getLocator();
    }

    private void setVisibleState(boolean visible) {
        this.visible = visible;
        this.active = visible;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (autoRefreshMenu != null) {
            refresh(autoRefreshMenu);
        }
        if (!visible) {
            return;
        }
        int yOffset = isHovered() ? 1 : 0;
        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVERED
                : isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUSED : Icon.TOOLBAR_BUTTON_BACKGROUND;
        bgIcon.getBlitter()
                .dest(getX() - 1, getY() + yOffset, bgIcon.width(), bgIcon.height())
                .zOffset(2)
                .blit(guiGraphics);

        if (nextTarget == null) {
            return;
        }

        Optional<Icon> icon = iconFor(nextTarget);
        if (icon.isPresent()) {
            icon.get().getBlitter()
                    .dest(getX(), getY() + 1 + yOffset)
                    .zOffset(3)
                    .blit(guiGraphics);
            return;
        }

        ItemStack stack = nextTarget.displayStack().copy();
        if (stack.isEmpty()) {
            stack = new ItemStack(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get());
        }
        stack.setCount(1);
        guiGraphics.renderItem(stack, getX(), getY() + 1 + yOffset, 0, 3);
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (nextTarget == null) {
            return List.of(Component.translatable("gui.wcwt.universal_terminal.empty"));
        }
        List<Component> tooltip = new ArrayList<>(2);
        tooltip.add(nextText(nextTarget));
        if (previousTarget != null) {
            tooltip.add(previousText(previousTarget));
        }
        tooltip.add(Component.translatable("gui.wcwt.cycle_terminal.middle"));
        return tooltip;
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX() - 1, getY(), Icon.TOOLBAR_BUTTON_BACKGROUND.width(),
                Icon.TOOLBAR_BUTTON_BACKGROUND.height());
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Action action = clickAction(button);
        if (action != null && active && visible && clicked(mouseX, mouseY)) {
            PacketDistributor.sendToServer(new SwitchUniversalTerminalPacket(action));
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

    private Optional<Icon> iconFor(WcwtUniversalTerminals.SwitchTarget target) {
        WTDefinition definition = WcwtUniversalTerminals.definitionFor(target.displayStack());
        return definition == null ? Optional.empty() : Optional.of(definition.icon());
    }

    private Component nextText(WcwtUniversalTerminals.SwitchTarget target) {
        WTDefinition definition = WcwtUniversalTerminals.definitionFor(target.displayStack());
        if (definition != null) {
            return TextConstants.cycleNext(definition);
        }
        return Component.translatable("gui.wcwt.cycle_terminal.desc", terminalName(target));
    }

    private Component previousText(WcwtUniversalTerminals.SwitchTarget target) {
        WTDefinition definition = WcwtUniversalTerminals.definitionFor(target.displayStack());
        if (definition != null) {
            return TextConstants.cyclePrevious(definition);
        }
        return Component.translatable("gui.wcwt.cycle_terminal.desc1", terminalName(target));
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
