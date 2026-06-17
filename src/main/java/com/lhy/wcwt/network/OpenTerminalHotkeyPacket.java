package com.lhy.wcwt.network;

import appeng.integration.modules.curios.CuriosIntegration;
import appeng.menu.locator.MenuLocators;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * WCWT 独立打开快捷键：不再挂到 AE2 的无线终端快捷键动作。
 */
public record OpenTerminalHotkeyPacket() implements CustomPacketPayload {
    public static final Type<OpenTerminalHotkeyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "open_terminal_hotkey"));

    public static final StreamCodec<ByteBuf, OpenTerminalHotkeyPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenTerminalHotkeyPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTerminalHotkeyPacket ignored, IPayloadContext context) {
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
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem
                    && terminalItem.openFromInventory(player, MenuLocators.forInventorySlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean openFromCurios(ServerPlayer player) {
        if (!CuriosBridge.isLoaded()) {
            return false;
        }
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap == null) {
            return false;
        }
        for (int slot = 0; slot < cap.getSlots(); slot++) {
            ItemStack stack = cap.getStackInSlot(slot);
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminalItem
                    && terminalItem.openFromInventory(player, MenuLocators.forCurioSlot(slot))) {
                return true;
            }
        }
        return false;
    }
}
