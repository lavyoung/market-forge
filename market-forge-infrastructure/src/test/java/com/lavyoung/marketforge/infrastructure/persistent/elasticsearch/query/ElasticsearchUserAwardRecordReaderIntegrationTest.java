package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler.UserAwardRecordDocumentAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.config.ElasticsearchClientConfig;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.config.ElasticsearchProperties;
import com.lavyoung.marketforge.types.model.PageResult;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link ElasticsearchUserAwardRecordReader} 真实 Elasticsearch 集成测试。
 *
 * <p>该测试用于验证官方 Java Client、Jackson 时间反序列化、查询 DSL、索引别名和
 * 应用查询结果转换能够协同工作。测试默认跳过，只有显式设置环境变量
 * {@code ELASTICSEARCH_INTEGRATION_TEST=true} 时才访问外部开发环境。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@EnabledIfEnvironmentVariable(named = "ELASTICSEARCH_INTEGRATION_TEST", matches = "true")
class ElasticsearchUserAwardRecordReaderIntegrationTest {

    /**
     * 验证 Canal 增量同步的文档可以通过应用查询读取器完整查询和转换。
     *
     * @throws IOException 关闭 Elasticsearch 传输资源失败时抛出
     */
    @Test
    void shouldSearchCanalSynchronizedUserAwardRecord() throws IOException {
        // Given
        ElasticsearchClientConfig clientConfig = new ElasticsearchClientConfig();
        ElasticsearchProperties properties = integrationProperties();
        RestClient restClient = clientConfig.elasticsearchRestClient(properties);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        try (ElasticsearchTransport transport = clientConfig.elasticsearchTransport(restClient, objectMapper)) {
            ElasticsearchClient client = clientConfig.elasticsearchClient(transport);
            ElasticsearchUserAwardRecordReader reader = new ElasticsearchUserAwardRecordReader(
                    client,
                    new UserAwardRecordDocumentAssembler()
            );
            UserAwardRecordSearchQuery query = new UserAwardRecordSearchQuery(
                    "cdc-test-user-001",
                    null,
                    "create",
                    null,
                    null,
                    1,
                    20
            );

            // When
            PageResult<UserAwardRecordSearchItem> result = reader.search(query);

            // Then
            assertEquals(1L, result.total());
            assertFalse(result.records().isEmpty());
            UserAwardRecordSearchItem record = result.records().get(0);
            assertEquals("CDC202610160000000000000001", record.orderId());
            assertEquals("cdc-test-user-001", record.userId());
            assertEquals("create", record.awardState());
            assertEquals(LocalDateTime.of(2026, 10, 16, 2, 54, 47), record.awardTime());
        }
    }

    /**
     * 根据环境变量构造真实 Elasticsearch 连接参数。
     *
     * @return 仅供集成测试使用的连接配置
     * @throws NullPointerException 显式启用集成测试但未提供密码时抛出
     */
    private ElasticsearchProperties integrationProperties() {
        String uri = Objects.requireNonNullElse(
                System.getenv("ELASTICSEARCH_URI"),
                "http://192.168.3.127:9200"
        );
        String username = Objects.requireNonNullElse(
                System.getenv("ELASTICSEARCH_USERNAME"),
                "elastic"
        );
        String password = Objects.requireNonNull(
                System.getenv("ELASTICSEARCH_PASSWORD"),
                "ELASTICSEARCH_PASSWORD must be configured when integration test is enabled"
        );
        return new ElasticsearchProperties(
                List.of(uri),
                username,
                password,
                Duration.ofSeconds(3),
                Duration.ofSeconds(10),
                Duration.ofSeconds(3)
        );
    }
}
