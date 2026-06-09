package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import io.netty.buffer.ByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public record PatternProviderFocusPacket(long providerId, int slot) implements CustomPacketPayload {
    public static final Type<PatternProviderFocusPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_provider_focus"));

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
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.lhy.wcwt.client.WcwtClientNetworkHandler.handlePatternProviderFocus(packet)));
    }
}

