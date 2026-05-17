package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResonatingLightningPatternActionPacket(Action action, int[] inputIdOnlySlots, int[] outputIdOnlySlots)
        implements CustomPacketPayload {

    public enum Action {
        CONVERT_TO_OVERLOAD,
        CONVERT_TO_RESONATING
    }

    public static final Type<ResonatingLightningPatternActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "resonating_lightning_pattern_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResonatingLightningPatternActionPacket> STREAM_CODEC =
            StreamCodec.ofMember(ResonatingLightningPatternActionPacket::write, ResonatingLightningPatternActionPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(action.ordinal());
        writeArray(buf, inputIdOnlySlots);
        writeArray(buf, outputIdOnlySlots);
    }

    private static ResonatingLightningPatternActionPacket read(RegistryFriendlyByteBuf buf) {
        Action action = Action.values()[buf.readVarInt()];
        return new ResonatingLightningPatternActionPacket(action, readArray(buf), readArray(buf));
    }

    private static void writeArray(RegistryFriendlyByteBuf buf, int[] values) {
        buf.writeVarInt(values.length);
        for (int value : values) {
            buf.writeVarInt(value);
        }
    }

    private static int[] readArray(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        int[] values = new int[size];
        for (int i = 0; i < size; i++) {
            values[i] = buf.readVarInt();
        }
        return values;
    }

    public static void handle(ResonatingLightningPatternActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                switch (packet.action()) {
                    case CONVERT_TO_OVERLOAD -> menu.convertSelectedPatternToOverload(
                            packet.inputIdOnlySlots(), packet.outputIdOnlySlots());
                    case CONVERT_TO_RESONATING -> menu.convertSelectedPatternToResonating();
                }
            }
        });
    }
}
