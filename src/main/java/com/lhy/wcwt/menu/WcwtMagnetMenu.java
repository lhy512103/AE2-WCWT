package com.lhy.wcwt.menu;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.slot.FakeSlot;
import appeng.util.ConfigMenuInventory;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.init.ModMenus;
import com.lhy.wcwt.network.TopActionPacket;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import net.minecraft.world.entity.player.Inventory;

public class WcwtMagnetMenu extends AEBaseMenu implements ISubMenu {
    private final WirelessComprehensiveWorkTerminalMenuHost host;

    public WcwtMagnetMenu(int id, Inventory playerInventory, WirelessComprehensiveWorkTerminalMenuHost host) {
        super(ModMenus.WCWT_MAGNET_MENU.get(), id, playerInventory, host);
        this.host = host;

        addConfigSlots(host.getMagnetPickupConfig().createMenuWrapper(), AE2wtlibSlotSemantics.PICKUP_CONFIG);
        addConfigSlots(host.getMagnetInsertConfig().createMenuWrapper(), AE2wtlibSlotSemantics.INSERT_CONFIG);
        createPlayerInventorySlots(playerInventory);

        registerClientAction(WirelessComprehensiveWorkTerminalMenu.TOP_ACTION, TopActionPacket.Action.class,
                this::handleTopAction);
    }

    private void addConfigSlots(ConfigMenuInventory inv, appeng.menu.SlotSemantic semantic) {
        for (int i = 0; i < inv.size(); i++) {
            addSlot(new FakeSlot(inv, i), semantic);
        }
    }

    public WirelessComprehensiveWorkTerminalMenuHost getWcwtHost() {
        return host;
    }

    public void handleTopAction(TopActionPacket.Action action) {
        if (isClientSide()) {
            sendClientAction(WirelessComprehensiveWorkTerminalMenu.TOP_ACTION, action);
        }
        switch (action) {
            case TOGGLE_PICKUP_MODE -> host.toggleMagnetPickupMode();
            case TOGGLE_INSERT_MODE -> host.toggleMagnetInsertMode();
            case COPY_UP -> host.copyMagnetInsertToPickup();
            case COPY_DOWN -> host.copyMagnetPickupToInsert();
            case SWITCH_FILTERS -> host.switchMagnetFilters();
            default -> {
            }
        }
    }

    @Override
    public ISubMenuHost getHost() {
        return host;
    }
}
