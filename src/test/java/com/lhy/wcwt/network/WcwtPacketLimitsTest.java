package com.lhy.wcwt.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.Test;

class WcwtPacketLimitsTest {
    private enum Mode {
        A,
        B
    }

    @Test
    void acceptsCountsWithinBounds() {
        assertEquals(0, WcwtPacketLimits.checkCount(0, 4, "test"));
        assertEquals(4, WcwtPacketLimits.checkCount(4, 4, "test"));
    }

    @Test
    void rejectsNegativeAndOversizedCounts() {
        assertThrows(DecoderException.class, () -> WcwtPacketLimits.checkCount(-1, 4, "test"));
        assertThrows(DecoderException.class, () -> WcwtPacketLimits.checkCount(5, 4, "test"));
    }

    @Test
    void readEnumRejectsUnknownOrdinals() {
        assertEquals(Mode.B, WcwtPacketLimits.readEnum(varInts(1), Mode.values(), "mode"));
        assertThrows(DecoderException.class, () -> WcwtPacketLimits.readEnum(varInts(2), Mode.values(), "mode"));
        assertThrows(DecoderException.class, () -> WcwtPacketLimits.readEnum(varInts(-1), Mode.values(), "mode"));
    }

    @Test
    void slotSyncRoundTripsAndRejectsMoreMappingsThanVisibleSlots() {
        var packet = new PatternProviderSlotSyncPacket(List.of(
                new PatternProviderSlotSyncPacket.Mapping(0, 7L, 3),
                new PatternProviderSlotSyncPacket.Mapping(35, 9L, 0)));
        ByteBuf buf = Unpooled.buffer();
        PatternProviderSlotSyncPacket.STREAM_CODEC.encode(buf, packet);
        assertEquals(packet, PatternProviderSlotSyncPacket.STREAM_CODEC.decode(buf));

        ByteBuf oversized = varInts(WirelessComprehensiveWorkTerminalMenu.PATTERN_PROVIDER_VISIBLE_SLOTS + 1);
        assertThrows(DecoderException.class, () -> PatternProviderSlotSyncPacket.STREAM_CODEC.decode(oversized));
    }

    @Test
    void pullPacketRejectsHugeIngredientCountBeforeAllocating() {
        var buf = registryBuf();
        buf.writeBoolean(false);
        buf.writeBoolean(false);
        buf.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> WcwtPullRecipeInputsPacket.STREAM_CODEC.decode(buf));
    }

    @Test
    void jeiTransferRejectsUnknownModeAndHugeCounts() {
        var badMode = registryBuf();
        badMode.writeBoolean(false);
        badMode.writeVarInt(99);
        assertThrows(DecoderException.class, () -> JeiCraftingTransferPacket.STREAM_CODEC.decode(badMode));

        var hugeInputs = registryBuf();
        hugeInputs.writeBoolean(false);
        hugeInputs.writeVarInt(0);
        hugeInputs.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> JeiCraftingTransferPacket.STREAM_CODEC.decode(hugeInputs));
    }

    @Test
    void resonatingActionRoundTripsAndRejectsHugeArrays() {
        var packet = new ResonatingLightningPatternActionPacket(
                ResonatingLightningPatternActionPacket.Action.CONVERT_TO_OVERLOAD, new int[] {1, 2}, new int[] {3});
        var buf = registryBuf();
        ResonatingLightningPatternActionPacket.STREAM_CODEC.encode(buf, packet);
        var decoded = ResonatingLightningPatternActionPacket.STREAM_CODEC.decode(buf);
        assertEquals(packet.action(), decoded.action());
        assertArrayEquals(packet.inputIdOnlySlots(), decoded.inputIdOnlySlots());
        assertArrayEquals(packet.outputIdOnlySlots(), decoded.outputIdOnlySlots());

        var huge = registryBuf();
        huge.writeVarInt(0);
        huge.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> ResonatingLightningPatternActionPacket.STREAM_CODEC.decode(huge));
    }

    private static ByteBuf varInts(int... values) {
        ByteBuf buf = Unpooled.buffer();
        for (int value : values) {
            ByteBufCodecs.VAR_INT.encode(buf, value);
        }
        return buf;
    }

    private static RegistryFriendlyByteBuf registryBuf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
