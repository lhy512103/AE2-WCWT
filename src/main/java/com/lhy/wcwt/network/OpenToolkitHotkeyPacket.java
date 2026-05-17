package com.lhy.wcwt.network;

import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.compat.CuriosBridge;
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
    public static final Type<OpenToolkitHotkeyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "open_toolkit_hotkey"));

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
            var locator = MenuLocators.forInventorySlot(slot);
            if (terminalItem.openFromInventory(player, locator)) {
                applyToolkitUiIfOpen(player);
                return true;
            }
        }
        return false;
    }

    private static boolean openFromCurios(ServerPlayer player) {
        if (!CuriosBridge.isLoaded()) {
            return false;
        }
        var slots = CuriosBridge.getVisibleSlots(player);
        for (var slot : slots) {
            ItemStack stack = slot.handler().getStackInSlot(slot.slotIndex());
            if (!(stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem)) {
                continue;
            }
            var locator = MenuLocators.forCurioSlot(slot.slotIndex());
            if (terminalItem.openFromInventory(player, locator)) {
                applyToolkitUiIfOpen(player);
                return true;
            }
        }
        return false;
    }

    private static void applyToolkitUiIfOpen(ServerPlayer player) {
        if (player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                && menu.getMenuHost() != null) {
            menu.getMenuHost().setCurrentExtendedUI(IExtendedUIHost.ExtendedUIType.TOOLKIT);
            menu.broadcastChanges();
        }
    }
}
