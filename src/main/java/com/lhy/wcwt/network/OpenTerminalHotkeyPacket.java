package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.locator.WcwtCurioLocator;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenTerminalHotkeyPacket() implements CustomPacketPayload {
    public static final Type<OpenTerminalHotkeyPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "open_terminal_hotkey"));

    public static final StreamCodec<ByteBuf, OpenTerminalHotkeyPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenTerminalHotkeyPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTerminalHotkeyPacket ignored, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || openFromInventory(player)) {
                return;
            }
            openFromCurios(player);
        });
    }

    private static boolean openFromInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem
                    && terminalItem.openFromInventory(player, slot)) {
                return true;
            }
        }
        return false;
    }

    private static void openFromCurios(ServerPlayer player) {
        var curios = CuriosBridge.getEquippedSlots(player);
        for (var equippedSlot : curios) {
            ItemStack stack = equippedSlot.handler().getStackInSlot(equippedSlot.slotIndex());
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem
                    && terminalItem.openFromCurio(player,
                    new WcwtCurioLocator(equippedSlot.identifier(), equippedSlot.slotIndex()), stack, false)) {
                return;
            }
        }
    }
}
