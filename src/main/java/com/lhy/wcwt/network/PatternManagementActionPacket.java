package com.lhy.wcwt.network;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.filter.IAEItemFilter;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.JecSearchCompat;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.util.PatternUploadMetadata;
import com.lhy.wcwt.util.PatternProviderSorts;
import com.extendedae_plus.util.PatternProviderDataUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import com.lhy.wcwt.compat.minecraft.network.codec.ByteBufCodecs;
import com.lhy.wcwt.compat.minecraft.network.codec.StreamCodec;
import com.lhy.wcwt.compat.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record PatternManagementActionPacket(Action action,
                                            long providerId,
                                            int cacheSlot,
                                            String searchText,
                                            String mappingText,
                                            boolean shiftQuickEnabled,
                                            boolean hasProviderLocation,
                                            long providerPosLong,
                                            String providerDimensionId,
                                            int providerFaceOrdinal) implements CustomPacketPayload {
    private static final boolean DEBUG_PROVIDER_UI = Boolean.getBoolean("wcwt.debug.providerUi");

    public static final Type<PatternManagementActionPacket> TYPE =
            new Type<>(com.lhy.wcwt.util.ResourceLocationCompat.id(WcwtMod.MOD_ID, "pattern_management_action"));

    public static final StreamCodec<ByteBuf, PatternManagementActionPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal).encode(buf, packet.action());
                ByteBufCodecs.VAR_LONG.encode(buf, packet.providerId());
                ByteBufCodecs.VAR_INT.encode(buf, packet.cacheSlot());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.searchText());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.mappingText());
                ByteBufCodecs.BOOL.encode(buf, packet.shiftQuickEnabled());
                ByteBufCodecs.BOOL.encode(buf, packet.hasProviderLocation());
                ByteBufCodecs.VAR_LONG.encode(buf, packet.providerPosLong());
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.providerDimensionId());
                ByteBufCodecs.VAR_INT.encode(buf, packet.providerFaceOrdinal());
            },
            buf -> new PatternManagementActionPacket(
                    ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal).decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

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
                    case OPEN_PROVIDER_UI -> {
                        openProviderUi(player, packet);
                        return;
                    }
                    case EXCHANGE_PROVIDER_SLOT -> exchangeProviderSlot(player, packet.providerId, packet.cacheSlot);
                    case QUICK_EXTRACT_PROVIDER_SLOT -> {
                        if (packet.shiftQuickEnabled()) {
                            quickExtractProviderSlot(player, packet.providerId, packet.cacheSlot);
                        }
                    }
                    case QUICK_INSERT_FIRST_PROVIDER -> {
                        if (packet.shiftQuickEnabled()) {
                            quickInsertEncodedPattern(player, packet.providerId, packet.cacheSlot);
                        }
                    }
                    case ADD_MAPPING, RELOAD_MAPPING, DELETE_MAPPING -> {
                        return;
                    }
                }
                ModNetworking.sendToPlayer(player, PatternProviderListPacket.buildForPlayer(player));
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
            provider = getProviderByOrdinal(providers, preferredProviderId);
        }
        String patternSearchText = PatternUploadMetadata.getProviderSearchText(sourceStack);
        if (provider == null && patternSearchText != null) {
            provider = findMatchingProviderBySearchText(providers, patternSearchText);
        }
        if (provider == null) {
            provider = providers.get(0);
        }
        var inv = provider.getTerminalPatternInventory();
        if (inv == null) {
            return;
        }

        ItemStack toInsert = PatternUploadMetadata.copyWithoutUploadData(sourceStack);
        int inserted = insertEncodedPatternsOnePerSlot(inv, toInsert);
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
        int insertedTotal = 0;
        boolean foundPattern = false;
        for (int cacheSlot = 0; cacheSlot < patternCache.size(); cacheSlot++) {
            var pattern = patternCache.getStackInSlot(cacheSlot);
            if (pattern.isEmpty() || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(pattern)) {
                continue;
            }

            foundPattern = true;
            PatternContainer targetProvider = provider;
            String patternSearchText = PatternUploadMetadata.getProviderSearchText(pattern);
            if (patternSearchText != null) {
                targetProvider = findMatchingProviderBySearchText(providers, patternSearchText);
            }
            if (targetProvider == null) {
                targetProvider = provider;
            }
            var inv = targetProvider.getTerminalPatternInventory();
            if (inv == null) {
                continue;
            }
            ItemStack toInsert = PatternUploadMetadata.copyWithoutUploadData(pattern);
            int inserted = insertEncodedPatternsOnePerSlot(inv, toInsert);
            if (inserted > 0) {
                pattern.shrink(inserted);
                patternCache.setItemDirect(cacheSlot, pattern.isEmpty() ? ItemStack.EMPTY : pattern);
                insertedTotal += inserted;
            }
            if (inserted < toInsert.getCount()) {
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

    private static void openProviderUi(ServerPlayer player, PatternManagementActionPacket packet) {
        Location location = resolveProviderLocation(player, packet);
        logProviderUi(
                "WCWT provider ui debug: player={}, providerId={}, hasPacketLocation={}, resolvedLevel={}, resolvedPos={}, resolvedFace={}",
                player.getScoreboardName(),
                packet.providerId(),
                packet.hasProviderLocation(),
                location.level != null ? location.level.dimension().location() : "null",
                location.pos,
                location.face);
        if (location.pos == null || location.level == null) {
            player.displayClientMessage(Component.translatable("extendedae_plus.message.provider.location_missing"), true);
            return;
        }

        if (tryOpenProviderTargetUi(player, location.level, location.pos, location.face)) {
            return;
        }
        player.displayClientMessage(Component.translatable("gui.wcwt.pattern_management.open_ui_failed"), true);
    }

    private static Location resolveProviderLocation(ServerPlayer player, PatternManagementActionPacket packet) {
        if (packet.hasProviderLocation()) {
            ResourceLocation dimId = ResourceLocation.tryParse(packet.providerDimensionId());
            if (dimId != null) {
                ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimId);
                ServerLevel level = player.server.getLevel(levelKey);
                if (level != null) {
                    Direction face = packet.providerFaceOrdinal() >= 0
                            && packet.providerFaceOrdinal() < Direction.values().length
                                    ? Direction.values()[packet.providerFaceOrdinal()]
                                    : null;
                    return new Location(level, BlockPos.of(packet.providerPosLong()), face);
                }
                logProviderUi(
                        "WCWT provider ui debug: packet dimension not loaded player={}, providerId={}, dim={}",
                        player.getScoreboardName(),
                        packet.providerId(),
                        dimId);
            }
            logProviderUi(
                    "WCWT provider ui debug: packet location parse result player={}, providerId={}, dimId={}, posLong={}, faceOrdinal={}",
                    player.getScoreboardName(),
                    packet.providerId(),
                    packet.providerDimensionId(),
                    packet.providerPosLong(),
                    packet.providerFaceOrdinal());
        }

        var provider = getProviderByOrdinal(player, packet.providerId);
        if (provider == null) {
            logProviderUi("WCWT provider ui debug: provider missing player={}, providerId={}",
                    player.getScoreboardName(), packet.providerId());
            return new Location(null, null, null);
        }
        logProviderUi("WCWT provider ui debug: fallback provider class={} for player={}, providerId={}",
                provider.getClass().getName(), player.getScoreboardName(), packet.providerId());
        return getLocation(provider);
    }

    private static boolean tryOpenProviderTargetUi(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face) {
        if (!level.isLoaded(pos)) {
            logProviderUi("WCWT provider ui debug: provider pos not loaded player={}, level={}, pos={}",
                    player.getScoreboardName(), level.dimension().location(), pos);
            return false;
        }
        if (face != null) {
            BlockPos targetPos = pos.relative(face);
            return tryOpenAt(player, level, targetPos) || tryUseTargetBlock(player, level, targetPos, face);
        }
        for (Direction direction : Direction.values()) {
            if (tryOpenAt(player, level, pos.relative(direction))) {
                return true;
            }
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) != null
                    && tryUseTargetBlock(player, level, pos.relative(direction), direction)) {
                return true;
            }
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) == null
                    && !level.getBlockState(pos.relative(direction)).isAir()
                    && tryUseTargetBlock(player, level, pos.relative(direction), direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryOpenAt(ServerPlayer player, ServerLevel level, BlockPos targetPos) {
        if (!level.isLoaded(targetPos)) {
            logProviderUi("WCWT provider ui debug: target pos not loaded player={}, level={}, pos={}",
                    player.getScoreboardName(), level.dimension().location(), targetPos);
            return false;
        }
        BlockEntity be = level.getBlockEntity(targetPos);
        var state = level.getBlockState(targetPos);
        logProviderUi(
                "WCWT provider ui debug: try target player={}, level={}, pos={}, block={}, blockEntity={}, beIsMenuProvider={}",
                player.getScoreboardName(),
                level.dimension().location(),
                targetPos,
                state.getBlock().getDescriptionId(),
                be != null ? be.getClass().getName() : "null",
                be instanceof MenuProvider);
        if (be instanceof MenuProvider menuProvider) {
            NetworkHooks.openScreen(player, menuProvider, targetPos);
            logProviderUi("WCWT provider ui debug: opened via block entity menu provider at {}", targetPos);
            return true;
        }
        var provider = state.getMenuProvider(level, targetPos);
        if (provider != null) {
            NetworkHooks.openScreen(player, provider, targetPos);
            logProviderUi("WCWT provider ui debug: opened via block state menu provider at {}", targetPos);
            return true;
        }
        logProviderUi("WCWT provider ui debug: no menu provider at {}", targetPos);
        return false;
    }

    private static boolean tryUseTargetBlock(ServerPlayer player, ServerLevel level, BlockPos targetPos, Direction providerToTarget) {
        if (!level.isLoaded(targetPos)) {
            logProviderUi("WCWT provider ui debug: use target pos not loaded player={}, level={}, pos={}",
                    player.getScoreboardName(), level.dimension().location(), targetPos);
            return false;
        }
        InteractionHand hand = player.getMainHandItem().isEmpty()
                ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            logProviderUi("WCWT provider ui debug: skip block use because both hands are occupied player={}, pos={}",
                    player.getScoreboardName(), targetPos);
            return false;
        }
        var state = level.getBlockState(targetPos);
        var hit = new BlockHitResult(Vec3.atCenterOf(targetPos), providerToTarget.getOpposite(), targetPos, false);
        if (state.getBlock() instanceof appeng.block.AEBaseEntityBlock<?> aeBlock) {
            InteractionResult result = aeBlock.onActivated(level, targetPos, player, hand, ItemStack.EMPTY, hit);
            logProviderUi("WCWT provider ui debug: activated AE target block player={}, pos={}, block={}, hand={}, result={}",
                    player.getScoreboardName(),
                    targetPos,
                    state.getBlock().getDescriptionId(),
                    hand,
                    result);
            return result.consumesAction();
        }
        InteractionResult result = state.use(level, player, hand, hit);
        logProviderUi("WCWT provider ui debug: used target block player={}, pos={}, block={}, hand={}, result={}",
                player.getScoreboardName(),
                targetPos,
                state.getBlock().getDescriptionId(),
                hand,
                result);
        return result.consumesAction();
    }

    private static void logProviderUi(String message, Object... args) {
        if (DEBUG_PROVIDER_UI) {
            WcwtMod.LOGGER.info(message, args);
        }
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
        if (searchText == null || searchText.isBlank()) {
            return null;
        }
        var matches = providers.stream()
                .filter(provider -> {
                    String displayName = getProviderDisplayName(provider);
                    return JecSearchCompat.contains(displayName, searchText);
                })
                .map(PatternManagementActionPacket::getProviderDisplayName)
                .distinct()
                .toList();
        if (matches.size() != 1) {
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

    private static String getProviderDisplayName(PatternContainer provider) {
        String bridgeName = ExtendedAePlusBridge.getProviderDisplayName(provider);
        if (bridgeName != null && !bridgeName.isBlank()) {
            return bridgeName;
        }
        return provider.getTerminalGroup().name().getString();
    }

    private static int insertEncodedPatternsOnePerSlot(InternalInventory inv, ItemStack stack) {
        if (stack.isEmpty() || !appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(stack)) {
            return 0;
        }
        int inserted = 0;
        for (int slot = 0; slot < inv.size() && inserted < stack.getCount(); slot++) {
            if (!inv.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            ItemStack single = stack.copy();
            single.setCount(1);
            if (!new EncodedPatternFilter().allowInsert(inv, slot, single)) {
                continue;
            }
            ItemStack remainder = inv.insertItem(slot, single, false);
            if (remainder.isEmpty()) {
                inserted++;
            }
        }
        return inserted;
    }

    private static Location getLocation(PatternContainer provider) {
        if (provider instanceof appeng.helpers.patternprovider.PatternProviderLogicHost host) {
            BlockEntity be = host.getBlockEntity();
            if (be != null && be.getLevel() instanceof ServerLevel level) {
                return new Location(level, be.getBlockPos(), getSingleTarget(host));
            }
        }
        if (provider instanceof BlockEntity be && be.getLevel() instanceof ServerLevel level) {
            return new Location(level, be.getBlockPos(), null);
        }
        if (provider instanceof appeng.parts.AEBasePart part && part.getLevel() instanceof ServerLevel level) {
            return new Location(level, part.getBlockEntity().getBlockPos(), part.getSide());
        }
        return new Location(null, null, null);
    }

    private static Direction getSingleTarget(appeng.helpers.patternprovider.PatternProviderLogicHost host) {
        var targets = host.getTargets();
        return targets != null && targets.size() == 1 ? targets.iterator().next() : null;
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
        static String getProviderDisplayName(PatternContainer provider) {
            return ModList.get().isLoaded("extendedae_plus")
                    ? PatternProviderDataUtil.getProviderDisplayName(provider)
                    : null;
        }
    }
}

