package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 扩展UI状态同步数据包
 */
public record ExtendedUIPacket(IExtendedUIHost.ExtendedUIType uiType) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ExtendedUIPacket> TYPE = 
        new CustomPacketPayload.Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "extended_ui"));
    
    public static final StreamCodec<ByteBuf, ExtendedUIPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(
            ordinal -> IExtendedUIHost.ExtendedUIType.values()[ordinal],
            IExtendedUIHost.ExtendedUIType::ordinal
        ),
        ExtendedUIPacket::uiType,
        ExtendedUIPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(ExtendedUIPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                var host = menu.getMenuHost();
                if (host != null) {
                    host.setCurrentExtendedUI(packet.uiType());
                }
            }
        });
    }
}

