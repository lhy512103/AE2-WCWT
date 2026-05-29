package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WcwtPickBlockPacket(ItemStack itemStack) implements CustomPacketPayload {
    public static final Type<WcwtPickBlockPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pick_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtPickBlockPacket> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(WcwtPickBlockPacket::new, WcwtPickBlockPacket::itemStack);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtPickBlockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                WcwtWirelessFeatures.pickBlock(player, packet.itemStack());
            }
        });
    }
}

