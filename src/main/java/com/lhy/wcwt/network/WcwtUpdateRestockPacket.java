package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public record WcwtUpdateRestockPacket(int slot, int amount) implements CustomPacketPayload {
    public static final Type<WcwtUpdateRestockPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "update_restock"));

    public static final StreamCodec<ByteBuf, WcwtUpdateRestockPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            WcwtUpdateRestockPacket::slot,
            ByteBufCodecs.INT,
            WcwtUpdateRestockPacket::amount,
            WcwtUpdateRestockPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtUpdateRestockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.lhy.wcwt.client.WcwtClientNetworkHandler.handleUpdateRestock(packet)));
    }
}
