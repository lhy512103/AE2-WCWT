package net.neoforged.neoforge.network;

import com.lhy.wcwt.network.ModNetworking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PacketDistributor {
    private PacketDistributor() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ModNetworking.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ModNetworking.sendToPlayer(player, payload);
    }
}
