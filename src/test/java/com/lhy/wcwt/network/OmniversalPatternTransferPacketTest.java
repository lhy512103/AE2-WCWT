package com.lhy.wcwt.network;

import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.compat.UselessModCompat;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OmniversalPatternTransferPacketTest {
    @Test
    void facadeLinksWithoutResolvingOptionalImplementationTypes() throws Exception {
        assertTrue(Class.forName(UselessModCompat.class.getName()).getDeclaredMethods().length > 0);
    }

    @Test
    void preservesDistinctMoldFingerprintsAndSourcesForTheSameRecipeId() {
        for (var fingerprint : List.of("mold-a", "mold-b")) {
            var packet = packet(7, fingerprint, EncodingMode.PROCESSING, false);
            var buf = buffer();
            try {
                OmniversalPatternTransferPacket.STREAM_CODEC.encode(buf, packet);
                assertEquals(packet, OmniversalPatternTransferPacket.STREAM_CODEC.decode(buf));
            } finally {
                buf.release();
            }
        }
    }

    @Test
    void rejectsSelectionsForClosedMenusOrManualCrafting() {
        assertTrue(packet(7, "mold", EncodingMode.PROCESSING, false).matchesEncodingMenu(7));
        assertFalse(packet(7, "mold", EncodingMode.PROCESSING, false).matchesEncodingMenu(8));
        assertFalse(packet(7, "mold", EncodingMode.PROCESSING, true).matchesEncodingMenu(7));
        assertFalse(packet(7, "mold", EncodingMode.CRAFTING, false).matchesEncodingMenu(7));
        assertFalse(packet(7, " ", EncodingMode.PROCESSING, false).matchesEncodingMenu(7));
    }

    @Test
    void rejectsOversizedIdentityBeforeReadingSlots() {
        var buf = buffer();
        try {
            buf.writeVarInt(7);
            buf.writeResourceLocation(ResourceLocation.parse("test:recipe"));
            buf.writeUtf("x".repeat(257));
            assertThrows(DecoderException.class, () -> OmniversalPatternTransferPacket.STREAM_CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void nestedTransferStillRejectsUnboundedSlotCounts() {
        var buf = buffer();
        try {
            buf.writeVarInt(7);
            buf.writeResourceLocation(ResourceLocation.parse("test:recipe"));
            buf.writeUtf("mold");
            buf.writeUtf("mekanism");
            buf.writeBoolean(false);
            buf.writeVarInt(EncodingMode.PROCESSING.ordinal());
            buf.writeVarInt(Integer.MAX_VALUE);
            assertThrows(DecoderException.class, () -> OmniversalPatternTransferPacket.STREAM_CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    private static OmniversalPatternTransferPacket packet(int id, String fingerprint, EncodingMode mode, boolean manual) {
        return new OmniversalPatternTransferPacket(id,
                new UselessModCompat.Selection(ResourceLocation.parse("test:recipe"), fingerprint, "mekanism"),
                new JeiCraftingTransferPacket(List.of(), List.of(), manual, mode));
    }

    @SuppressWarnings("deprecation")
    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
