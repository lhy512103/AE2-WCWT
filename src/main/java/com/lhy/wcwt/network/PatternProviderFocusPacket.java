package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.WcwtClientPacketHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternProviderFocusPacket(long providerId, int slot) implements CustomPacketPayload {
    public static final Type<PatternProviderFocusPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_provider_focus"));

    public static final StreamCodec<ByteBuf, PatternProviderFocusPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            PatternProviderFocusPacket::providerId,
            ByteBufCodecs.VAR_INT,
            PatternProviderFocusPacket::slot,
            PatternProviderFocusPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternProviderFocusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> WcwtClientPacketHandlers.handlePatternProviderFocus(packet));
    }
}
