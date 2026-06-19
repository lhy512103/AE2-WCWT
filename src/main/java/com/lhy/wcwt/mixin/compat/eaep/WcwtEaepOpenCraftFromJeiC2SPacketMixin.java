package com.lhy.wcwt.mixin.compat.eaep;

import appeng.api.stacks.GenericStack;
import com.lhy.wcwt.helpers.WcwtWirelessFeatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.function.Supplier;

@Mixin(targets = "com.extendedae_plus.network.crafting.OpenCraftFromJeiC2SPacket", remap = false)
public abstract class WcwtEaepOpenCraftFromJeiC2SPacketMixin {
    @Unique
    private static Field wcwt$stackField;

    @Inject(
            method = "handle(Lcom/extendedae_plus/network/crafting/OpenCraftFromJeiC2SPacket;Ljava/util/function/Supplier;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void wcwt$handleWithWcwt(@Coerce Object packet,
                                            Supplier<NetworkEvent.Context> contextSupplier,
                                            CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null || !WcwtWirelessFeatures.hasAnyTerminal(player)) {
            return;
        }

        GenericStack stack = wcwt$getStack(packet);
        context.enqueueWork(() -> WcwtWirelessFeatures.openJeiBookmarkCrafting(player, stack));
        context.setPacketHandled(true);
        ci.cancel();
    }

    @Unique
    private static GenericStack wcwt$getStack(Object packet) {
        try {
            Object value = wcwt$getStackField(packet).get(packet);
            return value instanceof GenericStack stack ? stack : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Unique
    private static Field wcwt$getStackField(Object packet) throws ReflectiveOperationException {
        Field field = wcwt$stackField;
        if (field == null || field.getDeclaringClass() != packet.getClass()) {
            field = packet.getClass().getDeclaredField("stack");
            field.setAccessible(true);
            wcwt$stackField = field;
        }
        return field;
    }
}
