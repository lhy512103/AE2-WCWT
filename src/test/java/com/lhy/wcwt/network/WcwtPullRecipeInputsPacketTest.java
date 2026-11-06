package com.lhy.wcwt.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lhy.wcwt.TestBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.Collections;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WcwtPullRecipeInputsPacketTest {
    private static final int PER_INGREDIENT = WcwtPacketLimits.MAX_INGREDIENT_ALTERNATIVES;
    private static final int FULL_INGREDIENTS = WcwtPacketLimits.MAX_TOTAL_INGREDIENT_ALTERNATIVES / PER_INGREDIENT;

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensure();
    }

    @Test
    void decodeAcceptsExactlyTheRequestTotal() {
        var buf = encodedRequest(FULL_INGREDIENTS);
        try {
            var packet = WcwtPullRecipeInputsPacket.STREAM_CODEC.decode(buf);
            assertEquals(FULL_INGREDIENTS, packet.requestedIngredients().size());
            assertEquals(PER_INGREDIENT, packet.requestedIngredients().get(0).alternatives().size());
        } finally {
            buf.release();
        }
    }

    @Test
    void decodeRejectsAlternativesBeyondTheRequestTotal() {
        var buf = encodedRequest(FULL_INGREDIENTS + 1);
        try {
            assertThrows(DecoderException.class, () -> WcwtPullRecipeInputsPacket.STREAM_CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void constructorTrimsLaterIngredientsToTheRequestTotal() {
        var alternatives = Collections.nCopies(PER_INGREDIENT, new ItemStack(Items.IRON_INGOT));
        var ingredient = new WcwtPullRecipeInputsPacket.RequestedIngredient(alternatives, 1);

        var packet = new WcwtPullRecipeInputsPacket(false, false, Collections.nCopies(FULL_INGREDIENTS + 1, ingredient));

        assertEquals(PER_INGREDIENT, packet.requestedIngredients().get(FULL_INGREDIENTS - 1).alternatives().size());
        assertEquals(0, packet.requestedIngredients().get(FULL_INGREDIENTS).alternatives().size());
    }

    /** A request of {@code ingredients} ingredients, each with the per-ingredient maximum of empty alternatives. */
    private static RegistryFriendlyByteBuf encodedRequest(int ingredients) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.OTHER);
        buf.writeBoolean(false);
        buf.writeBoolean(false);
        buf.writeVarInt(ingredients);
        for (int i = 0; i < ingredients; i++) {
            buf.writeVarInt(PER_INGREDIENT);
            for (int j = 0; j < PER_INGREDIENT; j++) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ItemStack.EMPTY);
            }
            buf.writeVarInt(1);
            buf.writeVarInt(-1);
        }
        buf.writeVarInt(0);
        return buf;
    }
}
