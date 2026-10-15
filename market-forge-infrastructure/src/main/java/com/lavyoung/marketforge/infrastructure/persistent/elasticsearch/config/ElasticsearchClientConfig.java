package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Elasticsearch 官方 Java Client 装配配置。
 *
 * <p>该配置负责把节点地址、身份认证、网络超时和 Jackson 序列化器组装为
 * {@link ElasticsearchClient}。客户端只允许由 Infrastructure 层适配器使用，
 * 上层业务模块应依赖领域查询端口。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchClientConfig {

    /**
     * 创建底层 Elasticsearch REST 客户端。
     *
     * <p>REST 客户端负责管理 HTTP 连接池。Spring 容器关闭时会自动调用
     * {@link RestClient#close()}，防止连接和线程资源泄漏。</p>
     *
     * @param properties Elasticsearch 连接配置
     * @return 配置完成的底层 REST 客户端
     * @throws IllegalArgumentException 节点 URI 格式非法时抛出
     * @throws ArithmeticException      超时时间转换为毫秒后超过整数范围时抛出
     */
    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(ElasticsearchProperties properties) {
        List<HttpHost> hosts = properties.uris().stream()
                .map(HttpHost::create)
                .toList();

        BasicCredentialsProvider credentialsProvider = createCredentialsProvider(properties);

        return RestClient.builder(hosts.toArray(HttpHost[]::new))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder.setConnectTimeout((int) properties.connectTimeout().toMillis())
                                .setSocketTimeout((int) properties.socketTimeout().toMillis())
                                .setConnectionRequestTimeout((int) properties.connectionRequestTimeout().toMillis())
                ).build();
    }

    /**
     * 创建 Elasticsearch JSON REST 传输层。
     *
     * <p>传输层复用 Spring Boot 管理的 {@link ObjectMapper}，从而继承项目统一的
     * Java 时间类型和 JSON 序列化配置。</p>
     *
     * @param restClient   底层 REST 客户端
     * @param objectMapper 项目统一的 Jackson 对象映射器
     * @return Elasticsearch REST 传输层
     */
    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, ObjectMapper objectMapper) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    /**
     * 创建 Elasticsearch 类型安全客户端。
     *
     * @param elasticsearchTransport Elasticsearch REST 传输层
     * @return 供 Infrastructure 查询适配器使用的客户端
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }

    private BasicCredentialsProvider createCredentialsProvider(ElasticsearchProperties properties) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(properties.username(), properties.password()));
        return credentialsProvider;
    }
}
