package com.lhy.wcwt.network;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtServerConfig;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
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
import java.util.Comparator;
import java.util.List;

public record PatternManagementActionPacket(Action action,
                                            long providerId,
                                            int cacheSlot,
                                            String searchText,
                                            String mappingText) implements CustomPacketPayload {
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
            PatternManagementActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternManagementActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu) {
                switch (packet.action) {
                    case UPLOAD_CACHE_SLOT -> uploadCachePatterns(player, menu, packet.providerId);
                    case OPEN_PROVIDER_UI -> openProviderUi(player, packet.providerId);
                    case EXCHANGE_PROVIDER_SLOT -> exchangeProviderSlot(player, packet.providerId, packet.cacheSlot);
                    case QUICK_EXTRACT_PROVIDER_SLOT -> {
                        if (WcwtServerConfig.patternManagementShiftQuickEnabled()) {
                            quickExtractProviderSlot(player, packet.providerId, packet.cacheSlot);
                        }
                    }
                    case QUICK_INSERT_FIRST_PROVIDER -> {
                        if (WcwtServerConfig.patternManagementShiftQuickEnabled()) {
                            quickInsertEncodedPattern(player, packet.providerId, packet.cacheSlot);
                        }
                    }
                    case ADD_MAPPING -> {
                        boolean ok = ExtendedAePlusBridge.addOrUpdateAliasMapping(packet.searchText, packet.mappingText);
                        player.displayClientMessage(Component.translatable(ok
                                ? "gui.wcwt.pattern_management.mapping_added"
                                : "gui.wcwt.pattern_management.mapping_failed"), true);
                    }
                    case RELOAD_MAPPING -> {
                        ExtendedAePlusBridge.loadRecipeTypeNames();
                        player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.mapping_reloaded"), true);
                    }
                    case DELETE_MAPPING -> {
                        int removed = ExtendedAePlusBridge.removeMappingsByCnValue(packet.mappingText);
                        player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.mapping_deleted", removed), true);
                    }
                }
                PacketDistributor.sendToPlayer(player, PatternProviderListPacket.buildForPlayer(player));
            }
        });
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
    private static void quickInsertEncodedPattern(ServerPlayer player, long preferredProviderId, int playerSlotIndex) {
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
            provider = getProviderByOrdinal(player, preferredProviderId);
        }
        if (provider == null) {
            provider = providers.get(0);
        }
        var inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return;
        }

        var filtered = new FilteredInternalInventory(inv, new EncodedPatternFilter());
        ItemStack toInsert = sourceStack.copy();
        ItemStack remain = filtered.addItems(toInsert);
        int inserted = toInsert.getCount() - remain.getCount();
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

        var provider = getProviderByOrdinal(player, providerId);
        if (provider == null) {
            player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.provider_missing"), true);
            return;
        }
        var inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return;
        }
        var filtered = new FilteredInternalInventory(inv, new EncodedPatternFilter());
        int insertedTotal = 0;
        boolean foundPattern = false;
        for (int cacheSlot = 0; cacheSlot < patternCache.size(); cacheSlot++) {
            var pattern = patternCache.getStackInSlot(cacheSlot);
            if (pattern.isEmpty() || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(pattern)) {
                continue;
            }

            foundPattern = true;
            ItemStack toInsert = pattern.copy();
            ItemStack remain = filtered.addItems(toInsert);
            int inserted = toInsert.getCount() - remain.getCount();
            if (inserted > 0) {
                pattern.shrink(inserted);
                patternCache.setItemDirect(cacheSlot, pattern.isEmpty() ? ItemStack.EMPTY : pattern);
                insertedTotal += inserted;
            }
            if (!remain.isEmpty()) {
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
        int index = (int) providerId - 1;
        if (index < 0 || index >= providers.size()) {
            return null;
        }
        return providers.get(index);
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

    private static final class ExtendedAePlusBridge {
        private static final String UTIL_CLASS = "com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil";

        static boolean addOrUpdateAliasMapping(String aliasKey, String cnValue) {
            try {
                var method = Class.forName(UTIL_CLASS).getMethod("addOrUpdateAliasMapping", String.class, String.class);
                Object result = method.invoke(null, aliasKey, cnValue);
                return result instanceof Boolean ok && ok;
            } catch (Throwable ignored) {
                return false;
            }
        }

        static void loadRecipeTypeNames() {
            try {
                Class.forName(UTIL_CLASS).getMethod("loadRecipeTypeNames").invoke(null);
            } catch (Throwable ignored) {
            }
        }

        static int removeMappingsByCnValue(String cnValue) {
            try {
                var method = Class.forName(UTIL_CLASS).getMethod("removeMappingsByCnValue", String.class);
                Object result = method.invoke(null, cnValue);
                return result instanceof Integer count ? count : 0;
            } catch (Throwable ignored) {
                return 0;
            }
        }
    }
}
