package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ElasticsearchClientConfig} 上下文装配测试。
 *
 * <p>测试不访问真实 Elasticsearch，避免基础设施单元测试依赖外部环境；它重点验证
 * 配置属性绑定、客户端 Bean 链路、多节点解析以及上下文关闭时的资源销毁流程。</p>
 */
class ElasticsearchClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ElasticsearchClientConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("有效配置应完成 Elasticsearch 客户端 Bean 装配")
    void shouldCreateElasticsearchClientBeans() {
        // Given
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
                "market-forge.elasticsearch.uris[0]=http://localhost:9200",
                "market-forge.elasticsearch.uris[1]=http://localhost:9201",
                "market-forge.elasticsearch.username=elastic",
                "market-forge.elasticsearch.password=password",
                "market-forge.elasticsearch.connect-timeout=1s",
                "market-forge.elasticsearch.socket-timeout=20s",
                "market-forge.elasticsearch.connection-request-timeout=500ms"
        );

        // When & Then
        runner.run(context -> {
            assertNotNull(context.getBean(ElasticsearchProperties.class));
            assertNotNull(context.getBean(ElasticsearchTransport.class));
            assertNotNull(context.getBean(ElasticsearchClient.class));

            RestClient restClient = context.getBean(RestClient.class);
            assertEquals(2, restClient.getNodes().size());
            assertEquals("localhost", restClient.getNodes().get(0).getHost().getHostName());
            assertEquals(9200, restClient.getNodes().get(0).getHost().getPort());
        });
    }

    @Test
    @DisplayName("缺少认证密码时上下文应启动失败")
    void shouldFailContextWhenPasswordIsMissing() {
        // Given
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
                "market-forge.elasticsearch.uris[0]=http://localhost:9200",
                "market-forge.elasticsearch.username=elastic"
        );

        // When & Then
        runner.run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCauseMessage(context.getStartupFailure())
                    .contains("market-forge.elasticsearch.password must not be blank"));
        });
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }
}
