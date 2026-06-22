package com.lhy.wcwt.menu.locator;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.MenuLocator;
import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.compat.CuriosBridge;
import com.lhy.wcwt.helpers.WirelessComprehensiveWorkTerminalMenuHost;
import com.lhy.wcwt.item.WirelessComprehensiveWorkTerminalItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Locates a WCWT terminal in a Curios slot.
 *
 * <p>AE2WTLib's CurioLocator only accepts its own universal terminal interface,
 * while WCWT is an AE2 wireless terminal subclass. Using a WCWT-owned locator
 * keeps AE2 menu reopen/return flows pointed at the actual WCWT menu host.
 */
public record WcwtCurioLocator(String identifier, int slotIndex) implements WcwtItemLocator {
    @Override
    @Nullable
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack stack = locateItem(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof WirelessComprehensiveWorkTerminalItem terminal)) {
            return null;
        }

        ItemMenuHost menuHost = new CurioWcwtMenuHost(player, this, stack, terminal);
        if (hostInterface.isInstance(menuHost)) {
            return hostInterface.cast(menuHost);
        }
        if (menuHost != null) {
            WcwtMod.LOGGER.warn(
                    "Item in Curios slot {}[{}] did not create a compatible WCWT menu of type {}: {}",
                    identifier, slotIndex, hostInterface, menuHost);
        }
        if (hostInterface.isInstance(stack)) {
            return hostInterface.cast(stack);
        }
        return null;
    }

    public ItemStack locateItem(Player player) {
        return CuriosBridge.getEquippedSlots(player).stream()
                .filter(slot -> slot.identifier().equals(identifier) && slot.slotIndex() == slotIndex)
                .findFirst()
                .map(slot -> slot.handler().getStackInSlot(slot.slotIndex()))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean storeItem(Player player, ItemStack stack) {
        return CuriosBridge.getEquippedSlots(player).stream()
                .filter(slot -> slot.identifier().equals(identifier) && slot.slotIndex() == slotIndex)
                .findFirst()
                .map(slot -> {
                    slot.handler().setStackInSlot(slot.slotIndex(), stack);
                    return true;
                })
                .orElse(false);
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

    private static final class CurioWcwtMenuHost extends WirelessComprehensiveWorkTerminalMenuHost {
        private final WcwtCurioLocator locator;

        private CurioWcwtMenuHost(Player player, WcwtCurioLocator locator, ItemStack stack,
                                  WirelessComprehensiveWorkTerminalItem terminal) {
            super(player, null, stack, (p, subMenu) -> returnToWcwt(terminal, p, locator, stack, subMenu));
            this.locator = locator;
        }

        @Override
        public boolean onBroadcastChanges(AbstractContainerMenu menu) {
            boolean result = super.onBroadcastChanges(menu);
            locator.storeItem(getPlayer(), getItemStack());
            return result;
        }

        private static void returnToWcwt(WirelessComprehensiveWorkTerminalItem terminal, Player player,
                                         WcwtCurioLocator locator, ItemStack stack, ISubMenu subMenu) {
            terminal.openFromCurio(player, locator, stack, true);
        }
    }
}
