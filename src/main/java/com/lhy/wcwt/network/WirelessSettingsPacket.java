package com.lhy.wcwt.network;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.menu.WirelessComprehensiveWorkTerminalMenu;
import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record WirelessSettingsPacket(boolean pickBlock, boolean restock, boolean magnet, boolean pickupToMe,
                                     boolean craftIfMissing) implements CustomPacketPayload {
    public static final Type<WirelessSettingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, "wireless_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessSettingsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::pickBlock,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::restock,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::magnet,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::pickupToMe,
                    ByteBufCodecs.BOOL, WirelessSettingsPacket::craftIfMissing,
                    WirelessSettingsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WirelessSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WirelessComprehensiveWorkTerminalMenu menu
                    && menu.getMenuHost() != null) {
                ItemStack stack = menu.getMenuHost().getItemStack();
                stack.set(AE2wtlibComponents.PICK_BLOCK, packet.pickBlock());
                stack.set(AE2wtlibComponents.CRAFT_IF_MISSING, packet.craftIfMissing());
                stack.set(AE2wtlibComponents.RESTOCK, packet.restock());
                setMagnetSettings(stack, packet.magnet(), packet.pickupToMe());
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setMagnetSettings(ItemStack stack, boolean magnet, boolean pickupToMe) {
        try {
            Field componentField = Class.forName("de.mari_023.ae2wtlib.AE2wtlibAdditionalComponents")
                    .getField("MAGNET_SETTINGS");
            DataComponentType component = (DataComponentType) componentField.get(null);
            Class<?> modeClass = Class.forName("de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode");
            Object fallback = Enum.valueOf((Class<Enum>) modeClass.asSubclass(Enum.class), "OFF");
            Object current = stack.getOrDefault(component, fallback);
            Method set = modeClass.getMethod("set", boolean.class, boolean.class);
            stack.set(component, set.invoke(current, magnet, pickupToMe));
        } catch (ReflectiveOperationException ignored) {
            // WTLib main classes are optional at compile time; missing classes simply disable these two toggles.
        }
    }
}
