package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StonecuttingRecipeSelectionPacket(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<StonecuttingRecipeSelectionPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "stonecutting_recipe_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StonecuttingRecipeSelectionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodec.of(RegistryFriendlyByteBuf::writeResourceLocation,
                            RegistryFriendlyByteBuf::readResourceLocation),
                    StonecuttingRecipeSelectionPacket::recipeId,
                    StonecuttingRecipeSelectionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StonecuttingRecipeSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.setStonecuttingRecipeId(packet.recipeId());
            }
        });
    }
}

