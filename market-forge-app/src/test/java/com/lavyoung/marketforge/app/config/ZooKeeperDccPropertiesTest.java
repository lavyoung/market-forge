package com.lavyoung.marketforge.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ZooKeeperDccProperties} 配置载体测试。
 *
 * <p>验证 ZooKeeper 连接参数和动态配置根路径能够完整保存，
 * 防止新增配置字段后构造顺序或访问方法出现错误。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class ZooKeeperDccPropertiesTest {

    /**
     * 验证所有配置属性都能被 record 正确保存。
     */
    @Test
    void shouldKeepZooKeeperDccProperties() {
        ZooKeeperDccProperties properties = new ZooKeeperDccProperties(
                "192.168.3.127:2181",
                1000,
                3,
                18000,
                30000,
                "/market-forge-dcc/config"
        );

        assertEquals("192.168.3.127:2181", properties.connectString());
        assertEquals(1000, properties.baseSleepTimeMs());
        assertEquals(3, properties.maxRetries());
        assertEquals(18000, properties.sessionTimeoutMs());
        assertEquals(30000, properties.connectionTimeoutMs());
        assertEquals("/market-forge-dcc/config", properties.rootPath());
    }
}
