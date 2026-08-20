package com.lhy.wcwt.compat;

import com.lhy.wcwt.WcwtMod;
import appeng.helpers.patternprovider.PatternContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtendedAePlusUploadCompat {
    private static final String LEGACY_UTIL_CLASS =
            "com.extendedae_plus.util.uploadPattern.ExtendedAEPatternUploadUtil";
    private static final String RECIPE_TYPE_CONFIG_CLASS =
            "com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig";
    private static final String CONFIG_RELATIVE = "extendedae_plus/recipe_type_names.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, String> RUNTIME_ALIASES = new ConcurrentHashMap<>();
    private static final boolean DEBUG_PATTERN_UPLOAD = Boolean.getBoolean("wcwt.debug.patternUpload");
    @Nullable
    private static volatile String lastRecipeSearchKey;

    private ExtendedAePlusUploadCompat() {
    }

    public static boolean addOrUpdateAliasMapping(String aliasKey, String value) {
        Boolean modern = invokeBoolean(RECIPE_TYPE_CONFIG_CLASS, "addOrUpdateAliasMapping",
                new Class<?>[]{String.class, String.class}, aliasKey, value);
        if (modern != null) {
            if (modern) {
                rememberAliasMapping(aliasKey, value);
            }
            logDebug("add mapping via modern aliasKey={}, value={}, result={}", aliasKey, value, modern);
            return modern;
        }
        Boolean legacy = invokeBoolean(LEGACY_UTIL_CLASS, "addOrUpdateAliasMapping",
                new Class<?>[]{String.class, String.class}, aliasKey, value);
        boolean ok = legacy != null ? legacy : addOrUpdateAliasMappingFallback(aliasKey, value);
        if (ok) {
            rememberAliasMapping(aliasKey, value);
        }
        logDebug("add mapping via {} aliasKey={}, value={}, result={}",
                legacy != null ? "legacy" : "fallback", aliasKey, value, ok);
        return ok;
    }

    public static void loadRecipeTypeNames() {
        RUNTIME_ALIASES.clear();
        if (!invokeVoid(RECIPE_TYPE_CONFIG_CLASS, "loadRecipeTypeNames")) {
            invokeVoid(LEGACY_UTIL_CLASS, "loadRecipeTypeNames");
        }
        logDebug("reload mappings");
    }

    public static int removeMappingsByCnValue(String value) {
        Integer modern = invokeInt(RECIPE_TYPE_CONFIG_CLASS, "removeMappingsByCnValue",
                new Class<?>[]{String.class}, value);
        if (modern != null) {
            if (modern > 0) {
                removeRuntimeMappingsByValue(value);
            }
            logDebug("remove mapping via modern value={}, removed={}", value, modern);
            return modern;
        }
        Integer legacy = invokeInt(LEGACY_UTIL_CLASS, "removeMappingsByCnValue",
                new Class<?>[]{String.class}, value);
        int removed = legacy != null ? legacy : removeMappingsByCnValueFallback(value);
        if (removed > 0) {
            removeRuntimeMappingsByValue(value);
        }
        logDebug("remove mapping via {} value={}, removed={}",
                legacy != null ? "legacy" : "fallback", value, removed);
        return removed;
    }

    @Nullable
    public static String resolveSearchKeyAlias(@Nullable String rawKey) {
        String normalized = normalize(rawKey);
        if (normalized == null) {
            return null;
        }
        String runtime = lookupRuntimeAlias(normalized);
        if (runtime != null) {
            logDebug("resolve key={} -> {} via runtime", normalized, runtime);
            return runtime;
        }
        String modern = invokeString(RECIPE_TYPE_CONFIG_CLASS, "resolveSearchKeyAlias",
                new Class<?>[]{String.class}, normalized);
        if (normalize(modern) != null) {
            String resolved = normalize(modern);
            if (!normalized.equals(resolved)) {
                rememberAliasMapping(normalized, resolved);
                logDebug("resolve key={} -> {} via modern", normalized, resolved);
                return resolved;
            }
        }
        String legacy = invokeString(LEGACY_UTIL_CLASS, "resolveSearchKeyAlias",
                new Class<?>[]{String.class}, normalized);
        if (normalize(legacy) != null) {
            String resolved = normalize(legacy);
            if (!normalized.equals(resolved)) {
                rememberAliasMapping(normalized, resolved);
                logDebug("resolve key={} -> {} via legacy", normalized, resolved);
                return resolved;
            }
        }
        String fallback = resolveSearchKeyAliasFallback(normalized);
        if (fallback != null) {
            rememberAliasMapping(normalized, fallback);
            logDebug("resolve key={} -> {} via fallback", normalized, fallback);
            return fallback;
        }
        String resolved = normalize(legacy) != null ? normalize(legacy)
                : normalize(modern) != null ? normalize(modern)
                : normalized;
        logDebug("resolve key={} -> {} via unchanged", normalized, resolved);
        return resolved;
    }

    public static void rememberAliasMapping(@Nullable String aliasKey, @Nullable String value) {
        String key = normalize(aliasKey);
        String resolved = normalize(value);
        if (key == null || resolved == null || key.equals(resolved)) {
            return;
        }
        RUNTIME_ALIASES.put(key.toLowerCase(Locale.ROOT), resolved);
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id != null) {
            RUNTIME_ALIASES.put(id.getPath().toLowerCase(Locale.ROOT), resolved);
        }
        logDebug("remember runtime alias key={}, value={}", key, resolved);
    }

    @Nullable
    public static String consumeLastProviderSearchKey() {
        String modern = invokeString(RECIPE_TYPE_CONFIG_CLASS, "consumeLastProviderSearchKey", new Class<?>[]{});
        if (normalize(modern) != null) {
            return normalize(modern);
        }
        String legacy = invokeString(LEGACY_UTIL_CLASS, "consumeLastProviderSearchKey", new Class<?>[]{});
        return normalize(legacy);
    }

    public static void rememberUploadSearchKey(@Nullable String key) {
        String normalized = normalize(key);
        if (normalized != null) {
            lastRecipeSearchKey = normalized;
            logDebug("remember upload search key={}", normalized);
        }
    }

    @Nullable
    public static String takeProviderSearchKeyForUpload() {
        String consumed = consumeLastProviderSearchKey();
        if (normalize(consumed) != null) {
            lastRecipeSearchKey = normalize(consumed);
            return lastRecipeSearchKey;
        }
        return lastRecipeSearchKey;
    }

    public static void presetCraftingProviderSearchKey() {
        rememberUploadSearchKey("crafting");
        if (!invokeVoid(RECIPE_TYPE_CONFIG_CLASS, "presetCraftingProviderSearchKey")) {
            invokeVoid(LEGACY_UTIL_CLASS, "presetCraftingProviderSearchKey");
        }
    }

    public static void setLastProviderSearchKey(String value) {
        rememberUploadSearchKey(value);
        if (invokeVoid(RECIPE_TYPE_CONFIG_CLASS, "setLastProviderSearchKey",
                new Class<?>[]{String.class}, value)) {
            return;
        }
        if (invokeVoid(RECIPE_TYPE_CONFIG_CLASS, "setLastProcessingName",
                new Class<?>[]{String.class}, value)) {
            return;
        }
        if (invokeVoid(LEGACY_UTIL_CLASS, "setLastProviderSearchKey",
                new Class<?>[]{String.class}, value)) {
            return;
        }
        invokeVoid(LEGACY_UTIL_CLASS, "setLastProcessingName", new Class<?>[]{String.class}, value);
    }

    @Nullable
    public static String mapRecipeTypeToSearchKey(@Nullable Recipe<?> recipe) {
        if (recipe == null) {
            return null;
        }
        String modern = invokeString(RECIPE_TYPE_CONFIG_CLASS, "mapRecipeTypeToSearchKey",
                new Class<?>[]{Recipe.class}, recipe);
        if (normalize(modern) != null) {
            return normalize(modern);
        }
        String legacy = invokeString(LEGACY_UTIL_CLASS, "mapRecipeTypeToSearchKey",
                new Class<?>[]{Recipe.class}, recipe);
        return normalize(legacy);
    }

    @Nullable
    public static String getProviderDisplayName(@Nullable PatternContainer provider) {
        if (provider == null) {
            return null;
        }
        String modern = invokeString(RECIPE_TYPE_CONFIG_CLASS, "getProviderDisplayName",
                new Class<?>[]{PatternContainer.class}, provider);
        if (normalize(modern) != null) {
            return normalize(modern);
        }
        String legacy = invokeString(LEGACY_UTIL_CLASS, "getProviderDisplayName",
                new Class<?>[]{PatternContainer.class}, provider);
        if (normalize(legacy) != null) {
            return normalize(legacy);
        }
        return provider.getTerminalGroup() == null ? null : provider.getTerminalGroup().name().getString();
    }
    @Nullable
    public static String deriveSearchKeyFromUnknownRecipe(@Nullable Object recipeBase) {
        if (recipeBase == null) {
            return null;
        }
        String modern = invokeString(RECIPE_TYPE_CONFIG_CLASS, "deriveSearchKeyFromUnknownRecipe",
                new Class<?>[]{Object.class}, recipeBase);
        if (normalize(modern) != null) {
            return normalize(modern);
        }
        String legacy = invokeString(LEGACY_UTIL_CLASS, "deriveSearchKeyFromUnknownRecipe",
                new Class<?>[]{Object.class}, recipeBase);
        return normalize(legacy);
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    private static Boolean invokeBoolean(String className, String methodName, Class<?>[] parameterTypes,
                                        Object... args) {
        try {
            Object value = getMethod(className, methodName, parameterTypes).invoke(null, args);
            return value instanceof Boolean result ? result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer invokeInt(String className, String methodName, Class<?>[] parameterTypes,
                                     Object... args) {
        try {
            Object value = getMethod(className, methodName, parameterTypes).invoke(null, args);
            return value instanceof Integer result ? result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static String invokeString(String className, String methodName, Class<?>[] parameterTypes,
                                       Object... args) {
        try {
            Object value = getMethod(className, methodName, parameterTypes).invoke(null, args);
            return value instanceof String result ? result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeVoid(String className, String methodName, Class<?>[] parameterTypes,
                                      Object... args) {
        try {
            getMethod(className, methodName, parameterTypes).invoke(null, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeVoid(String className, String methodName) {
        return invokeVoid(className, methodName, new Class<?>[]{});
    }

    private static Method getMethod(String className, String methodName, Class<?>[] parameterTypes)
            throws ReflectiveOperationException {
        return Class.forName(className).getMethod(methodName, parameterTypes);
    }

    @Nullable
    private static String lookupRuntimeAlias(String rawKey) {
        String resolved = RUNTIME_ALIASES.get(rawKey.toLowerCase(Locale.ROOT));
        if (normalize(resolved) != null) {
            return normalize(resolved);
        }
        ResourceLocation id = ResourceLocation.tryParse(rawKey);
        if (id != null) {
            return normalize(RUNTIME_ALIASES.get(id.getPath().toLowerCase(Locale.ROOT)));
        }
        return null;
    }

    @Nullable
    private static String resolveSearchKeyAliasFallback(String rawKey) {
        JsonObject config = readConfig();
        if (config == null) {
            return null;
        }
        String direct = getString(config.get(rawKey));
        if (direct != null) {
            return direct;
        }
        String lower = rawKey.toLowerCase(Locale.ROOT);
        for (var entry : config.entrySet()) {
            String key = entry.getKey();
            String value = getString(entry.getValue());
            if (value == null) {
                continue;
            }
            if (key.equalsIgnoreCase(rawKey)) {
                return value;
            }
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null && (id.toString().equalsIgnoreCase(rawKey)
                    || id.getPath().equalsIgnoreCase(lower))) {
                return value;
            }
            if (!key.contains(":") && key.equalsIgnoreCase(lower)) {
                return value;
            }
        }
        return null;
    }

    private static boolean addOrUpdateAliasMappingFallback(@Nullable String aliasKey, @Nullable String value) {
        String key = normalize(aliasKey);
        String resolved = normalize(value);
        if (key == null || resolved == null) {
            return false;
        }
        try {
            JsonObject config = readConfig();
            if (config == null) {
                config = new JsonObject();
            }
            config.addProperty(key, resolved);
            writeConfig(config);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static int removeMappingsByCnValueFallback(@Nullable String value) {
        String target = normalize(value);
        if (target == null) {
            return 0;
        }
        try {
            JsonObject config = readConfig();
            if (config == null) {
                return 0;
            }
            int removed = 0;
            for (Iterator<Map.Entry<String, JsonElement>> it = config.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                if (target.equals(getString(entry.getValue()))) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                writeConfig(config);
            }
            return removed;
        } catch (IOException ignored) {
            return 0;
        }
    }

    private static void removeRuntimeMappingsByValue(@Nullable String value) {
        String target = normalize(value);
        if (target == null) {
            return;
        }
        RUNTIME_ALIASES.entrySet().removeIf(entry -> target.equals(entry.getValue()));
    }

    @Nullable
    private static JsonObject readConfig() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return null;
        }
        try {
            JsonObject object = GSON.fromJson(Files.readString(path), JsonObject.class);
            return object != null ? object : new JsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeConfig(JsonObject config) throws IOException {
        Path path = configPath();
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(config));
        logDebug("write mapping config path={}", path);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_RELATIVE);
    }

    @Nullable
    private static String getString(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        return normalize(element.getAsString());
    }

    private static void logDebug(String message, Object... args) {
        if (DEBUG_PATTERN_UPLOAD) {
            WcwtMod.LOGGER.info("WCWT pattern upload debug: " + message, args);
        }
    }
}
