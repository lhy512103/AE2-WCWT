package com.lhy.wcwt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import com.lhy.wcwt.menu.WcwtTrashMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WcwtTrashScreen extends AEBaseScreen<WcwtTrashMenu> {
    public WcwtTrashScreen(WcwtTrashMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        AESubScreen.addBackButton(menu, "back", widgets);
    }

    protected boolean shouldAddToolbar() {
        return false;
    }
}
