package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WcwtPickBlockPacket(ItemStack itemStack) implements CustomPacketPayload {
    public static final Type<WcwtPickBlockPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pick_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtPickBlockPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeItem(packet.itemStack()),
                    buf -> new WcwtPickBlockPacket(buf.readItem()));

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

