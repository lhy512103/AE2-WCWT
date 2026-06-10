package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public record OpenEaepProviderSelectScreenPacket(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<OpenEaepProviderSelectScreenPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID,
                    "open_eaep_provider_select_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEaepProviderSelectScreenPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.entries().size());
                        for (var entry : packet.entries()) {
                            Entry.STREAM_CODEC.encode(buf, entry);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        var entries = new ArrayList<Entry>(size);
                        for (int i = 0; i < size; i++) {
                            entries.add(Entry.STREAM_CODEC.decode(buf));
                        }
                        return new OpenEaepProviderSelectScreenPacket(entries);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEaepProviderSelectScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.lhy.wcwt.client.WcwtClientNetworkHandler.openEaepProviderSelectScreen(packet)));
    }

    public record Entry(long providerId, String providerName, int emptySlots) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buf, entry) -> {
                    buf.writeVarLong(entry.providerId());
                    buf.writeUtf(entry.providerName());
                    buf.writeVarInt(entry.emptySlots());
                },
                buf -> new Entry(buf.readVarLong(), buf.readUtf(), buf.readVarInt()));
    }
}
