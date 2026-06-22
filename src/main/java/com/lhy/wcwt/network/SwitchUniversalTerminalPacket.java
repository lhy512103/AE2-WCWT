package com.lhy.wcwt.network;

import appeng.menu.AEBaseMenu;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.lhy.wcwt.menu.locator.WcwtEmbeddedTerminalLocator;
import com.lhy.wcwt.menu.locator.WcwtItemLocator;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SwitchUniversalTerminalPacket(Action action) implements CustomPacketPayload {
    public static final Type<SwitchUniversalTerminalPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "switch_universal_terminal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchUniversalTerminalPacket> STREAM_CODEC =
            StreamCodec.ofMember(SwitchUniversalTerminalPacket::write, SwitchUniversalTerminalPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static SwitchUniversalTerminalPacket read(RegistryFriendlyByteBuf buf) {
        return new SwitchUniversalTerminalPacket(Action.fromId(buf.readVarInt()));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(action.id());
    }

    public static void handle(SwitchUniversalTerminalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof AEBaseMenu menu)) {
                return;
            }
            if (menu.getLocator() instanceof WcwtEmbeddedTerminalLocator embeddedLocator) {
                embeddedLocator.flush(player);
            }
            WcwtItemLocator locator = WcwtUniversalTerminals.currentLocatorOf(menu);
            if (locator == null) {
                return;
            }
            switch (packet.action()) {
                case BASE -> WcwtUniversalTerminals.getBaseSwitchTarget(player, locator)
                        .ifPresent(target -> WcwtUniversalTerminals.switchTo(player, target));
                case PREVIOUS -> WcwtUniversalTerminals.getPreviousSwitchTarget(player, locator)
                        .ifPresent(target -> WcwtUniversalTerminals.switchTo(player, target));
                case NEXT -> WcwtUniversalTerminals.getNextSwitchTarget(player, locator)
                        .ifPresent(target -> WcwtUniversalTerminals.switchTo(player, target));
            }
        });
    }

    public enum Action {
        NEXT(0),
        PREVIOUS(1),
        BASE(2);

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
            return NEXT;
        }
    }
}
