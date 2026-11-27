package com.lhy.wcwt.helpers;

import appeng.menu.AEBaseMenu;
import com.lhy.wcwt.network.WcwtToolkitMenuBindPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Decides when {@code Inventory.getSelected} reports the selected toolkit cell instead of the
 * vanilla hotbar slot: only with no container open, since menus address the real hotbar by slot
 * index and AE2 maps the hand stack back to an inventory slot by identity.
 *
 * <p>One exception: a non-AE2 menu opened by using the selected extra-bar item, such as the Shrink
 * device, keeps the override while it stays open, because its screen and packets read the hand again.
 * Such a menu is refused when it locks the vanilla selected slot or holds the toolkit stack, since it
 * would then guard or edit the wrong stack.
 */
public final class WcwtToolkitHand {
    public static final String MENU_NEEDS_VANILLA_HOTBAR = "message.wcwt.toolkit.menu_needs_vanilla_hotbar";

    private WcwtToolkitHand() {
    }

    public static boolean isOverrideActive(Player player) {
        if (isUnreadyOrSpectator(player)) {
            return false;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu != player.inventoryMenu && !isBoundMenu(player, menu)) {
            return false;
        }
        return WcwtToolkitHotbarState.isToolkitSelected(player);
    }

    /** Server: marks {@code ServerPlayerGameMode.useItem} on the selected cell; returns the previous mark. */
    public static boolean beginToolkitUse(ServerPlayer player, InteractionHand hand) {
        var state = WcwtToolkitHotbarState.state(player);
        boolean previous = state.usingSelectedToolkit;
        state.usingSelectedToolkit = hand == InteractionHand.MAIN_HAND && isOverrideActive(player);
        return previous;
    }

    public static void endToolkitUse(ServerPlayer player, boolean previous) {
        WcwtToolkitHotbarState.state(player).usingSelectedToolkit = previous;
    }

    /**
     * Server: decides on a menu {@code openMenu} just created, before its open-screen packet is sent.
     * AE2 menus already find the cell through the toolkit locator and are left alone; any other menu
     * opened by the marked use is bound to the cell, or refused with a hint.
     */
    @Nullable
    public static AbstractContainerMenu admitMenu(ServerPlayer player, @Nullable AbstractContainerMenu menu) {
        var state = WcwtToolkitHotbarState.state(player);
        if (menu == null || !state.usingSelectedToolkit) {
            return menu;
        }
        state.usingSelectedToolkit = false;
        if (menu instanceof AEBaseMenu) {
            return menu;
        }
        if (touchesSelectedStack(player, menu)) {
            player.displayClientMessage(Component.translatable(MENU_NEEDS_VANILLA_HOTBAR), true);
            return null;
        }
        state.boundMenu = menu;
        PacketDistributor.sendToPlayer(player, new WcwtToolkitMenuBindPacket(menu.containerId));
        return menu;
    }

    /** Client: the server bound the menu it is about to open. */
    public static void bindClientMenu(Player player, int containerId) {
        var state = WcwtToolkitHotbarState.state(player);
        state.boundMenu = null;
        state.pendingBoundMenuId = containerId;
    }

    public static void unbindMenu(Player player, AbstractContainerMenu menu) {
        var state = WcwtToolkitHotbarState.state(player);
        if (state.boundMenu == menu) {
            state.boundMenu = null;
        }
    }

    private static boolean isBoundMenu(Player player, AbstractContainerMenu menu) {
        var state = WcwtToolkitHotbarState.state(player);
        if (state.boundMenu == menu) {
            return true;
        }
        if (state.pendingBoundMenuId < 0) {
            return false;
        }
        // The bind packet arrives right before the open-screen packet, so the next menu shown is that one.
        boolean bound = state.pendingBoundMenuId == menu.containerId;
        state.pendingBoundMenuId = -1;
        if (bound) {
            state.boundMenu = menu;
        }
        return bound;
    }

    private static boolean touchesSelectedStack(Player player, AbstractContainerMenu menu) {
        Inventory inventory = player.getInventory();
        ItemStack selected = WcwtToolkitHotbarState.getSelectedToolkit(player);
        for (Slot slot : menu.slots) {
            // Item-backed menus lock the slot of the stack they edit; here that is a vanilla slot, not the cell.
            if (slot.container == inventory && slot.getContainerSlot() == inventory.selected
                    && !slot.mayPickup(player)) {
                return true;
            }
            if (!selected.isEmpty() && slot.getItem() == selected) {
                return true;
            }
        }
        return false;
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
