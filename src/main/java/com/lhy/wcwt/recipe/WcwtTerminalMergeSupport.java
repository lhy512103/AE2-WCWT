package com.lhy.wcwt.recipe;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            applyTerminalComponents(result, terminal, leftovers);
        }
        return leftovers;
    }

    /**
     * Copies a source terminal's state onto the result without losing stored items. Components that hold
     * items (crafting grid, singularity, pattern slots, ...) are only taken when the result has none yet;
     * otherwise the source's items join the leftovers handed back to the player.
     */
    static void applyTerminalComponents(ItemStack result, ItemStack terminal, List<ItemStack> leftovers) {
        var leftoverType = ModComponents.CRAFTING_UPGRADE_LEFTOVERS.get();
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (var entry : terminal.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = entry.getKey();
            if (type == AEComponents.STORED_ENERGY
                    || type == AEComponents.ENERGY_CAPACITY
                    || type == AEComponents.UPGRADES) {
                continue;
            }
            Optional<?> value = entry.getValue();
            if (type == leftoverType) {
                value.ifPresent(contents -> collectItems(contents, leftovers));
                continue;
            }
            Object existing = result.get(type);
            if (value.isEmpty()) {
                if (!holdsItems(existing)) {
                    builder.remove(type);
                }
                continue;
            }
            Object incoming = value.get();
            if (holdsItems(existing) && isItemHolder(incoming)) {
                collectItems(incoming, leftovers);
                continue;
            }
            set(builder, type, incoming);
        }
        result.applyComponents(builder.build());
    }

    private static boolean isItemHolder(@Nullable Object value) {
        return value instanceof ItemContainerContents || value instanceof ItemStack;
    }

    private static boolean holdsItems(@Nullable Object value) {
        if (value instanceof ItemContainerContents contents) {
            return contents.nonEmptyItems().iterator().hasNext();
        }
        return value instanceof ItemStack stack && !stack.isEmpty();
    }

    private static void collectItems(Object value, List<ItemStack> leftovers) {
        if (value instanceof ItemContainerContents contents) {
            contents.nonEmptyItemsCopy().forEach(leftovers::add);
        } else if (value instanceof ItemStack stack && !stack.isEmpty()) {
            leftovers.add(stack.copy());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(DataComponentPatch.Builder builder, DataComponentType<T> type, Object value) {
        builder.set(type, (T) value);
    }
}