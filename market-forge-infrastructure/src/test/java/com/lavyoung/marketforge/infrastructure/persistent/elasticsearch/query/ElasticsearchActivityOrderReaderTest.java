package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.util.ObjectBuilder;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler.ActivityOrderDocumentAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.ActivityOrderDocument;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ElasticsearchActivityOrderReader} 查询行为测试。
 *
 * <p>通过捕获官方 Java Client 接收的 {@link SearchRequest}，验证索引别名、
 * 过滤条件、分页和稳定排序，同时验证响应映射与网络异常转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
class ElasticsearchActivityOrderReaderTest {

    /**
     * 验证完整查询条件能够生成正确 DSL，并将命中文档转换为分页结果。
     *
     * @throws IOException 模拟 Elasticsearch 客户端方法声明的受检异常
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildSearchRequestAndMapResponse() throws IOException {
        // Given
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        AtomicReference<SearchRequest> capturedRequest = new AtomicReference<>();
        SearchResponse<ActivityOrderDocument> response = responseWithOneHit();

        when(client.search(
                any(Function.class),
                eq(ActivityOrderDocument.class)
        )).thenAnswer(invocation -> {
            Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> requestFactory = invocation.getArgument(0);
            capturedRequest.set(requestFactory.apply(new SearchRequest.Builder()).build());
            return response;
        });

        ElasticsearchActivityOrderReader reader = new ElasticsearchActivityOrderReader(
                client,
                new ActivityOrderDocumentAssembler()
        );
        ActivityOrderSearchQuery query = new ActivityOrderSearchQuery(
                "user-001",
                100301L,
                "order-001",
                "used",
                LocalDateTime.of(2026, 10, 18, 0, 0),
                LocalDateTime.of(2026, 10, 19, 0, 0),
                2,
                20
        );

        // When
        PageResult<ActivityOrderSearchItem> result = reader.search(query);

        // Then
        SearchRequest request = capturedRequest.get();
        List<Query> filters = request.query().bool().filter();
        assertAll(
                () -> assertEquals(List.of("market_forge_raffle_activity_order"), request.index()),
                () -> assertEquals(20, request.from()),
                () -> assertEquals(20, request.size()),
                () -> assertEquals(true, request.trackTotalHits().enabled()),
                () -> assertEquals(5, filters.size()),
                () -> assertTermFilter(filters.get(0), "user_id", "user-001"),
                () -> assertTermFilter(filters.get(1), "activity_id", 100301L),
                () -> assertEquals(List.of("order-001"), filters.get(2).ids().values()),
                () -> assertTermFilter(filters.get(3), "state", "used"),
                () -> assertTimeRange(filters.get(4).range()),
                () -> assertEquals("order_time", request.sort().get(0).field().field()),
                () -> assertEquals(SortOrder.Desc, request.sort().get(0).field().order()),
                () -> assertEquals("source_database", request.sort().get(1).field().field()),
                () -> assertEquals(SortOrder.Asc, request.sort().get(1).field().order()),
                () -> assertEquals("id", request.sort().get(2).field().field()),
                () -> assertEquals(SortOrder.Desc, request.sort().get(2).field().order()),
                () -> assertEquals(1L, result.total()),
                () -> assertEquals(2, result.pageNumber()),
                () -> assertEquals(20, result.pageSize()),
                () -> assertEquals("order-001", result.records().get(0).orderId()),
                () -> assertEquals("user-001", result.records().get(0).userId())
        );
    }

    /**
     * 验证 Elasticsearch 网络异常会转换为统一业务异常。
     *
     * @throws IOException 模拟 Elasticsearch 客户端方法声明的受检异常
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldWrapIOExceptionAsBusinessException() throws IOException {
        // Given
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.search(any(Function.class), eq(ActivityOrderDocument.class)))
                .thenThrow(new IOException("connection refused"));
        ElasticsearchActivityOrderReader reader = new ElasticsearchActivityOrderReader(
                client,
                new ActivityOrderDocumentAssembler()
        );
        ActivityOrderSearchQuery query = new ActivityOrderSearchQuery(
                null, null, null, null, null, null, 1, 20
        );

        // When / Then
        assertThrows(BusinessException.class, () -> reader.search(query));
    }

    /**
     * 断言单个精确匹配条件的字段和值。
     *
     * @param query         Elasticsearch 查询条件
     * @param expectedField 期望字段
     * @param expectedValue 期望值
     */
    private void assertTermFilter(Query query, String expectedField, Object expectedValue) {
        TermQuery term = query.term();
        assertEquals(expectedField, term.field());
        if (expectedValue instanceof String stringValue) {
            assertEquals(stringValue, term.value().stringValue());
            return;
        }
        assertEquals(((Number) expectedValue).longValue(), term.value().longValue());
    }

    /**
     * 断言订单时间使用左闭右开范围。
     *
     * @param range Elasticsearch 时间范围条件
     */
    private void assertTimeRange(RangeQuery range) {
        assertAll(
                () -> assertEquals("order_time", range.field()),
                () -> assertEquals("2026-10-18 00:00:00", range.gte().to(String.class)),
                () -> assertEquals("2026-10-19 00:00:00", range.lt().to(String.class))
        );
    }

    /**
     * 构造包含一条活动订单的 Elasticsearch 响应。
     *
     * @return 搜索响应
     */
    private SearchResponse<ActivityOrderDocument> responseWithOneHit() {
        ActivityOrderDocument document = new ActivityOrderDocument(
                1L,
                "market-forge-001",
                "user-001",
                100301L,
                9011L,
                "周末幸运抽奖",
                100006L,
                OffsetDateTime.parse("2026-10-18T10:08:05+08:00"),
                10,
                1,
                10,
                "used",
                "raffle-001",
                OffsetDateTime.parse("2026-10-18T10:08:05+08:00"),
                OffsetDateTime.parse("2026-10-18T10:08:07+08:00")
        );

        return SearchResponse.of(response -> response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> hits
                        .total(total -> total.value(1).relation(TotalHitsRelation.Eq))
                        .hits(hit -> hit
                                .index("market_forge_raffle_activity_order_v1")
                                .id("order-001")
                                .source(document))));
    }
}
