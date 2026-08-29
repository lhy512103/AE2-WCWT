package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
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
        return WcwtReflect.invoke(bulkCell, getCutoffItemMethod)
                .filter(ItemStack.class::isInstance)
                .map(ItemStack.class::cast)
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    public static boolean switchCompressionCutoff(ItemStack cellStack, boolean towardMoreCompressed) {
        Object bulkCell = getBulkCellInventory(cellStack);
        if (bulkCell == null || !invokeBoolean(hasCompressionChainMethod, bulkCell)) {
            return false;
        }
        return WcwtReflect.run(bulkCell, switchCompressionCutoffMethod, towardMoreCompressed);
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
            bulkCellInventoryClass = WcwtReflect.findClass(MOD_ID, BULK_CELL_INVENTORY_CLASS).orElse(null);
            if (bulkCellInventoryClass != null) {
                hasCompressionChainMethod =
                        WcwtReflect.findMethod(bulkCellInventoryClass, "hasCompressionChain").orElse(null);
                isCompressionEnabledMethod =
                        WcwtReflect.findMethod(bulkCellInventoryClass, "isCompressionEnabled").orElse(null);
                getCutoffItemMethod =
                        WcwtReflect.findMethod(bulkCellInventoryClass, "getCutoffItem").orElse(null);
                switchCompressionCutoffMethod = WcwtReflect
                        .findMethod(bulkCellInventoryClass, "switchCompressionCutoff", boolean.class).orElse(null);
            }
            reflectionInitialized = true;
            return bulkCellInventoryClass != null;
        }
    }
}
