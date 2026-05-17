package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.EncodingMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record JeiCraftingTransferPacket(List<@Nullable GenericStack> inputs, List<@Nullable GenericStack> outputs,
                                        boolean toCraftingGrid, EncodingMode mode)
        implements CustomPacketPayload {
    public static final Type<JeiCraftingTransferPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "jei_crafting_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiCraftingTransferPacket> STREAM_CODEC =
            StreamCodec.ofMember(JeiCraftingTransferPacket::write, JeiCraftingTransferPacket::decode);

    public JeiCraftingTransferPacket {
        inputs = new ArrayList<>(inputs);
        outputs = new ArrayList<>(outputs);
    }

    private static JeiCraftingTransferPacket decode(RegistryFriendlyByteBuf buffer) {
        boolean toCraftingGrid = buffer.readBoolean();
        EncodingMode mode = EncodingMode.values()[buffer.readVarInt()];
        int inputSize = buffer.readVarInt();
        List<GenericStack> inputs = new ArrayList<>(inputSize);
        for (int i = 0; i < inputSize; i++) {
            inputs.add(readNullableStack(buffer));
        }
        int outputSize = buffer.readVarInt();
        List<GenericStack> outputs = new ArrayList<>(outputSize);
        for (int i = 0; i < outputSize; i++) {
            outputs.add(readNullableStack(buffer));
        }
        return new JeiCraftingTransferPacket(inputs, outputs, toCraftingGrid, mode);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(toCraftingGrid);
        buffer.writeVarInt(mode.ordinal());
        buffer.writeVarInt(inputs.size());
        for (GenericStack input : inputs) {
            writeNullableStack(buffer, input);
        }
        buffer.writeVarInt(outputs.size());
        for (GenericStack output : outputs) {
            writeNullableStack(buffer, output);
        }
    }

    @Nullable
    private static GenericStack readNullableStack(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? GenericStack.STREAM_CODEC.decode(buffer) : null;
    }

    private static void writeNullableStack(RegistryFriendlyByteBuf buffer, @Nullable GenericStack stack) {
        buffer.writeBoolean(stack != null);
        if (stack != null) {
            GenericStack.STREAM_CODEC.encode(buffer, stack);
        }
    }

    @Override
    public Type<JeiCraftingTransferPacket> type() {
        return TYPE;
    }

    public static void handle(JeiCraftingTransferPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.transferJeiRecipe(packet.inputs(), packet.outputs(), packet.toCraftingGrid(), packet.mode());
            }
        });
    }
}
