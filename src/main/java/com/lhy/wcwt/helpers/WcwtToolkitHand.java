package com.lhy.wcwt.helpers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Decides when {@code Inventory.getSelected} reports the selected toolkit cell instead of the
 * vanilla hotbar slot: only with no container open, since menus address the real hotbar by slot
 * index and AE2 maps the hand stack back to an inventory slot by identity.
 */
public final class WcwtToolkitHand {
    private WcwtToolkitHand() {
    }

    public static boolean isOverrideActive(Player player) {
        if (isUnreadyOrSpectator(player)) {
            return false;
        }
        if (player.containerMenu == null || player.containerMenu != player.inventoryMenu) {
            return false;
        }
        return WcwtToolkitHotbarState.isToolkitSelected(player);
    }

    /**
     * {@code Player.<init>} runs {@code setPos} before the game mode or client connection exist, and
     * other mods can call {@code getSelected} from there.
     */
    private static boolean isUnreadyOrSpectator(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.gameMode == null) {
            return true;
        }
        try {
            return player.isSpectator();
        } catch (NullPointerException ignored) {
            return true;
        }
    }
}
