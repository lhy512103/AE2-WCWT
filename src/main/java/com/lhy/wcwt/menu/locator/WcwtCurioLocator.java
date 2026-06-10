package com.lhy.wcwt.menu.locator;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.locator.MenuLocator;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Locates a WCWT terminal in a Curios slot.
 *
 * <p>AE2WTLib's CurioLocator only accepts its own universal terminal interface,
 * while WCWT is an AE2 wireless terminal subclass. Using a WCWT-owned locator
 * keeps AE2 menu reopen/return flows pointed at the actual WCWT menu host.
 */
public record WcwtCurioLocator(String identifier, int slotIndex) implements MenuLocator {
    @Override
    @Nullable
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack stack = locateItem(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal)) {
            return null;
        }

        if (hostInterface.isInstance(stack)) {
            return hostInterface.cast(stack);
        }

        ItemMenuHost menuHost = terminal.getMenuHost(player, this, stack);
        if (hostInterface.isInstance(menuHost)) {
            return hostInterface.cast(menuHost);
        }
        if (menuHost != null) {
            WcwtMod.LOGGER.warn(
                    "Item in Curios slot {}[{}] did not create a compatible WCWT menu of type {}: {}",
                    identifier, slotIndex, hostInterface, menuHost);
        }
        return null;
    }

    public ItemStack locateItem(Player player) {
        for (var curio : CuriosBridge.getEquippedSlots(player)) {
            if (identifier.equals(curio.identifier()) && slotIndex == curio.slotIndex()) {
                return curio.handler().getStackInSlot(curio.slotIndex());
            }
        }
        return ItemStack.EMPTY;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeUtf(identifier);
        buf.writeInt(slotIndex);
    }

    public static WcwtCurioLocator readFromPacket(FriendlyByteBuf buf) {
        return new WcwtCurioLocator(buf.readUtf(), buf.readInt());
    }

    @Override
    public String toString() {
        return "wcwt curio " + identifier + "[" + slotIndex + "]";
    }
}
