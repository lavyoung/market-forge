package com.lavyoung.marketforge.app.config.dcc;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * {@link DccConfigListener} 单元测试。
 *
 * <p>本测试不连接真实 ZooKeeper，只验证监听器构造参数和资源关闭边界。
 * CuratorCache 的真实创建、修改和删除事件由后续集成测试覆盖。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class DccConfigListenerTest {

    /**
     * 验证 ZooKeeper 客户端为空时拒绝创建监听器。
     */
    @Test
    void shouldRejectNullCuratorClient() {
        Consumer<String> consumer = value -> {
        };

        assertThrows(
                NullPointerException.class,
                () -> new DccConfigListener(
                        null,
                        "/market-forge-dcc/config/test",
                        consumer
                )
        );
    }

    /**
     * 验证节点路径必须是绝对路径。
     */
    @Test
    void shouldRejectRelativeNodePath() {
        CuratorFramework client = mock(CuratorFramework.class);
        Consumer<String> consumer = value -> {
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> new DccConfigListener(
                        client,
                        "market-forge-dcc/config/test",
                        consumer
                )
        );
    }

    /**
     * 验证配置回调为空时拒绝创建监听器。
     */
    @Test
    void shouldRejectNullValueConsumer() {
        CuratorFramework client = mock(CuratorFramework.class);

        assertThrows(
                NullPointerException.class,
                () -> new DccConfigListener(
                        client,
                        "/market-forge-dcc/config/test",
                        null
                )
        );
    }

    /**
     * 验证监听器关闭操作可以重复执行而不抛异常。
     *
     * <p>关闭幂等性可以降低 Spring 容器销毁和测试清理重复调用的风险。
     */
    @Test
    void shouldCloseListenerSafely() {
        CuratorFramework client = mock(CuratorFramework.class);
        DccConfigListener listener = new DccConfigListener(
                client,
                "/market-forge-dcc/config/test",
                value -> {
                }
        );

        assertDoesNotThrow(listener::close);
        assertDoesNotThrow(listener::close);
    }
}
