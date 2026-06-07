package com.lhy.wcwt.client;

import appeng.api.stacks.AEKey;
import com.lhy.wcwt.config.WcwtClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side favorite item list for WCWT terminal sorting and markers.
 */
public final class WcwtFavorites {
    private static final Map<String, AEKey> FAVORITES = new LinkedHashMap<>();
    private static boolean loaded;

    private WcwtFavorites() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        loaded = true;
        FAVORITES.clear();
        for (String serialized : WcwtClientConfig.favoritedKeys()) {
            AEKey key = deserializeKey(serialized);
            if (key != null) {
                FAVORITES.put(serialized, key);
            }
        }
    }

    public static boolean isEnabled() {
        return WcwtClientConfig.favoritedItemsFirst();
    }

    public static void setEnabled(boolean enabled) {
        WcwtClientConfig.setFavoritedItemsFirst(enabled);
    }

    public static boolean isFavorited(AEKey key) {
        ensureLoaded();
        return key != null && FAVORITES.containsValue(key);
    }

    public static boolean toggle(AEKey key) {
        ensureLoaded();
        if (key == null) {
            return false;
        }
        String serialized = serializeKey(key);
        if (serialized == null || serialized.isBlank()) {
            return false;
        }
        if (FAVORITES.containsKey(serialized)) {
            FAVORITES.remove(serialized);
        } else {
            FAVORITES.put(serialized, key);
        }
        save();
        return true;
    }

    public static Collection<AEKey> getFavoritedKeys() {
        ensureLoaded();
        return new ArrayList<>(FAVORITES.values());
    }

    public static Map<AEKey, Integer> getFavoritePriorities() {
        ensureLoaded();
        var result = new HashMap<AEKey, Integer>();
        int index = 0;
        for (AEKey key : FAVORITES.values()) {
            result.putIfAbsent(key, index++);
        }
        return result;
    }

    private static void save() {
        WcwtClientConfig.setFavoritedKeys(FAVORITES.keySet());
    }

    private static String serializeKey(AEKey key) {
        try {
            CompoundTag tag = key.toTagGeneric();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static AEKey deserializeKey(String serialized) {
        try {
            if (serialized == null || serialized.isBlank()) {
                return null;
            }
            byte[] bytes = Base64.getDecoder().decode(serialized);
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes));
            return AEKey.fromTagGeneric(tag);
        } catch (Exception ignored) {
            return null;
        }
    }
}
