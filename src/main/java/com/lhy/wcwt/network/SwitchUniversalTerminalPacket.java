package com.lhy.wcwt.network;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.locator.MenuHostLocator;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.locator.WcwtEmbeddedTerminalLocator;
import com.lhy.wcwt.universal.WcwtUniversalTerminals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SwitchUniversalTerminalPacket(Action action) implements CustomPacketPayload {
    public static final Type<SwitchUniversalTerminalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "switch_universal_terminal"));

    public static final StreamCodec<ByteBuf, SwitchUniversalTerminalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.idMapper(Action::fromId, Action::id),
                    SwitchUniversalTerminalPacket::action,
                    SwitchUniversalTerminalPacket::new);

    public enum Action {
        NEXT(0),
        PREVIOUS(1),
        BASE(2);

        private final int id;

        Action(int id) {
            this.id = id;
        }

        private int id() {
            return id;
        }

        private static Action fromId(int id) {
            for (Action action : values()) {
                if (action.id == id) {
                    return action;
                }
            }
            return NEXT;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
            MenuHostLocator locator = resolveLocator(menu);
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

    private static MenuHostLocator resolveLocator(AEBaseMenu menu) {
        if (menu.getTarget() instanceof ItemMenuHost<?> itemMenuHost) {
            MenuHostLocator locator = itemMenuHost.getLocator();
            if (locator != null) {
                return locator;
            }
        }
        return menu.getLocator();
    }
}
