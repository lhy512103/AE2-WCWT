package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

public record WcwtToolkitHotbarSelectionPacket(int bar, int slot) implements CustomPacketPayload {
    public static final Type<WcwtToolkitHotbarSelectionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_hotbar_selection"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, WcwtToolkitHotbarSelectionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, WcwtToolkitHotbarSelectionPacket::bar,
                    ByteBufCodecs.VAR_INT, WcwtToolkitHotbarSelectionPacket::slot,
                    WcwtToolkitHotbarSelectionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtToolkitHotbarSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            WcwtToolkitHotbarState.Bar bar = packet.bar >= 0 && packet.bar < WcwtToolkitHotbarState.Bar.values().length
                    ? WcwtToolkitHotbarState.Bar.values()[packet.bar] : WcwtToolkitHotbarState.Bar.CENTER;
            if (packet.slot < 0 || packet.slot >= 9) {
                return;
            }
            if (bar == WcwtToolkitHotbarState.Bar.CENTER) {
                player.getInventory().selected = packet.slot;
            }
            WcwtToolkitHotbarState.setSelection(player, bar, packet.slot);
        });
    }
}
