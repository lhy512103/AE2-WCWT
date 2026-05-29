package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternEncodingOptionPacket(int action, boolean value) implements CustomPacketPayload {
    public static final int ACTION_CLEAR = 0;
    public static final int ACTION_SUBSTITUTE = 1;
    public static final int ACTION_FLUID_SUBSTITUTE = 2;
    /** 处理样板：是否合并相同输入材料（见 AE2 {@code EncodingHelper#encodeProcessingRecipe}）。 */
    public static final int ACTION_PROCESSING_MERGE_MATERIALS = 3;

    public static final Type<PatternEncodingOptionPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_encoding_option"));

    public static final StreamCodec<ByteBuf, PatternEncodingOptionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PatternEncodingOptionPacket::action,
            ByteBufCodecs.BOOL,
            PatternEncodingOptionPacket::value,
            PatternEncodingOptionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternEncodingOptionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.handlePatternEncodingOption(packet.action(), packet.value());
            }
        });
    }
}

