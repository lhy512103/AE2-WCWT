package com.lhy.wcwt.compat.plus;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.filter.IAEItemFilter;
import com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil;
import com.lhy.wcwt.compat.JecSearchCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PlusEncodingUpload {
    private PlusEncodingUpload() {
    }

    public static UniqueUploadResult uploadUniqueMatch(ServerPlayer player, @Nullable IGrid grid,
                                                       ItemStack encodedPattern, @Nullable String searchText) {
        if (player == null || grid == null || encodedPattern == null || encodedPattern.isEmpty()
                || !PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            return UniqueUploadResult.NO_TARGET;
        }
        String query = PlusMapping.resolveProviderSearchKey(searchText);
        if (query.isEmpty()) {
            return UniqueUploadResult.NO_TARGET;
        }
        List<PatternContainer> providers = ExtendedAEPatternUploadUtil.listAvailableProvidersFromGrid(grid);
        List<PatternContainer> matches = new ArrayList<>();
        String targetName = null;
        for (PatternContainer provider : providers) {
            String name = PlusMapping.getProviderDisplayName(provider);
            if (!JecSearchCompat.contains(name, query)) {
                continue;
            }
            if (targetName == null) {
                targetName = name;
            } else if (!targetName.equals(name)) {
                return UniqueUploadResult.NO_TARGET;
            }
            matches.add(provider);
        }
        if (matches.isEmpty() || targetName == null) {
            return UniqueUploadResult.NO_TARGET;
        }
        ItemStack remaining = encodedPattern.copy();
        EncodedPatternFilter filter = new EncodedPatternFilter();
        for (PatternContainer provider : matches) {
            remaining = ExtendedAEPatternUploadUtil.insertIntoAccessiblePatternSlots(provider, remaining, filter);
            if (remaining.isEmpty()) {
                return UniqueUploadResult.uploaded(targetName);
            }
        }
        return UniqueUploadResult.failed(targetName);
    }

    public record UniqueUploadResult(boolean uploaded, boolean hadTarget, String providerName) {
        private static final UniqueUploadResult NO_TARGET = new UniqueUploadResult(false, false, "");

        private static UniqueUploadResult uploaded(String providerName) {
            return new UniqueUploadResult(true, true, providerName);
        }

        private static UniqueUploadResult failed(String providerName) {
            return new UniqueUploadResult(false, true, providerName);
        }
    }

    private static final class EncodedPatternFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && PatternDetailsHelper.isEncodedPattern(stack);
        }
    }
}
