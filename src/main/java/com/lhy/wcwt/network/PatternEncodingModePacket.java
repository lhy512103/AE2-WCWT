package com.lhy.wcwt.network;

import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternEncodingModePacket(EncodingMode mode) implements CustomPacketPayload {
    public static final Type<PatternEncodingModePacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_encoding_mode"));

    private static final StreamCodec<ByteBuf, EncodingMode> MODE_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> EncodingMode.values()[id], EncodingMode::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternEncodingModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    MODE_STREAM_CODEC,
                    PatternEncodingModePacket::mode,
                    PatternEncodingModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternEncodingModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.setPatternEncodingMode(packet.mode());
            }
        });
    }
}

