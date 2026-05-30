package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
        context.enqueueWork(() -> handleClient(packet));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PatternProviderFocusPacket packet) {
        if (Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen) {
            screen.focusPatternProviderSlot(packet.providerId(), packet.slot());
        }
    }
}

