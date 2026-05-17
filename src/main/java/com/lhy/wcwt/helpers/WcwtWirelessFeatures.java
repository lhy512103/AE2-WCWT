package com.lhy.wcwt.helpers;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.integration.modules.curios.CuriosIntegration;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;
import com.google.common.collect.Maps;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.network.WcwtPickBlockPacket;
import com.lhy.wcwt.network.WcwtRestockAmountsPacket;
import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import de.mari_023.ae2wtlib.api.AE2wtlibTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class WcwtWirelessFeatures {
    private static final ResourceLocation MAGNET_CARD_ID =
            ResourceLocation.fromNamespaceAndPath("ae2wtlib", "magnet_card");
    private static final double DEFAULT_MAGNET_RANGE = 16.0;
    private static final WeakHashMap<ServerPlayer, Integer> RESTOCK_SYNC_TICKS = new WeakHashMap<>();

    private WcwtWirelessFeatures() {
    }

    public static void tickMagnet(ServerPlayer player, ItemStack terminal) {
        syncRestockAmounts(player, terminal);
        if (player.isShiftKeyDown() || !hasMagnetCard(terminal) || !getMagnetSetting(terminal, "magnet")) {
            return;
        }
        if (getGrid(player, terminal) == null) {
            return;
        }

        IPartitionList filter = createFilter(terminal, AE2wtlibComponents.PICKUP_CONFIG, player);
        IncludeExclude mode = terminal.getOrDefault(AE2wtlibComponents.PICKUP_MODE, IncludeExclude.BLACKLIST);
        if (filter.isEmpty() && mode == IncludeExclude.WHITELIST) {
            return;
        }

        List<ItemEntity> nearby = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(getMagnetRange()), EntitySelector.ENTITY_STILL_ALIVE);
        for (ItemEntity itemEntity : nearby) {
            AEItemKey key = AEItemKey.of(itemEntity.getItem());
            if (key != null
                    && filter.matchesFilter(key, mode)
                    && !itemEntity.getPersistentData().contains("PreventRemoteMovement")) {
                itemEntity.playerTouch(player);
            }
        }
    }

    private static void syncRestockAmounts(ServerPlayer player, ItemStack terminal) {
        boolean enabled = terminal.getOrDefault(AE2wtlibComponents.RESTOCK, false);
        int tick = player.getServer() == null ? 0 : player.getServer().getTickCount();
        Integer lastTick = RESTOCK_SYNC_TICKS.get(player);
        if (lastTick != null && tick - lastTick < 20) {
            return;
        }
        RESTOCK_SYNC_TICKS.put(player, tick);

        if (!enabled) {
            PacketDistributor.sendToPlayer(player, new WcwtRestockAmountsPacket(false, new HashMap<>()));
            return;
        }

        IGrid grid = getGrid(player, terminal);
        if (grid == null || grid.getStorageService() == null) {
            PacketDistributor.sendToPlayer(player, new WcwtRestockAmountsPacket(false, new HashMap<>()));
            return;
        }

        HashMap<Holder<Item>, Long> items = Maps.newHashMap();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.is(AE2wtlibTags.NO_RESTOCK) || stack.getMaxStackSize() == 1
                    || items.containsKey(stack.getItem().builtInRegistryHolder())) {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            long amount = key == null ? 0 : grid.getStorageService().getCachedInventory().get(key);
            items.put(stack.getItem().builtInRegistryHolder(), amount);
        }
        PacketDistributor.sendToPlayer(player, new WcwtRestockAmountsPacket(true, items));
    }

    public static void restock(ServerPlayer player, ItemStack item, ItemStack now, Consumer<ItemStack> setStack) {
        ItemStack terminal = findTerminalStack(player, stack -> stack.getOrDefault(AE2wtlibComponents.RESTOCK, false));
        if (terminal.isEmpty() || item.isEmpty() || item.is(AE2wtlibTags.NO_RESTOCK) || item.getMaxStackSize() == 1
                || player.isCreative()) {
            return;
        }

        IGrid grid = getGrid(player, terminal);
        if (grid == null || grid.getStorageService() == null) {
            return;
        }

        int count = now.getCount();
        int toAdd = item.getMaxStackSize() - count;
        if (toAdd == 0 || (!now.isEmpty() && !ItemStack.isSameItemSameComponents(item, now))) {
            return;
        }

        long changed;
        if (toAdd > 0) {
            changed = grid.getStorageService().getInventory()
                    .extract(AEItemKey.of(item), toAdd, appeng.api.config.Actionable.MODULATE, new PlayerSource(player));
        } else {
            changed = -grid.getStorageService().getInventory()
                    .insert(AEItemKey.of(item), -toAdd, appeng.api.config.Actionable.MODULATE, new PlayerSource(player));
        }
        if (changed == 0) {
            return;
        }

        item.setCount(count + (int) changed);
        setStack.accept(item);
    }

    public static boolean insertPickupIntoME(ItemEntity entity, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }

        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return false;
        }

        ItemStack terminal = findTerminalStack(serverPlayer, term -> canInsertPickup(term, stack, serverPlayer));
        if (terminal.isEmpty()) {
            return false;
        }

        IGrid grid = getGrid(serverPlayer, terminal);
        if (grid == null || grid.getStorageService() == null) {
            return false;
        }

        long inserted = grid.getStorageService().getInventory()
                .insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, new PlayerSource(serverPlayer));
        if (inserted <= 0) {
            return false;
        }

        serverPlayer.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), (int) inserted);
        serverPlayer.onItemPickup(entity);
        stack.setCount((int) (stack.getCount() - inserted));
        // If any amount was stored into the ME network, suppress vanilla pickup for this touch
        // so the same items don't also flow into the player's inventory.
        return true;
    }

    public static void pickBlock(ItemStack stack) {
        PacketDistributor.sendToServer(new WcwtPickBlockPacket(stack));
    }

    public static void pickBlock(ServerPlayer player, ItemStack requestedStack) {
        var terminalTarget = findTerminalTarget(player,
                stack -> stack.getOrDefault(AE2wtlibComponents.PICK_BLOCK, false));
        if (terminalTarget == null) {
            return;
        }

        ItemStack terminal = terminalTarget.stack();
        IGrid grid = getGrid(player, terminal);
        if (grid == null || grid.getStorageService() == null) {
            return;
        }

        var networkInventory = grid.getStorageService().getInventory();
        var playerSource = new PlayerSource(player);

        var inventory = player.getInventory();
        int targetSlot = inventory.getSuitableHotbarSlot();
        var toReplace = inventory.getItem(targetSlot);

        var insert = networkInventory.insert(AEItemKey.of(toReplace), toReplace.getCount(), Actionable.SIMULATE,
                playerSource);
        if (insert < toReplace.getCount()) {
            return;
        }

        int targetAmount = requestedStack.getMaxStackSize();
        var what = AEItemKey.of(requestedStack);
        if (what == null) {
            return;
        }

        var extracted = networkInventory.extract(what, targetAmount, Actionable.SIMULATE, playerSource);
        if (extracted == 0) {
            if (!terminal.getOrDefault(AE2wtlibComponents.CRAFT_IF_MISSING, false)
                    || grid.getCraftingService().getCraftingFor(what).isEmpty()) {
                return;
            }
            CraftAmountMenu.open(player, terminalTarget.locator(), what, 1);
            return;
        }

        insert = networkInventory.insert(AEItemKey.of(toReplace), toReplace.getCount(), Actionable.MODULATE,
                playerSource);
        if (insert < toReplace.getCount()) {
            toReplace.setCount(toReplace.getCount() - (int) insert);
            inventory.setItem(targetSlot, toReplace);
            return;
        }

        extracted = networkInventory.extract(what, targetAmount, Actionable.MODULATE, playerSource);
        if (extracted == 0) {
            inventory.setItem(targetSlot, ItemStack.EMPTY);
            return;
        }

        requestedStack.setCount((int) extracted);
        inventory.setItem(targetSlot, requestedStack);
        inventory.selected = targetSlot;
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean canInsertPickup(ItemStack terminal, ItemStack stack, ServerPlayer player) {
        if (terminal.getOrDefault(AE2wtlibComponents.RESTOCK, false) && isRestocking(stack, player)) {
            return true;
        }
        if (player.isShiftKeyDown() || !hasMagnetCard(terminal) || !getMagnetSetting(terminal, "pickupToME")) {
            return false;
        }

        IPartitionList filter = createFilter(terminal, AE2wtlibComponents.INSERT_CONFIG, player);
        IncludeExclude mode = terminal.getOrDefault(AE2wtlibComponents.INSERT_MODE, IncludeExclude.BLACKLIST);
        if (filter.isEmpty() && mode == IncludeExclude.WHITELIST) {
            return false;
        }

        AEItemKey key = AEItemKey.of(stack);
        return key != null && filter.matchesFilter(key, mode);
    }

    private static boolean isRestocking(ItemStack stack, Player player) {
        if (stack.is(AE2wtlibTags.NO_RESTOCK)) {
            return false;
        }
        if (stack.getMaxStackSize() == 1) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (ItemStack.isSameItemSameComponents(stack, player.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack findTerminalStack(ServerPlayer player, java.util.function.Predicate<ItemStack> predicate) {
        var target = findTerminalTarget(player, predicate);
        return target == null ? ItemStack.EMPTY : target.stack();
    }

    private static TerminalTarget findTerminalTarget(ServerPlayer player,
                                                     java.util.function.Predicate<ItemStack> predicate) {
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap != null) {
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack stack = cap.getStackInSlot(i);
                if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                    return new TerminalTarget(stack, MenuLocators.forCurioSlot(i));
                }
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                return new TerminalTarget(stack, MenuLocators.forInventorySlot(i));
            }
        }
        return null;
    }

    private static IGrid getGrid(ServerPlayer player, ItemStack terminal) {
        if (terminal.getItem() instanceof WirelessComprehensiveWorkTerminalItem item) {
            return item.getLinkedGrid(terminal, player.level(), null);
        }
        return null;
    }

    private static IPartitionList createFilter(ItemStack terminal,
                                               net.minecraft.core.component.DataComponentType<CompoundTag> component,
                                               Player player) {
        ConfigInventory config = ConfigInventory.configTypes(27)
                .supportedType(appeng.api.stacks.AEKeyType.items())
                .build();
        config.readFromChildTag(terminal.getOrDefault(component, new CompoundTag()), "", player.registryAccess());
        IPartitionList.Builder builder = IPartitionList.builder();
        builder.fuzzyMode(FuzzyMode.IGNORE_ALL);
        for (int i = 0; i < config.size(); i++) {
            builder.add(config.getKey(i));
        }
        return builder.build();
    }

    /** 终端物品是否已安装 ae2wtlib 磁力卡（用于 UI 显示「磁力过滤器」入口）。 */
    public static boolean hasMagnetCardInstalled(ItemStack terminal) {
        return hasMagnetCard(terminal);
    }

    private static boolean hasMagnetCard(ItemStack terminal) {
        Item card = BuiltInRegistries.ITEM.get(MAGNET_CARD_ID);
        return card != null
                && terminal.getItem() instanceof IUpgradeableItem upgradeable
                && upgradeable.getUpgrades(terminal).isInstalled(card);
    }

    private static double getMagnetRange() {
        try {
            Field configField = Class.forName("de.mari_023.ae2wtlib.AE2wtlibConfig").getField("CONFIG");
            Object config = configField.get(null);
            return ((Number) config.getClass().getMethod("magnetCardRange").invoke(config)).doubleValue();
        } catch (ReflectiveOperationException e) {
            return DEFAULT_MAGNET_RANGE;
        }
    }

    private static boolean getMagnetSetting(ItemStack terminal, String methodName) {
        try {
            Field componentField = Class.forName("de.mari_023.ae2wtlib.AE2wtlibAdditionalComponents")
                    .getField("MAGNET_SETTINGS");
            @SuppressWarnings("rawtypes")
            net.minecraft.core.component.DataComponentType component =
                    (net.minecraft.core.component.DataComponentType) componentField.get(null);
            Class<?> modeClass = Class.forName("de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object fallback = Enum.valueOf((Class<Enum>) modeClass.asSubclass(Enum.class), "OFF");
            Object mode = terminal.getOrDefault(component, fallback);
            Method method = modeClass.getMethod(methodName);
            return (boolean) method.invoke(mode);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private record TerminalTarget(ItemStack stack, ItemMenuHostLocator locator) {
        private TerminalTarget {
            Objects.requireNonNull(stack, "stack");
            Objects.requireNonNull(locator, "locator");
        }
    }
}
