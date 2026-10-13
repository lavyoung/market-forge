package com.lavyoung.marketforge.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ZooKeeper 动态配置中心连接属性。
 *
 * <p>该对象保存 ZooKeeper 客户端连接参数和动态配置根路径，
 * 不负责创建客户端，也不负责执行字段绑定。
 *
 * @param connectString       ZooKeeper 服务连接地址
 * @param baseSleepTimeMs     Curator 重试初始等待时间，单位为毫秒
 * @param maxRetries          Curator 最大重试次数
 * @param sessionTimeoutMs    ZooKeeper 会话超时时间，单位为毫秒
 * @param connectionTimeoutMs 建立 ZooKeeper 连接的超时时间，单位为毫秒
 * @param rootPath            动态配置在 ZooKeeper 中的根路径
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@ConfigurationProperties(prefix = "zookeeper.dcc")
public record ZooKeeperDccProperties(
        String connectString,
        int baseSleepTimeMs,
        int maxRetries,
        int sessionTimeoutMs,
        int connectionTimeoutMs,
        String rootPath
) {
}
