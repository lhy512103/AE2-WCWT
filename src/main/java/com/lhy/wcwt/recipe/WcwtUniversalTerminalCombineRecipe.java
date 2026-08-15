package com.lhy.wcwt.recipe;

import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.init.ModItems;
import com.lhy.wcwt.init.ModRecipes;
import com.mojang.serialization.MapCodec;
import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class WcwtUniversalTerminalCombineRecipe implements CraftingRecipe {
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Inputs inputs = findInputs(input);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(AE2wtlibAPI.getWUT());
        result.set(WTDefinition.of(inputs.wcwt()).componentType(), com.mojang.datafixers.util.Unit.INSTANCE);
        result.set(inputs.otherDefinition().componentType(), com.mojang.datafixers.util.Unit.INSTANCE);

        var leftovers = WcwtTerminalMergeSupport.mergeInto(
                result,
                java.util.List.of(inputs.wcwt(), inputs.otherTerminal()));
        if (!leftovers.isEmpty()) {
            result.set(ModComponents.CRAFTING_UPGRADE_LEFTOVERS.get(), ItemContainerContents.fromItems(leftovers));
        }
        return result;
    }

    @Nullable
    private static Inputs findInputs(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return null;
        }

        ItemStack wcwt = ItemStack.EMPTY;
        ItemStack other = ItemStack.EMPTY;
        WTDefinition otherDefinition = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get())) {
                if (!wcwt.isEmpty()) {
                    return null;
                }
                wcwt = stack;
                continue;
            }
            if (AE2wtlibAPI.isUniversalTerminal(stack)) {
                return null;
            }
            WTDefinition definition = WTDefinition.ofOrNull(stack);
            if (definition == null || !other.isEmpty()) {
                return null;
            }
            other = stack;
            otherDefinition = definition;
        }

        return wcwt.isEmpty() || other.isEmpty() || otherDefinition == null
                ? null
                : new Inputs(wcwt, other, otherDefinition);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(AE2wtlibAPI.getWUT());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get()));
        ingredients.add(Ingredient.of(WTDefinition.wirelessTerminals().stream()
                .filter(definition -> definition.item() != ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get())
                .map(definition -> new ItemStack(definition.item()))));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WCWT_UNIVERSAL_TERMINAL_COMBINE.get();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    private record Inputs(ItemStack wcwt, ItemStack otherTerminal, WTDefinition otherDefinition) {
    }

    public static final class Serializer implements RecipeSerializer<WcwtUniversalTerminalCombineRecipe> {
        private static final WcwtUniversalTerminalCombineRecipe INSTANCE =
                new WcwtUniversalTerminalCombineRecipe();
        private static final MapCodec<WcwtUniversalTerminalCombineRecipe> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, WcwtUniversalTerminalCombineRecipe> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<WcwtUniversalTerminalCombineRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WcwtUniversalTerminalCombineRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}