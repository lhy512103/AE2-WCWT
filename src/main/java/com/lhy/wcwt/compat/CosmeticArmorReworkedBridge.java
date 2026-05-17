package com.lhy.wcwt.compat;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class CosmeticArmorReworkedBridge {
    private static final String MOD_ID = "cosmeticarmorreworked";
    private static final String MOD_OBJECTS_CLASS = "lain.mods.cos.impl.ModObjects";
    private static final String PAYLOAD_SET_SKIN_ARMOR_CLASS =
            "lain.mods.cos.impl.network.payload.PayloadSetSkinArmor";
    private static final String PAYLOAD_SYNC_COS_ARMOR_CLASS =
            "lain.mods.cos.impl.network.payload.PayloadSyncCosArmor";

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

    public static void setSkinArmor(Player player, int slot, boolean enabled) {
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return;
        }
        try {
            Method method = inventory.getClass().getMethod("setSkinArmor", int.class, boolean.class);
            method.invoke(inventory, slot, enabled);
            sendSkinArmorPacket(slot, enabled);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional integration: leave the WCWT screen usable if the other mod changes internals.
        }
    }

    public static void syncSlotToClient(Player player, int slot) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return;
        }
        try {
            boolean skinArmor = isSkinArmor(player, slot);
            ItemStack stack = getStack(player, slot);
            Class<?> payloadClass = Class.forName(PAYLOAD_SYNC_COS_ARMOR_CLASS);
            Constructor<?> constructor = payloadClass.getConstructor(UUID.class, int.class, boolean.class, ItemStack.class);
            Object payload = constructor.newInstance(player.getUUID(), slot, skinArmor, stack);
            if (payload instanceof CustomPacketPayload customPayload) {
                PacketDistributor.sendToPlayer(serverPlayer, customPayload);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Cosmetic Armor Reworked will still sync future changes through its own listeners.
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
        Class<?> modObjects = Class.forName(MOD_OBJECTS_CLASS);
        Field field = modObjects.getField("invMan");
        return field.get(null);
    }

    private static void sendSkinArmorPacket(int slot, boolean enabled) throws ReflectiveOperationException {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        Class<?> payloadClass = Class.forName(PAYLOAD_SET_SKIN_ARMOR_CLASS);
        Constructor<?> constructor = payloadClass.getConstructor(int.class, boolean.class);
        Object payload = constructor.newInstance(slot, enabled);
        if (payload instanceof CustomPacketPayload customPayload) {
            PacketDistributor.sendToServer(customPayload);
        }
    }
}
