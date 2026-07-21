package com.lhy.wcwt.network;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.client.WirelessComprehensiveWorkTerminalScreen;
import com.lhy.wcwt.util.PatternProviderSorts;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record PatternProviderListPacket(List<Entry> entries, String resolvedSearchText) implements CustomPacketPayload {
    private static final boolean DEBUG_PERF = Boolean.getBoolean("wcwt.debug.perf");

    public static final Type<PatternProviderListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_provider_list"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Int2ObjectMap<ItemStack>> SLOTS_CODEC =
            ByteBufCodecs.map(Int2ObjectArrayMap::new, ByteBufCodecs.SHORT.map(Short::intValue, Integer::shortValue),
                    ItemStack.OPTIONAL_STREAM_CODEC, 512);

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternProviderListPacket> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PatternProviderListPacket::entries,
            ByteBufCodecs.STRING_UTF8,
            PatternProviderListPacket::resolvedSearchText,
            PatternProviderListPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternProviderListPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> handleClient(packet));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PatternProviderListPacket packet) {
        if (Minecraft.getInstance().screen instanceof WirelessComprehensiveWorkTerminalScreen screen) {
            screen.updatePatternProviders(packet.entries(), packet.resolvedSearchText());
        }
    }

    public record Entry(long providerId,
                        PatternContainerGroup group,
                        String mappedDisplayName,
                        int inventorySize,
                        Int2ObjectMap<ItemStack> slots,
                        @Nullable BlockPos pos,
                        @Nullable ResourceKey<Level> dimension,
                        @Nullable Direction face) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buf, entry) -> entry.write(buf),
                Entry::read);

        private static Entry read(RegistryFriendlyByteBuf buf) {
            long providerId = buf.readVarLong();
            PatternContainerGroup group = PatternContainerGroup.readFromPacket(buf);
            String mappedDisplayName = buf.readUtf();
            int inventorySize = buf.readVarInt();
            var slots = SLOTS_CODEC.decode(buf);
            BlockPos pos = buf.readBoolean() ? BlockPos.of(buf.readLong()) : null;
            ResourceKey<Level> dimension = buf.readBoolean() ? buf.readResourceKey(Registries.DIMENSION) : null;
            Direction face = buf.readBoolean() ? buf.readEnum(Direction.class) : null;
            return new Entry(providerId, group, mappedDisplayName, inventorySize, slots, pos, dimension, face);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarLong(providerId);
            group.writeToPacket(buf);
            buf.writeUtf(mappedDisplayName);
            buf.writeVarInt(inventorySize);
            SLOTS_CODEC.encode(buf, slots);
            buf.writeBoolean(pos != null);
            if (pos != null) {
                buf.writeLong(pos.asLong());
            }
            buf.writeBoolean(dimension != null);
            if (dimension != null) {
                buf.writeResourceKey(dimension);
            }
            buf.writeBoolean(face != null);
            if (face != null) {
                buf.writeEnum(face);
            }
        }
    }

    public record Request(boolean subscribe, String searchText) implements CustomPacketPayload {
        public static final Type<Request> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "pattern_provider_list_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                Request::subscribe,
                ByteBufCodecs.STRING_UTF8,
                Request::searchText,
                Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Request packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    if (player.containerMenu instanceof com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu menu) {
                        boolean serveNow = menu.shouldServeImmediatePatternProviderRequest();
                        menu.requestPatternProviderSyncSubscription(packet.subscribe());
                        if (!serveNow) {
                            return;
                        }
                    }
                    PacketDistributor.sendToPlayer(player, buildForPlayer(player, packet.searchText));
                }
            });
        }
    }

    public static PatternProviderListPacket buildForPlayer(ServerPlayer player) {
        return buildForPlayer(player, "");
    }

    public static PatternProviderListPacket buildForPlayer(ServerPlayer player, String searchText) {
        long totalStartNs = DEBUG_PERF ? System.nanoTime() : 0L;
        String resolvedSearchText = normalizeSearchText(searchText);
        var entries = new ArrayList<Entry>();
        var menu = player.containerMenu;
        if (!(menu instanceof appeng.menu.AEBaseMenu baseMenu)) {
            return new PatternProviderListPacket(List.of(), resolvedSearchText);
        }
        var target = baseMenu.getTarget();
        if (!(target instanceof appeng.api.networking.security.IActionHost host) || host.getActionableNode() == null) {
            return new PatternProviderListPacket(List.of(), resolvedSearchText);
        }
        var grid = host.getActionableNode().getGrid();
        if (grid == null) {
            return new PatternProviderListPacket(List.of(), resolvedSearchText);
        }

        var providers = new ArrayList<PatternContainer>();
        int machineClassCount = 0;
        for (var machineClass : grid.getMachineClasses()) {
            machineClassCount++;
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
            for (var container : grid.getActiveMachines(containerClass)) {
                if (container == null || !container.isVisibleInTerminal()) {
                    continue;
                }
                InternalInventory inv = container.getTerminalPatternInventory();
                if (inv == null || inv.size() <= 0) {
                    continue;
                }
                var group = container.getTerminalGroup();
                if (group == null) {
                    continue;
                }
                providers.add(container);
            }
        }

        providers.sort(PatternProviderSorts.STABLE);
        long id = 1;
        int copiedNonEmptySlots = 0;
        int totalInventorySlots = 0;
        for (var container : providers) {
            InternalInventory inv = container.getTerminalPatternInventory();
            totalInventorySlots += inv.size();
            var slots = new Int2ObjectArrayMap<ItemStack>();
            for (int i = 0; i < inv.size(); i++) {
                var stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    slots.put(i, stack.copy());
                    copiedNonEmptySlots++;
                }
            }
            var location = getLocation(container);
            entries.add(new Entry(id++, container.getTerminalGroup(), getMappedProviderDisplayName(container), inv.size(), slots,
                    location.pos, location.dimension, location.face));
        }

        if (DEBUG_PERF) {
            long totalNs = System.nanoTime() - totalStartNs;
            WcwtMod.LOGGER.info(
                    "WCWT perf: providerList build player={}, totalMs={}, machineClasses={}, providers={}, inventorySlots={}, copiedStacks={}",
                    player.getScoreboardName(),
                    String.format(java.util.Locale.ROOT, "%.3f", totalNs / 1_000_000.0D),
                    machineClassCount,
                    providers.size(),
                    totalInventorySlots,
                    copiedNonEmptySlots);
        }

        return new PatternProviderListPacket(entries, resolvedSearchText);
    }

    private static Location getLocation(PatternContainer container) {
        if (container instanceof appeng.helpers.patternprovider.PatternProviderLogicHost host) {
            BlockEntity be = host.getBlockEntity();
            if (be != null && be.getLevel() != null) {
                return new Location(be.getBlockPos(), be.getLevel().dimension(), getSingleTarget(host));
            }
        }
        if (container instanceof BlockEntity be && be.getLevel() != null) {
            return new Location(be.getBlockPos(), be.getLevel().dimension(), null);
        }
        if (container instanceof appeng.parts.AEBasePart part && part.getLevel() != null) {
            return new Location(part.getBlockEntity().getBlockPos(), part.getLevel().dimension(), part.getSide());
        }
        return new Location(null, null, null);
    }

    @Nullable
    private static Direction getSingleTarget(appeng.helpers.patternprovider.PatternProviderLogicHost host) {
        var targets = host.getTargets();
        return targets != null && targets.size() == 1 ? targets.iterator().next() : null;
    }

    private static String getMappedProviderDisplayName(PatternContainer provider) {
        return "";
    }

    private static String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return "";
        }
        String trimmed = searchText.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private record Location(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dimension, @Nullable Direction face) {
    }
}
