package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class CuriosBridge {
    private static final String MOD_ID = "curios";

    private CuriosBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
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
                .ifPresent(payload -> PacketDistributor.sendToServer(payload));
    }

    private static final class Impl {
        private Impl() {
        }

        static List<CurioSlotSpec> getVisibleSlots(Player player) {
            try {
                return collectVisibleSlots(player);
            } catch (Throwable ignored) {
                return List.of();
            }
        }

        private static List<CurioSlotSpec> collectVisibleSlots(Player player) {
            var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElse(null);
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
                if (!(itemHandler instanceof IItemHandlerModifiable modifiable)) {
                    continue;
                }
                int slots = stacksHandler.getSlots();
                boolean canToggleRendering = stacksHandler.canToggleRendering();
                var renderStatuses = stacksHandler.getRenders();
                ResourceLocation icon = top.theillusivec4.curios.api.CuriosApi.getSlot(entry.getKey(), player.level())
                        .map(top.theillusivec4.curios.api.type.ISlotType::getIcon)
                        .orElse(null);
                for (int slot = 0; slot < slots; slot++) {
                    result.add(new CurioSlotSpec(
                            entry.getKey(),
                            slot,
                            modifiable,
                            icon,
                            canToggleRendering,
                            slot >= renderStatuses.size() || Boolean.TRUE.equals(renderStatuses.get(slot))));
                }
            }
            return result;
        }
    }

    public record CurioSlotSpec(
            String identifier,
            int slotIndex,
            IItemHandlerModifiable handler,
            ResourceLocation icon,
            boolean canToggleRendering,
            boolean renderStatus) {
    }
}
