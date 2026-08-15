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
                                  long preferredProviderId,
                                  String uploadProviderName,
                                  boolean fallbackToEditSlot,
                                  boolean useEaepUploadScreen)
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
                ByteBufCodecs.VAR_LONG.encode(buf, packet.preferredProviderId());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.uploadProviderName());
                ByteBufCodecs.BOOL.encode(buf, packet.fallbackToEditSlot());
                ByteBufCodecs.BOOL.encode(buf, packet.useEaepUploadScreen());
            }, buf -> new EncodePatternPacket(
                    MODE_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    public EncodePatternPacket(EncodingMode mode) {
        this(mode, false, "", -1, "", false, false);
    }

    public EncodePatternPacket(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                               long preferredProviderId, String uploadProviderName, boolean fallbackToEditSlot) {
        this(mode, uploadEnabled, providerSearchText, preferredProviderId, uploadProviderName,
                fallbackToEditSlot, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EncodePatternPacket packet, IPayloadContext context) {
        if (DEBUG_ENCODE || DEBUG_PATTERN_UPLOAD) {
            WcwtMod.LOGGER.info(
                    "WCWT encode debug: packet received, mode={}, uploadEnabled={}, providerSearchText={}, preferredProviderId={}, uploadProviderName={}, fallbackToEditSlot={}, useEaepUploadScreen={}, player={}",
                    packet.mode(), packet.uploadEnabled(), packet.providerSearchText(), packet.preferredProviderId(),
                    packet.uploadProviderName(), packet.fallbackToEditSlot(), packet.useEaepUploadScreen(), context.player().getName().getString());
        }
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.encodePattern(packet.mode(), packet.uploadEnabled(), packet.providerSearchText(),
                        packet.preferredProviderId(), packet.uploadProviderName(), packet.fallbackToEditSlot(), packet.useEaepUploadScreen());
            } else if (DEBUG_ENCODE) {
                WcwtMod.LOGGER.info("WCWT encode debug: packet ignored, current menu={}",
                        context.player().containerMenu == null ? "null" : context.player().containerMenu.getClass().getName());
            }
        });
    }
}
