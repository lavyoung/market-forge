package com.lavyoung.marketforge.app.config.dcc;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * {@link DccFieldBinder} 单元测试。
 *
 * <p>测试重点是动态配置声明的边界校验，确保错误配置在应用启动阶段
 * 直接失败，而不是等到 ZooKeeper 发生变更后才暴露问题。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class DccFieldBinderTest {

    /**
     * 验证根路径为空时拒绝创建绑定器。
     *
     * <p>Given 根路径为空，When 创建绑定器，Then 抛出非法参数异常。
     */
    @Test
    void shouldRejectBlankRootPath() {
        CuratorFramework curatorClient = mock(CuratorFramework.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> new DccFieldBinder("   ", curatorClient)
        );
    }

    /**
     * 验证相对路径不能作为 ZooKeeper 根路径。
     *
     * <p>Given 根路径不是绝对路径，When 创建绑定器，Then 抛出非法参数异常。
     */
    @Test
    void shouldRejectRelativeRootPath() {
        CuratorFramework curatorClient = mock(CuratorFramework.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> new DccFieldBinder("market-forge-dcc", curatorClient)
        );
    }

    /**
     * 验证目标对象为空时拒绝绑定。
     *
     * <p>Given 绑定器已经创建，When 传入空目标对象，Then 抛出空指针异常。
     */
    @Test
    void shouldRejectNullTarget() {
        CuratorFramework curatorClient = mock(CuratorFramework.class);
        DccFieldBinder binder = new DccFieldBinder(
                "/market-forge-dcc/config",
                curatorClient
        );

        assertThrows(
                NullPointerException.class,
                () -> binder.bind(null)
        );
    }

    /**
     * 验证动态配置声明必须包含默认值。
     *
     * <p>Given 注解声明缺少冒号和默认值，When 扫描目标对象，
     * Then 抛出非法参数异常。
     */
    @Test
    void shouldRejectMalformedDccDefinition() throws Exception {
        CuratorFramework curatorClient = mock(CuratorFramework.class);
        DccFieldBinder binder = new DccFieldBinder(
                "/market-forge-dcc/config",
                curatorClient
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> binder.bind(new MalformedDccTarget())
        );
    }

    /**
     * 验证当前不支持的字段类型会被拒绝。
     *
     * <p>Given 动态配置字段是未支持的类型，When 执行绑定，
     * Then 抛出非法参数异常，而不是静默忽略配置。
     */
    @Test
    void shouldRejectUnsupportedFieldType() throws Exception {
        CuratorFramework curatorClient = mock(CuratorFramework.class);
        DccFieldBinder binder = new DccFieldBinder(
                "/market-forge-dcc/config",
                curatorClient
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> binder.bind(new UnsupportedTypeDccTarget())
        );
    }

    /**
     * 配置声明格式错误的测试对象。
     */
    private static final class MalformedDccTarget {

        /**
         * 缺少默认值的动态配置。
         */
        @DccValue("activity.degrade.switch")
        private boolean degradeSwitch;
    }

    /**
     * 使用未支持类型的测试对象。
     */
    private static final class UnsupportedTypeDccTarget {

        /**
         * 当前绑定器尚未支持的枚举字段。
         */
        @DccValue("activity.status:ACTIVE")
        private TestStatus status;
    }

    /**
     * 用于验证非法字段类型的测试枚举。
     */
    private enum TestStatus {
        ACTIVE
    }
}
