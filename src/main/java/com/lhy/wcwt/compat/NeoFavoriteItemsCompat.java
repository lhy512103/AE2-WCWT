package com.lhy.wcwt.compat;

import com.lhy.wcwt.compat.reflect.WcwtReflect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class NeoFavoriteItemsCompat {
    private static final String MOD_ID = "neo_favorite_items";
    private static final String SERVICE =
            "mycraft.yuyears.neofavoriteitems.application.ServerFavoriteService";

    private NeoFavoriteItemsCompat() {
    }

    public static boolean preventsReplace(Inventory inventory, int slot, ItemStack replacement) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        // Query before touching network storage; a cancelled Inventory#setItem would lose extracted items.
        // If the installed API is incompatible, leave both inventories untouched.
        boolean preventsRemove = WcwtReflect.invokeStatic(MOD_ID, SERVICE, "shouldPreventInventoryRemove",
                        new Class<?>[]{Inventory.class, int.class}, inventory, slot)
                .filter(Boolean.class::isInstance).map(Boolean.class::cast).orElse(true);
        return preventsRemove || WcwtReflect.invokeStatic(MOD_ID, SERVICE, "shouldPreventInventorySet",
                        new Class<?>[]{Inventory.class, int.class, ItemStack.class}, inventory, slot, replacement)
                .filter(Boolean.class::isInstance).map(Boolean.class::cast).orElse(true);
    }
}
