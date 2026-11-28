package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;

import java.util.ArrayList;
import java.util.List;

public final class CuriosBridge {
    private static final String MOD_ID = "curios";

    private CuriosBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static List<ItemStack> equippedItems(Player player) {
        return isLoaded() ? Impl.equippedItems(player) : List.of();
    }

    public static ItemStack equippedItem(Player player, int slot) {
        return isLoaded() && slot >= 0 ? Impl.equippedItem(player, slot) : ItemStack.EMPTY;
    }

    public static List<CurioSlotSpec> getVisibleSlots(Player player) {
        if (!isLoaded() || player == null) {
            return List.of();
        }
        return Impl.getVisibleSlots(player);
    }

    public static void toggleRender(String identifier, int slotIndex) {
        if (!isLoaded()) {
            return;
        }
        WcwtReflect.construct(MOD_ID, "top.theillusivec4.curios.common.network.client.CPacketToggleRender",
                        new Class<?>[]{String.class, int.class}, identifier, slotIndex)
                .filter(CustomPacketPayload.class::isInstance)
                .map(CustomPacketPayload.class::cast)
                .ifPresent(payload -> com.lhy.wcwt.network.WcwtPackets.sendToServer(payload));
    }

    private static final class Impl {
        private Impl() {
        }

        static List<ItemStack> equippedItems(Player player) {
            var handler = CuriosApi.getCuriosInventory(player).orElse(null);
            if (handler == null) {
                return List.of();
            }
            // Keep Curios' flattened slot order and read the actual stacks so card updates persist.
            var items = new ArrayList<ItemStack>();
            for (var group : handler.getCurios().values()) {
                var stacks = group.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    items.add(stacks.getStackInSlot(slot));
                }
            }
            return items;
        }

        static ItemStack equippedItem(Player player, int slot) {
            var handler = CuriosApi.getCuriosInventory(player).orElse(null);
            if (handler != null) {
                for (var group : handler.getCurios().values()) {
                    var stacks = group.getStacks();
                    int size = stacks.getSlots();
                    if (slot < size) {
                        return stacks.getStackInSlot(slot);
                    }
                    slot -= size;
                }
            }
            return ItemStack.EMPTY;
        }

        static List<CurioSlotSpec> getVisibleSlots(Player player) {
            try {
                return collectVisibleSlots(player);
            } catch (Throwable ignored) {
                return List.of();
            }
        }

        private static List<CurioSlotSpec> collectVisibleSlots(Player player) {
            var handler = CuriosApi.getCuriosInventory(player).orElse(null);
            if (handler == null) {
                return List.of();
            }
            var result = new ArrayList<CurioSlotSpec>();
            for (var entry : handler.getCurios().entrySet()) {
                var stacksHandler = entry.getValue();
                if (stacksHandler == null || !stacksHandler.isVisible()) {
                    continue;
                }
                var itemHandler = stacksHandler.getStacks();
                int slots = stacksHandler.getSlots();
                boolean canToggleRendering = stacksHandler.canToggleRendering();
                var renderStatuses = stacksHandler.getRenders();
                var type = CuriosSlotTypes.getSlotType(entry.getKey(), player.level().isClientSide());
                Identifier icon = type == null ? null : type.getIcon();
                for (int slot = 0; slot < slots; slot++) {
                    result.add(new CurioSlotSpec(
                            entry.getKey(),
                            slot,
                            itemHandler,
                            icon,
                            canToggleRendering,
                            slot >= renderStatuses.size() || Boolean.TRUE.equals(renderStatuses.get(slot))));
                }
            }
            return result;
        }
    }

    // Curios supplies equipment slots through the legacy handler, including equip and unequip rules.
    @SuppressWarnings("removal")
    public record CurioSlotSpec(
            String identifier,
            int slotIndex,
            IItemHandlerModifiable handler,
            Identifier icon,
            boolean canToggleRendering,
            boolean renderStatus) {
    }
}
