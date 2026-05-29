package de.mari_023.ae2wtlib.api;

import java.util.function.Supplier;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.Upgrades;

public final class AE2wtlibAPI {
    private AE2wtlibAPI() {
    }

    public static boolean hasQuantumBridgeCard(Supplier<IUpgradeInventory> upgrades) {
        var inventory = upgrades.get();
        if (inventory == null) {
            return false;
        }

        for (int slot = 0; slot < inventory.size(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && Upgrades.isUpgradeCardItem(stack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
