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
    private static final boolean DEBUG_PATTERN_UPLOAD = Boolean.getBoolean("wcwt.debug.patternUpload");
    public static final CustomPacketPayload.Type<EncodePatternPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "encode_pattern"));
    private static final StreamCodec<ByteBuf, EncodingMode> MODE_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> EncodingMode.values()[id], EncodingMode::ordinal);
    public static final StreamCodec<RegistryFriendlyByteBuf, EncodePatternPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                MODE_STREAM_CODEC.encode(buf, packet.mode());
                ByteBufCodecs.BOOL.encode(buf, packet.uploadEnabled());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.providerSearchText());
                ByteBufCodecs.BOOL.encode(buf, packet.fallbackToEditSlot());
            }, buf -> new EncodePatternPacket(
                    MODE_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    public EncodePatternPacket(EncodingMode mode) {
        this(mode, false, "", false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EncodePatternPacket packet, IPayloadContext context) {
        if (DEBUG_ENCODE || DEBUG_PATTERN_UPLOAD) {
            WcwtMod.LOGGER.info(
                    "WCWT encode debug: packet received, mode={}, uploadEnabled={}, providerSearchText={}, fallbackToEditSlot={}, player={}",
                    packet.mode(), packet.uploadEnabled(), packet.providerSearchText(),
                    packet.fallbackToEditSlot(), context.player().getName().getString());
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
