package com.lhy.wcwt.client;

import appeng.api.config.IncludeExclude;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import com.lhy.wcwt.menu.WcwtMagnetMenu;
import com.lhy.wcwt.network.TopActionPacket;
import de.mari_023.ae2wtlib.api.TextConstants;
import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.gui.IconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WcwtMagnetScreen extends AEBaseScreen<WcwtMagnetMenu> {
    public WcwtMagnetScreen(WcwtMagnetMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        AESubScreen.addBackButton(menu, "back", widgets);

        widgets.add("pickup_mode", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.TOGGLE_PICKUP_MODE),
                Icon.YES) {
            @Override
            protected Icon getIcon() {
                return icon(menu.getWcwtHost().getMagnetPickupMode());
            }

            @Override
            public Component getMessage() {
                return TextConstants.getPickupMode(menu.getWcwtHost().getMagnetPickupMode());
            }
        });
        widgets.add("insert_mode", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.TOGGLE_INSERT_MODE),
                Icon.YES) {
            @Override
            protected Icon getIcon() {
                return icon(menu.getWcwtHost().getMagnetInsertMode());
            }

            @Override
            public Component getMessage() {
                return TextConstants.getInsertMode(menu.getWcwtHost().getMagnetInsertMode());
            }
        });
        widgets.add("copy_up", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.COPY_UP), Icon.UP)
                .withTooltip(TextConstants.COPY_PICKUP));
        widgets.add("copy_down", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.COPY_DOWN), Icon.DOWN)
                .withTooltip(TextConstants.COPY_INSERT));
        widgets.add("switch", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.SWITCH_FILTERS), Icon.SWITCH)
                .withTooltip(TextConstants.SWITCH));
    }

    private Icon icon(IncludeExclude includeExclude) {
        return includeExclude == IncludeExclude.WHITELIST ? Icon.YES : Icon.NO;
    }

    protected boolean shouldAddToolbar() {
        return false;
    }
}
