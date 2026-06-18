package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SplitUniversalTerminalPacket(boolean offhand) implements CustomPacketPayload {
    public static final Type<SplitUniversalTerminalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "split_universal_terminal"));

    public static final StreamCodec<ByteBuf, SplitUniversalTerminalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SplitUniversalTerminalPacket::offhand,
                    SplitUniversalTerminalPacket::new);

    public SplitUniversalTerminalPacket(InteractionHand hand) {
        this(hand == InteractionHand.OFF_HAND);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SplitUniversalTerminalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            InteractionHand hand = packet.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(hand);
            var terminals = WcwtUniversalTerminals.splitInstalledTerminals(stack);
            if (terminals.isEmpty()) {
                return;
            }
            for (ItemStack terminal : terminals) {
                if (!player.getInventory().add(terminal)) {
                    player.drop(terminal, false);
                }
            }
            player.inventoryMenu.broadcastChanges();
        });
    }
}
