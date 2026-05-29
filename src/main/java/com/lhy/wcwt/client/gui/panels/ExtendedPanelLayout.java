package com.lhy.wcwt.client.gui.panels;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny reader for WCWT's extended panel JSON files.
 * These panels are not full AE2 sub-screens, so we only consume the fields we position manually.
 */
public final class ExtendedPanelLayout {
    private static final Pattern BREAK_AFTER_COLS = Pattern.compile("BREAK_AFTER_(\\d+)COLS");
    private static final String BASE_PATH = "screens/terminals/encoding/";

    private final JsonObject root;

    private ExtendedPanelLayout(JsonObject root) {
        this.root = root;
    }

    public static ExtendedPanelLayout load(String fileName) {
        ResourceLocation id = com.lhy.wcwt.util.ResourceLocationCompat.id("ae2", BASE_PATH + fileName);
        return load(id);
    }

    public static ExtendedPanelLayout load(ResourceLocation id) {
        try {
            Optional<net.minecraft.server.packs.resources.Resource> resource =
                    Minecraft.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) {
                return new ExtendedPanelLayout(new JsonObject());
            }
            try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                return new ExtendedPanelLayout(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            return new ExtendedPanelLayout(new JsonObject());
        }
    }

    public Rect widget(String id, Rect fallback) {
        return readRect("widgets", id, fallback);
    }

    public Rect widget(String id, Rect fallback, int containerWidth, int containerHeight) {
        return readRect("widgets", id, fallback, containerWidth, containerHeight);
    }

    public Rect slot(String id, Rect fallback) {
        return readRect("slots", id, fallback);
    }

    public Rect slot(String id, Rect fallback, int containerWidth, int containerHeight) {
        return readRect("slots", id, fallback, containerWidth, containerHeight);
    }

    public int slotColumns(String id, int fallback) {
        JsonObject slot = getObject("slots", id);
        if (slot == null || !slot.has("grid")) {
            return fallback;
        }
        Matcher matcher = BREAK_AFTER_COLS.matcher(slot.get("grid").getAsString());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    public int widgetInt(String id, String key, int fallback) {
        JsonObject widget = getObject("widgets", id);
        return widget == null ? fallback : getInt(widget, key, fallback);
    }

    public int slotInt(String id, String key, int fallback) {
        JsonObject slot = getObject("slots", id);
        return slot == null ? fallback : getInt(slot, key, fallback);
    }

    private Rect readRect(String groupName, String id, Rect fallback) {
        return readRect(groupName, id, fallback, -1, -1);
    }

    private Rect readRect(String groupName, String id, Rect fallback, int containerWidth, int containerHeight) {
        JsonObject object = getObject(groupName, id);
        if (object == null) {
            return fallback;
        }
        int width = getInt(object, "width", fallback.width());
        int height = getInt(object, "height", fallback.height());
        int left = getPosition(object, "left", "right", fallback.left(), width, containerWidth);
        int top = getPosition(object, "top", "bottom", fallback.top(), height, containerHeight);
        return new Rect(
                left,
                top,
                width,
                height);
    }

    private JsonObject getObject(String groupName, String id) {
        if (!root.has(groupName) || !root.get(groupName).isJsonObject()) {
            return null;
        }
        JsonObject group = root.getAsJsonObject(groupName);
        if (!group.has(id) || !group.get(id).isJsonObject()) {
            return null;
        }
        return group.getAsJsonObject(id);
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static int getPosition(JsonObject object, String directKey, String oppositeKey,
                                   int fallback, int size, int containerSize) {
        if (object.has(directKey)) {
            return object.get(directKey).getAsInt();
        }
        if (containerSize >= 0 && object.has(oppositeKey)) {
            return containerSize - object.get(oppositeKey).getAsInt();
        }
        return fallback;
    }

    public record Rect(int left, int top, int width, int height) {
    }
}

