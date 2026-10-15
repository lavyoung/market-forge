package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ElasticsearchProperties} 单元测试。
 *
 * <p>验证连接配置的默认值、显式值、不可变性和必要参数边界，防止错误配置
 * 延迟到第一次访问 Elasticsearch 时才暴露。</p>
 */
class ElasticsearchPropertiesTest {

    @Test
    @DisplayName("未配置超时时间时应使用默认值")
    void shouldUseDefaultTimeoutsWhenTimeoutsAreAbsent() {
        // Given
        List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));

        // When
        ElasticsearchProperties properties = new ElasticsearchProperties(
                uris,
                "elastic",
                "password",
                null,
                null,
                null
        );
        uris.add("http://localhost:9201");

        // Then
        assertAll(
                () -> assertEquals(List.of("http://localhost:9200"), properties.uris()),
                () -> assertEquals(Duration.ofSeconds(3), properties.connectTimeout()),
                () -> assertEquals(Duration.ofSeconds(10), properties.socketTimeout()),
                () -> assertEquals(Duration.ofSeconds(3), properties.connectionRequestTimeout()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> properties.uris().add("http://localhost:9202")
                )
        );
    }

    @Test
    @DisplayName("配置超时时间时应保留显式值")
    void shouldKeepExplicitTimeouts() {
        // Given
        Duration connectTimeout = Duration.ofSeconds(1);
        Duration socketTimeout = Duration.ofSeconds(20);
        Duration connectionRequestTimeout = Duration.ofMillis(500);

        // When
        ElasticsearchProperties properties = new ElasticsearchProperties(
                List.of("http://localhost:9200"),
                "elastic",
                "password",
                connectTimeout,
                socketTimeout,
                connectionRequestTimeout
        );

        // Then
        assertAll(
                () -> assertEquals(connectTimeout, properties.connectTimeout()),
                () -> assertEquals(socketTimeout, properties.socketTimeout()),
                () -> assertEquals(connectionRequestTimeout, properties.connectionRequestTimeout())
        );
    }

    @Test
    @DisplayName("节点列表为空时应拒绝创建配置")
    void shouldRejectMissingUris() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> properties(null, "elastic", "password")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> properties(List.of(), "elastic", "password")
                )
        );
    }

    @Test
    @DisplayName("认证凭据为空白时应拒绝创建配置")
    void shouldRejectBlankCredentials() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> properties(List.of("http://localhost:9200"), " ", "password")
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> properties(List.of("http://localhost:9200"), "elastic", " ")
                )
        );
    }

    private ElasticsearchProperties properties(
            List<String> uris,
            String username,
            String password
    ) {
        return new ElasticsearchProperties(
                uris,
                username,
                password,
                null,
                null,
                null
        );
    }
}
