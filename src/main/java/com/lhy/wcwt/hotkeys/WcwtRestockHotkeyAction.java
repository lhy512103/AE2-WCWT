package com.lhy.wcwt.hotkeys;

import appeng.api.features.HotkeyAction;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.world.entity.player.Player;

public final class WcwtRestockHotkeyAction implements HotkeyAction {
    @Override
    public boolean run(Player player) {
        return WcwtWirelessFeatures.toggleRestockHotkey(player);
    }
}
