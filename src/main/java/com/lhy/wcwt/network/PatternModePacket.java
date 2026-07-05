package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternModePacket(int mode, boolean value) implements CustomPacketPayload {
    public static final int MODE_PATTERN_ITEM_SUBSTITUTIONS = 0;
    public static final int MODE_PATTERN_FLUID_SUBSTITUTIONS = 1;
    public static final int MODE_MANUAL_ITEM_SUBSTITUTION = 2;
    public static final int MODE_MANUAL_FLUID_SUBSTITUTION = 3;

    public static final CustomPacketPayload.Type<PatternModePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_mode"));

    public static final StreamCodec<ByteBuf, PatternModePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PatternModePacket::mode,
            ByteBufCodecs.BOOL,
            PatternModePacket::value,
            PatternModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.handlePatternMode(packet.mode(), packet.value());
            }
        });
    }
}
