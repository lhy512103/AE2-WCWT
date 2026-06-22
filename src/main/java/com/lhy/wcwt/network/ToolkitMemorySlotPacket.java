package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToolkitMemorySlotPacket(int slotIndex, boolean remember) implements CustomPacketPayload {
    public static final Type<ToolkitMemorySlotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_memory_slot"));

    public static final StreamCodec<ByteBuf, ToolkitMemorySlotPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ToolkitMemorySlotPacket::slotIndex,
            ByteBufCodecs.BOOL,
            ToolkitMemorySlotPacket::remember,
            ToolkitMemorySlotPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToolkitMemorySlotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.setToolkitMemorySlot(packet.slotIndex(), packet.remember());
            }
        });
    }
}
