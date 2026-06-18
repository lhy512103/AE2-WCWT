package com.lhy.wcwt.compat.eaep;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageHelper;
import appeng.core.localization.PlayerMessages;
import appeng.me.helpers.PlayerSource;
import appeng.menu.me.crafting.CraftAmountMenu;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.network.crafting.SetSearchTextS2CPacket;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtCurioLocator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.Nullable;

public final class WcwtEaepJeiCraftingCompat {
    private WcwtEaepJeiCraftingCompat() {
    }

    public static boolean handleOpenCraftFromJei(ServerPlayer player, GenericStack stack) {
        if (player == null || stack == null || stack.what() == null) {
            return false;
        }

        WcwtCurioTarget target = findWcwtCurioTarget(player);
        if (target == null) {
            return false;
        }

        GridAccess access = resolveGrid(player, target);
        if (access.grid() == null) {
            displayLinkFailure(player, access.statusDescription());
            return true;
        }

        AEKey what = stack.what();
        var crafting = access.grid().getCraftingService();
        if (crafting == null || !crafting.isCraftable(what)) {
            sendSearchText(player, what);
            return true;
        }

        CraftAmountMenu.open(player, target.locator(), what, 1);
        return true;
    }

    public static boolean handlePullFromJeiOrCraft(ServerPlayer player, GenericStack stack) {
        if (player == null || stack == null || !(stack.what() instanceof AEItemKey itemKey)) {
            return false;
        }

        WcwtCurioTarget target = findWcwtCurioTarget(player);
        if (target == null) {
            return false;
        }

        GridAccess access = resolveGrid(player, target);
        if (access.grid() == null) {
            displayLinkFailure(player, access.statusDescription());
            return true;
        }

        Inventory inventory = player.getInventory();
        int freeSlot = inventory.getFreeSlot();
        if (freeSlot < 0) {
            return true;
        }

        int amount = itemKey.toStack(1).getMaxStackSize();
        var energy = access.grid().getEnergyService();
        var storage = access.grid().getStorageService().getInventory();
        var playerSource = new PlayerSource(player);
        long extracted = StorageHelper.poweredExtraction(energy, storage, itemKey, amount, playerSource);
        if (extracted > 0) {
            inventory.setItem(freeSlot, itemKey.toStack((int) Math.min(extracted, Integer.MAX_VALUE)));
            if (target.stack().getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal) {
                terminal.usePower(player, Math.max(0.5, extracted * 0.05), target.stack());
            }
            target.commit();
            player.containerMenu.broadcastChanges();
            return true;
        }

        var crafting = access.grid().getCraftingService();
        if (crafting != null && crafting.isCraftable(stack.what())) {
            CraftAmountMenu.open(player, target.locator(), stack.what(), 1);
        }
        return true;
    }

    @Nullable
    private static WcwtCurioTarget findWcwtCurioTarget(ServerPlayer player) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem) {
                return new WcwtCurioTarget(
                        curio.identifier(),
                        curio.slotIndex(),
                        curio.handler(),
                        stack,
                        new WcwtCurioLocator(curio.identifier(), curio.slotIndex()));
            }
        }
        return null;
    }

    private static GridAccess resolveGrid(ServerPlayer player, WcwtCurioTarget target) {
        if (!(target.stack().getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal)) {
            return GridAccess.missing(null);
        }

        if (!terminal.hasPower(player, 0.5, target.stack())) {
            return GridAccess.missing(PlayerMessages.DeviceNotPowered.text());
        }

        IGrid linkedGrid = terminal.getLinkedGrid(target.stack(), player.level(), null);
        if (linkedGrid != null) {
            return GridAccess.available(linkedGrid);
        }

        var host = new WirelessComprehensiveWorkTerminalMenuHost(player, null, target.stack(), (p, sm) -> {
        });
        boolean canOpen = host.canOpenFromAnyLink();
        IGridNode node = host.getActionableNode();
        if (node != null && node.getGrid() != null) {
            return GridAccess.available(node.getGrid());
        }
        return GridAccess.missing(canOpen ? host.getCurrentLinkStatusDescription() : host.getCurrentLinkStatusDescription());
    }

    private static void displayLinkFailure(ServerPlayer player, @Nullable Component statusDescription) {
        if (statusDescription != null) {
            player.displayClientMessage(statusDescription, true);
        }
    }

    private static void sendSearchText(ServerPlayer player, AEKey what) {
        String searchText = what.getDisplayName().getString();
        if (searchText == null || searchText.isEmpty()) {
            return;
        }
        ModNetwork.CHANNEL.sendTo(
                new SetSearchTextS2CPacket(searchText),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }

    private record WcwtCurioTarget(String identifier, int slotIndex,
                                   net.minecraftforge.items.IItemHandlerModifiable handler,
                                   ItemStack stack, WcwtCurioLocator locator) {
        private void commit() {
            handler.setStackInSlot(slotIndex, stack);
        }
    }

    private record GridAccess(@Nullable IGrid grid, @Nullable Component statusDescription) {
        private static GridAccess available(IGrid grid) {
            return new GridAccess(grid, null);
        }

        private static GridAccess missing(@Nullable Component statusDescription) {
            return new GridAccess(null, statusDescription);
        }
    }
}
