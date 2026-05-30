package com.lhy.wcwt.menu;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.slot.AppEngSlot;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import net.minecraft.world.entity.player.Inventory;

public class WcwtTrashMenu extends AEBaseMenu implements ISubMenu {
    private final WirelessComprehensiveWorkTerminalMenuHost host;

    public WcwtTrashMenu(int id, Inventory playerInventory, WirelessComprehensiveWorkTerminalMenuHost host) {
        super(ModMenus.WCWT_TRASH_MENU.get(), id, playerInventory, host);
        this.host = host;

        var trash = host.getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_TRASH);
        if (trash != null) {
            for (int i = 0; i < trash.size(); i++) {
                addSlot(new AppEngSlot(trash, i), AE2wtlibSlotSemantics.TRASH);
            }
        }
        createPlayerInventorySlots(playerInventory);
    }

    @Override
    public ISubMenuHost getHost() {
        return host;
    }
}
