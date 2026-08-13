package com.lhy.wcwt.migration;

import appeng.util.inv.AppEngInternalInventory;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.init.ModComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WcwtLegacyUniversalTerminalMigration {
    private static final int LEGACY_TERMINAL_SLOTS = 16;
    private static final String TERMINALS_TAG = "universal_terminals";
    private static final String CURRENT_TERMINAL_TAG = "current_universal_terminal";

    private WcwtLegacyUniversalTerminalMigration() {
    }

    public static void migrate(Player player, ItemStack wcwt) {
        CompoundTag stackTag = wcwt.getTag();
        if (stackTag == null || !stackTag.contains(ModComponents.ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag root = stackTag.getCompound(ModComponents.ROOT_TAG);
        if (!root.contains(TERMINALS_TAG)) {
            return;
        }

        var terminals = new AppEngInternalInventory(LEGACY_TERMINAL_SLOTS);
        terminals.readFromNBT(root, TERMINALS_TAG);
        root.remove(TERMINALS_TAG);
        root.remove(CURRENT_TERMINAL_TAG);
        stackTag.put(ModComponents.ROOT_TAG, root);

        int restored = 0;
        for (int slot = 0; slot < terminals.size(); slot++) {
            ItemStack terminal = terminals.getStackInSlot(slot);
            if (terminal.isEmpty()) {
                continue;
            }
            ItemStack restoredTerminal = terminal.copy();
            restoredTerminal.setCount(1);
            if (!player.getInventory().add(restoredTerminal)) {
                player.drop(restoredTerminal, false);
            }
            restored++;
        }
        if (restored > 0) {
            WcwtMod.LOGGER.info("Restored {} legacy embedded wireless terminal(s) for player {}",
                    restored, player.getScoreboardName());
        }
    }
}