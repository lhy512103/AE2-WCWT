package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TopActionPacket(Action action) implements CustomPacketPayload {
    public static final Type<TopActionPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "top_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TopActionPacket> STREAM_CODEC =
            StreamCodec.ofMember(TopActionPacket::write, TopActionPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static TopActionPacket read(RegistryFriendlyByteBuf buf) {
        return new TopActionPacket(Action.fromId(buf.readVarInt()));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(action.id());
    }

    public static void handle(TopActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.handleTopAction(packet.action());
            }
        });
    }

    public enum Action {
        TOGGLE_PICKUP_MODE(0),
        TOGGLE_INSERT_MODE(1),
        COPY_UP(2),
        COPY_DOWN(3),
        SWITCH_FILTERS(4),
        OPEN_MAGNET_MENU(5),
        OPEN_TRASH_MENU(6),
        CLEAR_MANUAL_WORKSPACE(7);

        private final int id;

        Action(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Action fromId(int id) {
            for (Action action : values()) {
                if (action.id == id) {
                    return action;
                }
            }
            return TOGGLE_PICKUP_MODE;
        }
    }
}

