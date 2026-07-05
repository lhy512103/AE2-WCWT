package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 元件工作台操作网络包
 * 对应三个按钮：分区存储、清除、复制模式切换
 */
public record CellWorkbenchActionPacket(Action action) implements CustomPacketPayload {

    public enum Action {
        PARTITION,   // 读取当前 StorageCell 内容填入 config 槽（分区存储）
        CLEAR,       // 清空所有 config 槽
        COPY_MODE,   // 切换 CopyMode（CLEAR_ON_REMOVE ↔ KEEP_ON_REMOVE）
        COMPRESSION_CUTOFF_PREVIOUS, // MEGA Cells 大宗压缩截断：向较小形态循环
        COMPRESSION_CUTOFF_NEXT      // MEGA Cells 大宗压缩截断：向较大/更压缩形态循环
    }

    public static final Type<CellWorkbenchActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "cell_workbench_action"));

    public static final StreamCodec<FriendlyByteBuf, CellWorkbenchActionPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT
                    .map(i -> new CellWorkbenchActionPacket(Action.values()[i]),
                            p -> p.action().ordinal())
                    .cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CellWorkbenchActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                switch (packet.action()) {
                    case PARTITION  -> menu.partitionCell();
                    case CLEAR      -> menu.clearCellConfig();
                    case COPY_MODE  -> menu.toggleCellCopyMode();
                    case COMPRESSION_CUTOFF_PREVIOUS -> menu.cycleCellCompressionCutoff(false);
                    case COMPRESSION_CUTOFF_NEXT -> menu.cycleCellCompressionCutoff(true);
                }
            }
        });
    }
}
