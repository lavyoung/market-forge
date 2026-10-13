package com.lavyoung.marketforge.app.config.dcc;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DCC ZooKeeper 集成测试。
 *
 * <p>本测试启动 Curator 内嵌 ZooKeeper，验证真实的节点创建、修改、
 * UTF-8 内容传递以及 {@link DccFieldBinder} 字段动态更新链路。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class DccConfigIntegrationTest {

    private static final String ROOT_PATH = "/market-forge-dcc/config";

    private static TestingServer testingServer;

    private static CuratorFramework curatorClient;

    /**
     * 启动内嵌 ZooKeeper 和 Curator 客户端。
     *
     * @throws Exception 测试服务器或客户端启动失败时抛出
     */
    @BeforeAll
    static void setUp() throws Exception {
        testingServer = new TestingServer(true);
        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(testingServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(100, 3))
                .build();
        curatorClient.start();
        curatorClient.blockUntilConnected(10, TimeUnit.SECONDS);
    }

    /**
     * 关闭 Curator 客户端和内嵌 ZooKeeper。
     *
     * @throws Exception 资源关闭失败时抛出
     */
    @AfterAll
    static void tearDown() throws Exception {
        if (curatorClient != null) {
            curatorClient.close();
        }

        if (testingServer != null) {
            testingServer.close();
        }
    }

    /**
     * 验证配置节点创建和修改可以被监听器接收。
     *
     * <p>Given 一个监听器，When ZooKeeper 节点先创建再修改，
     * Then 回调依次收到 UTF-8 配置内容。
     *
     * @throws Exception Curator 操作或等待回调失败时抛出
     */
    @Test
    void shouldReceiveCreatedAndChangedNodeValue() throws Exception {
        String nodePath = ROOT_PATH + "/listener-test";
        CountDownLatch changedLatch = new CountDownLatch(2);
        java.util.List<String> receivedValues =
                java.util.Collections.synchronizedList(
                        new java.util.ArrayList<>()
                );

        DccConfigListener listener = new DccConfigListener(
                curatorClient,
                nodePath,
                value -> {
                    receivedValues.add(value);
                    changedLatch.countDown();
                }
        );

        try {
            listener.start();

            curatorClient.create()
                    .creatingParentsIfNeeded()
                    .forPath(
                            nodePath,
                            "开启".getBytes(StandardCharsets.UTF_8)
                    );

            curatorClient.setData()
                    .forPath(
                            nodePath,
                            "关闭".getBytes(StandardCharsets.UTF_8)
                    );

            assertTrue(
                    changedLatch.await(10, TimeUnit.SECONDS),
                    "配置创建和修改事件未在规定时间内到达"
            );
            assertEquals("开启", receivedValues.get(0));
            assertEquals("关闭", receivedValues.get(1));
        } finally {
            listener.close();
        }
    }

    /**
     * 验证字段绑定器能够使用 ZooKeeper 当前值初始化字段，
     * 并在后续修改时更新字段。
     *
     * @throws Exception Curator 操作或字段绑定失败时抛出
     */
    @Test
    void shouldBindAndUpdateBooleanField() throws Exception {
        String nodePath = ROOT_PATH + "/activity.degrade.switch";

        curatorClient.create()
                .creatingParentsIfNeeded()
                .forPath(nodePath, "false".getBytes(StandardCharsets.UTF_8));

        DccFieldBinder binder = new DccFieldBinder(
                ROOT_PATH,
                curatorClient
        );
        ActivityDccTarget target = new ActivityDccTarget();

        try {
            binder.bind(target);

            assertTrue(
                    waitForValue(() -> target.degradeSwitch, false),
                    "字段未使用 ZooKeeper 初始值完成绑定"
            );

            curatorClient.setData()
                    .forPath(nodePath, "true".getBytes(StandardCharsets.UTF_8));

            assertTrue(
                    waitForValue(() -> target.degradeSwitch, true),
                    "字段未随 ZooKeeper 配置变更而更新"
            );
        } finally {
            binder.close();
        }
    }

    /**
     * 在限定时间内等待字段达到预期值。
     *
     * @param valueSupplier 字段读取函数
     * @param expectedValue 预期字段值
     * @return 在限定时间内达到预期返回 {@code true}
     * @throws InterruptedException 当前线程被中断时抛出
     */
    private static boolean waitForValue(
            java.util.function.BooleanSupplier valueSupplier,
            boolean expectedValue) throws InterruptedException {

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(10);

        while (System.nanoTime() < deadline) {
            if (valueSupplier.getAsBoolean() == expectedValue) {
                return true;
            }

            Thread.sleep(50);
        }

        return false;
    }

    /**
     * 动态配置测试对象。
     */
    private static final class ActivityDccTarget {

        /**
         * 活动降级开关。
         */
        @DccValue("activity.degrade.switch:false")
        private volatile boolean degradeSwitch;
    }
}
