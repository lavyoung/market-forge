package com.lavyoung.marketforge.app.config.dcc;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置字段绑定器。
 *
 * <p>本类负责扫描业务对象中的 {@link DccValue} 字段，
 * 将字段和 ZooKeeper 配置节点建立绑定关系。
 *
 * <p>字段绑定完成后，ZooKeeper 中的配置发生变化时，
 * 监听器会把字符串配置转换成字段实际类型，
 * 再写回业务对象。
 *
 * <p>本类不负责扫描 Spring Bean。Spring Bean 的扫描和调用
 * {@link #bind(Object)} 由后续注册器负责。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
@Slf4j
public class DccFieldBinder implements AutoCloseable {

    /**
     * ZooKeeper 动态配置根路径。
     */
    private final String rootPath;

    /**
     * Curator ZooKeeper 客户端。
     */
    private final CuratorFramework curatorClient;

    /**
     * 已注册的动态配置监听器。
     */
    private final List<DccConfigListener> listeners = new ArrayList<>();

    /**
     * 已经注册过的配置键。
     *
     * <p>使用并发集合，避免多个 Bean 同时绑定相同配置键。
     */
    private final Map<String, Object> registeredKeys = new ConcurrentHashMap<>();

    /**
     * 创建字段绑定器。
     *
     * @param rootPath      ZooKeeper 动态配置根路径
     * @param curatorClient Curator ZooKeeper 客户端
     * @throws NullPointerException     参数为空时抛出
     * @throws IllegalArgumentException 根路径为空或不是绝对路径时抛出
     */
    public DccFieldBinder(String rootPath, CuratorFramework curatorClient) {
        this.rootPath = requireRootPath(rootPath);
        this.curatorClient = Objects.requireNonNull(curatorClient, "curatorClient must not be null");
    }

    /**
     * 扫描对象中的动态配置字段并完成绑定。
     *
     * <p>当前实现扫描对象的全部声明字段。
     * 每个被 {@link DccValue} 标记的字段都会经历以下流程：
     *
     * <ol>
     *     <li>解析配置键和默认值；</li>
     *     <li>使用默认值初始化字段；</li>
     *     <li>创建 ZooKeeper 配置监听器；</li>
     *     <li>配置变化后更新字段。</li>
     * </ol>
     *
     * @param target 待绑定的业务对象
     * @throws NullPointerException     目标对象为空时抛出
     * @throws IllegalArgumentException 配置声明或字段类型不合法时抛出
     * @throws Exception                创建或启动 ZooKeeper 监听器失败时抛出
     */
    public synchronized void bind(Object target) throws Exception {
        Objects.requireNonNull(target, "target must not be null");
        Class<?> targetClass = target.getClass();

        for (Field field : targetClass.getDeclaredFields()) {
            DccValue annotation = field.getAnnotation(DccValue.class);
            if (annotation == null) {
                continue;
            }
            // 解析dcc配置
            DccProperty dccProperty = parseProperty(annotation.value());
            // 注册配置字段
            registerField(target, field, dccProperty);
        }
    }

    /**
     * 解析 {@code key:defaultValue} 格式的配置声明。
     *
     * <p>使用 {@code split(":", 2)}，确保默认值内部即使包含冒号，
     * 也不会被错误拆分。
     *
     * @param definition 配置定义
     * @return 解析后的配置键和默认值
     * @throws NullPointerException     配置定义为空时抛出
     * @throws IllegalArgumentException 配置定义格式错误时抛出
     */
    private static DccProperty parseProperty(String definition) {
        Objects.requireNonNull(definition, "DCC definition must not be null");
        String[] parts = definition.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("DCC definition must use key:defaultValue format: " + definition);
        }
        String key = parts[0].trim();
        String defaultValue = parts[1].trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("DCC key must not be blank");
        }
        return new DccProperty(key, defaultValue);
    }

    /**
     * 注册单个动态配置字段。
     *
     * @param target   动态配置所属对象
     * @param field    动态配置字段
     * @param property 解析后的配置定义
     * @throws Exception 启动配置监听器失败时抛出
     */
    private void registerField(Object target, Field field, DccProperty property) throws Exception {
        Object previousTarget = registeredKeys.putIfAbsent(property.key, target);

        if (previousTarget != null && previousTarget != target) {
            throw new IllegalArgumentException("Duplicate DCC key: " + property.key());
        }

        setFieldValue(target, field, property.defaultValue);
        String nodePath = rootPath + "/" + property.key;
        DccConfigListener listener = new DccConfigListener(curatorClient, nodePath, value -> setFieldValue(target, field, value));
        listeners.add(listener);
        listener.start();
        log.info("DCC 字段绑定完成，key={}, field={}.{}", property.key(), target.getClass().getSimpleName(), field.getName());
    }

    /**
     * 将字符串配置转换成字段实际类型并写入字段。
     *
     * @param target   目标对象
     * @param field    待修改字段
     * @param rawValue 字符串配置值
     * @throws IllegalArgumentException 配置值无法转换时抛出
     */
    private static void setFieldValue(Object target, Field field, String rawValue) {
        boolean canAccess = field.canAccess(target);

        try {
            field.setAccessible(true);
            Object convertValue = convertValue(field.getType(), rawValue);
            field.set(target, convertValue);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to update DCC field: " + field.getName(), e);
        } finally {
            field.setAccessible(canAccess);
        }
    }

    /**
     * 将字符串转换成字段声明类型。
     *
     * @param fieldType 字段类型
     * @param rawValue  原始字符串
     * @return 转换后的字段值
     * @throws IllegalArgumentException 不支持字段类型或值格式错误时抛出
     */
    private static Object convertValue(Class<?> fieldType, String rawValue) {
        Objects.requireNonNull(rawValue, "DCC value must not be null");
        try {
            if (fieldType == String.class) {
                return rawValue;
            }

            if (fieldType == boolean.class || fieldType == Boolean.class) {
                return Boolean.parseBoolean(rawValue);
            }

            if (fieldType == int.class || fieldType == Integer.class) {
                return Integer.parseInt(rawValue);
            }

            if (fieldType == long.class || fieldType == Long.class) {
                return Long.parseLong(rawValue);
            }

            if (fieldType == double.class || fieldType == Double.class) {
                return Double.parseDouble(rawValue);
            }

            throw new IllegalArgumentException("Unsupported DCC field type: " + fieldType.getName());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid DCC value: " + rawValue + ", fieldType=" + fieldType.getName(), exception);
        }
    }

    /**
     * 校验动态配置根路径。
     *
     * @param rootPath 动态配置根路径
     * @return 清理首尾空白后的根路径
     * @throws NullPointerException     根路径为空时抛出
     * @throws IllegalArgumentException 根路径不是绝对路径时抛出
     */
    private static String requireRootPath(String rootPath) {
        Objects.requireNonNull(rootPath, "rootPath must not be null");

        String normalizedPath = rootPath.trim();

        if (!normalizedPath.startsWith("/")) {
            throw new IllegalArgumentException("rootPath must be an absolute ZooKeeper path");
        }
        return normalizedPath.endsWith("/") ? normalizedPath.substring(0, normalizedPath.length() - 1) : normalizedPath;
    }

    /**
     * 关闭全部动态配置监听器并释放绑定资源。
     *
     * <p>监听器本身的 {@code close()} 不会抛出受检异常，
     * 因此这里不再声明 {@code throws Exception}，
     * 便于使用方法引用批量关闭。
     */
    @Override
    public void close() {
        listeners.forEach(DccConfigListener::close);
        listeners.clear();
        registeredKeys.clear();
        log.info("DCC 字段绑定器已关闭");
    }

    /**
     * 已解析的动态配置定义。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     */
    private record DccProperty(String key, String defaultValue) {

    }
}
