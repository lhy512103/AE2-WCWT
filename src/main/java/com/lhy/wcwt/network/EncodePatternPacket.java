package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import appeng.parts.encoding.EncodingMode;
import io.netty.buffer.ByteBuf;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EncodePatternPacket(EncodingMode mode,
                                  boolean uploadEnabled,
                                  String providerSearchText,
                                  boolean fallbackToEditSlot,
                                  long preferredProviderId,
                                  String preferredProviderName,
                                  boolean useEaepUploadScreen)
        implements CustomPacketPayload {
    private static final boolean DEBUG_ENCODE = Boolean.getBoolean("wcwt.debug.encode");
    public static final CustomPacketPayload.Type<EncodePatternPacket> TYPE =
            new CustomPacketPayload.Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "encode_pattern"));
    private static final StreamCodec<ByteBuf, EncodingMode> MODE_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> EncodingMode.values()[id], EncodingMode::ordinal);
    public static final StreamCodec<RegistryFriendlyByteBuf, EncodePatternPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                MODE_STREAM_CODEC.encode(buf, packet.mode());
                ByteBufCodecs.BOOL.encode(buf, packet.uploadEnabled());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.providerSearchText());
                ByteBufCodecs.BOOL.encode(buf, packet.fallbackToEditSlot());
                ByteBufCodecs.VAR_LONG.encode(buf, packet.preferredProviderId());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.preferredProviderName());
                ByteBufCodecs.BOOL.encode(buf, packet.useEaepUploadScreen());
            },
            buf -> new EncodePatternPacket(
                    MODE_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    public EncodePatternPacket(EncodingMode mode) {
        this(mode, false, "", false, -1L, "", false);
    }

    public EncodePatternPacket(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                               boolean fallbackToEditSlot) {
        this(mode, uploadEnabled, providerSearchText, fallbackToEditSlot, -1L, "", false);
    }

    public EncodePatternPacket(EncodingMode mode, boolean uploadEnabled, String providerSearchText,
                               boolean fallbackToEditSlot, long preferredProviderId,
                               String preferredProviderName) {
        this(mode, uploadEnabled, providerSearchText, fallbackToEditSlot, preferredProviderId,
                preferredProviderName, false);
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
                        packet.fallbackToEditSlot(), packet.preferredProviderId(), packet.preferredProviderName(),
                        packet.useEaepUploadScreen());
            } else if (DEBUG_ENCODE) {
                WcwtMod.LOGGER.info("WCWT encode debug: packet ignored, current menu={}",
                        context.player().containerMenu == null ? "null" : context.player().containerMenu.getClass().getName());
            }
        });
    }
}

