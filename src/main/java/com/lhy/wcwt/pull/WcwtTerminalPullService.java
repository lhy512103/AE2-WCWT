package com.lhy.wcwt.pull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.helpers.ICraftingGridMenu;
import appeng.items.storage.ViewCellItem;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.util.prioritylist.IPartitionList;

import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket;
import com.lhy.wcwt.network.WcwtPullRecipeInputsPacket.RequestedIngredient;

/**
 * JEI「从 ME 拉配方物品」服务端逻辑（参考 ae2utility {@code TerminalPullService}，内置在本模组中）。
 */
public final class WcwtTerminalPullService {
    private static final int MAX_DETAIL_ITEMS = 5;
    private static final int MAX_PROCESSING_TRANSFER_STACK_SIZE = 64;

    private WcwtTerminalPullService() {
    }

    public static void handle(Player player, WcwtPullRecipeInputsPacket payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(serverPlayer.containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu)) {
            return;
        }

        if (!menu.getLinkStatus().connected()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.pull_no_link"));
            return;
        }

        List<RequestedIngredient> requestedIngredients = sanitizeRequestedIngredients(payload.requestedIngredients());
        if (requestedIngredients.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.wcwt.pull_no_inputs"));
            return;
        }

        var filter = ViewCellItem.createItemFilter(menu.getViewCells());
        var host = menu.getHost();
        var storage = host.getInventory();
        var energy = resolveEnergySource(host);
        var actionSource = resolveActionSource(serverPlayer, host);
        var gridNode = menu.getGridNode();
        ICraftingService craftingService = payload.craftMissing() && gridNode != null && menu.getLinkStatus().connected()
                ? gridNode.getGrid().getCraftingService()
                : null;
        var playerInventory = menu.getPlayerInventory();

        var targetMode = WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode
                .fromOrdinal(payload.manualWorkspaceMode());
        if (targetMode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING) {
            menu.moveManualWorkspaceToNetwork(targetMode);
        }
        InternalInventory craftingGrid = getPullTargetInventory(menu, targetMode);

        var playerInventorySnapshot = snapshotInventory(playerInventory.items);
        var reservedPlayerItems = new int[playerInventorySnapshot.size()];
        var craftingGridSnapshot = craftingGrid != null ? snapshotInventory(craftingGrid) : List.<ItemStack>of();
        var reservedCraftingGridItems = craftingGrid != null ? new int[craftingGridSnapshot.size()] : null;
        int transferSets = payload.maxTransfer()
                ? Math.max(1, computeMaxTransferSets(menu, storage, filter, craftingService, payload.craftMissing(),
                        requestedIngredients, playerInventorySnapshot, craftingGridSnapshot, targetMode))
                : 1;
        List<RequestedIngredient> scaledIngredients = scaleRequestedIngredients(requestedIngredients, transferSets);
        boolean craftingGridChanged = false;

        List<ItemStack> filteredItems = new ArrayList<>();
        List<ItemStack> missingItems = new ArrayList<>();
        List<ItemStack> skippedItems = new ArrayList<>();
        Map<AEItemKey, List<Integer>> autoCraftRequests = new LinkedHashMap<>();

        for (int ingredientIndex = 0; ingredientIndex < scaledIngredients.size(); ingredientIndex++) {
            RequestedIngredient requested = scaledIngredients.get(ingredientIndex);
            if (requested.alternatives().isEmpty()) {
                continue;
            }

            var wideIngredient = WcwtMeIngredientExtraction.ingredientFromItemStacks(requested.alternatives());
            int satisfiedByInventory = consumeFromPlayerInventory(playerInventorySnapshot, menu, requested.alternatives(),
                    wideIngredient, requested.count(), reservedPlayerItems);
            int satisfiedByCraftingGrid = consumeFromCraftingGrid(craftingGridSnapshot, requested.alternatives(),
                    wideIngredient, requested.count() - satisfiedByInventory, reservedCraftingGridItems, ingredientIndex);
            int missingAmount = requested.count() - satisfiedByInventory - satisfiedByCraftingGrid;
            if (missingAmount <= 0) {
                continue;
            }

            List<ItemStack> extractedStacks = WcwtMeIngredientExtraction.extractAlternatives(storage, energy, actionSource,
                    filter, requested.alternatives(), missingAmount);
            int extractedCount = extractedStacks.stream().mapToInt(ItemStack::getCount).sum();
            int remainingToHandle = missingAmount - extractedCount;
            if (remainingToHandle > 0 && payload.craftMissing() && craftingService != null) {
                var craftableKey = findCraftableAlternative(requested.alternatives(), filter, craftingService);
                if (craftableKey != null) {
                    autoCraftRequests.computeIfAbsent(craftableKey, ignored -> new ArrayList<>());
                    var slots = autoCraftRequests.get(craftableKey);
                    for (int i = 0; i < remainingToHandle; i++) {
                        slots.add(ingredientIndex);
                    }
                    remainingToHandle = 0;
                }
            }
            if (remainingToHandle > 0) {
                missingItems.add(getDisplayStack(requested).copyWithCount(remainingToHandle));
            }

            if (extractedStacks.isEmpty()) {
                continue;
            }

            for (ItemStack extractedStack : extractedStacks) {
                ItemStack remainder = extractedStack;
                if (craftingGrid != null) {
                    remainder = insertIntoCraftingGrid(craftingGrid, ingredientIndex, remainder);
                    if (remainder.getCount() != extractedStack.getCount()) {
                        craftingGridChanged = true;
                    }
                }
                remainder = insertIntoPlayerInventory(playerInventory, menu, remainder);
                if (!remainder.isEmpty()) {
                    skippedItems.add(remainder.copy());
                    var remainderKey = AEItemKey.of(remainder);
                    if (remainderKey != null) {
                        StorageHelper.poweredInsert(energy, storage, remainderKey, remainder.getCount(), actionSource);
                    }
                }
            }
        }

        if (craftingGridChanged && craftingGrid != null) {
            menu.slotsChanged(craftingGrid.toContainer());
        }

        sendFeedback(serverPlayer, filteredItems, missingItems, skippedItems);

        if (!autoCraftRequests.isEmpty()) {
            openAutoCraftMenu(serverPlayer, menu, host, autoCraftRequests);
        }
    }

    @Nullable
    private static InternalInventory getPullTargetInventory(WirelessComprehensiveWorkTerminalMenu menu,
            WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode mode) {
        return switch (mode) {
            case CRAFTING -> menu.getCraftingMatrix();
            case SMITHING -> menu.getMenuHost() == null ? null
                    : menu.getMenuHost().getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_SMITHING);
            case ANVIL -> menu.getMenuHost() == null ? null
                    : menu.getMenuHost().getSubInventory(WirelessComprehensiveWorkTerminalMenuHost.INV_MANUAL_ANVIL);
        };
    }

    private static List<RequestedIngredient> scaleRequestedIngredients(List<RequestedIngredient> requestedIngredients, int transferSets) {
        if (transferSets <= 1) {
            return requestedIngredients;
        }

        List<RequestedIngredient> scaled = new ArrayList<>(requestedIngredients.size());
        for (RequestedIngredient ingredient : requestedIngredients) {
            scaled.add(new RequestedIngredient(ingredient.alternatives(),
                    Math.max(1, Math.multiplyExact(ingredient.count(), transferSets))));
        }
        return scaled;
    }

    private static int getMaxTransferSetsForTarget(
            WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode targetMode,
            List<RequestedIngredient> requestedIngredients) {
        if (targetMode == WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode.CRAFTING) {
            return 64;
        }

        int maxSets = MAX_PROCESSING_TRANSFER_STACK_SIZE;
        for (RequestedIngredient ingredient : requestedIngredients) {
            int maxStackSize = ingredient.alternatives().stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .mapToInt(ItemStack::getMaxStackSize)
                    .max()
                    .orElse(MAX_PROCESSING_TRANSFER_STACK_SIZE);
            if (maxStackSize <= 1) {
                return 1;
            }
            maxSets = Math.min(maxSets, Math.max(1, maxStackSize / Math.max(1, ingredient.count())));
        }
        return Math.max(1, maxSets);
    }

    private static List<RequestedIngredient> sanitizeRequestedIngredients(List<RequestedIngredient> requestedIngredients) {
        List<RequestedIngredient> sanitized = new ArrayList<>();
        for (RequestedIngredient ingredient : requestedIngredients) {
            if (ingredient == null || ingredient.alternatives().isEmpty()) {
                continue;
            }

            List<ItemStack> alternatives = WcwtIngredientPriorities.deduplicateItemAlternatives(
                    ingredient.alternatives().stream()
                            .filter(stack -> stack != null && !stack.isEmpty())
                            .map(ItemStack::copy)
                            .toList());
            if (alternatives.isEmpty()) {
                continue;
            }

            int maxAllowed = alternatives.stream()
                    .mapToInt(ItemStack::getMaxStackSize)
                    .max()
                    .orElse(64);
            int count = Math.max(1, Math.min(ingredient.count(), maxAllowed));
            sanitized.add(new RequestedIngredient(alternatives, count));
        }
        return sanitized;
    }

    private static List<ItemStack> snapshotInventory(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static List<ItemStack> snapshotInventory(InternalInventory inventory) {
        List<ItemStack> snapshot = new ArrayList<>(inventory.size());
        for (int i = 0; i < inventory.size(); i++) {
            snapshot.add(inventory.getStackInSlot(i).copy());
        }
        return snapshot;
    }

    private static int computeMaxTransferSets(MEStorageMenu menu, MEStorage storage, @Nullable IPartitionList filter,
            @Nullable ICraftingService craftingService, boolean craftMissing, List<RequestedIngredient> requestedIngredients,
            List<ItemStack> playerInventorySnapshot, List<ItemStack> craftingGridSnapshot,
            WirelessComprehensiveWorkTerminalMenu.ManualWorkspaceMode targetMode) {
        Map<AEItemKey, Long> availableByKey = new LinkedHashMap<>();

        for (int i = 0; i < playerInventorySnapshot.size(); i++) {
            if (menu.isPlayerInventorySlotLocked(i)) {
                continue;
            }
            addStackAmount(availableByKey, playerInventorySnapshot.get(i));
        }
        for (ItemStack stack : craftingGridSnapshot) {
            addStackAmount(availableByKey, stack);
        }
        for (var entry : storage.getAvailableStacks()) {
            if (entry.getLongValue() <= 0 || !(entry.getKey() instanceof AEItemKey itemKey)) {
                continue;
            }
            if (filter != null && !filter.isListed(itemKey)) {
                continue;
            }
            availableByKey.merge(itemKey, entry.getLongValue(), Long::sum);
        }

        List<RequestedIngredient> orderedIngredients = new ArrayList<>(requestedIngredients);
        orderedIngredients.sort((left, right) -> Integer.compare(left.alternatives().size(), right.alternatives().size()));

        int completedSets = 0;
        int maxSets = getMaxTransferSetsForTarget(targetMode, requestedIngredients);
        while (completedSets < maxSets
                && tryReserveSingleSet(availableByKey, orderedIngredients, filter, craftingService, craftMissing)) {
            completedSets++;
        }
        return completedSets;
    }

    private static boolean tryReserveSingleSet(Map<AEItemKey, Long> availableByKey, List<RequestedIngredient> orderedIngredients,
            @Nullable IPartitionList filter, @Nullable ICraftingService craftingService, boolean craftMissing) {
        Map<AEItemKey, Long> remaining = new LinkedHashMap<>(availableByKey);

        for (RequestedIngredient ingredient : orderedIngredients) {
            if (ingredient.alternatives().isEmpty()) {
                return false;
            }
            var wideIngredient = WcwtMeIngredientExtraction.ingredientFromItemStacks(ingredient.alternatives());
            for (int unit = 0; unit < ingredient.count(); unit++) {
                if (!WcwtMeIngredientExtraction.reserveOneUnit(remaining, ingredient.alternatives(), wideIngredient)) {
                    if (craftMissing && craftingService != null
                            && findCraftableAlternative(ingredient.alternatives(), filter, craftingService) != null) {
                        continue;
                    }
                    return false;
                }
            }
        }

        availableByKey.clear();
        availableByKey.putAll(remaining);
        return true;
    }

    private static void addStackAmount(Map<AEItemKey, Long> availableByKey, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        var key = AEItemKey.of(stack);
        if (key != null) {
            availableByKey.merge(key, (long) stack.getCount(), Long::sum);
        }
    }

    private static int consumeFromPlayerInventory(List<ItemStack> inventorySnapshot, MEStorageMenu menu,
            List<ItemStack> alternatives, @Nullable Ingredient wideIngredient, int amount,
            int[] reservedPlayerItems) {
        int matched = 0;
        for (int i = 0; i < inventorySnapshot.size(); i++) {
            if (menu.isPlayerInventorySlotLocked(i)) {
                continue;
            }

            ItemStack stack = inventorySnapshot.get(i);
            if (stack.isEmpty() || !matchesAnyAlternative(stack, alternatives, wideIngredient)) {
                continue;
            }

            int available = stack.getCount() - reservedPlayerItems[i];
            if (available <= 0) {
                continue;
            }

            int consumed = Math.min(available, amount - matched);
            reservedPlayerItems[i] += consumed;
            matched += consumed;
            if (matched >= amount) {
                return matched;
            }
        }
        return matched;
    }

    private static int consumeFromCraftingGrid(List<ItemStack> craftingGridSnapshot, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient, int amount,
            @Nullable int[] reservedCraftingGridItems, int requestedSlot) {
        if (craftingGridSnapshot.isEmpty() || reservedCraftingGridItems == null || amount <= 0 || requestedSlot < 0
                || requestedSlot >= craftingGridSnapshot.size()) {
            return 0;
        }

        ItemStack stack = craftingGridSnapshot.get(requestedSlot);
        if (stack.isEmpty() || !matchesAnyAlternative(stack, alternatives, wideIngredient)) {
            return 0;
        }

        int available = stack.getCount() - reservedCraftingGridItems[requestedSlot];
        if (available <= 0) {
            return 0;
        }

        int consumed = Math.min(available, amount);
        reservedCraftingGridItems[requestedSlot] += consumed;
        return consumed;
    }

    @Nullable
    private static AEItemKey findCraftableAlternative(List<ItemStack> alternatives, @Nullable IPartitionList filter,
            ICraftingService craftingService) {
        for (ItemStack alternative : alternatives) {
            var key = AEItemKey.of(alternative);
            if (key == null || filter != null && !filter.isListed(key)) {
                continue;
            }

            var craftableKey = craftingService.getFuzzyCraftable(key,
                    candidate -> candidate instanceof AEItemKey itemKey
                            && itemKey.matches(alternative)
                            && (filter == null || filter.isListed(itemKey)));
            if (craftableKey instanceof AEItemKey itemKey) {
                return itemKey;
            }
        }
        return null;
    }

    private static boolean matchesAnyAlternative(ItemStack stack, List<ItemStack> alternatives,
            @Nullable Ingredient wideIngredient) {
        for (ItemStack alternative : alternatives) {
            if (ItemStack.isSameItemSameComponents(stack, alternative)) {
                return true;
            }
        }
        if (wideIngredient != null && wideIngredient.test(stack)) {
            return true;
        }
        for (ItemStack alternative : alternatives) {
            if (!alternative.isEmpty() && ItemStack.isSameItem(stack, alternative)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack getDisplayStack(RequestedIngredient requestedIngredient) {
        return requestedIngredient.alternatives().isEmpty()
                ? ItemStack.EMPTY
                : requestedIngredient.alternatives().getFirst().copyWithCount(requestedIngredient.count());
    }

    private static ItemStack insertIntoCraftingGrid(InternalInventory craftingGrid, int requestedSlot, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (requestedSlot < 0 || requestedSlot >= craftingGrid.size()) {
            return stack.copy();
        }
        return craftingGrid.insertItem(requestedSlot, stack.copy(), false);
    }

    private static ItemStack insertIntoPlayerInventory(Inventory inventory, MEStorageMenu menu, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();

        for (int i = 0; i < inventory.items.size(); i++) {
            if (menu.isPlayerInventorySlotLocked(i)) {
                continue;
            }

            ItemStack slotStack = inventory.items.get(i);
            if (!canStacksMerge(slotStack, remaining)) {
                continue;
            }

            int space = Math.min(slotStack.getMaxStackSize(), inventory.getMaxStackSize()) - slotStack.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            slotStack.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                inventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        for (int i = 0; i < inventory.items.size(); i++) {
            if (menu.isPlayerInventorySlotLocked(i)) {
                continue;
            }

            if (!inventory.items.get(i).isEmpty()) {
                continue;
            }

            int moved = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()));
            inventory.items.set(i, remaining.copyWithCount(moved));
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                inventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        inventory.setChanged();
        return remaining;
    }

    private static boolean canStacksMerge(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty() && ItemStack.isSameItemSameComponents(first, second);
    }

    private static IEnergySource resolveEnergySource(Object host) {
        if (host instanceof IEnergySource energySource) {
            return energySource;
        }
        if (host instanceof IActionHost actionHost) {
            return (amount, mode, multiplier) -> {
                var node = actionHost.getActionableNode();
                if (node != null && node.isActive()) {
                    return node.getGrid().getEnergyService().extractAEPower(amount, mode, multiplier);
                }
                return 0.0;
            };
        }
        return IEnergySource.empty();
    }

    private static IActionSource resolveActionSource(ServerPlayer player, Object host) {
        if (host instanceof IActionHost actionHost) {
            return IActionSource.ofPlayer(player, actionHost);
        }
        return IActionSource.ofPlayer(player);
    }

    private static void openAutoCraftMenu(ServerPlayer player, MEStorageMenu menu, Object host,
            Map<AEItemKey, List<Integer>> autoCraftRequests) {
        List<ICraftingGridMenu.AutoCraftEntry> entries = autoCraftRequests.entrySet().stream()
                .map(entry -> new ICraftingGridMenu.AutoCraftEntry(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        if (entries.isEmpty()) {
            return;
        }

        if (menu instanceof ICraftingGridMenu craftingGridMenu) {
            craftingGridMenu.startAutoCrafting(entries);
            return;
        }

        IActionHost actionHost = host instanceof IActionHost terminalHost ? terminalHost : null;
        CraftConfirmMenu.openWithCraftingList(actionHost, player, menu.getLocator(), entries);
    }

    private static void sendFeedback(ServerPlayer player, List<ItemStack> filteredItems, List<ItemStack> missingItems,
            List<ItemStack> skippedItems) {
        if (!filteredItems.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.wcwt.pull_filtered", summarizeStacks(filteredItems)));
        }

        if (!missingItems.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.wcwt.pull_missing", summarizeStacks(missingItems)));
        }

        if (!skippedItems.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.wcwt.pull_full", summarizeStacks(skippedItems)));
        }
    }

    private static String summarizeStacks(List<ItemStack> stacks) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            summary.merge(stack.getHoverName().getString(), stack.getCount(), Integer::sum);
        }

        return summary.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getKey(), "Air"))
                .limit(MAX_DETAIL_ITEMS)
                .map(entry -> entry.getKey() + " x" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
