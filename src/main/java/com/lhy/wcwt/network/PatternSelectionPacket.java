package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 样板选中状态同步数据包
 */
public record PatternSelectionPacket(int slotIndex) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PatternSelectionPacket> TYPE = 
        new CustomPacketPayload.Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_selection"));
    
    public static final StreamCodec<ByteBuf, PatternSelectionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        PatternSelectionPacket::slotIndex,
        PatternSelectionPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PatternSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.selectPatternForAdvancedCoding(packet.slotIndex());
            }
        });
    }
}

