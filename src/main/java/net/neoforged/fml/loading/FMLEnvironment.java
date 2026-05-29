package net.neoforged.fml.loading;

import net.neoforged.api.distmarker.Dist;

public final class FMLEnvironment {
    public static final Dist dist = switch (net.minecraftforge.fml.loading.FMLEnvironment.dist) {
        case CLIENT -> Dist.CLIENT;
        case DEDICATED_SERVER -> Dist.DEDICATED_SERVER;
    };

    private FMLEnvironment() {
    }
}
