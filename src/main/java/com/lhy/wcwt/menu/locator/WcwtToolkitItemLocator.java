package com.lhy.wcwt.menu.locator;

import appeng.menu.locator.ItemMenuHostLocator;
import com.lhy.wcwt.helpers.WcwtToolkitHand;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Locates an item menu on a toolkit extra-bar cell.
 *
 * <p>With a side page selected the main hand is a toolkit cell, which AE2's
 * {@code MenuLocators.forHand} cannot find in the player inventory and throws on. Wrenches, network
 * tools, wireless terminals and other item menus opened from the extra bar resolve here instead.
 */
public record WcwtToolkitItemLocator(int toolkitIndex, @Nullable BlockHitResult hit) implements ItemMenuHostLocator {

    @Nullable
    public static ItemMenuHostLocator forHand(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !WcwtToolkitHand.isOverrideActive(player)) {
            return null;
        }
        int index = WcwtToolkitHotbarState.toolkitIndex(player);
        return WcwtToolkitHotbarState.isValidToolkitIndex(index) ? new WcwtToolkitItemLocator(index, null) : null;
    }

    @Nullable
    public static ItemMenuHostLocator forUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(forHand(player, context.getHand()) instanceof WcwtToolkitItemLocator toolkit)) {
            return null;
        }
        return new WcwtToolkitItemLocator(toolkit.toolkitIndex(), new BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()));
    }

    @Override
    public ItemStack locateItem(Player player) {
        return WcwtToolkitHotbarState.stackAt(player, toolkitIndex);
    }

    @Override
    public @Nullable BlockHitResult hitResult() {
        return hit;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeVarInt(toolkitIndex);
        buf.writeBoolean(hit != null);
        if (hit != null) {
            buf.writeBlockHitResult(hit);
        }
    }

    public static WcwtToolkitItemLocator readFromPacket(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        BlockHitResult hit = buf.readBoolean() ? buf.readBlockHitResult() : null;
        return new WcwtToolkitItemLocator(index, hit);
    }
}
