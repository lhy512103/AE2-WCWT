package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Drops the selected extra-bar cell; the vanilla hotbar drop path is never used for it. */
public record WcwtToolkitHotbarDropPacket(boolean all) implements CustomPacketPayload {
    public static final Type<WcwtToolkitHotbarDropPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_hotbar_drop"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, WcwtToolkitHotbarDropPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, WcwtToolkitHotbarDropPacket::all,
                    WcwtToolkitHotbarDropPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtToolkitHotbarDropPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && WcwtToolkitHotbarState.tryAcquirePacketBudget(player)) {
                WcwtToolkitHotbarState.dropSelected(player, packet.all);
            }
        });
    }
}
