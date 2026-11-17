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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * WCWT 客户端收藏项管理。
 * 仅影响当前客户端的排序与角标显示，不与服务端同步。
 */
public final class WcwtFavorites {
    private static final Map<String, AEKey> FAVORITES = new LinkedHashMap<>();
    /** The same keys as a set: the screen asks per slot per frame whether a key is favorited. */
    private static final Set<AEKey> FAVORITE_KEYS = new HashSet<>();
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
        FAVORITE_KEYS.clear();
        for (String serialized : WcwtClientConfig.favoritedKeys()) {
            AEKey key = deserializeKey(serialized);
            if (key != null) {
                FAVORITES.put(serialized, key);
                FAVORITE_KEYS.add(key);
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
        return key != null && FAVORITE_KEYS.contains(key);
    }

    public static boolean isEmpty() {
        ensureLoaded();
        return FAVORITE_KEYS.isEmpty();
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
        boolean nowFavorited;
        if (FAVORITES.containsKey(serialized)) {
            AEKey removed = FAVORITES.remove(serialized);
            if (removed != null && !FAVORITES.containsValue(removed)) {
                FAVORITE_KEYS.remove(removed);
            }
            nowFavorited = false;
        } else {
            FAVORITES.put(serialized, key);
            FAVORITE_KEYS.add(key);
            nowFavorited = true;
        }
        save();
        return nowFavorited;
    }

    public static Collection<AEKey> getFavoritedKeys() {
        ensureLoaded();
        return new ArrayList<>(FAVORITES.values());
    }

    private static void save() {
        WcwtClientConfig.setFavoritedKeys(FAVORITES.keySet());
    }

    private static String serializeKey(AEKey key) {
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return null;
            }
            CompoundTag tag = key.toTagGeneric(minecraft.level.registryAccess());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static AEKey deserializeKey(String serialized) {
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || serialized == null || serialized.isBlank()) {
                return null;
            }
            byte[] bytes = Base64.getDecoder().decode(serialized);
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            return AEKey.fromTagGeneric(minecraft.level.registryAccess(), tag);
        } catch (Exception ignored) {
            return null;
        }
    }
}
