package com.lhy.wcwt.helpers;

import appeng.api.upgrades.IUpgradeInventory;
import com.lhy.wcwt.api.IExtendedUIHost;
import com.lhy.wcwt.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class ExtendedUiUpgradeCards {
    private ExtendedUiUpgradeCards() {
    }

    public static int maskFor(IUpgradeInventory upgrades) {
        int mask = 0;
        for (var type : IExtendedUIHost.ExtendedUIType.values()) {
            if (type != IExtendedUIHost.ExtendedUIType.NONE && canOpen(upgrades, type)) {
                mask |= mask(type);
            }
        }
        return mask;
    }

    public static boolean isInstalled(IUpgradeInventory upgrades, IExtendedUIHost.ExtendedUIType type) {
        var card = cardFor(type);
        return card != null && upgrades.isInstalled(card);
    }

    public static boolean canOpen(IUpgradeInventory upgrades, IExtendedUIHost.ExtendedUIType type) {
        return type == IExtendedUIHost.ExtendedUIType.NONE
                || (isEnvironmentAvailable(type) && isInstalled(upgrades, type));
    }

    public static boolean isEnvironmentAvailable(IExtendedUIHost.ExtendedUIType type) {
        return switch (type) {
            case ADVANCED_CODING, TOOL_SLOTS_BOX, TOOLKIT -> true;
            case COSMETIC_ARMOR -> ModList.get().isLoaded("cosmeticarmorreworked");
            case CURIOS -> ModList.get().isLoaded("curios");
            case RESONATING_LIGHTNING_PATTERN_CODING ->
                    ModList.get().isLoaded("ae2cs") || ModList.get().isLoaded("ae2lt");
            case NONE -> false;
        };
    }

    public static int mask(IExtendedUIHost.ExtendedUIType type) {
        return switch (type) {
            case ADVANCED_CODING -> 1;
            case COSMETIC_ARMOR -> 1 << 1;
            case CURIOS -> 1 << 2;
            case TOOL_SLOTS_BOX -> 1 << 3;
            case TOOLKIT -> 1 << 4;
            case RESONATING_LIGHTNING_PATTERN_CODING -> 1 << 5;
            case NONE -> 0;
        };
    }

    @Nullable
    public static Item cardFor(IExtendedUIHost.ExtendedUIType type) {
        return switch (type) {
            case ADVANCED_CODING -> ModItems.ADVANCED_CODING_CARD.get();
            case COSMETIC_ARMOR -> ModItems.COSMETIC_ARMOR_CARD.get();
            case CURIOS -> ModItems.CURIOS_CARD.get();
            case TOOL_SLOTS_BOX -> ModItems.TOOL_SLOTS_BOX_CARD.get();
            case TOOLKIT -> ModItems.TOOLKIT_CARD.get();
            case RESONATING_LIGHTNING_PATTERN_CODING -> ModItems.RESONATING_LIGHTNING_PATTERN_CODING_CARD.get();
            case NONE -> null;
        };
    }
}
