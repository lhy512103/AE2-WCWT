package com.lhy.wcwt.client.gui.panels;

import org.jetbrains.annotations.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
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
    /** Parsed once per group and id: the screen asks for the same rectangles every frame. */
    private final Map<String, Map<String, ParsedRect>> parsedRects = new HashMap<>();

    private ExtendedPanelLayout(JsonObject root) {
        this.root = root;
    }

    public static ExtendedPanelLayout load(String fileName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ae2", BASE_PATH + fileName);
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
        ParsedRect parsed = parsedRects.computeIfAbsent(groupName, ignored -> new HashMap<>())
                .computeIfAbsent(id, ignored -> parseRect(groupName, id));
        if (parsed == ParsedRect.MISSING) {
            return fallback;
        }
        int width = parsed.width() != null ? parsed.width() : fallback.width();
        int height = parsed.height() != null ? parsed.height() : fallback.height();
        int left = position(parsed.left(), parsed.right(), fallback.left(), containerWidth);
        int top = position(parsed.top(), parsed.bottom(), fallback.top(), containerHeight);
        return new Rect(left, top, width, height);
    }

    private ParsedRect parseRect(String groupName, String id) {
        JsonObject object = getObject(groupName, id);
        if (object == null) {
            return ParsedRect.MISSING;
        }
        return new ParsedRect(optInt(object, "width"), optInt(object, "height"), optInt(object, "left"),
                optInt(object, "right"), optInt(object, "top"), optInt(object, "bottom"));
    }

    private static int position(@Nullable Integer direct, @Nullable Integer opposite, int fallback, int containerSize) {
        if (direct != null) {
            return direct;
        }
        if (containerSize >= 0 && opposite != null) {
            return containerSize - opposite;
        }
        return fallback;
    }

    @Nullable
    private static Integer optInt(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsInt() : null;
    }

    private record ParsedRect(@Nullable Integer width, @Nullable Integer height, @Nullable Integer left,
                              @Nullable Integer right, @Nullable Integer top, @Nullable Integer bottom) {
        private static final ParsedRect MISSING = new ParsedRect(null, null, null, null, null, null);
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

    public record Rect(int left, int top, int width, int height) {
    }
}
