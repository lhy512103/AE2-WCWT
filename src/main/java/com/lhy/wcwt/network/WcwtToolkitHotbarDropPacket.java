package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Drops the selected extra-bar cell; the vanilla hotbar drop path is never used for it. */
public record WcwtToolkitHotbarDropPacket(boolean all) implements CustomPacketPayload {
    public static final Type<WcwtToolkitHotbarDropPacket> TYPE = new Type<>(
            com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "toolkit_hotbar_drop"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtToolkitHotbarDropPacket> STREAM_CODEC =
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
