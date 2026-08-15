package com.lhy.wcwt.recipe;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class WcwtTerminalMergeSupport {
    private WcwtTerminalMergeSupport() {
    }

    static List<ItemStack> mergeInto(ItemStack result, Iterable<ItemStack> terminals) {
        if (!(result.getItem() instanceof ItemWT resultItem)) {
            return List.of();
        }

        List<ItemStack> sources = new ArrayList<>();
        List<ItemStack> leftovers = new ArrayList<>();
        var targetUpgrades = resultItem.getUpgrades(result);
        for (ItemStack terminal : terminals) {
            if (terminal.isEmpty() || !(terminal.getItem() instanceof ItemWT terminalItem)) {
                continue;
            }
            sources.add(terminal);
            for (ItemStack upgrade : terminalItem.getUpgrades(terminal)) {
                if (!upgrade.isEmpty()) {
                    ItemStack remaining = targetUpgrades.addItems(upgrade.copy());
                    if (!remaining.isEmpty()) {
                        leftovers.add(remaining);
                    }
                }
            }
        }

        if (resultItem instanceof ItemWUT universalTerminal) {
            universalTerminal.onUpgradesChanged(result, targetUpgrades);
        } else if (resultItem instanceof WirelessComprehensiveWorkTerminalItem wcwt) {
            wcwt.updatePowerMultiplier(result);
        }

        for (ItemStack terminal : sources) {
            ItemWT terminalItem = (ItemWT) terminal.getItem();
            resultItem.injectAEPower(
                    result,
                    terminalItem.getAECurrentPower(terminal),
                    Actionable.MODULATE);
            var components = terminal.getComponentsPatch()
                    .forget(type -> type == AEComponents.STORED_ENERGY)
                    .forget(type -> type == AEComponents.ENERGY_CAPACITY)
                    .forget(type -> type == AEComponents.UPGRADES);
            result.applyComponents(components);
        }
        return leftovers;
    }
}