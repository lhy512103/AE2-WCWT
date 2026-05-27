package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.gui.widgets.PatternMultiplierButton;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 样板倍增器数据包
 * 客户端 → 服务器：应用倍增/除法/交换操作
 */
public record PatternMultiplierPacket(
    PatternMultiplierButton.MultiplierType multiplierType,
    boolean applyToEditorProcessing
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<PatternMultiplierPacket> TYPE = 
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_multiplier")
        );
    
    public static final StreamCodec<ByteBuf, PatternMultiplierPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(
            ordinal -> PatternMultiplierButton.MultiplierType.values()[ordinal],
            PatternMultiplierButton.MultiplierType::ordinal
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
