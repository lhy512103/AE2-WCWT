package com.lhy.wcwt.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 通过反射桥接 Cosmetic Armor Reworked（1.20.1 Forge 版）。
 *
 * <p>该模组在运行时（runtimeOnly）才存在，编译期不可见，因此全部走反射；
 * 找不到对应类/方法时安静降级，保证 WCWT 界面仍可用。
 *
 * <p>装饰盔甲库存数据通过 {@code ModObjects.invMan}（{@code InventoryManager}）按玩家 UUID 取得，
 * 返回的 {@code InventoryCosArmor} 继承 {@code CAStacksBase}，提供
 * {@code isSkinArmor(int)} / {@code setSkinArmor(int,boolean)} / {@code getStackInSlot(int)}。
 *
 * <p>「切换显示/隐藏」与该模组自带界面的开关按钮逻辑保持一致：客户端先改本地库存，
 * 再通过该模组自己的 {@code NetworkManager}（{@code ModObjects.network}）发送
 * {@code PacketSetSkinArmor} 到服务端，由其同步给所有跟踪玩家。
 */
public final class CosmeticArmorReworkedBridge {
    private static final String MOD_ID = "cosmeticarmorreworked";
    private static final String MOD_OBJECTS_CLASS = "lain.mods.cos.impl.ModObjects";
    private static final String INVENTORY_COS_ARMOR_CLASS = "lain.mods.cos.impl.inventory.InventoryCosArmor";
    private static final String NETWORK_PACKET_INTERFACE = "lain.mods.cos.impl.network.NetworkManager$NetworkPacket";
    private static final String PACKET_SET_SKIN_ARMOR_CLASS =
            "lain.mods.cos.impl.network.packet.PacketSetSkinArmor";
    private static final String PACKET_SYNC_COS_ARMOR_CLASS =
            "lain.mods.cos.impl.network.packet.PacketSyncCosArmor";

    private CosmeticArmorReworkedBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    @Nullable
    public static Container getCosmeticArmorInventory(Player player) {
        Object inventory = getInventoryObject(player);
        return inventory instanceof Container container ? container : null;
    }

    public static ItemStack getStack(Player player, int slot) {
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return ItemStack.EMPTY;
        }
        try {
            Method method = inventory.getClass().getMethod("getStackInSlot", int.class);
            Object result = method.invoke(inventory, slot);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean isSkinArmor(Player player, int slot) {
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return false;
        }
        try {
            Method method = inventory.getClass().getMethod("isSkinArmor", int.class);
            Object result = method.invoke(inventory, slot);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * 在客户端切换某个装饰盔甲槽的显示/隐藏，并把改动发往服务端持久化与同步。
     */
    public static void setSkinArmor(Player player, int slot, boolean enabled) {
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return;
        }
        try {
            Method method = inventory.getClass().getMethod("setSkinArmor", int.class, boolean.class);
            method.invoke(inventory, slot, enabled);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return;
        }
        // 仅在客户端把开关同步给服务端；服务端会再广播 PacketSyncCosArmor 给所有跟踪玩家。
        if (player.level().isClientSide()) {
            sendSkinArmorPacket(slot, enabled);
        }
    }

    /**
     * 服务端在打开 WCWT 界面时，主动把指定槽的装饰盔甲状态同步给该玩家客户端，
     * 避免客户端缓存尚未拿到最新数据。
     */
    public static void syncSlotToClient(Player player, int slot) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return;
        }
        try {
            Class<?> inventoryClass = Class.forName(INVENTORY_COS_ARMOR_CLASS);
            Class<?> packetClass = Class.forName(PACKET_SYNC_COS_ARMOR_CLASS);
            Constructor<?> constructor = packetClass.getConstructor(UUID.class, inventoryClass, int.class);
            Object packet = constructor.newInstance(player.getUUID(), inventory, slot);
            sendTo(packet, serverPlayer);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Cosmetic Armor Reworked 自身的监听器仍会在后续变更时同步。
        }
    }

    @Nullable
    private static Object getInventoryObject(Player player) {
        if (!isLoaded() || player == null) {
            return null;
        }
        try {
            Object manager = getInventoryManager();
            UUID uuid = player.getUUID();
            String methodName = player.level().isClientSide ? "getCosArmorInventoryClient" : "getCosArmorInventory";
            Method method = manager.getClass().getMethod(methodName, UUID.class);
            return method.invoke(manager, uuid);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object getInventoryManager() throws ReflectiveOperationException {
        return getModObjectsField("invMan");
    }

    private static Object getNetworkManager() throws ReflectiveOperationException {
        return getModObjectsField("network");
    }

    private static Object getModObjectsField(String fieldName) throws ReflectiveOperationException {
        Class<?> modObjects = Class.forName(MOD_OBJECTS_CLASS);
        Field field = modObjects.getField(fieldName);
        return field.get(null);
    }

    private static void sendSkinArmorPacket(int slot, boolean enabled) {
        try {
            Class<?> packetClass = Class.forName(PACKET_SET_SKIN_ARMOR_CLASS);
            Constructor<?> constructor = packetClass.getConstructor(int.class, boolean.class);
            Object packet = constructor.newInstance(slot, enabled);
            sendToServer(packet);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 该模组改了内部实现时保持 WCWT 界面可用。
        }
    }

    private static void sendToServer(Object packet) throws ReflectiveOperationException {
        Object network = getNetworkManager();
        Class<?> packetInterface = Class.forName(NETWORK_PACKET_INTERFACE);
        Method method = network.getClass().getMethod("sendToServer", packetInterface);
        method.invoke(network, packet);
    }

    private static void sendTo(Object packet, ServerPlayer player) throws ReflectiveOperationException {
        Object network = getNetworkManager();
        Class<?> packetInterface = Class.forName(NETWORK_PACKET_INTERFACE);
        Method method = network.getClass().getMethod("sendTo", packetInterface, ServerPlayer.class);
        method.invoke(network, packet, player);
    }
}
