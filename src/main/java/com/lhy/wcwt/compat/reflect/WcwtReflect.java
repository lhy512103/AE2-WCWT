package com.lhy.wcwt.compat.reflect;

import com.lhy.wcwt.WcwtMod;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WCWT 对可选模组 / AE2 内部成员的反射访问统一入口。
 *
 * <p>项目里散落的 {@code Class.forName(...)} 全部收敛到这里，目的是保证三件事：
 *
 * <ol>
 *   <li><b>不阻断启动、不阻断进游戏。</b>所有查找与调用都吞掉 {@link Throwable}
 *       （含 {@link LinkageError} / {@code NoClassDefFoundError}），
 *       失败返回 {@link Optional#empty()} 或调用方给的默认值，绝不向外抛。</li>
 *   <li><b>模组未加载就不查。</b>传入 modId 时先过 {@code ModList.get().isLoaded(modId)}，
 *       未加载直接返回 empty，既不触发类加载也不打日志。</li>
 *   <li><b>只解析一次、只告警一次。</b>类和成员查找结果（含失败）全部缓存，
 *       失败原因对每个 key 只 warn 一次，避免每次开界面都刷屏。</li>
 * </ol>
 *
 * <p>用法：
 * <pre>{@code
 * // 可选模组的静态方法
 * Optional<Object> value = WcwtReflect.invokeStatic("extendedae_plus",
 *         "com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig",
 *         "resolveSearchKeyAlias", new Class<?>[]{String.class}, key);
 *
 * // AE2 自身的私有成员（declared + setAccessible）
 * WcwtReflect.findDeclaredMethod(MEStorageScreen.class, "updateScrollbar")
 *         .ifPresent(m -> WcwtReflect.invoke(this, m));
 * }</pre>
 *
 * <p>排查问题时加 {@code -Dwcwt.debug.reflect=true}，会输出每次失败的完整堆栈（不做去重）。
 */
public final class WcwtReflect {
    /** 打开后每次反射失败都打完整堆栈，用于排查可选模组版本不匹配。 */
    public static final boolean DEBUG = Boolean.getBoolean("wcwt.debug.reflect");

    private static final Map<String, Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Object>> ENUM_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private WcwtReflect() {
    }

    // ─────────────────────────── 类查找 ───────────────────────────

    /**
     * 查找类。modId 非空且未加载时直接返回 empty，不触发类加载也不告警。
     *
     * @param modId 目标所属模组 id；传 {@code null} 表示不做前置判断（用于 AE2 之类的硬依赖）
     */
    public static Optional<Class<?>> findClass(@Nullable String modId, String className) {
        if (!isModLoaded(modId)) {
            return Optional.empty();
        }
        return CLASS_CACHE.computeIfAbsent(className, WcwtReflect::loadClass);
    }

    public static Optional<Class<?>> findClass(String className) {
        return findClass(null, className);
    }

    // ─────────────────────────── 方法查找 ───────────────────────────

    /** 查找 public 方法（含继承）。 */
    public static Optional<Method> findMethod(@Nullable String modId, String className, String name,
                                              Class<?>... parameterTypes) {
        return findClass(modId, className)
                .flatMap(owner -> findMethod(owner, name, parameterTypes));
    }

    /** 在已知类上查找 public 方法（含继承）。 */
    public static Optional<Method> findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        String key = memberKey(owner, name, parameterTypes);
        return METHOD_CACHE.computeIfAbsent(key,
                ignored -> resolve(() -> owner.getMethod(name, parameterTypes), key, false));
    }

    /** 查找声明方法（含 private），并尝试 setAccessible。用于访问 AE2 内部实现。 */
    public static Optional<Method> findDeclaredMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        String key = memberKey(owner, name, parameterTypes) + "#declared";
        return METHOD_CACHE.computeIfAbsent(key,
                ignored -> resolve(() -> owner.getDeclaredMethod(name, parameterTypes), key, true));
    }

    // ─────────────────────────── 字段查找 ───────────────────────────

    /** 查找 public 字段（含继承）。 */
    public static Optional<Field> findField(@Nullable String modId, String className, String name) {
        return findClass(modId, className).flatMap(owner -> findField(owner, name));
    }

    /** 在已知类上查找 public 字段（含继承）。 */
    public static Optional<Field> findField(Class<?> owner, String name) {
        String key = owner.getName() + "#" + name;
        return FIELD_CACHE.computeIfAbsent(key,
                ignored -> resolve(() -> owner.getField(name), key, false));
    }

    /** 查找声明字段（含 private），并尝试 setAccessible。 */
    public static Optional<Field> findDeclaredField(Class<?> owner, String name) {
        String key = owner.getName() + "#" + name + "#declared";
        return FIELD_CACHE.computeIfAbsent(key,
                ignored -> resolve(() -> owner.getDeclaredField(name), key, true));
    }

    // ─────────────────────────── 调用 ───────────────────────────

    /** 调用静态方法，返回结果值（void 方法返回 empty）。 */
    public static Optional<Object> invokeStatic(@Nullable String modId, String className, String name,
                                                Class<?>[] parameterTypes, Object... args) {
        return findMethod(modId, className, name, parameterTypes)
                .flatMap(method -> invoke(null, method, args));
    }

    /** 调用静态方法（无参）。 */
    public static Optional<Object> invokeStatic(@Nullable String modId, String className, String name) {
        return invokeStatic(modId, className, name, new Class<?>[0]);
    }

    /**
     * 执行方法调用。target 为 {@code null} 表示静态方法。
     * 调用失败（参数类型不符、目标抛异常、类初始化失败等）返回 empty。
     */
    public static Optional<Object> invoke(@Nullable Object target, @Nullable Method method, Object... args) {
        if (method == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(method.invoke(target, args));
        } catch (Throwable t) {
            logDebug("invoke failed: {} on {}", method.getName(),
                    target == null ? method.getDeclaringClass().getName() : target.getClass().getName(), t);
            return Optional.empty();
        }
    }

    /**
     * 执行调用并只关心是否成功，不取返回值。
     * 和 {@link #invoke} 的区别：void 方法的返回值是 null，用 {@code Optional} 无法区分
     * 「调用成功」和「方法不存在」，这里用 boolean 表达。
     */
    public static boolean run(@Nullable Object target, @Nullable Method method, Object... args) {
        if (method == null) {
            return false;
        }
        try {
            method.invoke(target, args);
            return true;
        } catch (Throwable t) {
            logDebug("invoke failed: {} on {}", method.getName(),
                    target == null ? method.getDeclaringClass().getName() : target.getClass().getName(), t);
            return false;
        }
    }

    /** 执行静态方法并只关心是否成功（无参）。 */
    public static boolean runStatic(@Nullable String modId, String className, String name) {
        return runStatic(modId, className, name, new Class<?>[0]);
    }

    /** 执行静态方法并只关心是否成功。 */
    public static boolean runStatic(@Nullable String modId, String className, String name,
                                    Class<?>[] parameterTypes, Object... args) {
        return findMethod(modId, className, name, parameterTypes)
                .map(method -> run(null, method, args))
                .orElse(false);
    }

    /** 读取字段值。target 为 {@code null} 表示静态字段。 */
    public static Optional<Object> readField(@Nullable Object target, @Nullable Field field) {
        if (field == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(field.get(target));
        } catch (Throwable t) {
            logDebug("read field failed: {}", field.getName(), t);
            return Optional.empty();
        }
    }

    /** 读取静态字段。 */
    public static Optional<Object> readStaticField(@Nullable String modId, String className, String name) {
        return findField(modId, className, name).flatMap(field -> readField(null, field));
    }

    /** 写入字段值，返回是否成功。target 为 {@code null} 表示静态字段。 */
    public static boolean writeField(@Nullable Object target, @Nullable Field field, @Nullable Object value) {
        if (field == null) {
            return false;
        }
        try {
            field.set(target, value);
            return true;
        } catch (Throwable t) {
            logDebug("write field failed: {}", field.getName(), t);
            return false;
        }
    }

    // ─────────────────────────── 枚举与构造 ───────────────────────────

    /** 取可选模组里的枚举常量，失败返回 empty。 */
    public static Optional<Object> enumConstant(@Nullable String modId, String className, String constantName) {
        String key = className + "#" + constantName;
        Optional<Object> cached = ENUM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (!isModLoaded(modId)) {
            return Optional.empty();
        }
        Optional<Object> value = findClass(modId, className).flatMap(owner -> {
            if (!owner.isEnum()) {
                warnOnce(key, className + " is not an enum");
                return Optional.empty();
            }
            try {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object constant = Enum.valueOf((Class<Enum>) owner.asSubclass(Enum.class), constantName);
                return Optional.of(constant);
            } catch (Throwable t) {
                warnOnce(key, "enum constant " + constantName + " missing", t);
                return Optional.empty();
            }
        });
        ENUM_CACHE.put(key, value);
        return value;
    }

    /** 反射构造实例，失败返回 empty。 */
    public static Optional<Object> construct(@Nullable String modId, String className,
                                             Class<?>[] parameterTypes, Object... args) {
        if (!isModLoaded(modId)) {
            return Optional.empty();
        }
        return findClass(modId, className).flatMap(owner -> {
            try {
                Constructor<?> constructor = owner.getConstructor(parameterTypes);
                return Optional.ofNullable(constructor.newInstance(args));
            } catch (Throwable t) {
                warnOnce(owner.getName() + "#<init>", "constructor unavailable", t);
                return Optional.empty();
            }
        });
    }

    /** 判断 stack 所属物品是否为目标类的实例（等价于 {@code Class.isInstance}）。 */
    public static boolean isInstance(@Nullable String modId, String className, Object value) {
        if (value == null) {
            return false;
        }
        return findClass(modId, className).map(owner -> owner.isInstance(value)).orElse(false);
    }

    // ─────────────────────────── 内部实现 ───────────────────────────

    /**
     * 加载类但不触发静态初始化（{@code initialize=false}）。
     * 可选模组的静态初始化块一旦抛异常就会炸到调用栈上，这里延后到真正调用时再触发，
     * 而那时已经在 {@link #invoke} 的 try 里，不会外泄。
     */
    private static Optional<Class<?>> loadClass(String className) {
        try {
            return Optional.ofNullable(
                    Class.forName(className, false, WcwtReflect.class.getClassLoader()));
        } catch (Throwable t) {
            warnOnce(className, "class not found", t);
            return Optional.empty();
        }
    }

    private static <T extends java.lang.reflect.AccessibleObject & java.lang.reflect.Member> Optional<T> resolve(
            MemberResolver<T> resolver, String key, boolean makeAccessible) {
        try {
            T member = resolver.resolve();
            if (makeAccessible) {
                member.setAccessible(true);
            }
            return Optional.of(member);
        } catch (Throwable t) {
            warnOnce(key, "member not found", t);
            return Optional.empty();
        }
    }

    /** 允许抛 checked 反射异常的查找器；{@link java.util.function.Supplier} 做不到这一点。 */
    @FunctionalInterface
    private interface MemberResolver<T> {
        T resolve() throws ReflectiveOperationException;
    }

    private static boolean isModLoaded(@Nullable String modId) {
        if (modId == null) {
            return true;
        }
        try {
            return ModList.get().isLoaded(modId);
        } catch (Throwable t) {
            // ModList 在极早期（类初始化阶段）可能还没就绪；此时不去猜，直接放行让后面的查找兜住。
            logDebug("ModList lookup failed for {}", modId, t);
            return true;
        }
    }

    private static String memberKey(Class<?> owner, String name, Class<?>[] parameterTypes) {
        var builder = new StringBuilder(owner.getName()).append('#').append(name).append('(');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[i] == null ? "<null>" : parameterTypes[i].getName());
        }
        return builder.append(')').toString();
    }

    private static void warnOnce(String key, String message) {
        warnOnce(key, message, null);
    }

    private static void warnOnce(String key, String message, @Nullable Throwable cause) {
        if (WARNED.add(key)) {
            if (cause == null) {
                WcwtMod.LOGGER.warn("WCWT optional integration disabled ({}): {}", key, message);
            } else {
                WcwtMod.LOGGER.warn("WCWT optional integration disabled ({}): {}", key, message, cause);
            }
        } else {
            logDebug("optional integration still unavailable: {}", key);
        }
    }

    /**
     * 调试日志。末尾若跟了 {@link Throwable}，SLF4J 会自动把它当作异常堆栈打印。
     * 只在 {@code -Dwcwt.debug.reflect=true} 时输出，不做去重。
     */
    private static void logDebug(String message, Object... args) {
        if (!DEBUG) {
            return;
        }
        WcwtMod.LOGGER.info("WCWT reflect debug: " + message, args);
    }
}
