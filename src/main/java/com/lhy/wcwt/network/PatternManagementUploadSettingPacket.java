package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternManagementUploadSettingPacket(boolean enabled,
                                                   int displayMode,
                                                   boolean showSlots,
                                                   int searchMode) implements CustomPacketPayload {
    public static final Type<PatternManagementUploadSettingPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_management_upload_setting"));

    public static final StreamCodec<ByteBuf, PatternManagementUploadSettingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PatternManagementUploadSettingPacket::enabled,
            ByteBufCodecs.INT,
            PatternManagementUploadSettingPacket::displayMode,
            ByteBufCodecs.BOOL,
            PatternManagementUploadSettingPacket::showSlots,
            ByteBufCodecs.INT,
            PatternManagementUploadSettingPacket::searchMode,
            PatternManagementUploadSettingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternManagementUploadSettingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.setPatternManagementUploadEnabled(packet.enabled());
                menu.setPatternManagementDisplayMode(packet.displayMode());
                menu.setPatternManagementShowSlots(packet.showSlots());
                menu.setPatternManagementSearchMode(packet.searchMode());
            }
        });
    }
}

