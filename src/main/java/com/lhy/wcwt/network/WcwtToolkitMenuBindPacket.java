package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtToolkitHand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent right before the open-screen packet of a menu opened by the selected extra-bar item, so the
 * client keeps reporting that item as the main hand while the screen builds and stays open.
 */
public record WcwtToolkitMenuBindPacket(int containerId) implements CustomPacketPayload {
    public static final Type<WcwtToolkitMenuBindPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_menu_bind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtToolkitMenuBindPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, WcwtToolkitMenuBindPacket::containerId,
                    WcwtToolkitMenuBindPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtToolkitMenuBindPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null && context.player().level().isClientSide()) {
                WcwtToolkitHand.bindClientMenu(context.player(), packet.containerId());
            }
        });
    }
}
