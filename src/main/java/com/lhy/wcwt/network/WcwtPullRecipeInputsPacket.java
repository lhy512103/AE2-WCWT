package com.lhy.wcwt.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.pull.WcwtTerminalPullService;

public record WcwtPullRecipeInputsPacket(boolean maxTransfer, boolean craftMissing,
        List<RequestedIngredient> requestedIngredients, int manualWorkspaceMode)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WcwtPullRecipeInputsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pull_recipe_inputs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WcwtPullRecipeInputsPacket> STREAM_CODEC =
            StreamCodec.ofMember(WcwtPullRecipeInputsPacket::write, WcwtPullRecipeInputsPacket::decode);

    public WcwtPullRecipeInputsPacket(boolean maxTransfer, boolean craftMissing,
            List<RequestedIngredient> requestedIngredients) {
        this(maxTransfer, craftMissing, requestedIngredients,
                WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING.ordinal());
    }

    public WcwtPullRecipeInputsPacket(boolean maxTransfer, boolean craftMissing,
            List<RequestedIngredient> requestedIngredients, int manualWorkspaceMode) {
        this.maxTransfer = maxTransfer;
        this.craftMissing = craftMissing;
        this.requestedIngredients = requestedIngredients.stream().map(RequestedIngredient::copy).toList();
        this.manualWorkspaceMode = WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode
                .fromOrdinal(manualWorkspaceMode)
                .ordinal();
    }

    private static WcwtPullRecipeInputsPacket decode(RegistryFriendlyByteBuf buffer) {
        boolean maxTransfer = buffer.readBoolean();
        boolean craftMissing = buffer.readBoolean();
        int size = buffer.readVarInt();
        List<RequestedIngredient> ingredients = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ingredients.add(RequestedIngredient.decode(buffer));
        }
        int manualWorkspaceMode = buffer.readVarInt();
        return new WcwtPullRecipeInputsPacket(maxTransfer, craftMissing, ingredients, manualWorkspaceMode);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(maxTransfer);
        buffer.writeBoolean(craftMissing);
        buffer.writeVarInt(requestedIngredients.size());
        for (RequestedIngredient ingredient : requestedIngredients) {
            ingredient.write(buffer);
        }
        buffer.writeVarInt(manualWorkspaceMode);
    }

    @Override
    public Type<WcwtPullRecipeInputsPacket> type() {
        return TYPE;
    }

    public static void handle(WcwtPullRecipeInputsPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> WcwtTerminalPullService.handle(ctx.player(), payload));
    }

    public record RequestedIngredient(List<ItemStack> alternatives, int count) {
        public RequestedIngredient(List<ItemStack> alternatives, int count) {
            this.alternatives = alternatives.stream().map(ItemStack::copy).toList();
            this.count = count;
        }

        private static RequestedIngredient decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<ItemStack> alternatives = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                alternatives.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            return new RequestedIngredient(alternatives, buffer.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(alternatives.size());
            for (ItemStack alternative : alternatives) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, alternative);
            }
            buffer.writeVarInt(count);
        }

        public RequestedIngredient copy() {
            return new RequestedIngredient(alternatives, count);
        }
    }
}
