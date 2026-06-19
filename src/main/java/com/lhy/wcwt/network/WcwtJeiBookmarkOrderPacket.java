package com.lhy.wcwt.network;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.minecraft.network.RegistryFriendlyByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * JEI 书签快捷操作的统一服务端请求。
 *
 * <ul>
 *     <li>{@link Action#OPEN_CRAFT}：鼠标中键「下单」，打开自动合成数量界面。</li>
 *     <li>{@link Action#PULL_OR_CRAFT}：Ctrl+左键「取出/合成」，从网络取出一整组该物品；
 *     若网络中没有库存且可合成，则打开合成数量界面。</li>
 * </ul>
 */
public record WcwtJeiBookmarkOrderPacket(@Nullable GenericStack stack, Action action) implements CustomPacketPayload {
    public enum Action {
        OPEN_CRAFT,
        PULL_OR_CRAFT;

        private static final Action[] VALUES = values();

        static Action fromId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : PULL_OR_CRAFT;
        }
    }

    public WcwtJeiBookmarkOrderPacket(@Nullable GenericStack stack) {
        this(stack, Action.PULL_OR_CRAFT);
    }

    public static final Type<WcwtJeiBookmarkOrderPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "jei_bookmark_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtJeiBookmarkOrderPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        GenericStack.writeBuffer(packet.stack(), buf);
                        buf.writeByte(packet.action().ordinal());
                    },
                    buf -> {
                        GenericStack stack = GenericStack.readBuffer(buf);
                        return new WcwtJeiBookmarkOrderPacket(stack, Action.fromId(buf.readByte()));
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WcwtJeiBookmarkOrderPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (packet.action() == Action.OPEN_CRAFT) {
                    WcwtWirelessFeatures.openJeiBookmarkCrafting(player, packet.stack());
                } else {
                    WcwtWirelessFeatures.orderJeiBookmark(player, packet.stack());
                }
            }
        });
    }
}
