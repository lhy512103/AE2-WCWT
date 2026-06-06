package com.lhy.wcwt.client;

import appeng.api.config.IncludeExclude;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import com.lhy.wcwt.menu.WcwtMagnetMenu;
import com.lhy.wcwt.network.TopActionPacket;
import de.mari_023.ae2wtlib.api.TextConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WcwtMagnetScreen extends AEBaseScreen<WcwtMagnetMenu> {
    public WcwtMagnetScreen(WcwtMagnetMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        AESubScreen.addBackButton(menu, "back", widgets);

        widgets.add("pickup_mode", new IconButton(
                button -> menu.handleTopAction(TopActionPacket.Action.TOGGLE_PICKUP_MODE)) {
            @Override
            protected Icon getIcon() {
                return icon(menu.getWcwtHost().getMagnetPickupMode());
            }

            @Override
            public Component getMessage() {
                return TextConstants.getPickupMode(menu.getWcwtHost().getMagnetPickupMode());
            }
        });
        widgets.add("insert_mode", new IconButton(
                button -> menu.handleTopAction(TopActionPacket.Action.TOGGLE_INSERT_MODE)) {
            @Override
            protected Icon getIcon() {
                return icon(menu.getWcwtHost().getMagnetInsertMode());
            }

            @Override
            public Component getMessage() {
                return TextConstants.getInsertMode(menu.getWcwtHost().getMagnetInsertMode());
            }
        });
        widgets.add("copy_up", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.COPY_UP)) {
            @Override
            protected Icon getIcon() {
                return Icon.ARROW_UP;
            }

            @Override
            public Component getMessage() {
                return TextConstants.COPY_PICKUP;
            }
        });
        widgets.add("copy_down", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.COPY_DOWN)) {
            @Override
            protected Icon getIcon() {
                return Icon.ARROW_DOWN;
            }

            @Override
            public Component getMessage() {
                return TextConstants.COPY_INSERT;
            }
        });
        widgets.add("switch", new IconButton(button -> menu.handleTopAction(TopActionPacket.Action.SWITCH_FILTERS)) {
            @Override
            protected Icon getIcon() {
                return Icon.SCHEDULING_ROUND_ROBIN;
            }

            @Override
            public Component getMessage() {
                return TextConstants.SWITCH;
            }
        });
    }

    private Icon icon(IncludeExclude includeExclude) {
        return includeExclude == IncludeExclude.WHITELIST ? Icon.WHITELIST : Icon.BLACKLIST;
    }

    protected boolean shouldAddToolbar() {
        return false;
    }
}
