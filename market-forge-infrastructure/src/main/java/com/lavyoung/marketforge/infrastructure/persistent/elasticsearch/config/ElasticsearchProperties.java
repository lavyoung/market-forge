package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Elasticsearch 客户端连接配置。
 *
 * <p>该配置对象负责接收不同运行环境提供的节点地址、认证信息和网络超时时间。
 * 它只描述连接参数，不创建客户端，也不包含任何索引或业务查询配置。</p>
 *
 * @param uris                     Elasticsearch HTTP 节点地址列表
 * @param username                 Elasticsearch Basic Authentication 用户名
 * @param password                 Elasticsearch Basic Authentication 密码
 * @param connectTimeout           建立 TCP 连接的最大等待时间
 * @param socketTimeout            建立连接后等待响应数据的最大时间
 * @param connectionRequestTimeout 从 HTTP 连接池获取连接的最大等待时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
@ConfigurationProperties(prefix = "market-forge.elasticsearch")
public record ElasticsearchProperties(
        List<String> uris,
        String username,
        String password,
        Duration connectTimeout,
        Duration socketTimeout,
        Duration connectionRequestTimeout
) {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(3);

    /**
     * 校验必要配置并补充默认超时时间。
     *
     * <p>节点列表在对象创建时复制为不可变集合，避免配置对象构造完成后被外部代码修改。
     * 超时时间未配置时使用适合开发环境的保守默认值。</p>
     *
     * @param uris                     Elasticsearch HTTP 节点地址列表
     * @param username                 Elasticsearch 认证用户名
     * @param password                 Elasticsearch 认证密码
     * @param connectTimeout           建立连接的最大等待时间
     * @param socketTimeout            等待响应数据的最大时间
     * @param connectionRequestTimeout 从连接池获取连接的最大等待时间
     * @throws IllegalArgumentException 节点列表为空、用户名为空或密码为空时抛出
     */
    public ElasticsearchProperties {
        if (uris == null || uris.isEmpty()) {
            throw new IllegalArgumentException("market-forge.elasticsearch.uris must not be empty");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("market-forge.elasticsearch.username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("market-forge.elasticsearch.password must not be blank");
        }

        uris = List.copyOf(uris);
        connectTimeout = Objects.requireNonNullElse(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
        socketTimeout = Objects.requireNonNullElse(socketTimeout, DEFAULT_SOCKET_TIMEOUT);
        connectionRequestTimeout = Objects.requireNonNullElse(connectionRequestTimeout, DEFAULT_CONNECTION_REQUEST_TIMEOUT);
    }
}
