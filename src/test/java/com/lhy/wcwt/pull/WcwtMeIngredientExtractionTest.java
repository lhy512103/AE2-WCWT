package com.lhy.wcwt.pull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.util.prioritylist.IPartitionList;
import com.lhy.wcwt.TestBootstrap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WcwtMeIngredientExtractionTest {
    private static final IEnergySource UNLIMITED_POWER = (amount, mode, multiplier) -> amount;

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensure();
    }

    @Test
    void snapshotIndexesKeysByItemAndKeepsAmounts() {
        var counter = new KeyCounter();
        counter.add(plainIron(), 2);
        counter.add(variantIron(7), 9);
        counter.add(gold(), 5);
        counter.add(variantIron(8), 0);

        var available = WcwtMeIngredientExtraction.AvailableItems.of(counter, null);

        assertEquals(2, available.keysFor(Items.IRON_INGOT).size());
        assertEquals(1, available.keysFor(Items.GOLD_INGOT).size());
        assertEquals(0, available.keysFor(Items.DIAMOND).size());
        assertEquals(9, available.amount(variantIron(7)));
        assertEquals(0, available.amount(variantIron(8)));
    }

    @Test
    void snapshotHonoursThePartitionList() {
        var counter = new KeyCounter();
        counter.add(plainIron(), 2);
        counter.add(gold(), 5);
        var builder = IPartitionList.builder();
        builder.add(gold());

        var available = WcwtMeIngredientExtraction.AvailableItems.of(counter, builder.build());

        assertTrue(available.keysFor(Items.IRON_INGOT).isEmpty());
        assertEquals(5, available.amount(gold()));
    }

    @Test
    void extractsTheExactKeyFirstThenOtherVariantsOfTheSameItem() {
        var storage = new MapStorage();
        storage.counter.add(plainIron(), 2);
        storage.counter.add(variantIron(7), 10);
        storage.counter.add(gold(), 5);
        var available = WcwtMeIngredientExtraction.AvailableItems.of(storage.counter, null);

        List<ItemStack> extracted = WcwtMeIngredientExtraction.extractAlternatives(storage, available, UNLIMITED_POWER,
                IActionSource.empty(), null, List.of(new ItemStack(Items.IRON_INGOT)), 5);

        assertEquals(2, extracted.size());
        assertEquals(plainIron(), AEItemKey.of(extracted.get(0)));
        assertEquals(2, extracted.get(0).getCount());
        assertEquals(variantIron(7), AEItemKey.of(extracted.get(1)));
        assertEquals(3, extracted.get(1).getCount());
        assertEquals(0, storage.counter.get(plainIron()));
        assertEquals(7, storage.counter.get(variantIron(7)));
        assertEquals(5, storage.counter.get(gold()));
    }

    @Test
    void componentSpecificAlternativesNeverFallBackToOtherVariants() {
        var storage = new MapStorage();
        storage.counter.add(plainIron(), 4);
        var available = WcwtMeIngredientExtraction.AvailableItems.of(storage.counter, null);

        List<ItemStack> extracted = WcwtMeIngredientExtraction.extractAlternatives(storage, available, UNLIMITED_POWER,
                IActionSource.empty(), null, List.of(variantIron(7).toStack()), 1);

        assertTrue(extracted.isEmpty());
        assertEquals(4, storage.counter.get(plainIron()));
    }

    @Test
    void reservationCandidatesListExactKeyThenSameItemAndSkipOtherItems() {
        Map<AEItemKey, Long> remaining = new LinkedHashMap<>();
        remaining.put(gold(), 5L);
        remaining.put(variantIron(7), 9L);
        remaining.put(plainIron(), 2L);
        var alternatives = List.of(new ItemStack(Items.IRON_INGOT));
        var wide = WcwtMeIngredientExtraction.ingredientFromItemStacks(alternatives);

        List<AEItemKey> candidates = WcwtMeIngredientExtraction.reservationCandidates(remaining, alternatives, wide);

        assertEquals(List.of(plainIron(), variantIron(7)), candidates);
        assertEquals(4, WcwtMeIngredientExtraction.reserveFromCandidates(remaining, candidates, 4));
        assertEquals(0L, remaining.get(plainIron()));
        assertEquals(7L, remaining.get(variantIron(7)));
        assertEquals(5L, remaining.get(gold()));
    }

    @Test
    void reserveAmountReportsHowMuchCouldBeReserved() {
        Map<AEItemKey, Long> remaining = new LinkedHashMap<>();
        remaining.put(plainIron(), 2L);
        var alternatives = List.of(new ItemStack(Items.IRON_INGOT));
        var wide = WcwtMeIngredientExtraction.ingredientFromItemStacks(alternatives);

        assertEquals(2, WcwtMeIngredientExtraction.reserveAmount(remaining, alternatives, wide, 5));
        assertEquals(0, WcwtMeIngredientExtraction.reserveAmount(remaining, alternatives, wide, 1));
    }

    private static AEItemKey plainIron() {
        return AEItemKey.of(new ItemStack(Items.IRON_INGOT));
    }

    private static AEItemKey variantIron(int model) {
        var stack = new ItemStack(Items.IRON_INGOT);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(model));
        return AEItemKey.of(stack);
    }

    private static AEItemKey gold() {
        return AEItemKey.of(new ItemStack(Items.GOLD_INGOT));
    }

    /** A storage backed by a counter, so extraction results and remaining amounts can be asserted. */
    private static final class MapStorage implements MEStorage {
        final KeyCounter counter = new KeyCounter();

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (mode == Actionable.MODULATE) {
                counter.add(what, amount);
            }
            return amount;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            long taken = Math.min(Math.max(0, counter.get(what)), amount);
            if (mode == Actionable.MODULATE && taken > 0) {
                counter.add(what, -taken);
            }
            return taken;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            List<AEKey> keys = new ArrayList<>();
            for (var entry : counter) {
                keys.add(entry.getKey());
            }
            for (AEKey key : keys) {
                out.add(key, counter.get(key));
            }
        }

        @Override
        public Component getDescription() {
            return Component.literal("test storage");
        }
    }
}
