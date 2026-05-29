package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 工具包独立快捷键：即使未打开终端，也能直接打开 WCWT 并切到工具包界面。
 */
public record OpenToolkitHotkeyPacket() implements CustomPacketPayload {
    private static final boolean DEBUG_TOOLKIT = Boolean.getBoolean("wcwt.debug.toolkit");
    public static final Type<OpenToolkitHotkeyPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "open_toolkit_hotkey"));

    public static final StreamCodec<ByteBuf, OpenToolkitHotkeyPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenToolkitHotkeyPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenToolkitHotkeyPacket ignored, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: received hotkey packet for player={}, currentMenu={}",
                        player.getScoreboardName(), player.containerMenu == null ? "<null>"
                                : player.containerMenu.getClass().getName());
            }

            if (openFromInventory(player)) {
                return;
            }
            openFromCurios(player);
        });
    }

    private static boolean openFromInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)) {
                continue;
            }
            WirelessComprehensiveWorkTerminalMenuHost.setPendingExtendedUi(player, IExtendedUIHost.ExtendedUIType.TOOLKIT);
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: trying inventory terminal slot={} for player={}",
                        slot, player.getScoreboardName());
            }
            if (terminalItem.openFromInventory(player, slot)) {
                if (DEBUG_TOOLKIT) {
                    WcwtMod.LOGGER.info("WCWT toolkit debug: inventory terminal open succeeded, slot={}, menu={}",
                            slot, player.containerMenu == null ? "<null>" : player.containerMenu.getClass().getName());
                }
                applyToolkitUiIfOpen(player);
                return true;
            }
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: inventory terminal open failed, slot={}", slot);
            }
        }
        return false;
    }

    private static boolean openFromCurios(ServerPlayer player) {
        return false;
    }

    private static void applyToolkitUiIfOpen(ServerPlayer player) {
        if (player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                && menu.getMenuHost() != null) {
            menu.getMenuHost().setCurrentExtendedUI(IExtendedUIHost.ExtendedUIType.TOOLKIT);
            menu.broadcastChanges();
            if (DEBUG_TOOLKIT) {
                WcwtMod.LOGGER.info("WCWT toolkit debug: applied toolkit UI after open, syncedType={}, hostType={}",
                        menu.getSyncedExtendedUIType(), menu.getMenuHost().getCurrentExtendedUI());
            }
        } else if (DEBUG_TOOLKIT) {
            WcwtMod.LOGGER.info("WCWT toolkit debug: applyToolkitUiIfOpen skipped, currentMenu={}",
                    player.containerMenu == null ? "<null>" : player.containerMenu.getClass().getName());
        }
    }
}

