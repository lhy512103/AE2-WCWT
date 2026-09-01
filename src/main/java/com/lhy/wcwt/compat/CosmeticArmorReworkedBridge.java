package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

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
        return WcwtReflect.findMethod(inventory.getClass(), "getStackInSlot", int.class)
                .flatMap(method -> WcwtReflect.invoke(inventory, method, slot))
                .filter(ItemStack.class::isInstance)
                .map(ItemStack.class::cast)
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isSkinArmor(Player player, int slot) {
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return false;
        }
        return WcwtReflect.findMethod(inventory.getClass(), "isSkinArmor", int.class)
                .flatMap(method -> WcwtReflect.invoke(inventory, method, slot))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    public static void setSkinArmor(Player player, int slot, boolean enabled) {
        Object inventory = getInventoryObject(player);
        if (inventory == null) {
            return;
        }
        // 可选联动：目标模组改了内部结构时，WCWT 界面保持可用，只是这项开关失效。
        boolean applied = WcwtReflect
                .findMethod(inventory.getClass(), "setSkinArmor", int.class, boolean.class)
                .map(method -> WcwtReflect.run(inventory, method, slot, enabled))
                .orElse(false);
        if (applied) {
            sendSkinArmorPacket(slot, enabled);
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
        boolean skinArmor = isSkinArmor(player, slot);
        ItemStack stack = getStack(player, slot);
        WcwtReflect.construct(MOD_ID, PAYLOAD_SYNC_COS_ARMOR_CLASS,
                        new Class<?>[]{UUID.class, int.class, boolean.class, ItemStack.class},
                        player.getUUID(), slot, skinArmor, stack)
                .filter(CustomPacketPayload.class::isInstance)
                .map(CustomPacketPayload.class::cast)
                // 同步失败也不影响游戏：Cosmetic Armor 自己的监听器会在下次变更时补上。
                .ifPresent(payload -> PacketDistributor.sendToPlayer(serverPlayer, payload));
    }

    @Nullable
    private static Object getInventoryObject(Player player) {
        if (!isLoaded() || player == null) {
            return null;
        }
        Object manager = getInventoryManager();
        if (manager == null) {
            return null;
        }
        String methodName = player.level().isClientSide ? "getCosArmorInventoryClient" : "getCosArmorInventory";
        return WcwtReflect.findMethod(manager.getClass(), methodName, UUID.class)
                .flatMap(method -> WcwtReflect.invoke(manager, method, player.getUUID()))
                .orElse(null);
    }

    @Nullable
    private static Object getInventoryManager() {
        return WcwtReflect.readStaticField(MOD_ID, MOD_OBJECTS_CLASS, "invMan").orElse(null);
    }

    private static void sendSkinArmorPacket(int slot, boolean enabled) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        WcwtReflect.construct(MOD_ID, PAYLOAD_SET_SKIN_ARMOR_CLASS,
                        new Class<?>[]{int.class, boolean.class}, slot, enabled)
                .filter(CustomPacketPayload.class::isInstance)
                .map(CustomPacketPayload.class::cast)
                .ifPresent(payload -> PacketDistributor.sendToServer(payload));
    }
}
