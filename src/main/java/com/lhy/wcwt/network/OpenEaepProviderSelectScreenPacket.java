package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record OpenEaepProviderSelectScreenPacket(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<OpenEaepProviderSelectScreenPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "open_eaep_provider_select_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEaepProviderSelectScreenPacket> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(OpenEaepProviderSelectScreenPacket::new,
                    OpenEaepProviderSelectScreenPacket::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEaepProviderSelectScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> handleClient(packet));
    }

    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    private static void handleClient(OpenEaepProviderSelectScreenPacket packet) {
        com.lhy.wcwt.client.WcwtClientNetworkHandler.openEaepProviderSelectScreen(packet);
    }

    public record Entry(long providerId, String providerName, int emptySlots) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Entry::providerId,
                ByteBufCodecs.STRING_UTF8, Entry::providerName,
                ByteBufCodecs.VAR_INT, Entry::emptySlots,
                Entry::new);
    }
}
