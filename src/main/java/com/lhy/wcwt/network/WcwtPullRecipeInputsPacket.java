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
        this.requestedIngredients = limitTotalAlternatives(requestedIngredients);
        this.manualWorkspaceMode = WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode
                .fromOrdinal(manualWorkspaceMode)
                .ordinal();
    }

    /**
     * Keeps the alternatives of a whole request within {@link WcwtPacketLimits#MAX_TOTAL_INGREDIENT_ALTERNATIVES}
     * by trimming later ingredients, so the server never has to look at more than that many stacks.
     */
    private static List<RequestedIngredient> limitTotalAlternatives(List<RequestedIngredient> ingredients) {
        int budget = WcwtPacketLimits.MAX_TOTAL_INGREDIENT_ALTERNATIVES;
        List<RequestedIngredient> limited = new ArrayList<>(ingredients.size());
        for (RequestedIngredient ingredient : ingredients) {
            RequestedIngredient copy = ingredient.copy();
            if (copy.alternatives().size() > budget) {
                copy = new RequestedIngredient(copy.alternatives().subList(0, budget), copy.count(), copy.slotIndex());
            }
            budget -= copy.alternatives().size();
            limited.add(copy);
        }
        return List.copyOf(limited);
    }

    private static WcwtPullRecipeInputsPacket decode(RegistryFriendlyByteBuf buffer) {
        boolean maxTransfer = buffer.readBoolean();
        boolean craftMissing = buffer.readBoolean();
        int size = WcwtPacketLimits.readCount(buffer, WcwtPacketLimits.MAX_RECIPE_INGREDIENTS, "requested ingredient");
        List<RequestedIngredient> ingredients = new ArrayList<>(size);
        int totalAlternatives = 0;
        for (int i = 0; i < size; i++) {
            RequestedIngredient ingredient = RequestedIngredient.decode(buffer);
            // A client trims to the limit before sending, so exceeding it on the wire is a malformed packet.
            totalAlternatives = WcwtPacketLimits.checkCount(totalAlternatives + ingredient.alternatives().size(),
                    WcwtPacketLimits.MAX_TOTAL_INGREDIENT_ALTERNATIVES, "ingredient alternative total");
            ingredients.add(ingredient);
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

    public record RequestedIngredient(List<ItemStack> alternatives, int count, int slotIndex) {
        public RequestedIngredient(List<ItemStack> alternatives, int count) {
            this(alternatives, count, -1);
        }

        public RequestedIngredient(List<ItemStack> alternatives, int count, int slotIndex) {
            this.alternatives = alternatives.stream()
                    .limit(WcwtPacketLimits.MAX_INGREDIENT_ALTERNATIVES)
                    .map(ItemStack::copy)
                    .toList();
            this.count = count;
            this.slotIndex = slotIndex;
        }

        private static RequestedIngredient decode(RegistryFriendlyByteBuf buffer) {
            int size = WcwtPacketLimits.readCount(buffer, WcwtPacketLimits.MAX_INGREDIENT_ALTERNATIVES,
                    "ingredient alternative");
            List<ItemStack> alternatives = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                alternatives.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            int count = buffer.readVarInt();
            int slotIndex = buffer.readVarInt();
            return new RequestedIngredient(alternatives, count, slotIndex);
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(alternatives.size());
            for (ItemStack alternative : alternatives) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, alternative);
            }
            buffer.writeVarInt(count);
            buffer.writeVarInt(slotIndex);
        }

        public RequestedIngredient copy() {
            return new RequestedIngredient(alternatives, count, slotIndex);
        }
    }
}
