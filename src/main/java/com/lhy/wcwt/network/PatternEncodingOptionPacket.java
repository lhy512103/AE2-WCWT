package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternEncodingOptionPacket(int action, boolean value) implements CustomPacketPayload {
    public static final int ACTION_CLEAR = 0;
    public static final int ACTION_SUBSTITUTE = 1;
    public static final int ACTION_FLUID_SUBSTITUTE = 2;
    /** 处理样板：是否合并相同输入材料（见 AE2 {@code EncodingHelper#encodeProcessingRecipe}）。 */
    public static final int ACTION_PROCESSING_MERGE_MATERIALS = 3;

    public static final Type<PatternEncodingOptionPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_encoding_option"));

    public static final StreamCodec<ByteBuf, PatternEncodingOptionPacket> STREAM_CODEC =
            StreamCodec.ofMember(PatternEncodingOptionPacket::write, PatternEncodingOptionPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static PatternEncodingOptionPacket read(ByteBuf buf) {
        return new PatternEncodingOptionPacket(readVarInt(buf), buf.readBoolean());
    }

    private void write(ByteBuf buf) {
        writeVarInt(buf, action);
        buf.writeBoolean(value);
    }

    private static int readVarInt(ByteBuf buf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = buf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
        } while ((read & 0b10000000) != 0);
        return result;
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static void handle(PatternEncodingOptionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.handlePatternEncodingOption(packet.action(), packet.value());
            }
        });
    }
}

