package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

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

        var curiosHandler = WcwtReflect.invokeStatic(MOD_ID, CURIOS_API_CLASS, "getCuriosInventory",
                        new Class<?>[]{net.minecraft.world.entity.LivingEntity.class}, player)
                .flatMap(value -> Optional.ofNullable(unwrapOptional(value)))
                .orElse(null);
        if (curiosHandler == null) {
            return List.of();
        }

        var mapObject = invokeNamed(curiosHandler, "getCurios").orElse(null);
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

            Object stackHandler = invokeNamed(stacksHandler, "getStacks").orElse(null);
            if (!(stackHandler instanceof IItemHandlerModifiable itemHandler)) {
                continue;
            }

            int slots = invokeInt(stackHandler, "getSlots", itemHandler.getSlots());
            boolean canToggleRendering = invokeBoolean(stacksHandler, "canToggleRendering", true);
            Object renders = invokeNamed(stacksHandler, "getRenders").orElse(null);
            List<Boolean> renderStatuses = renders instanceof List<?> list
                    ? list.stream().map(Boolean.class::cast)
                            .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
                    : new ArrayList<>();
            ResourceLocation icon = getSlotIcon(player, identifier);
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
    }

    private static Optional<Object> invokeNamed(Object target, String methodName) {
        return WcwtReflect.findMethod(target.getClass(), methodName)
                .flatMap(method -> WcwtReflect.invoke(target, method));
    }

    private static boolean invokeBoolean(Object target, String methodName, boolean fallback) {
        return invokeNamed(target, methodName)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(fallback);
    }

    private static int invokeInt(Object target, String methodName, int fallback) {
        return invokeNamed(target, methodName)
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .orElse(fallback);
    }

    private static ResourceLocation getSlotIcon(Player player, String identifier) {
        var slotType = WcwtReflect.invokeStatic(MOD_ID, CURIOS_API_CLASS, "getSlot",
                        new Class<?>[]{String.class, net.minecraft.world.level.Level.class},
                        identifier, player.level())
                .flatMap(value -> Optional.ofNullable(unwrapOptional(value)))
                .orElse(null);
        if (slotType != null) {
            var icon = invokeNamed(slotType, "getIcon")
                    .filter(ResourceLocation.class::isInstance)
                    .map(ResourceLocation.class::cast);
            if (icon.isPresent()) {
                return icon.get();
            }
        }
        return ResourceLocation.fromNamespaceAndPath("curios", "slot/empty_curio_slot");
    }

    public static void toggleRender(String identifier, int slotIndex) {
        if (!isLoaded()) {
            return;
        }
        // Curios 内部包改了也不该把槽位整条弄崩，只是渲染开关失效。
        WcwtReflect.construct(MOD_ID, TOGGLE_RENDER_PACKET_CLASS,
                        new Class<?>[]{String.class, int.class}, identifier, slotIndex)
                .filter(CustomPacketPayload.class::isInstance)
                .map(CustomPacketPayload.class::cast)
                .ifPresent(payload -> PacketDistributor.sendToServer(payload));
    }

    @Nullable
    private static Object unwrapOptional(@Nullable Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
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
