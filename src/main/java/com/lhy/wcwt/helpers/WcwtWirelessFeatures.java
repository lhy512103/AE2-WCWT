package com.lhy.wcwt.helpers;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;
import com.google.common.collect.Maps;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.init.ModComponents;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.network.ModNetworking;
import com.lhy.wcwt.network.WcwtPickBlockPacket;
import com.lhy.wcwt.network.WcwtRestockAmountsPacket;
import com.lhy.wcwt.network.WcwtUpdateRestockPacket;
import de.mari_023.ae2wtlib.curio.CurioLocator;
import de.mari_023.ae2wtlib.api.AE2wtlibTags;
import de.mari_023.ae2wtlib.api.TextConstants;
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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class WcwtWirelessFeatures {
    private static final ResourceLocation MAGNET_CARD_ID =
            com.lhy.wcwt.util.ResourceLocationCompat.id("ae2wtlib", "magnet_card");
    private static final double DEFAULT_MAGNET_RANGE = 16.0;
    private static final boolean DEBUG_MAGNET = Boolean.getBoolean("wcwt.debug.magnet");
    private static final WeakHashMap<ServerPlayer, Integer> RESTOCK_SYNC_TICKS = new WeakHashMap<>();
    private static final WeakHashMap<ServerPlayer, Integer> MAGNET_DEBUG_TICKS = new WeakHashMap<>();

    private WcwtWirelessFeatures() {
    }

    public static void tickPlayerMagnet(ServerPlayer player) {
        TerminalTarget terminalTarget = findTerminalTarget(player,
                stack -> hasMagnetCard(stack) || getBoolean(stack, "restock"));
        if (terminalTarget != null) {
            tickMagnet(player, terminalTarget);
        }
    }

    public static void tickMagnet(ServerPlayer player, ItemStack terminal) {
        tickMagnet(player, new TerminalTarget(terminal, null, null));
    }

    private static void tickMagnet(ServerPlayer player, TerminalTarget terminalTarget) {
        ItemStack terminal = terminalTarget.stack();
        syncRestockAmounts(player, terminal);
        boolean sneaking = player.isShiftKeyDown();
        boolean hasMagnetCard = hasMagnetCard(terminal);
        boolean magnetEnabled = getMagnetSetting(terminal, "magnet");
        boolean pickupToMeEnabled = getMagnetSetting(terminal, "pickupToME");
        IGrid grid = getGrid(player, terminalTarget);
        debugMagnetTick(player, terminal, sneaking, hasMagnetCard, magnetEnabled, pickupToMeEnabled, grid);
        if (sneaking || !hasMagnetCard || !magnetEnabled) {
            return;
        }
        if (grid == null) {
            return;
        }

        IPartitionList filter = createFilter(terminal, "pickup_config", player);
        IncludeExclude mode = getIncludeExclude(terminal, "pickup_mode_whitelist");

        List<ItemEntity> nearby = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(getMagnetRange()), EntitySelector.ENTITY_STILL_ALIVE);
        debugMagnet(player, "magnet pull scan: nearby={}, pickupMode={}, range={}",
                nearby.size(), mode, getMagnetRange());
        for (ItemEntity itemEntity : nearby) {
            ItemStack entityStack = itemEntity.getItem();
            AEItemKey key = AEItemKey.of(entityStack);
            boolean matchesFilter = matchesFilter(key, filter, mode);
            boolean prevented = itemEntity.getPersistentData().contains("PreventRemoteMovement");
            debugMagnet(player, "magnet pull candidate: stack={}, key={}, matchesFilter={}, prevented={}",
                    describeStack(entityStack), key, matchesFilter, prevented);
            if (key != null
                    && matchesFilter
                    && !prevented) {
                itemEntity.playerTouch(player);
            }
        }
    }

    private static void syncRestockAmounts(ServerPlayer player, ItemStack terminal) {
        boolean enabled = getBoolean(terminal, "restock");
        int tick = player.getServer() == null ? 0 : player.getServer().getTickCount();
        Integer lastTick = RESTOCK_SYNC_TICKS.get(player);
        if (lastTick != null && tick - lastTick < 20) {
            return;
        }
        RESTOCK_SYNC_TICKS.put(player, tick);

        if (!enabled) {
            ModNetworking.sendToPlayer(player, new WcwtRestockAmountsPacket(false, new HashMap<>()));
            return;
        }

        IGrid grid = getGrid(player, terminal);
        if (grid == null || grid.getStorageService() == null) {
            ModNetworking.sendToPlayer(player, new WcwtRestockAmountsPacket(false, new HashMap<>()));
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
        ModNetworking.sendToPlayer(player, new WcwtRestockAmountsPacket(true, items));
    }

    public static void restock(ServerPlayer player, ItemStack item, ItemStack now, Consumer<ItemStack> setStack) {
        TerminalTarget terminalTarget = findTerminalTarget(player, stack -> getBoolean(stack, "restock"));
        if (terminalTarget == null || item.isEmpty() || item.is(AE2wtlibTags.NO_RESTOCK) || item.getMaxStackSize() == 1
                || player.isCreative()) {
            return;
        }

        IGrid grid = getGrid(player, terminalTarget);
        if (grid == null || grid.getStorageService() == null) {
            return;
        }

        int count = now.getCount();
        int toAdd = Math.max(item.getMaxStackSize() / 2, 1) - count;
        if (toAdd == 0 || (!now.isEmpty() && !sameItemAndTag(item, now))) {
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
        ModNetworking.sendToPlayer(player, new WcwtUpdateRestockPacket(
                player.getInventory().findSlotMatchingUnusedItem(item), item.getCount()));
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    public static boolean insertPickupIntoME(ItemEntity entity, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }

        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            debugMagnet(serverPlayer, "pickup-to-me skipped: entity stack empty");
            return false;
        }

        TerminalTarget terminalTarget = findTerminalTarget(serverPlayer,
                term -> canInsertPickup(term, stack, serverPlayer));
        if (terminalTarget == null) {
            debugMagnet(serverPlayer, "pickup-to-me skipped: no eligible terminal for stack={}", describeStack(stack));
            return false;
        }
        ItemStack terminal = terminalTarget.stack();

        IGrid grid = getGrid(serverPlayer, terminalTarget);
        if (grid == null || grid.getStorageService() == null) {
            debugMagnet(serverPlayer, "pickup-to-me skipped: grid/storage missing, stack={}, terminal={}",
                    describeStack(stack), describeStack(terminal));
            return false;
        }

        long inserted = grid.getStorageService().getInventory()
                .insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, new PlayerSource(serverPlayer));
        if (inserted <= 0) {
            debugMagnet(serverPlayer, "pickup-to-me insert failed: stack={}, terminal={}",
                    describeStack(stack), describeStack(terminal));
            return false;
        }

        debugMagnet(serverPlayer, "pickup-to-me inserted: inserted={}, beforeStack={}, terminal={}",
                inserted, describeStack(stack), describeStack(terminal));
        serverPlayer.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), (int) inserted);
        serverPlayer.onItemPickup(entity);
        stack.setCount((int) (stack.getCount() - inserted));
        // If any amount was stored into the ME network, suppress vanilla pickup for this touch
        // so the same items don't also flow into the player's inventory.
        return true;
    }

    public static void pickBlock(ItemStack stack) {
        ModNetworking.sendToServer(new WcwtPickBlockPacket(stack));
    }

    public static boolean hasPickBlockTerminal(Player player) {
        return findTerminalStack(player, stack -> getBoolean(stack, "pick_block")).getItem()
                instanceof WirelessComprehensiveWorkTerminalItem;
    }

    public static boolean toggleMagnetHotkey(Player player) {
        TerminalTarget terminalTarget = findHotkeyTerminalTarget(player, WcwtWirelessFeatures::hasMagnetCard);
        if (terminalTarget == null || terminalTarget.stack().isEmpty()) {
            return false;
        }
        ItemStack terminal = terminalTarget.stack();

        String nextMode = switch (getMagnetModeName(terminal)) {
            case "OFF" -> {
                player.displayClientMessage(TextConstants.HOTKEY_MAGNETCARD_INVENTORY, true);
                yield "PICKUP_INVENTORY";
            }
            case "PICKUP_INVENTORY" -> {
                player.displayClientMessage(TextConstants.HOTKEY_MAGNETCARD_ME, true);
                yield "PICKUP_ME";
            }
            case "PICKUP_ME" -> {
                player.displayClientMessage(TextConstants.PICKUP_ME_NO_MAGNET, true);
                yield "PICKUP_ME_NO_MAGNET";
            }
            case "PICKUP_ME_NO_MAGNET" -> {
                player.displayClientMessage(TextConstants.HOTKEY_MAGNETCARD_OFF, true);
                yield "OFF";
            }
            default -> null;
        };

        return nextMode != null && setMagnetMode(terminal, nextMode);
    }

    public static boolean toggleRestockHotkey(Player player) {
        TerminalTarget terminalTarget = findHotkeyTerminalTarget(player, stack -> true);
        if (terminalTarget == null || terminalTarget.stack().isEmpty()) {
            return false;
        }

        ItemStack terminal = terminalTarget.stack();
        boolean enabled = !getBoolean(terminal, "restock");
        getRootTag(terminal).putBoolean("restock", enabled);
        player.displayClientMessage(enabled ? TextConstants.RESTOCK_ON : TextConstants.RESTOCK_OFF, true);
        return true;
    }

    public static void pickBlock(ServerPlayer player, ItemStack requestedStack) {
        if (requestedStack.isEmpty() || player.getInventory().findSlotMatchingItem(requestedStack) != -1) {
            return;
        }
        var terminalTarget = findTerminalTarget(player,
                stack -> getBoolean(stack, "pick_block"));
        if (terminalTarget == null) {
            return;
        }

        ItemStack terminal = terminalTarget.stack();
        IGrid grid = getGrid(player, terminalTarget);
        if (grid == null || grid.getStorageService() == null) {
            debugMagnet(player, "pick-block skipped: grid/storage missing, requested={}, terminal={}",
                    describeStack(requestedStack), describeStack(terminal));
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
            debugMagnet(player, "pick-block skipped: cannot store replaced stack={}, inserted={}, terminal={}",
                    describeStack(toReplace), insert, describeStack(terminal));
            return;
        }

        int targetAmount = requestedStack.getMaxStackSize();
        var what = AEItemKey.of(requestedStack);
        if (what == null) {
            return;
        }

        var extracted = networkInventory.extract(what, targetAmount, Actionable.SIMULATE, playerSource);
        if (extracted == 0) {
            if (!getBoolean(terminal, "craft_if_missing")
                    || grid.getCraftingService().getCraftingFor(what).isEmpty()) {
                debugMagnet(player, "pick-block skipped: requested stack missing, requested={}, craftIfMissing={}, terminal={}",
                        describeStack(requestedStack), getBoolean(terminal, "craft_if_missing"),
                        describeStack(terminal));
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
        debugMagnet(player, "pick-block inserted into hotbar: requested={}, extracted={}, targetSlot={}, terminal={}",
                describeStack(requestedStack), extracted, targetSlot, describeStack(terminal));
    }

    private static boolean canInsertPickup(ItemStack terminal, ItemStack stack, ServerPlayer player) {
        if (getBoolean(terminal, "restock") && isRestocking(stack, player)) {
            debugMagnet(player, "pickup-to-me eligible by restock: stack={}, terminal={}",
                    describeStack(stack), describeStack(terminal));
            return true;
        }
        boolean sneaking = player.isShiftKeyDown();
        boolean hasMagnetCard = hasMagnetCard(terminal);
        boolean pickupToMeEnabled = getMagnetSetting(terminal, "pickupToME");
        if (sneaking || !hasMagnetCard || !pickupToMeEnabled) {
            debugMagnet(player, "pickup-to-me ineligible: stack={}, sneaking={}, hasMagnetCard={}, pickupToME={}, terminal={}",
                    describeStack(stack), sneaking, hasMagnetCard, pickupToMeEnabled, describeStack(terminal));
            return false;
        }

        IPartitionList filter = createFilter(terminal, "insert_config", player);
        IncludeExclude mode = getIncludeExclude(terminal, "insert_mode_whitelist");

        AEItemKey key = AEItemKey.of(stack);
        boolean matches = matchesFilter(key, filter, mode);
        debugMagnet(player, "pickup-to-me filter check: stack={}, key={}, insertMode={}, matches={}, terminal={}",
                describeStack(stack), key, mode, matches, describeStack(terminal));
        return matches;
    }

    private static boolean isRestocking(ItemStack stack, Player player) {
        if (stack.is(AE2wtlibTags.NO_RESTOCK)) {
            return false;
        }
        if (stack.getMaxStackSize() == 1) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (sameItemAndTag(stack, player.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static TerminalTarget findHotkeyTerminalTarget(Player player,
                                                          java.util.function.Predicate<ItemStack> predicate) {
        if (player.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                && menu.getLocator() != null) {
            MenuLocator locator = menu.getLocator();
            ItemStack current = locator.locate(player, ItemStack.class);
            if (current.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(current)) {
                return new TerminalTarget(current, locator, null);
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return findTerminalTarget(serverPlayer, predicate);
        }

        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                return new TerminalTarget(stack, new CurioLocator(curio.identifier(), curio.slotIndex()), null);
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                var locator = MenuLocators.forInventorySlot(i);
                ItemStack located = locator.locate(player, ItemStack.class);
                return new TerminalTarget(located != null ? located : ItemStack.EMPTY, locator, i);
            }
        }
        return null;
    }

    private static ItemStack findTerminalStack(Player player, java.util.function.Predicate<ItemStack> predicate) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                return stack;
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findTerminalStack(ServerPlayer player, java.util.function.Predicate<ItemStack> predicate) {
        var target = findTerminalTarget(player, predicate);
        return target == null ? ItemStack.EMPTY : target.stack();
    }

    private static TerminalTarget findTerminalTarget(ServerPlayer player,
                                                     java.util.function.Predicate<ItemStack> predicate) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            ItemStack stack = curio.handler().getStackInSlot(curio.slotIndex());
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                debugMagnet(player, "terminal target found in curios: slot={}, stack={}",
                        curio.slotIndex(), describeStack(stack));
                return new TerminalTarget(stack, new CurioLocator(curio.identifier(), curio.slotIndex()), null);
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem && predicate.test(stack)) {
                debugMagnet(player, "terminal target found in inventory: slot={}, stack={}", i, describeStack(stack));
                return new TerminalTarget(stack, MenuLocators.forInventorySlot(i), i);
            }
        }
        debugMagnet(player, "terminal target not found");
        return null;
    }

    private static IGrid getGrid(ServerPlayer player, ItemStack terminal) {
        return getGrid(player, new TerminalTarget(terminal, null, null));
    }

    private static IGrid getGrid(ServerPlayer player, TerminalTarget terminalTarget) {
        ItemStack terminal = terminalTarget.stack();
        if (terminal.getItem() instanceof WirelessComprehensiveWorkTerminalItem item) {
            IGrid linkedGrid = item.getLinkedGrid(terminal, player.level(), null);
            if (linkedGrid != null) {
                return linkedGrid;
            }
            var host = new WirelessComprehensiveWorkTerminalMenuHost(
                    player,
                    terminalTarget.inventorySlot(),
                    terminal,
                    (p, sm) -> {
                    });
            host.canOpenFromAnyLink();
            IGridNode node = host.getActionableNode();
            if (node != null) {
                return node.getGrid();
            }
            debugMagnet(player, "grid resolve failed through WCWT host: terminal={}, locator={}",
                    describeStack(terminal), terminalTarget.locator());
        }
        return null;
    }

    private static IPartitionList createFilter(ItemStack terminal, String key, Player player) {
        ConfigInventory config = ConfigInventory.configTypes(appeng.api.stacks.AEKeyType.items()::equals, 27, () -> {
        });
        config.readFromChildTag(getRootTag(terminal).getCompound(key), "");
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

    private static String getMagnetModeName(ItemStack terminal) {
        boolean magnet = getBoolean(terminal, "magnet");
        boolean pickupToMe = getBoolean(terminal, "pickup_to_me");
        if (!magnet && !pickupToMe) {
            return "OFF";
        }
        if (magnet && !pickupToMe) {
            return "PICKUP_INVENTORY";
        }
        if (magnet) {
            return "PICKUP_ME";
        }
        return "PICKUP_ME_NO_MAGNET";
    }

    private static boolean setMagnetMode(ItemStack terminal, String modeName) {
        CompoundTag root = getRootTag(terminal);
        switch (modeName) {
            case "OFF" -> {
                root.putBoolean("magnet", false);
                root.putBoolean("pickup_to_me", false);
            }
            case "PICKUP_INVENTORY" -> {
                root.putBoolean("magnet", true);
                root.putBoolean("pickup_to_me", false);
            }
            case "PICKUP_ME" -> {
                root.putBoolean("magnet", true);
                root.putBoolean("pickup_to_me", true);
            }
            case "PICKUP_ME_NO_MAGNET" -> {
                root.putBoolean("magnet", false);
                root.putBoolean("pickup_to_me", true);
            }
            default -> {
                return false;
            }
        }
        return true;
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
        return switch (methodName) {
            case "magnet" -> getBoolean(terminal, "magnet");
            case "pickupToME" -> getBoolean(terminal, "pickup_to_me");
            default -> false;
        };
    }

    private static boolean getBoolean(ItemStack stack, String key) {
        return getRootTag(stack).getBoolean(key);
    }

    private static IncludeExclude getIncludeExclude(ItemStack stack, String key) {
        return getBoolean(stack, key) ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    private static boolean matchesFilter(@Nullable AEItemKey key, IPartitionList filter, IncludeExclude mode) {
        return key != null && (filter.isEmpty() || filter.matchesFilter(key, mode));
    }

    private static CompoundTag getRootTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag root = tag.getCompound(ModComponents.ROOT_TAG);
        tag.put(ModComponents.ROOT_TAG, root);
        return root;
    }

    private static void debugMagnetTick(ServerPlayer player, ItemStack terminal, boolean sneaking,
                                        boolean hasMagnetCard, boolean magnetEnabled,
                                        boolean pickupToMeEnabled, @Nullable IGrid grid) {
        if (!DEBUG_MAGNET) {
            return;
        }
        int tick = player.getServer() == null ? 0 : player.getServer().getTickCount();
        Integer lastTick = MAGNET_DEBUG_TICKS.get(player);
        if (lastTick != null && tick - lastTick < 20) {
            return;
        }
        MAGNET_DEBUG_TICKS.put(player, tick);
        WcwtMod.LOGGER.info(
                "WCWT magnet debug: tick player={}, terminal={}, sneaking={}, hasMagnetCard={}, magnet={}, pickupToME={}, grid={}, storage={}, range={}",
                player.getScoreboardName(),
                describeStack(terminal),
                sneaking,
                hasMagnetCard,
                magnetEnabled,
                pickupToMeEnabled,
                grid != null,
                grid != null && grid.getStorageService() != null,
                getMagnetRange());
    }

    private static void debugMagnet(ServerPlayer player, String message, Object... args) {
        if (!DEBUG_MAGNET) {
            return;
        }
        Object[] withPlayer = new Object[args.length + 1];
        withPlayer[0] = player.getScoreboardName();
        System.arraycopy(args, 0, withPlayer, 1, args.length);
        WcwtMod.LOGGER.info("WCWT magnet debug: player={}, " + message, withPlayer);
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static boolean sameItemAndTag(ItemStack first, ItemStack second) {
        return ItemStack.isSameItem(first, second) && Objects.equals(first.getTag(), second.getTag());
    }

    private record TerminalTarget(ItemStack stack, @Nullable MenuLocator locator, @Nullable Integer inventorySlot) {
        private TerminalTarget {
            Objects.requireNonNull(stack, "stack");
        }
    }
}
