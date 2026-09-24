package com.lhy.wcwt.menu;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.ExtendedAePlusMatrixUploadCompat;
import com.lhy.wcwt.compat.LightningTechCraftingUploadCompat;
import com.lhy.wcwt.compat.NeoEcoApiCompat;
import com.lhy.wcwt.compat.plus.PlusEncodingUpload;
import com.lhy.wcwt.compat.plus.PlusPresence;
import com.lhy.wcwt.util.PatternProviderIds;
import com.lhy.wcwt.util.PatternProviderSorts;
import com.lhy.wcwt.util.PatternUploadMetadata;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * Pattern providers reachable from a terminal menu, and uploading a freshly encoded pattern to them:
 * Lightning Tech's Tianshu array, ECO storage, the ExtendedAE Plus assembler matrix, or a uniquely
 * matching provider.
 */
final class WcwtPatternUploadTargets {
    private static final boolean DEBUG_PATTERN_UPLOAD = Boolean.getBoolean("wcwt.debug.patternUpload");

    private final Supplier<@Nullable IGrid> grid;
    private final Supplier<Player> player;
    private final BooleanSupplier serverSide;
    private final IntPredicate returnBlankPatterns;

    /**
     * Vanilla {@code broadcastChanges} reads every provider slot each tick and each read resolves its
     * provider, so a network-wide scan is reused for the rest of the server tick.
     */
    private long cachedProvidersTick = Long.MIN_VALUE;
    @Nullable
    private List<PatternContainer> cachedProviders;

    WcwtPatternUploadTargets(Supplier<@Nullable IGrid> grid, Supplier<Player> player, BooleanSupplier serverSide,
                             IntPredicate returnBlankPatterns) {
        this.grid = grid;
        this.player = player;
        this.serverSide = serverSide;
        this.returnBlankPatterns = returnBlankPatterns;
    }

    List<PatternContainer> providers() {
        Player currentPlayer = player.get();
        if (serverSide.getAsBoolean() && currentPlayer != null) {
            long tick = currentPlayer.level().getGameTime();
            if (cachedProviders != null && cachedProvidersTick == tick) {
                return cachedProviders;
            }
            cachedProviders = scanProviders();
            cachedProvidersTick = tick;
            return cachedProviders;
        }
        return scanProviders();
    }

    /**
     * A cheap content hash used to skip pushing an unchanged provider list: provider count and sizes,
     * plus slot, count and item-with-components of every non-empty slot. Nothing is copied or serialized.
     */
    long signature() {
        var providers = providers();
        long hash = 1469598103934665603L;
        hash = hash * 1099511628211L + providers.size();
        for (var provider : providers) {
            InternalInventory inv = provider.getTerminalPatternInventory();
            if (inv == null) {
                hash = hash * 1099511628211L + 17L;
                continue;
            }
            hash = hash * 1099511628211L + inv.size();
            for (int i = 0; i < inv.size(); i++) {
                var stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                hash = hash * 1099511628211L + i;
                hash = hash * 1099511628211L + stack.getCount();
                hash = hash * 1099511628211L + ItemStack.hashItemAndComponents(stack);
            }
        }
        return hash;
    }

    MatrixUploadResult uploadToMatrix(ItemStack encodedPattern) {
        if (!(player.get() instanceof ServerPlayer serverPlayer)
                || encodedPattern.isEmpty()
                || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return MatrixUploadResult.FAILURE;
        }

        ItemStack uploadStack = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);

        var currentGrid = grid.get();
        if (currentGrid == null) {
            return MatrixUploadResult.FAILURE;
        }

        try {
            MatrixUploadResult tianshuUpload = uploadToTianshuCraftingArray(uploadStack, serverPlayer);
            if (tianshuUpload.state() != MatrixUploadState.FAILURE) {
                return tianshuUpload;
            }
        } catch (Throwable ignored) {
        }
        try {
            EcoUploadDuplicateResult ecoDuplicate = findEcoDuplicatePattern(uploadStack);
            if (ecoDuplicate.duplicate()) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.eco_pattern_duplicate"));
                recordEaepProviderUpload(ecoDuplicate.provider(), ecoDuplicate.slot());
                return MatrixUploadResult.uploaded(ecoDuplicate.providerId(), ecoDuplicate.slot());
            }
            if (NeoEcoApiCompat.uploadPatternToEcoStorage(currentGrid, uploadStack.copy())) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.eco_pattern_uploaded"));
                return findEcoUploadResult(uploadStack);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (assemblerMatrixContainsPattern(uploadStack)) {
                serverPlayer.sendSystemMessage(Component.translatable("extendedae_plus.message.matrix.duplicate"));
                return returnBlankPatterns.test(uploadStack.getCount())
                        ? MatrixUploadResult.DUPLICATE_RETURNED
                        : MatrixUploadResult.DUPLICATE_ABORTED;
            }
            if (ExtendedAePlusMatrixUploadCompat.uploadPatternToMatrix(serverPlayer, uploadStack.copy(), currentGrid)) {
                return findMatrixUploadResult(uploadStack);
            }
        } catch (Throwable ignored) {
        }
        return MatrixUploadResult.FAILURE;
    }

    UploadAttemptResult uploadToMatchingProvider(ItemStack encodedPattern, String searchText) {
        if (!PlusPresence.available() || !(player.get() instanceof ServerPlayer serverPlayer)) {
            return UploadAttemptResult.NO_TARGET;
        }
        ItemStack uploadStack = PatternUploadMetadata.copyWithoutUploadData(encodedPattern);
        PlusEncodingUpload.UniqueUploadResult result = PlusEncodingUpload.uploadUniqueMatch(
                serverPlayer, grid.get(), uploadStack, searchText);
        if (DEBUG_PATTERN_UPLOAD) {
            WcwtMod.LOGGER.info("WCWT pattern upload debug: server unique upload query={}, uploaded={}, hadTarget={}, providerName={}",
                    searchText, result.uploaded(), result.hadTarget(), result.providerName());
        }
        if (!result.hadTarget()) {
            return UploadAttemptResult.NO_TARGET;
        }
        long providerId = -1L;
        int slot = -1;
        if (result.uploaded()) {
            MatrixUploadResult located = findMatrixUploadResult(uploadStack);
            providerId = located.providerId();
            slot = result.slot() >= 0 ? result.slot() : located.slot();
        }
        return new UploadAttemptResult(result.uploaded(), true, result.providerName(), providerId, slot);
    }

    private List<PatternContainer> scanProviders() {
        var currentGrid = grid.get();
        if (currentGrid == null) {
            return List.of();
        }

        var providers = new ArrayList<PatternContainer>();
        for (var machineClass : currentGrid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
            for (var container : currentGrid.getActiveMachines(containerClass)) {
                if (container != null
                        && container.isVisibleInTerminal()
                        && container.getTerminalPatternInventory() != null
                        && container.getTerminalPatternInventory().size() > 0
                        && container.getTerminalGroup() != null) {
                    providers.add(container);
                }
            }
        }
        providers.sort(PatternProviderSorts.STABLE);
        return providers;
    }

    private EcoUploadDuplicateResult findEcoDuplicatePattern(ItemStack encodedPattern) {
        for (var provider : providers()) {
            if (!NeoEcoApiCompat.isEcoPatternProvider(provider)) {
                continue;
            }
            int duplicateSlot = findMatchingPatternSlot(provider, encodedPattern);
            if (duplicateSlot >= 0) {
                return new EcoUploadDuplicateResult(true, PatternProviderIds.idOf(provider), duplicateSlot, provider);
            }
        }
        return EcoUploadDuplicateResult.NONE;
    }

    private MatrixUploadResult uploadToTianshuCraftingArray(ItemStack encodedPattern, ServerPlayer serverPlayer) {
        if (!LightningTechCraftingUploadCompat.isAvailable()
                || !LightningTechCraftingUploadCompat.isCraftingPattern(encodedPattern, serverPlayer.level())) {
            return MatrixUploadResult.FAILURE;
        }
        PatternContainer target = null;
        for (var provider : providers()) {
            if (!LightningTechCraftingUploadCompat.isTianshuCraftingArray(provider)) {
                continue;
            }
            int duplicateSlot = LightningTechCraftingUploadCompat.findDuplicateSlot(
                    provider, encodedPattern, serverPlayer.level());
            if (duplicateSlot >= 0) {
                serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.tianshu_pattern_duplicate"));
                recordEaepProviderUpload(provider, duplicateSlot);
                return MatrixUploadResult.uploaded(PatternProviderIds.idOf(provider), duplicateSlot);
            }
            if (target == null && LightningTechCraftingUploadCompat.insertCraftingPattern(provider, encodedPattern, true)) {
                target = provider;
            }
        }
        if (target == null || !LightningTechCraftingUploadCompat.insertCraftingPattern(target, encodedPattern, false)) {
            return MatrixUploadResult.FAILURE;
        }
        serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.tianshu_pattern_uploaded"));
        int insertedSlot = findLastInsertedPatternSlot(target, encodedPattern);
        recordEaepProviderUpload(target, insertedSlot);
        return MatrixUploadResult.uploaded(PatternProviderIds.idOf(target), insertedSlot);
    }

    private MatrixUploadResult findEcoUploadResult(ItemStack encodedPattern) {
        for (var provider : providers()) {
            if (!NeoEcoApiCompat.isEcoPatternProvider(provider)) {
                continue;
            }
            int insertedSlot = findMatchingPatternSlot(provider, encodedPattern);
            if (insertedSlot >= 0) {
                recordEaepProviderUpload(provider, insertedSlot);
                return MatrixUploadResult.uploaded(PatternProviderIds.idOf(provider), insertedSlot);
            }
        }
        return MatrixUploadResult.UPLOADED;
    }

    private MatrixUploadResult findMatrixUploadResult(ItemStack encodedPattern) {
        for (var provider : providers()) {
            int insertedSlot = findLastInsertedPatternSlot(provider, encodedPattern);
            if (insertedSlot >= 0) {
                return MatrixUploadResult.uploaded(PatternProviderIds.idOf(provider), insertedSlot);
            }
        }
        return MatrixUploadResult.UPLOADED;
    }

    private boolean assemblerMatrixContainsPattern(ItemStack encodedPattern) {
        if (!ExtendedAePlusMatrixUploadCompat.isAssemblerMatrixAvailable()) {
            return false;
        }
        for (var provider : providers()) {
            if (ExtendedAePlusMatrixUploadCompat.isAssemblerMatrix(provider)
                    && findMatchingPatternSlot(provider, encodedPattern) >= 0) {
                return true;
            }
        }
        return false;
    }

    private void recordEaepProviderUpload(PatternContainer provider, int slot) {
        if (!PlusPresence.available() || slot < 0 || !(player.get() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PlusEncodingUpload.recordLastProviderUpload(serverPlayer, grid.get(), provider, slot);
    }

    private static int findLastInsertedPatternSlot(PatternContainer provider, ItemStack encodedPattern) {
        InternalInventory inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return -1;
        }
        for (int i = inv.size() - 1; i >= 0; i--) {
            var stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && PatternUploadMetadata.isSamePatternIgnoringUploadData(stack, encodedPattern)) {
                return i;
            }
        }
        return -1;
    }

    private static int findMatchingPatternSlot(PatternContainer provider, ItemStack encodedPattern) {
        InternalInventory inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return -1;
        }
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && PatternUploadMetadata.isSamePatternIgnoringUploadData(stack, encodedPattern)) {
                return i;
            }
        }
        return -1;
    }

    private record EcoUploadDuplicateResult(boolean duplicate, long providerId, int slot,
                                            @Nullable PatternContainer provider) {
        private static final EcoUploadDuplicateResult NONE = new EcoUploadDuplicateResult(false, -1, -1, null);
    }

    record MatrixUploadResult(MatrixUploadState state, long providerId, int slot) {
        private static final MatrixUploadResult UPLOADED = new MatrixUploadResult(MatrixUploadState.UPLOADED, -1, -1);
        private static final MatrixUploadResult DUPLICATE_RETURNED =
                new MatrixUploadResult(MatrixUploadState.DUPLICATE_RETURNED, -1, -1);
        private static final MatrixUploadResult DUPLICATE_ABORTED =
                new MatrixUploadResult(MatrixUploadState.DUPLICATE_ABORTED, -1, -1);
        private static final MatrixUploadResult FAILURE = new MatrixUploadResult(MatrixUploadState.FAILURE, -1, -1);

        private static MatrixUploadResult uploaded(long providerId, int slot) {
            return new MatrixUploadResult(MatrixUploadState.UPLOADED, providerId, slot);
        }
    }

    enum MatrixUploadState {
        UPLOADED,
        DUPLICATE_RETURNED,
        DUPLICATE_ABORTED,
        FAILURE
    }

    record UploadAttemptResult(boolean uploaded, boolean hadTarget, String providerName, long providerId, int slot) {
        private static final UploadAttemptResult NO_TARGET = new UploadAttemptResult(false, false, "", -1, -1);
    }
}
