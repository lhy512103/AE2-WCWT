package com.lhy.wcwt.network;

import com.google.common.collect.Maps;
import com.lhy.wcwt.WcwtMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;

public record WcwtRestockAmountsPacket(boolean enabled, HashMap<Holder<Item>, Long> items)
        implements CustomPacketPayload {
    public static final Type<WcwtRestockAmountsPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "restock_amounts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtRestockAmountsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, WcwtRestockAmountsPacket::enabled,
                    ByteBufCodecs.map(Maps::newHashMapWithExpectedSize,
                            ByteBufCodecs.holderRegistry(Registries.ITEM), ByteBufCodecs.VAR_LONG),
                    WcwtRestockAmountsPacket::items,
                    WcwtRestockAmountsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtRestockAmountsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.lhy.wcwt.client.WcwtClientNetworkHandler.handleRestockAmounts(packet)));
    }
}

