package net.neoforged.fml;

public final class ModList {
    private static final ModList INSTANCE = new ModList();

    private ModList() {
    }

    public static ModList get() {
        return INSTANCE;
    }

    public boolean isLoaded(String modId) {
        return net.minecraftforge.fml.ModList.get().isLoaded(modId);
    }
}
