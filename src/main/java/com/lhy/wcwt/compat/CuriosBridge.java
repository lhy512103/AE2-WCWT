package com.lhy.wcwt.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CuriosBridge {
    private static final String MOD_ID = "curios";
    private static final String CURIOS_API_CLASS = "top.theillusivec4.curios.api.CuriosApi";
    private static final String TOGGLE_RENDER_PACKET_CLASS =
            "top.theillusivec4.curios.common.network.client.CPacketToggleRender";

    private CuriosBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static List<CurioSlotSpec> getVisibleSlots(Player player) {
        if (!isLoaded() || player == null) {
            return List.of();
        }

        try {
            Class<?> curiosApi = Class.forName(CURIOS_API_CLASS);
            Method getCuriosInventory = curiosApi.getMethod(
                    "getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optional = getCuriosInventory.invoke(null, player);
            if (!(optional instanceof Optional<?> maybeHandler) || maybeHandler.isEmpty()) {
                return List.of();
            }

            Object curiosHandler = maybeHandler.get();
            Method getCurios = curiosHandler.getClass().getMethod("getCurios");
            Object mapObject = getCurios.invoke(curiosHandler);
            if (!(mapObject instanceof Map<?, ?> curioMap)) {
                return List.of();
            }

            var result = new ArrayList<CurioSlotSpec>();
            for (var entry : curioMap.entrySet()) {
                if (!(entry.getKey() instanceof String identifier) || entry.getValue() == null) {
                    continue;
                }
                Object stacksHandler = entry.getValue();
                if (!invokeBoolean(stacksHandler, "isVisible", true)) {
                    continue;
                }

                Object stackHandler = invokeObject(stacksHandler, "getStacks");
                if (!(stackHandler instanceof IItemHandlerModifiable itemHandler)) {
                    continue;
                }

                int slots = invokeInt(stackHandler, "getSlots", itemHandler.getSlots());
                boolean canToggleRendering = invokeBoolean(stacksHandler, "canToggleRendering", true);
                Object renders = invokeObject(stacksHandler, "getRenders");
                List<Boolean> renderStatuses = renders instanceof List<?> list
                        ? list.stream().map(Boolean.class::cast).collect(java.util.stream.Collectors.toCollection(ArrayList::new))
                        : new ArrayList<>();
                ResourceLocation icon = getSlotIcon(curiosApi, player, identifier);
                for (int slot = 0; slot < slots; slot++) {
                    result.add(new CurioSlotSpec(
                            identifier,
                            slot,
                            itemHandler,
                            icon,
                            canToggleRendering,
                            slot >= renderStatuses.size() || renderStatuses.get(slot)));
                }
            }
            return result;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return List.of();
        }
    }

    private static Object invokeObject(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static boolean invokeBoolean(Object target, String methodName, boolean fallback)
            throws ReflectiveOperationException {
        Object result = invokeObject(target, methodName);
        return result instanceof Boolean value ? value : fallback;
    }

    private static int invokeInt(Object target, String methodName, int fallback) throws ReflectiveOperationException {
        Object result = invokeObject(target, methodName);
        return result instanceof Integer value ? value : fallback;
    }

    private static ResourceLocation getSlotIcon(Class<?> curiosApi, Player player, String identifier)
            throws ReflectiveOperationException {
        Method getSlot = curiosApi.getMethod("getSlot", String.class, net.minecraft.world.level.Level.class);
        Object optional = getSlot.invoke(null, identifier, player.level());
        if (optional instanceof Optional<?> maybeSlotType && maybeSlotType.isPresent()) {
            Object slotType = maybeSlotType.get();
            Method getIcon = slotType.getClass().getMethod("getIcon");
            Object icon = getIcon.invoke(slotType);
            if (icon instanceof ResourceLocation resourceLocation) {
                return resourceLocation;
            }
        }
        return ResourceLocation.fromNamespaceAndPath("curios", "slot/empty_curio_slot");
    }

    public static void toggleRender(String identifier, int slotIndex) {
        if (!isLoaded()) {
            return;
        }
        try {
            Class<?> payloadClass = Class.forName(TOGGLE_RENDER_PACKET_CLASS);
            Constructor<?> constructor = payloadClass.getConstructor(String.class, int.class);
            Object payload = constructor.newInstance(identifier, slotIndex);
            if (payload instanceof CustomPacketPayload customPayload) {
                PacketDistributor.sendToServer(customPayload);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional integration: the slot remains usable even if Curios internals change.
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
