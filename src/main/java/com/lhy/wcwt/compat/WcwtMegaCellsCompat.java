package com.lhy.wcwt.compat;

import appeng.api.storage.StorageCells;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class WcwtMegaCellsCompat {
    private static final String MOD_ID = "megacells";
    private static final String BULK_CELL_INVENTORY_CLASS =
            "gripe._90.megacells.item.cell.BulkCellInventory";

    private static volatile boolean reflectionInitialized;
    private static Class<?> bulkCellInventoryClass;
    private static Method hasCompressionChainMethod;
    private static Method isCompressionEnabledMethod;
    private static Method getCutoffItemMethod;
    private static Method switchCompressionCutoffMethod;

    private WcwtMegaCellsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack getCompressionCutoffItem(ItemStack cellStack) {
        Object bulkCell = getBulkCellInventory(cellStack);
        if (bulkCell == null || !invokeBoolean(hasCompressionChainMethod, bulkCell)
                || !invokeBoolean(isCompressionEnabledMethod, bulkCell)) {
            return ItemStack.EMPTY;
        }
        try {
            Object value = getCutoffItemMethod.invoke(bulkCell);
            return value instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean switchCompressionCutoff(ItemStack cellStack, boolean towardMoreCompressed) {
        Object bulkCell = getBulkCellInventory(cellStack);
        if (bulkCell == null || !invokeBoolean(hasCompressionChainMethod, bulkCell)) {
            return false;
        }
        try {
            switchCompressionCutoffMethod.invoke(bulkCell, towardMoreCompressed);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getBulkCellInventory(ItemStack cellStack) {
        if (cellStack.isEmpty() || !isLoaded() || !initReflection()) {
            return null;
        }
        try {
            Object cell = StorageCells.getCellInventory(cellStack, null);
            return bulkCellInventoryClass.isInstance(cell) ? cell : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Method method, Object target) {
        try {
            Object value = method.invoke(target);
            return value instanceof Boolean result && result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean initReflection() {
        if (reflectionInitialized) {
            return bulkCellInventoryClass != null;
        }
        synchronized (WcwtMegaCellsCompat.class) {
            if (reflectionInitialized) {
                return bulkCellInventoryClass != null;
            }
            try {
                bulkCellInventoryClass = Class.forName(BULK_CELL_INVENTORY_CLASS);
                hasCompressionChainMethod = bulkCellInventoryClass.getMethod("hasCompressionChain");
                isCompressionEnabledMethod = bulkCellInventoryClass.getMethod("isCompressionEnabled");
                getCutoffItemMethod = bulkCellInventoryClass.getMethod("getCutoffItem");
                switchCompressionCutoffMethod = bulkCellInventoryClass.getMethod("switchCompressionCutoff",
                        boolean.class);
            } catch (Throwable ignored) {
                bulkCellInventoryClass = null;
                hasCompressionChainMethod = null;
                isCompressionEnabledMethod = null;
                getCutoffItemMethod = null;
                switchCompressionCutoffMethod = null;
            }
            reflectionInitialized = true;
            return bulkCellInventoryClass != null;
        }
    }
}
