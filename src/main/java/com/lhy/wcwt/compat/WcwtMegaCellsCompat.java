package com.lhy.wcwt.compat;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ICellWorkbenchItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WcwtMegaCellsCompat {
    private static final String MOD_ID = "megacells";
    private static final String BULK_CELL_INVENTORY_CLASS =
            "gripe._90.megacells.item.cell.BulkCellInventory";
    private static final String COMPRESSION_SERVICE_CLASS =
            "gripe._90.megacells.misc.CompressionService";

    private static volatile boolean reflectionInitialized;
    private static Class<?> bulkCellInventoryClass;
    private static Method isCompressionEnabledMethod;
    private static Method getFilterItemMethod;
    private static Method getStoredItemMethod;
    private static Field compressionServiceInstanceField;
    private static Method compressionServiceGetChainMethod;
    private static Method compressionVariantItemMethod;

    private WcwtMegaCellsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack getCompressionCutoffItem(ItemStack cellStack) {
        Object bulkCell = getBulkCellInventory(cellStack);
        if (bulkCell == null || !invokeBoolean(isCompressionEnabledMethod, bulkCell)) {
            return ItemStack.EMPTY;
        }

        AEItemKey filterKey = getFilterKey(bulkCell);
        if (filterKey == null || getCompressionChain(filterKey).size() <= 1) {
            return ItemStack.EMPTY;
        }
        return filterKey.toStack(1);
    }

    public static boolean switchCompressionCutoff(ItemStack cellStack, boolean towardMoreCompressed) {
        Object bulkCell = getBulkCellInventory(cellStack);
        if (bulkCell == null || !invokeBoolean(isCompressionEnabledMethod, bulkCell)) {
            return false;
        }

        AEItemKey currentKey = getFilterKey(bulkCell);
        if (currentKey == null) {
            currentKey = getStoredKey(bulkCell);
        }
        AEItemKey nextKey = getCompressionCutoffNeighbor(currentKey, towardMoreCompressed);
        if (nextKey == null || nextKey.equals(currentKey)
                || !(cellStack.getItem() instanceof ICellWorkbenchItem cellWorkbenchItem)) {
            return false;
        }

        var config = cellWorkbenchItem.getConfigInventory(cellStack);
        if (config == null || config.size() <= 0) {
            return false;
        }
        config.setStack(0, new GenericStack(nextKey, 0));
        return true;
    }

    private static AEItemKey getFilterKey(Object bulkCell) {
        return invokeAEItemKey(getFilterItemMethod, bulkCell);
    }

    private static AEItemKey getStoredKey(Object bulkCell) {
        return invokeAEItemKey(getStoredItemMethod, bulkCell);
    }

    private static AEItemKey getCompressionCutoffNeighbor(AEItemKey currentKey, boolean towardMoreCompressed) {
        if (currentKey == null) {
            return null;
        }
        List<AEItemKey> chain = getCompressionChain(currentKey);
        if (chain.size() <= 1) {
            return null;
        }
        int currentIndex = chain.indexOf(currentKey);
        if (currentIndex < 0) {
            return null;
        }
        int direction = towardMoreCompressed ? 1 : -1;
        return chain.get(Math.floorMod(currentIndex + direction, chain.size()));
    }

    private static List<AEItemKey> getCompressionChain(AEItemKey key) {
        if (key == null || compressionServiceInstanceField == null || compressionServiceGetChainMethod == null
                || compressionVariantItemMethod == null) {
            return List.of();
        }
        try {
            Object service = compressionServiceInstanceField.get(null);
            Object optionalValue = compressionServiceGetChainMethod.invoke(service, key);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return List.of();
            }
            Object chain = optional.get();
            if (!(chain instanceof Iterable<?> iterable)) {
                return List.of();
            }

            List<AEItemKey> result = new ArrayList<>();
            for (Object variant : iterable) {
                Object value = compressionVariantItemMethod.invoke(variant);
                if (value instanceof AEItemKey itemKey) {
                    result.add(itemKey);
                }
            }
            return result;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static AEItemKey invokeAEItemKey(Method method, Object target) {
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof AEItemKey itemKey ? itemKey : null;
        } catch (Throwable ignored) {
            return null;
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
        if (method == null) {
            return false;
        }
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
                isCompressionEnabledMethod = bulkCellInventoryClass.getMethod("isCompressionEnabled");
                getFilterItemMethod = bulkCellInventoryClass.getMethod("getFilterItem");
                getStoredItemMethod = bulkCellInventoryClass.getMethod("getStoredItem");
                initCompressionServiceReflection();
            } catch (Throwable ignored) {
                bulkCellInventoryClass = null;
                isCompressionEnabledMethod = null;
                getFilterItemMethod = null;
                getStoredItemMethod = null;
                compressionServiceInstanceField = null;
                compressionServiceGetChainMethod = null;
                compressionVariantItemMethod = null;
            }
            reflectionInitialized = true;
            return bulkCellInventoryClass != null;
        }
    }

    private static void initCompressionServiceReflection() {
        try {
            Class<?> compressionServiceClass = Class.forName(COMPRESSION_SERVICE_CLASS);
            compressionServiceInstanceField = compressionServiceClass.getField("INSTANCE");
            compressionServiceGetChainMethod = compressionServiceClass.getMethod("getChain", AEItemKey.class);
            Class<?> compressionVariantClass =
                    Class.forName(COMPRESSION_SERVICE_CLASS + "$Variant");
            compressionVariantItemMethod = compressionVariantClass.getMethod("item");
        } catch (Throwable ignored) {
            compressionServiceInstanceField = null;
            compressionServiceGetChainMethod = null;
            compressionVariantItemMethod = null;
        }
    }
}
