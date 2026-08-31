package com.lhy.wcwt.compat.plus;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.filter.IAEItemFilter;
import com.extendedae_plus.network.ReturnLastPatternC2SPacket;
import com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil;
import com.lhy.wcwt.compat.JecSearchCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
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
        for (int index = 0; index < matches.size(); index++) {
            PatternContainer provider = matches.get(index);
            InternalInventory inv = provider.getTerminalPatternInventory();
            if (inv == null) {
                continue;
            }
            ItemStack[] before = snapshot(inv);
            remaining = ExtendedAEPatternUploadUtil.insertIntoAccessiblePatternSlots(provider, remaining, filter);
            int slot = firstChangedSlot(inv, before);
            if (slot >= 0) {
                ExtendedAEPatternUploadUtil.recordProviderUpload(player, -1L - index, provider, slot);
            }
            if (remaining.isEmpty()) {
                return UniqueUploadResult.uploaded(targetName, slot);
            }
        }
        return UniqueUploadResult.failed(targetName);
    }

    public static void requestReturnLastPattern() {
        PacketDistributor.sendToServer(ReturnLastPatternC2SPacket.INSTANCE);
    }

    private static ItemStack[] snapshot(InternalInventory inv) {
        ItemStack[] before = new ItemStack[inv.size()];
        for (int i = 0; i < inv.size(); i++) {
            before[i] = inv.getStackInSlot(i).copy();
        }
        return before;
    }

    private static int firstChangedSlot(InternalInventory inv, ItemStack[] before) {
        for (int i = 0; i < before.length && i < inv.size(); i++) {
            if (!ItemStack.matches(before[i], inv.getStackInSlot(i))) {
                return i;
            }
        }
        return -1;
    }

    public record UniqueUploadResult(boolean uploaded, boolean hadTarget, String providerName, int slot) {
        private static final UniqueUploadResult NO_TARGET = new UniqueUploadResult(false, false, "", -1);

        private static UniqueUploadResult uploaded(String providerName, int slot) {
            return new UniqueUploadResult(true, true, providerName, slot);
        }

        private static UniqueUploadResult failed(String providerName) {
            return new UniqueUploadResult(false, true, providerName, -1);
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
