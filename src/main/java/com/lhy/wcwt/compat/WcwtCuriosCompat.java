package com.lhy.wcwt.compat;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

public final class WcwtCuriosCompat {
    public static final String TERMINAL_SLOT = "wireless_comprehensive_work_terminal";
    public static final ResourceLocation TERMINAL_SLOT_VALIDATOR =
            ResourceLocation.fromNamespaceAndPath(WcwtMod.MOD_ID, TERMINAL_SLOT);

    private WcwtCuriosCompat() {
    }

    public static void registerPredicates() {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }

        CuriosApi.registerCurioPredicate(TERMINAL_SLOT_VALIDATOR,
                result -> result.stack().is(ModItems.WIRELESS_COMPREHENSIVE_WORK_TERMINAL.get()));
    }
}
