package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SplitUniversalTerminalPacket(boolean offhand) implements CustomPacketPayload {
    public static final Type<SplitUniversalTerminalPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "split_universal_terminal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SplitUniversalTerminalPacket> STREAM_CODEC =
            StreamCodec.ofMember(SplitUniversalTerminalPacket::write, SplitUniversalTerminalPacket::read);

    public SplitUniversalTerminalPacket(InteractionHand hand) {
        this(hand == InteractionHand.OFF_HAND);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static SplitUniversalTerminalPacket read(RegistryFriendlyByteBuf buf) {
        return new SplitUniversalTerminalPacket(buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(offhand);
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
