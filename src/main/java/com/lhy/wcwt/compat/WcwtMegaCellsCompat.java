package com.lhy.wcwt.compat;

import appeng.api.storage.StorageCells;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class WcwtMegaCellsCompat {
    private static final String MOD_ID = "megacells";
    private static final String BULK_CELL_CLASS = "gripe._90.megacells.item.cell.BulkCellInventory";

    private WcwtMegaCellsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack getCompressionCutoffItem(ItemStack cellStack) {
        if (cellStack.isEmpty() || !isLoaded()) {
            return ItemStack.EMPTY;
        }
        try {
            Object bulkCell = bulkCell(cellStack);
            if (bulkCell == null) {
                return ItemStack.EMPTY;
            }
            Class<?> cls = bulkCell.getClass();
            boolean hasChain = Boolean.TRUE.equals(cls.getMethod("hasCompressionChain").invoke(bulkCell));
            boolean enabled = Boolean.TRUE.equals(cls.getMethod("isCompressionEnabled").invoke(bulkCell));
            if (!hasChain || !enabled) {
                return ItemStack.EMPTY;
            }
            Object cutoff = cls.getMethod("getCutoffItem").invoke(bulkCell);
            return cutoff instanceof ItemStack stack && !stack.isEmpty() ? stack.copy() : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean switchCompressionCutoff(ItemStack cellStack, boolean towardMoreCompressed) {
        if (cellStack.isEmpty() || !isLoaded()) {
            return false;
        }
        try {
            Object bulkCell = bulkCell(cellStack);
            if (bulkCell == null) {
                return false;
            }
            Class<?> cls = bulkCell.getClass();
            boolean hasChain = Boolean.TRUE.equals(cls.getMethod("hasCompressionChain").invoke(bulkCell));
            if (!hasChain) {
                return false;
            }
            cls.getMethod("switchCompressionCutoff", boolean.class).invoke(bulkCell, towardMoreCompressed);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @org.jetbrains.annotations.Nullable
    private static Object bulkCell(ItemStack cellStack) {
        Object cell = StorageCells.getCellInventory(cellStack, null);
        if (cell == null) {
            return null;
        }
        try {
            Class<?> bulkClass = Class.forName(BULK_CELL_CLASS);
            return bulkClass.isInstance(cell) ? cell : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
