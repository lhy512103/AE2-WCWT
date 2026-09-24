package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PatternProviderSlotSyncPacket(List<Mapping> mappings) implements CustomPacketPayload {
    public static final Type<PatternProviderSlotSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_provider_slot_sync"));

    public static final StreamCodec<ByteBuf, PatternProviderSlotSyncPacket> STREAM_CODEC = StreamCodec.of(
            PatternProviderSlotSyncPacket::write,
            PatternProviderSlotSyncPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static PatternProviderSlotSyncPacket read(ByteBuf buf) {
        int count = WcwtPacketLimits.readCount(buf,
                WirelessComprehensiveWorkTerminalMenu.PATTERN_PROVIDER_VISIBLE_SLOTS, "pattern provider slot mapping");
        var mappings = new java.util.ArrayList<Mapping>(count);
        for (int i = 0; i < count; i++) {
            mappings.add(new Mapping(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));
        }
        return new PatternProviderSlotSyncPacket(mappings);
    }

    private static void write(ByteBuf buf, PatternProviderSlotSyncPacket packet) {
        ByteBufCodecs.VAR_INT.encode(buf, packet.mappings().size());
        for (var mapping : packet.mappings()) {
            ByteBufCodecs.VAR_INT.encode(buf, mapping.visibleSlot());
            ByteBufCodecs.VAR_LONG.encode(buf, mapping.providerId());
            ByteBufCodecs.VAR_INT.encode(buf, mapping.providerSlot());
        }
    }

    public static void handle(PatternProviderSlotSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player
                    && player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.setPatternProviderSlotSync(packet.mappings());
            }
        });
    }

    public record Mapping(int visibleSlot, long providerId, int providerSlot) {
    }
}
