package com.lavyoung.marketforge.app.config.dcc;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * ZooKeeper 动态配置节点监听器。
 *
 * <p>一个监听器实例对应 ZooKeeper 中的一个具体配置节点。
 * 当节点首次加载或节点内容发生变化时，监听器会读取节点字节数据，
 * 按 UTF-8 编码转换为字符串，再交给业务回调处理。
 *
 * <p>本类只负责配置监听和数据转换，不负责字段反射、业务判断
 * 或配置类型转换，保持基础设施职责单一。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Slf4j
public final class DccConfigListener implements AutoCloseable {

    /**
     * Curator ZooKeeper 客户端。
     */
    private final CuratorFramework curatorClient;

    /**
     * 被监听的 ZooKeeper 节点路径。
     */
    private final String nodePath;

    /**
     * 配置值变化后的处理回调。
     */
    private final Consumer<String> valueConsumer;

    /**
     * Curator 节点缓存监听器。
     */
    private final CuratorCache curatorCache;

    /**
     * 创建 ZooKeeper 动态配置监听器。
     *
     * @param curatorClient Curator ZooKeeper 客户端
     * @param nodePath      ZooKeeper 节点路径
     * @param valueConsumer 配置值变化后的处理回调
     * @throws NullPointerException     参数为空时抛出
     * @throws IllegalArgumentException 节点路径为空或不是绝对路径时抛出
     */
    public DccConfigListener(CuratorFramework curatorClient, String nodePath, Consumer<String> valueConsumer) {
        this.curatorClient = Objects.requireNonNull(curatorClient, "curatorClient must not be null");
        this.nodePath = requireNodePath(nodePath);
        this.valueConsumer = Objects.requireNonNull(valueConsumer, "valueConsumer must not be null");
        ;
        this.curatorCache = CuratorCache.build(curatorClient, nodePath);
    }

    /**
     * 启动节点监听。
     *
     * <p>{@code buildInitial=true} 表示启动时立即读取当前节点内容，
     * 从而保证应用启动时就能得到最新配置，而不是必须等待下一次变更。
     *
     * @throws Exception 启动节点缓存失败时抛出
     */
    public void start() throws Exception {
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(this::handleCreated)
                .forChanges(this::handleChanged)
                .forDeletes(this::handleDeleted)
                .forInitialized(this::handleInitialized).build();

        curatorCache.listenable().addListener(listener);
        curatorCache.start();
        log.info("ZooKeeper 动态配置监听已启动，path={}", nodePath);
    }

    /**
     * 处理节点创建事件。
     *
     * @param childData 新创建的节点数据
     */
    private void handleCreated(ChildData childData) {
        if (!isTargetNode(childData)) {
            return;
        }
        String value = decodeValue(childData);
        notifyValueChanged(value);
    }

    /**
     * 处理节点修改事件。
     *
     * @param oldData 修改前的节点数据
     * @param newData 修改后的节点数据
     */
    private void handleChanged(ChildData oldData, ChildData newData) {
        if (!isTargetNode(newData)) {
            return;
        }
        String value = decodeValue(newData);
        notifyValueChanged(value);
    }

    /**
     * 处理节点删除事件。
     *
     * <p>删除配置时暂时只记录日志，不直接把空字符串推送给业务字段。
     * 后续字段绑定器可以根据默认值策略决定是否恢复默认配置。
     *
     * @param childData 被删除的节点数据
     */
    private void handleDeleted(ChildData childData) {
        if (!isTargetNode(childData)) {
            return;
        }
        log.warn("ZooKeeper 动态配置节点已删除，path={}", nodePath);
    }

    /**
     * 处理缓存初始化完成事件。
     *
     * <p>CuratorCache 启动后会先把 ZooKeeper 当前数据加载到本地缓存。
     * 初始化完成后，再从缓存中读取目标节点并执行一次回调。
     */
    private void handleInitialized() {
        curatorCache.get(nodePath).ifPresent(childData -> {
            String value = decodeValue(childData);
            notifyValueChanged(value);
        });
        log.debug("ZooKeeper 动态配置缓存初始化完成，path={}", nodePath);
    }

    /**
     * 将节点数据转换为 UTF-8 字符串。
     *
     * @param childData ZooKeeper 节点数据
     * @return UTF-8 编码的配置值
     */
    private String decodeValue(ChildData childData) {
        byte[] data = childData.getData();

        if (data == null) {
            return "";
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * 将配置值交给业务回调。
     *
     * @param value 配置字符串
     */
    private void notifyValueChanged(String value) {
        log.info("ZooKeeper 动态配置发生变化，path={}, value={}", nodePath, value);
        valueConsumer.accept(value);
    }

    /**
     * 判断事件节点是否为当前监听节点。
     *
     * @param childData 节点数据
     * @return 是当前节点返回 {@code true}，否则返回 {@code false}
     */
    private boolean isTargetNode(ChildData childData) {
        return childData != null && nodePath.equals(childData.getPath());
    }

    /**
     * 校验节点路径。
     *
     * @param nodePath ZooKeeper 节点路径
     * @return 清理首尾空白后的节点路径
     * @throws NullPointerException     节点路径为空时抛出
     * @throws IllegalArgumentException 节点路径为空白或不是绝对路径时抛出
     */
    private static String requireNodePath(String nodePath) {
        Objects.requireNonNull(nodePath, "nodePath must not be null");
        String normalizedPath = nodePath.trim();
        if (normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("nodePath must not be blank");
        }

        if (!normalizedPath.startsWith("/")) {
            throw new IllegalArgumentException("nodePath must be an absolute ZooKeeper path");
        }

        return normalizedPath;
    }

    /**
     * 关闭监听器并释放 CuratorCache 资源。
     *
     * <p>本方法只关闭当前节点缓存，
     * 不关闭由 Spring 管理的 Curator 客户端。
     */
    @Override
    public void close() {
        curatorCache.close();

        log.info("ZooKeeper 动态配置监听已关闭，path={}", nodePath);
    }
}
