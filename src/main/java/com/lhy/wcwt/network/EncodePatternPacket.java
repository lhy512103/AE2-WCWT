package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import appeng.parts.encoding.EncodingMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EncodePatternPacket(EncodingMode mode,
                                  boolean uploadEnabled,
                                  String providerSearchText,
                                  boolean fallbackToEditSlot)
        implements CustomPacketPayload {
    private static final boolean DEBUG_ENCODE = Boolean.getBoolean("wcwt.debug.encode");
    public static final CustomPacketPayload.Type<EncodePatternPacket> TYPE =
            new CustomPacketPayload.Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "encode_pattern"));
    private static final StreamCodec<ByteBuf, EncodingMode> MODE_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> EncodingMode.values()[id], EncodingMode::ordinal);
    public static final StreamCodec<RegistryFriendlyByteBuf, EncodePatternPacket> STREAM_CODEC = StreamCodec.composite(
            MODE_STREAM_CODEC,
            EncodePatternPacket::mode,
            ByteBufCodecs.BOOL,
            EncodePatternPacket::uploadEnabled,
            ByteBufCodecs.STRING_UTF8,
            EncodePatternPacket::providerSearchText,
            ByteBufCodecs.BOOL,
            EncodePatternPacket::fallbackToEditSlot,
            EncodePatternPacket::new);

    public EncodePatternPacket(EncodingMode mode) {
        this(mode, false, "", false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EncodePatternPacket packet, IPayloadContext context) {
        if (DEBUG_ENCODE) {
            WcwtMod.LOGGER.info("WCWT encode debug: packet received, mode={}, player={}",
                    packet.mode(), context.player().getName().getString());
        }
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.encodePattern(packet.mode(), packet.uploadEnabled(), packet.providerSearchText(),
                        packet.fallbackToEditSlot());
            } else if (DEBUG_ENCODE) {
                WcwtMod.LOGGER.info("WCWT encode debug: packet ignored, current menu={}",
                        context.player().containerMenu == null ? "null" : context.player().containerMenu.getClass().getName());
            }
        });
    }
}

