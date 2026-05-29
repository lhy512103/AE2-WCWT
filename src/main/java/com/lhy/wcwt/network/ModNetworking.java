package com.lhy.wcwt.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            com.lhy.wcwt.util.ResourceLocationCompat.id("wcwt", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetworking() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static <T extends CustomPacketPayload> void registerServerbound(int id, Class<T> payloadClass,
            CustomPacketPayload.Type<T> type,
            net.minecraft.network.codec.StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, net.neoforged.neoforge.network.handling.IPayloadContext> handler) {
        register(id, payloadClass, type.id(), codec, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    public static <T extends CustomPacketPayload> void registerClientbound(int id, Class<T> payloadClass,
            CustomPacketPayload.Type<T> type,
            net.minecraft.network.codec.StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, net.neoforged.neoforge.network.handling.IPayloadContext> handler) {
        register(id, payloadClass, type.id(), codec, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void register(int id, Class<T> payloadClass, ResourceLocation ignoredId,
            net.minecraft.network.codec.StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, net.neoforged.neoforge.network.handling.IPayloadContext> handler,
            NetworkDirection direction) {
        var castCodec = (net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, T>) codec;
        CHANNEL.registerMessage(id, payloadClass, (payload, buffer) -> {
            RegistryFriendlyByteBuf registryBuf = wrap(buffer);
            castCodec.encode(registryBuf, payload);
        }, buffer -> castCodec.decode(wrap(buffer)), (payload, contextSupplier) -> {
            var context = contextSupplier.get();
            handler.accept(payload, new PayloadContextBridge(context));
            context.setPacketHandled(true);
        }, Optional.of(direction));
    }

    private static RegistryFriendlyByteBuf wrap(FriendlyByteBuf buffer) {
        if (buffer instanceof RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            return registryFriendlyByteBuf;
        }
        return new RegistryFriendlyByteBuf(buffer);
    }

    private record PayloadContextBridge(NetworkEvent.Context context)
            implements net.neoforged.neoforge.network.handling.IPayloadContext {
        @Override
        public net.minecraft.world.entity.player.Player player() {
            return context.getSender() != null ? context.getSender() : net.minecraft.client.Minecraft.getInstance().player;
        }

        @Override
        public void enqueueWork(Runnable runnable) {
            context.enqueueWork(runnable);
        }
    }
}
