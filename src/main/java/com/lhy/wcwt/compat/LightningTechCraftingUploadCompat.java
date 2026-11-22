package com.lhy.wcwt.compat;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Uses Lightning Tech's Tianshu upload APIs via reflection when available so WCWT crafting
 * auto-upload matches the Tianshu terminal's preferred Matter Warping Matrix target.
 */
public final class LightningTechCraftingUploadCompat {
    private static final String AE2_LIGHTNING_TECH = "ae2lt";

    private static volatile boolean initialized;
    private static volatile boolean apiAvailable;
    private static Class<?> matrixStorageClass;
    private static Class<?> matrixControllerClass;
    private static Method storageGetLevel;
    private static Method storageGetControllerPos;
    private static Method controllerIsFormed;
    private static Method routingClassify;
    private static Method routingIsMatrixGroup;
    private static Object routeCraftingAssembler;
    private static Method duplicateCheck;
    private static Method checkResultDuplicate;
    private static Method checkResultMatchedSlot;

    private LightningTechCraftingUploadCompat() {
    }

    private static void ensureInit() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!ModList.get().isLoaded(AE2_LIGHTNING_TECH)) {
            return;
        }
        try {
            matrixStorageClass = Class.forName("com.moakiee.ae2lt.blockentity.MatrixPatternStorageBlockEntity");
            matrixControllerClass = Class.forName("com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity");
            Class<?> routingClass = Class.forName("com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternUploadRouting");
            Class<?> routeEnum = Class.forName("com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternUploadRouting$Route");
            Class<?> dupFilterClass = Class.forName("com.moakiee.ae2lt.logic.tianshu.terminal.PatternEncodingDuplicateFilter");
            Class<?> checkResultClass = Class.forName("com.moakiee.ae2lt.logic.tianshu.terminal.PatternEncodingDuplicateFilter$CheckResult");

            storageGetLevel = matrixStorageClass.getMethod("getLevel");
            storageGetControllerPos = matrixStorageClass.getMethod("getControllerPos");
            controllerIsFormed = matrixControllerClass.getMethod("isFormed");
            routingClassify = routingClass.getMethod("classify", ItemStack.class, Level.class);
            routingIsMatrixGroup = routingClass.getMethod("isMatterWarpingMatrixGroup",
                    appeng.api.implementations.blockentities.PatternContainerGroup.class);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object routeVal = Enum.valueOf((Class<Enum>) routeEnum, "CRAFTING_ASSEMBLER");
            routeCraftingAssembler = routeVal;
            duplicateCheck = dupFilterClass.getMethod("checkEquivalentPattern",
                    InternalInventory.class, ItemStack.class, Level.class);
            checkResultDuplicate = checkResultClass.getMethod("duplicate");
            checkResultMatchedSlot = checkResultClass.getMethod("matchedSlot");
            apiAvailable = true;
        } catch (Throwable ignored) {
            apiAvailable = false;
        }
    }

    public static boolean isAvailable() {
        ensureInit();
        return apiAvailable;
    }

    public static boolean isCraftingPattern(ItemStack pattern, Level level) {
        if (!isAvailable()) {
            return false;
        }
        try {
            return routingClassify.invoke(null, pattern, level) == routeCraftingAssembler;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isTianshuCraftingArray(PatternContainer provider) {
        if (!isAvailable() || provider == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(routingIsMatrixGroup.invoke(null, provider.getTerminalGroup()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Object craftingArraySystem(PatternContainer provider) {
        if (!isAvailable() || !matrixStorageClass.isInstance(provider)) {
            return null;
        }
        try {
            Level level = (Level) storageGetLevel.invoke(provider);
            BlockPos controllerPos = (BlockPos) storageGetControllerPos.invoke(provider);
            if (level == null || controllerPos == null || !level.isLoaded(controllerPos)) {
                return null;
            }
            var controller = level.getBlockEntity(controllerPos);
            if (matrixControllerClass.isInstance(controller)
                    && Boolean.TRUE.equals(controllerIsFormed.invoke(controller))) {
                return controller;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static int findDuplicateSlot(PatternContainer provider, ItemStack pattern, Level level) {
        if (!isAvailable() || provider == null || pattern == null || pattern.isEmpty()) {
            return -1;
        }
        InternalInventory inventory = provider.getTerminalPatternInventory();
        if (inventory == null) {
            return -1;
        }
        try {
            Object result = duplicateCheck.invoke(null, inventory, pattern, level);
            if (result != null && Boolean.TRUE.equals(checkResultDuplicate.invoke(result))) {
                return (Integer) checkResultMatchedSlot.invoke(result);
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    public static boolean insertCraftingPattern(PatternContainer provider, ItemStack pattern, boolean simulate) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        InternalInventory inventory = provider.getTerminalPatternInventory();
        if (inventory == null) {
            return false;
        }
        ItemStack toInsert = pattern.copyWithCount(1);
        int slot = firstFreePatternSlot(inventory, toInsert);
        if (slot < 0) {
            return false;
        }
        return inventory.insertItem(slot, toInsert, simulate).isEmpty();
    }

    private static int firstFreePatternSlot(InternalInventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty() && inventory.isItemValid(slot, stack)) {
                return slot;
            }
        }
        return -1;
    }
}
