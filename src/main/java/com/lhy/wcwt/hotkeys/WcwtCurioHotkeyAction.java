package com.lhy.wcwt.hotkeys;

import appeng.api.features.HotkeyAction;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import de.mari_023.ae2wtlib.curio.CurioLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WcwtCurioHotkeyAction implements HotkeyAction {
    @Override
    public boolean run(Player player) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal) {
                if (terminal.openFromCurio(player, new CurioLocator(curio.identifier(), curio.slotIndex()), stack, false)) {
                    return true;
                }
            }
        }
        return false;
    }
}
