package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record ToolkitMemorySyncPacket(int containerId, int firstSlot, List<ItemStack> memories) implements CustomPacketPayload {
    public static final Type<ToolkitMemorySyncPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WcwtMod.MOD_ID, "toolkit_memory_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolkitMemorySyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToolkitMemorySyncPacket::containerId,
            ByteBufCodecs.VAR_INT, ToolkitMemorySyncPacket::firstSlot,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(WcwtServerConfig.MAX_TOOLKIT_SLOTS)),
            ToolkitMemorySyncPacket::memories,
            ToolkitMemorySyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToolkitMemorySyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                    && menu.containerId == packet.containerId()) {
                menu.applyToolkitMemorySnapshot(packet.firstSlot(), packet.memories());
            }
        });
    }
}
