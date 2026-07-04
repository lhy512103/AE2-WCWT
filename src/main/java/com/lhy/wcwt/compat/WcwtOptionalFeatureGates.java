package com.lhy.wcwt.compat;

import java.util.ArrayList;
import java.util.List;

import com.lhy.wcwt.init.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class WcwtOptionalFeatureGates {
    private static final String CURIOS = "curios";
    private static final String COSMETIC_ARMOR_REWORKED = "cosmeticarmorreworked";
    private static final String AE2_CRYSTAL_SCIENCE = "ae2cs";
    private static final String AE2_LIGHTNING_TECH = "ae2lt";

    private WcwtOptionalFeatureGates() {
    }

    public static boolean isCuriosAvailable() {
        return ModList.get().isLoaded(CURIOS);
    }

    public static boolean isCosmeticArmorAvailable() {
        return ModList.get().isLoaded(COSMETIC_ARMOR_REWORKED);
    }

    public static boolean isResonatingLightningPatternCodingAvailable() {
        return ModList.get().isLoaded(AE2_CRYSTAL_SCIENCE) || ModList.get().isLoaded(AE2_LIGHTNING_TECH);
    }

    public static boolean isUpgradeCardVisible(Item item) {
        if (item == ModItems.CURIOS_CARD.get()) {
            return isCuriosAvailable();
        }
        if (item == ModItems.COSMETIC_ARMOR_CARD.get()) {
            return isCosmeticArmorAvailable();
        }
        if (item == ModItems.RESONATING_LIGHTNING_PATTERN_CODING_CARD.get()) {
            return isResonatingLightningPatternCodingAvailable();
        }
        return true;
    }

    public static List<ItemStack> hiddenUpgradeCardStacks() {
        List<ItemStack> hidden = new ArrayList<>();
        addHiddenIfUnavailable(hidden, ModItems.CURIOS_CARD.get());
        addHiddenIfUnavailable(hidden, ModItems.COSMETIC_ARMOR_CARD.get());
        addHiddenIfUnavailable(hidden, ModItems.RESONATING_LIGHTNING_PATTERN_CODING_CARD.get());
        return hidden;
    }

    private static void addHiddenIfUnavailable(List<ItemStack> hidden, Item item) {
        if (!isUpgradeCardVisible(item)) {
            hidden.add(new ItemStack(item));
        }
    }
}
