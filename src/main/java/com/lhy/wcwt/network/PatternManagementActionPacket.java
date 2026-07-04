package com.lhy.wcwt.network;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.JecSearchCompat;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.util.PatternUploadMetadata;
import com.lhy.wcwt.util.PatternProviderSorts;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PatternManagementActionPacket(Action action,
                                            long providerId,
                                            int cacheSlot,
                                            String searchText,
                                            String mappingText,
                                            boolean shiftQuickEnabled) implements CustomPacketPayload {
    private static final boolean DEBUG_PATTERN_UPLOAD = Boolean.getBoolean("wcwt.debug.patternUpload");

    public static final Type<PatternManagementActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_management_action"));

    public static final StreamCodec<ByteBuf, PatternManagementActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal),
            PatternManagementActionPacket::action,
            ByteBufCodecs.VAR_LONG,
            PatternManagementActionPacket::providerId,
            ByteBufCodecs.VAR_INT,
            PatternManagementActionPacket::cacheSlot,
            ByteBufCodecs.STRING_UTF8,
            PatternManagementActionPacket::searchText,
            ByteBufCodecs.STRING_UTF8,
            PatternManagementActionPacket::mappingText,
            ByteBufCodecs.BOOL,
            PatternManagementActionPacket::shiftQuickEnabled,
            PatternManagementActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternManagementActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                menu.requestPatternProviderSyncSubscription();
                switch (packet.action) {
                    case UPLOAD_CACHE_SLOT -> uploadCachePatterns(player, menu, packet.providerId);
                    case OPEN_PROVIDER_UI -> openProviderUi(player, packet.providerId);
                    case EXCHANGE_PROVIDER_SLOT -> exchangeProviderSlot(player, packet.providerId, packet.cacheSlot);
                    case QUICK_EXTRACT_PROVIDER_SLOT -> quickExtractProviderSlot(player, packet.providerId, packet.cacheSlot);
                    case QUICK_INSERT_FIRST_PROVIDER -> {
                        if (packet.shiftQuickEnabled()) {
                            quickInsertEncodedPattern(player, packet.providerId, packet.cacheSlot, packet.searchText);
                        }
                    }
                    case ADD_MAPPING, RELOAD_MAPPING, DELETE_MAPPING -> {
                        return;
                    }
                }
                PacketDistributor.sendToPlayer(player, PatternProviderListPacket.buildForPlayer(player, packet.searchText));
            }
        });
    }

    private static String normalizeSearchText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void exchangeProviderSlot(ServerPlayer player, long providerId, int providerSlot) {
        if (providerSlot < 0) {
            return;
        }
        var provider = getProviderByOrdinal(player, providerId);
        if (provider == null) {
            return;
        }
        var inv = provider.getTerminalPatternInventory();
        if (inv == null || providerSlot >= inv.size()) {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        ItemStack slotStack = inv.getStackInSlot(providerSlot);
        if (carried.isEmpty()) {
            if (!slotStack.isEmpty()) {
                player.containerMenu.setCarried(inv.extractItem(providerSlot, slotStack.getCount(), false));
            }
            return;
        }
        if (!appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(carried)) {
            return;
        }

        ItemStack toPlace = carried.copy();
        toPlace.setCount(1);
        if (slotStack.isEmpty()) {
            inv.setItemDirect(providerSlot, toPlace);
            carried.shrink(1);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else if (carried.getCount() == 1) {
            inv.setItemDirect(providerSlot, carried.copy());
            player.containerMenu.setCarried(slotStack.copy());
        }
    }

    private static void quickExtractProviderSlot(ServerPlayer player, long providerId, int providerSlot) {
        if (providerSlot < 0) {
            return;
        }
        var provider = getProviderByOrdinal(player, providerId);
        if (provider == null) {
            return;
        }
        var inv = provider.getTerminalPatternInventory();
        if (inv == null || providerSlot >= inv.size()) {
            return;
        }
        var slotStack = inv.getStackInSlot(providerSlot);
        if (slotStack.isEmpty()) {
            return;
        }
        var extracted = inv.extractItem(providerSlot, slotStack.getCount(), false);
        if (!extracted.isEmpty()) {
            player.getInventory().placeItemBackInInventory(extracted, false);
        }
    }

    /**
     * Shift 从背包快捷塞入编码样板；{@code preferredProviderId} 为客户端当前选中的供应器（与列表 1-based id 一致），
     * 无效时回退为排序后的第一个供应器。
     */
    private static void quickInsertEncodedPattern(ServerPlayer player, long preferredProviderId, int playerSlotIndex,
                                                  String clientResolvedSearchText) {
        if (playerSlotIndex < 0 || playerSlotIndex >= player.containerMenu.slots.size()) {
            return;
        }
        var menuSlot = player.containerMenu.slots.get(playerSlotIndex);
        if (menuSlot.container != player.getInventory()) {
            return;
        }
        var providers = listProviders(player);
        if (providers.isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.provider_missing"), true);
            return;
        }

        var sourceStack = menuSlot.getItem();
        if (sourceStack.isEmpty() || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(sourceStack)) {
            return;
        }

        PatternContainer provider = null;
        if (preferredProviderId > 0) {
            provider = getProviderByOrdinal(providers, preferredProviderId);
        }
        String patternSearchText = normalizeSearchText(clientResolvedSearchText);
        if (patternSearchText == null) {
            patternSearchText = normalizeSearchText(PatternUploadMetadata.getProviderSearchText(sourceStack));
        }
        if (provider == null && patternSearchText != null) {
            provider = findMatchingProviderBySearchText(providers, patternSearchText);
        }
        if (provider == null) {
            provider = providers.get(0);
        }
        logDebug("quick insert target preferredProviderId={}, resolvedPatternSearchText={}, targetName={}, sourceStack={}",
                preferredProviderId, patternSearchText, getProviderDisplayName(provider), sourceStack);
        InsertResult result = insertPatternIntoProviderGroup(providers, provider, sourceStack);
        int inserted = result.inserted();
        if (inserted <= 0) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.upload_failed"), true);
            return;
        }

        var updatedSource = sourceStack.copy();
        updatedSource.shrink(inserted);
        menuSlot.setByPlayer(updatedSource);
        menuSlot.setChanged();
    }

    private static void uploadCachePatterns(ServerPlayer player, WirelessComprehensiveWorkTerminalMenu menu, long providerId) {
        var host = menu.getMenuHost();
        if (host == null) {
            return;
        }
        var patternCache = host.getPatternCacheInventory();
        if (patternCache == null) {
            return;
        }

        var providers = listProviders(player);
        var provider = getProviderByOrdinal(providers, providerId);
        if (provider == null) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.provider_missing"), true);
            return;
        }
        logDebug("upload cache target providerId={}, targetName={}, cacheSize={}",
                providerId, getProviderDisplayName(provider), patternCache.size());
        int insertedTotal = 0;
        boolean foundPattern = false;
        for (int cacheSlot = 0; cacheSlot < patternCache.size(); cacheSlot++) {
            var pattern = patternCache.getStackInSlot(cacheSlot);
            if (pattern.isEmpty() || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(pattern)) {
                continue;
            }

            foundPattern = true;
            InsertResult result = insertPatternIntoProviderGroup(providers, provider, pattern);
            int inserted = result.inserted();
            logDebug("upload cache slot={}, metadata={}, inserted={}, remaining={}, targetName={}",
                    cacheSlot, PatternUploadMetadata.getProviderSearchText(pattern), inserted,
                    result.remaining().getCount(), getProviderDisplayName(provider));
            if (inserted > 0) {
                pattern.shrink(inserted);
                patternCache.setItemDirect(cacheSlot, pattern.isEmpty() ? ItemStack.EMPTY : pattern);
                insertedTotal += inserted;
            }
            if (!result.remaining().isEmpty()) {
                break;
            }
        }

        if (!foundPattern) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.no_pattern"), true);
        } else if (insertedTotal > 0) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.uploaded"), true);
        } else {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.upload_failed"), true);
        }
    }

    private static void openProviderUi(ServerPlayer player, long providerId) {
        var provider = getProviderByOrdinal(player, providerId);
        if (provider == null) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.provider_missing"), true);
            return;
        }

        Location location = getLocation(provider);
        if (location.pos == null || location.level == null) {
            player.displayClientMessage(Component.translatable("extendedae_plus.message.provider.location_missing"), true);
            return;
        }

        if (tryOpenNeighbor(player, location.level, location.pos, location.face)) {
            return;
        }
        if (provider instanceof MenuProvider menuProvider) {
            player.openMenu(menuProvider, location.pos);
            return;
        }
        var selfProvider = location.level.getBlockState(location.pos).getMenuProvider(location.level, location.pos);
        if (selfProvider != null) {
            player.openMenu(selfProvider, location.pos);
            return;
        }
        player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.open_ui_failed"), true);
    }

    private static boolean tryOpenNeighbor(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face) {
        if (face != null && tryOpenAt(player, level, pos.relative(face))) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (direction != face && tryOpenAt(player, level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryOpenAt(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider menuProvider) {
            player.openMenu(menuProvider, pos);
            return true;
        }
        var provider = level.getBlockState(pos).getMenuProvider(level, pos);
        if (provider != null) {
            player.openMenu(provider, pos);
            return true;
        }
        return false;
    }

    private static PatternContainer getProviderByOrdinal(ServerPlayer player, long providerId) {
        var providers = listProviders(player);
        return getProviderByOrdinal(providers, providerId);
    }

    private static PatternContainer getProviderByOrdinal(List<PatternContainer> providers, long providerId) {
        int index = (int) providerId - 1;
        if (index < 0 || index >= providers.size()) {
            return null;
        }
        return providers.get(index);
    }

    private static InsertResult insertPatternIntoProviderGroup(List<PatternContainer> providers,
                                                               PatternContainer targetProvider,
                                                               ItemStack pattern) {
        if (targetProvider == null || pattern.isEmpty()
                || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(pattern)) {
            return new InsertResult(0, pattern);
        }

        ItemStack remaining = PatternUploadMetadata.copyWithoutUploadData(pattern);
        int insertedTotal = 0;
        for (PatternContainer provider : buildSameNameTryList(providers, targetProvider)) {
            ItemStack beforeInsert = remaining.copy();
            ItemStack afterInsert = insertPatternIntoProvider(provider, beforeInsert);
            insertedTotal += remaining.getCount() - afterInsert.getCount();
            remaining = afterInsert;
            if (remaining.isEmpty()) {
                break;
            }
        }
        return new InsertResult(insertedTotal, remaining);
    }

    private static ItemStack insertPatternIntoProvider(PatternContainer provider, ItemStack pattern) {
        var inv = provider.getTerminalPatternInventory();
        if (inv == null || pattern.isEmpty()) {
            return pattern;
        }
        var filtered = new FilteredInternalInventory(inv, new EncodedPatternFilter());
        return filtered.addItems(pattern.copy());
    }

    private static List<PatternContainer> buildSameNameTryList(List<PatternContainer> providers,
                                                               PatternContainer targetProvider) {
        String targetName = getProviderDisplayName(targetProvider);
        var tryList = new ArrayList<PatternContainer>();
        tryList.add(targetProvider);
        for (var provider : providers) {
            if (provider == targetProvider) {
                continue;
            }
            String providerName = getProviderDisplayName(provider);
            if (targetName.equals(providerName)) {
                tryList.add(provider);
            }
        }
        return tryList;
    }

    private static List<PatternContainer> listProviders(ServerPlayer player) {
        if (!(player.containerMenu instanceof appeng.menu.AEBaseMenu baseMenu)) {
            return List.of();
        }
        var target = baseMenu.getTarget();
        if (!(target instanceof appeng.api.networking.security.IActionHost host) || host.getActionableNode() == null) {
            return List.of();
        }
        var grid = host.getActionableNode().getGrid();
        if (grid == null) {
            return List.of();
        }

        var providers = new ArrayList<PatternContainer>();
        for (var machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
            for (var container : grid.getActiveMachines(containerClass)) {
                if (container != null && container.isVisibleInTerminal()
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

    private static PatternContainer findMatchingProviderBySearchText(List<PatternContainer> providers, String searchText) {
        String resolvedSearchText = normalizeSearchText(searchText);
        logDebug("find matching provider searchText={}, resolvedSearchText={}", searchText, resolvedSearchText);
        if (resolvedSearchText == null || resolvedSearchText.isBlank()) {
            return null;
        }
        var matches = providers.stream()
                .filter(provider -> {
                    String displayName = getProviderDisplayName(provider);
                    return JecSearchCompat.contains(displayName, resolvedSearchText);
                })
                .map(PatternManagementActionPacket::getProviderDisplayName)
                .distinct()
                .toList();
        if (matches.size() != 1) {
            logDebug("find matching provider ambiguous matches={}", matches);
            return null;
        }
        String targetName = matches.get(0);
        for (var provider : providers) {
            if (targetName.equals(getProviderDisplayName(provider))) {
                return provider;
            }
        }
        return null;
    }

    private static void logDebug(String message, Object... args) {
        if (DEBUG_PATTERN_UPLOAD) {
            WcwtMod.LOGGER.info("WCWT pattern upload debug: " + message, args);
        }
    }

    private static String getProviderDisplayName(PatternContainer provider) {
        return provider.getTerminalGroup().name().getString();
    }

    private static Location getLocation(PatternContainer provider) {
        if (provider instanceof BlockEntity be && be.getLevel() instanceof ServerLevel level) {
            return new Location(level, be.getBlockPos(), null);
        }
        if (provider instanceof appeng.parts.AEBasePart part && part.getLevel() instanceof ServerLevel level) {
            return new Location(level, part.getBlockEntity().getBlockPos(), part.getSide());
        }
        return new Location(null, null, null);
    }

    private record Location(ServerLevel level, BlockPos pos, Direction face) {
    }

    private record InsertResult(int inserted, ItemStack remaining) {
    }

    public enum Action {
        UPLOAD_CACHE_SLOT,
        OPEN_PROVIDER_UI,
        EXCHANGE_PROVIDER_SLOT,
        QUICK_EXTRACT_PROVIDER_SLOT,
        QUICK_INSERT_FIRST_PROVIDER,
        ADD_MAPPING,
        RELOAD_MAPPING,
        DELETE_MAPPING
    }

    private static class EncodedPatternFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(stack);
        }
    }

}
