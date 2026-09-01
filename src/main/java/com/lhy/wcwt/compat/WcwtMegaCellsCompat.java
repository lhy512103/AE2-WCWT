package com.lhy.wcwt.compat;

import appeng.api.storage.StorageCells;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class WcwtMegaCellsCompat {
    private static final String MOD_ID = "megacells";

    private WcwtMegaCellsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack getCompressionCutoffItem(ItemStack cellStack) {
        if (cellStack.isEmpty() || !isLoaded()) {
            return ItemStack.EMPTY;
        }
        return Impl.getCompressionCutoffItem(cellStack);
    }

    public static boolean switchCompressionCutoff(ItemStack cellStack, boolean towardMoreCompressed) {
        if (cellStack.isEmpty() || !isLoaded()) {
            return false;
        }
        return Impl.switchCompressionCutoff(cellStack, towardMoreCompressed);
    }

    private static final class Impl {
        private Impl() {
        }

        static ItemStack getCompressionCutoffItem(ItemStack cellStack) {
            try {
                var bulkCell = bulkCell(cellStack);
                if (bulkCell == null || !bulkCell.hasCompressionChain() || !bulkCell.isCompressionEnabled()) {
                    return ItemStack.EMPTY;
                }
                ItemStack cutoff = bulkCell.getCutoffItem();
                return cutoff == null || cutoff.isEmpty() ? ItemStack.EMPTY : cutoff.copy();
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        static boolean switchCompressionCutoff(ItemStack cellStack, boolean towardMoreCompressed) {
            try {
                var bulkCell = bulkCell(cellStack);
                if (bulkCell == null || !bulkCell.hasCompressionChain()) {
                    return false;
                }
                bulkCell.switchCompressionCutoff(towardMoreCompressed);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        @org.jetbrains.annotations.Nullable
        private static gripe._90.megacells.item.cell.BulkCellInventory bulkCell(ItemStack cellStack) {
            Object cell = StorageCells.getCellInventory(cellStack, null);
            return cell instanceof gripe._90.megacells.item.cell.BulkCellInventory bulk ? bulk : null;
        }
    }
}
