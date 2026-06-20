package com.lhy.wcwt.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public final class CuriosBridge {
    private static final String MOD_ID = "curios";
    private static final String TOGGLE_RENDER_PACKET_CLASS =
            "top.theillusivec4.curios.common.network.client.CPacketToggleRender";
    private static final String NETWORK_HANDLER_CLASS =
            "top.theillusivec4.curios.common.network.NetworkHandler";

    private CuriosBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static List<CurioSlotSpec> getVisibleSlots(Player player) {
        return getSlots(player, true);
    }

    public static List<CurioSlotSpec> getEquippedSlots(Player player) {
        return getSlots(player, false);
    }

    private static List<CurioSlotSpec> getSlots(Player player, boolean onlyVisible) {
        if (!isLoaded() || player == null) {
            return List.of();
        }

        ICuriosItemHandler curiosHandler = resolveCuriosHandler(player);
        if (curiosHandler == null) {
            return List.of();
        }

        var result = new ArrayList<CurioSlotSpec>();
        for (var entry : curiosHandler.getCurios().entrySet()) {
            String identifier = entry.getKey();
            ICurioStacksHandler stacksHandler = entry.getValue();
            if (identifier == null || stacksHandler == null || (onlyVisible && !stacksHandler.isVisible())) {
                continue;
            }

            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            IItemHandlerModifiable itemHandler = stackHandler;

            int slots = stacksHandler.getSlots();
            boolean canToggleRendering = stacksHandler.canToggleRendering();
            List<Boolean> renderStatuses = stacksHandler.getRenders();
            ResourceLocation icon = getSlotIcon(identifier);
            for (int slot = 0; slot < slots; slot++) {
                result.add(new CurioSlotSpec(
                        identifier,
                        slot,
                        itemHandler,
                        icon,
                        canToggleRendering,
                        slot >= renderStatuses.size() || Boolean.TRUE.equals(renderStatuses.get(slot))));
            }
        }
        return result;
    }

    private static ICuriosItemHandler resolveCuriosHandler(Player player) {
        LazyOptional<ICuriosItemHandler> optional = CuriosApi.getCuriosHelper().getCuriosHandler(player);
        return optional.resolve().orElse(null);
    }

    private static ResourceLocation getSlotIcon(String identifier) {
        return CuriosApi.getSlot(identifier)
                .map(ISlotType::getIcon)
                .orElse(com.lhy.wcwt.util.ResourceLocationCompat.id("curios", "slot/empty_curio_slot"));
    }

    public static void toggleRender(String identifier, int slotIndex) {
        if (!isLoaded()) {
            return;
        }
        try {
            // Curios on Forge 1.20.1 routes client packets through its own SimpleChannel
            // (NetworkHandler.INSTANCE), not through a NeoForge-style CustomPacketPayload.
            // CPacketToggleRender lives in the implementation jar, so reach it reflectively.
            Class<?> packetClass = Class.forName(TOGGLE_RENDER_PACKET_CLASS);
            Constructor<?> constructor = packetClass.getConstructor(String.class, int.class);
            Object packet = constructor.newInstance(identifier, slotIndex);

            Class<?> networkHandlerClass = Class.forName(NETWORK_HANDLER_CLASS);
            Object channel = networkHandlerClass.getField("INSTANCE").get(null);
            channel.getClass().getMethod("sendToServer", Object.class).invoke(channel, packet);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Curios 未提供该客户端包/通道时，保留为可选兼容能力。
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
