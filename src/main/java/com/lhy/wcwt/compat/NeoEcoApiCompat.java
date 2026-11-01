package com.lhy.wcwt.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import cn.dancingsnow.neoecoae.api.IECOPatternStorage;
import cn.dancingsnow.neoecoae.api.IECOPatternStorageService;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Direct Neo ECO AE API integration. This class is only called on the server side. */
public final class NeoEcoApiCompat {
    private NeoEcoApiCompat() {
    }

    public static boolean isAvailable() {
        var mods = ModList.get();
        return mods != null && mods.isLoaded("neoecoae");
    }

    public static boolean uploadPatternToEcoStorage(IGrid grid, ItemStack pattern) {
        if (!isAvailable()) {
            return false;
        }
        IECOPatternStorageService service = grid.getService(IECOPatternStorageService.class);
        if (service == null) {
            return false;
        }
        IECOPatternStorage storage = service.getPatternStorage();
        return storage != null && storage.insertPattern(pattern);
    }

    public static Object patternStorageSystem(IGrid grid) {
        if (!isAvailable()) {
            return null;
        }
        IECOPatternStorageService service = grid.getService(IECOPatternStorageService.class);
        return service == null ? null : service.getPatternStorage();
    }

    public static boolean isEcoPatternProvider(PatternContainer provider) {
        return isAvailable() && provider instanceof IECOPatternStorage;
    }
}
