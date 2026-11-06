package com.lhy.wcwt.pull;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.EncodingMode;
import com.lhy.wcwt.TestBootstrap;
import com.lhy.wcwt.network.JeiCraftingTransferPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class WcwtComponentIngredientEncodingTest {
    @BeforeAll
    static void bootstrap() {
        // Shared with the other bootstrapped tests: registries can only be set up once per JVM.
        TestBootstrap.ensure();
    }

    @Test
    void usesFullCraftableKeyWhenPartialExampleOmitsRarity() {
        Ingredient ingredient = partial(12);
        ItemStack produced = core(12, Rarity.UNCOMMON);
        var context = context(Map.of(key(produced), 10), Map.of(), Map.of());
        ItemStack selected = resolve(context, ingredient, failIfScanned());

        assertTrue(ingredient.test(produced));
        assertFalse(ItemStack.isSameItemSameComponents(ingredient.getItems()[0], produced));
        assertTrue(ItemStack.isSameItemSameComponents(produced, selected));
        assertEquals(Rarity.UNCOMMON, selected.get(DataComponents.RARITY));
    }

    @Test
    void neverSelectsAnotherStageOrTypeEvenWithHigherNetworkPriority() {
        ItemStack expected = core(12, Rarity.UNCOMMON);
        var context = context(Map.of(key(expected), 1, key(core(11, Rarity.EPIC)), 200,
                key(core(22, Rarity.RARE)), 300), Map.of(), Map.of());
        assertTrue(ItemStack.isSameItemSameComponents(expected, resolve(context, partial(12), failIfScanned())));
    }

    @Test
    void explicitFavoriteWinsOverNetworkAndBookmarkWithoutDroppingItsComponents() {
        ItemStack favorite = core(12, Rarity.EPIC);
        var context = context(Map.of(key(core(12, Rarity.UNCOMMON)), 100),
                Map.of(key(core(12, Rarity.RARE)), 0), Map.of(key(favorite), 2));
        assertEquals(key(favorite), key(resolve(context, partial(12), failIfScanned())));
    }

    @Test
    void explicitBookmarkWinsButWrongStageFavoritesAreRejected() {
        ItemStack bookmark = core(12, Rarity.RARE);
        var context = context(Map.of(key(core(12, Rarity.UNCOMMON)), 100), Map.of(key(bookmark), 0),
                Map.of(key(core(11, Rarity.EPIC)), 0));
        assertEquals(key(bookmark), key(resolve(context, partial(12), failIfScanned())));
    }

    @Test
    void usesActualPreviousStageResultWhenNothingIsStoredOrCraftable() {
        ItemStack produced = core(12, Rarity.UNCOMMON);
        ItemStack selected = resolve(WcwtIngredientPriorities.PriorityContext.EMPTY, partial(12),
                () -> List.of(core(11, Rarity.EPIC), produced));
        assertEquals(key(produced), key(selected));
        assertNotSame(produced, selected);
    }

    @Test
    void fallsBackToPredicateExampleWhenNoValidProductionResultExists() {
        Ingredient ingredient = partial(12);
        ItemStack selected = resolve(WcwtIngredientPriorities.PriorityContext.EMPTY, ingredient,
                () -> List.of(core(11, Rarity.EPIC)));
        assertEquals(key(ingredient.getItems()[0]), key(selected));
    }

    @Test
    void strictComponentAndDisplayOnlySelectionsKeepTheirExactKeys() {
        ItemStack displayed = core(12, Rarity.COMMON);
        var context = context(Map.of(key(core(12, Rarity.UNCOMMON)), 100), Map.of(), Map.of());
        Ingredient strict = DataComponentIngredient.of(true, displayed);
        assertFalse(WcwtComponentIngredientEncoding.isPartialComponentIngredient(strict));
        assertEquals(key(displayed), key(resolve(context, strict, failIfScanned())));
        Ingredient displayOnly = Ingredient.of(displayed);
        assertFalse(WcwtComponentIngredientEncoding.isPartialComponentIngredient(displayOnly));
        assertEquals(key(displayed), key(resolve(context, displayOnly, failIfScanned())));
    }

    @Test
    void selectedFullKeySurvivesRecipeTransferPacketRoundTrip() {
        ItemStack produced = core(12, Rarity.UNCOMMON);
        var context = context(Map.of(key(produced), 1), Map.of(), Map.of());
        ItemStack selected = resolve(context, partial(12), failIfScanned());
        var packet = new JeiCraftingTransferPacket(List.of(GenericStack.fromItemStack(selected)), List.of(),
                false, EncodingMode.CRAFTING);
        // Normal NeoForge loading marks these registries as synced before sending item packets.
        // Use local synced copies rather than changing the vanilla bootstrap registries globally.
        var packetRegistries = new RegistryAccess.ImmutableRegistryAccess(List.of(
                syncedRegistry(BuiltInRegistries.ITEM, List.of(Items.DIAMOND)),
                syncedRegistry(BuiltInRegistries.DATA_COMPONENT_TYPE,
                        List.of(DataComponents.CUSTOM_MODEL_DATA, DataComponents.RARITY))));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), packetRegistries);
        try {
            JeiCraftingTransferPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = JeiCraftingTransferPacket.STREAM_CODEC.decode(buffer);
            assertEquals(key(produced), decoded.inputs().getFirst().what());
            assertEquals(1, decoded.inputs().getFirst().amount());
            assertEquals(EncodingMode.CRAFTING, decoded.mode());
        } finally {
            buffer.release();
        }
    }

    private static <T> Registry<T> syncedRegistry(Registry<T> source, List<T> values) {
        Registry<T> registry = new RegistryBuilder<>(source.key()).sync(true).disableRegistrationCheck().create();
        for (T value : values) {
            Registry.register(registry, source.getKey(value), value);
        }
        return registry.freeze();
    }

    private static ItemStack resolve(WcwtIngredientPriorities.PriorityContext context, Ingredient ingredient,
                                     Supplier<List<ItemStack>> results) {
        return WcwtComponentIngredientEncoding.resolve(context, ingredient,
                List.of(ingredient.getItems()[0]), results);
    }

    private static Ingredient partial(int stageAndType) {
        return DataComponentIngredient.of(false, DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(stageAndType), Items.DIAMOND);
    }

    private static ItemStack core(int stageAndType, Rarity rarity) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(stageAndType));
        stack.set(DataComponents.RARITY, rarity);
        return stack;
    }

    private static AEItemKey key(ItemStack stack) {
        return AEItemKey.of(stack);
    }

    private static WcwtIngredientPriorities.PriorityContext context(Map<AEKey, Integer> network,
            Map<AEKey, Integer> bookmarks, Map<AEKey, Integer> favorites) {
        return new WcwtIngredientPriorities.PriorityContext(network, bookmarks, favorites);
    }

    private static Supplier<List<ItemStack>> failIfScanned() {
        return () -> {
            fail("Existing or explicit candidates must avoid scanning recipes");
            return List.of();
        };
    }
}
