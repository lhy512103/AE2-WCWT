package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 复制样板网络包
 * 客户端点击「复制样板」按钮 → 服务端执行 copyPattern()
 */
public record CopyPatternPacket() implements CustomPacketPayload {

    public static final Type<CopyPatternPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "copy_pattern"));

    public static final StreamCodec<FriendlyByteBuf, CopyPatternPacket> STREAM_CODEC =
            StreamCodec.unit(new CopyPatternPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CopyPatternPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.copyPattern();
            }
        });
    }
}
