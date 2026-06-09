package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.PatternMultiplierType;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 样板倍增器数据包
 * 客户端 → 服务器：应用倍增/除法/交换操作
 */
public record PatternMultiplierPacket(
    PatternMultiplierType multiplierType,
    boolean applyToEditorProcessing
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PatternMultiplierPacket> TYPE = 
        new CustomPacketPayload.Type<>(
            com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_multiplier")
        );
    
    public static final StreamCodec<ByteBuf, PatternMultiplierPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(
            ordinal -> PatternMultiplierType.values()[ordinal],
            PatternMultiplierType::ordinal
        ),
        PatternMultiplierPacket::multiplierType,
        ByteBufCodecs.BOOL,
        PatternMultiplierPacket::applyToEditorProcessing,
        PatternMultiplierPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PatternMultiplierPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.applyPatternMultiplier(packet.multiplierType(), packet.applyToEditorProcessing());
            }
        });
    }
}

