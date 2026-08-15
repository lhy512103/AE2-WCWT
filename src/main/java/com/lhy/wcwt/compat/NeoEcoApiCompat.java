package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import cn.dancingsnow.neoecoae.api.ECOPatternInsertionResult;
import cn.dancingsnow.neoecoae.api.IECOPatternStorage;
import cn.dancingsnow.neoecoae.api.IECOPatternStorageService;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class NeoEcoApiCompat {
    private static final String MOD_ID = "neoecoae";

    private NeoEcoApiCompat() {
    }

    public static boolean uploadPatternToEcoStorage(IGrid grid, ItemStack pattern) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        IECOPatternStorageService service = grid.getService(IECOPatternStorageService.class);
        if (service == null) {
            return false;
        }
        IECOPatternStorage storage = service.getPatternStorage();
        return storage != null && storage.insertPattern(pattern) == ECOPatternInsertionResult.INSERTED;
    }

    public static boolean isEcoPatternProvider(PatternContainer provider) {
        return ModList.get().isLoaded(MOD_ID) && provider instanceof IECOPatternStorage;
    }
}