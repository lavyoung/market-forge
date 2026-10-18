package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityOrderReader;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler.ActivityOrderDocumentAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.ActivityOrderDocument;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 基于 Elasticsearch 的活动订单读取器。
 *
 * <p>负责将应用层查询条件转换为 Elasticsearch DSL，访问活动订单统一索引别名，
 * 并把命中文档转换为应用层分页结果。</p>
 *
 * <p>该实现只承担运营读模型查询，不参与活动订单创建、状态修改或额度入账。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ElasticsearchActivityOrderReader implements IActivityOrderReader {

    private static final String INDEX_ALIAS = "market_forge_raffle_activity_order";

    private final ElasticsearchClient client;
    private final ActivityOrderDocumentAssembler documentAssembler;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ActivityOrderSearchItem> search(ActivityOrderSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        try {
            SearchResponse<ActivityOrderDocument> response = executeSearch(query);
            List<ActivityOrderSearchItem> records = response.hits().hits().stream()
                    .map(this::toSearchItem)
                    .toList();

            long total = response.hits().total() == null
                    ? records.size()
                    : response.hits().total().value();

            return new PageResult<>(records, total, query.pageNumber(), query.pageSize());
        } catch (IOException exception) {
            log.error("Elasticsearch 活动订单查询失败，query={}", query, exception);
            throw BusinessException.of(CommonResponseCode.SYSTEM_ERROR, exception);
        }
    }

    /**
     * 执行活动订单分页查询。
     *
     * <p>查询使用索引别名而不是具体版本索引。排序首先使用业务下单时间，
     * 再使用来源物理库和分片内主键，保证多个分库合并后仍具有稳定顺序。</p>
     *
     * @param query 已通过应用层校验的查询条件
     * @return Elasticsearch 原始搜索响应
     * @throws IOException Elasticsearch 网络访问或响应解析失败时抛出
     */
    private SearchResponse<ActivityOrderDocument> executeSearch(ActivityOrderSearchQuery query) throws IOException {
        BoolQuery boolQuery = buildQuery(query);

        return client.search(builder -> builder
                        .index(INDEX_ALIAS)
                        .from(query.offset())
                        .size(query.pageSize())
                        .trackTotalHits(value -> value.enabled(true))
                        .query(value -> value.bool(boolQuery))
                        .sort(value -> value.field(field -> field.field("order_time").order(SortOrder.Desc)))
                        .sort(value -> value.field(field -> field.field("source_database").order(SortOrder.Asc)))
                        .sort(value -> value.field(field -> field.field("id").order(SortOrder.Desc))),
                ActivityOrderDocument.class
        );
    }

    /**
     * 将应用查询条件转换为 Elasticsearch 过滤条件。
     *
     * <p>所有结构化字段均使用精确过滤，不参与相关度评分。订单号存储在 ES
     * 文档 {@code _id} 中，因此使用 IDs 查询。时间范围保持左闭右开语义。</p>
     *
     * @param query 活动订单查询条件
     * @return Elasticsearch 布尔查询
     */
    private BoolQuery buildQuery(ActivityOrderSearchQuery query) {
        BoolQuery.Builder builder = new BoolQuery.Builder();

        if (query.userId() != null) {
            builder.filter(filter -> filter.term(term -> term.field("user_id").value(query.userId())));
        }

        if (query.activityId() != null) {
            builder.filter(filter -> filter.term(term -> term.field("activity_id").value(query.activityId())));
        }

        if (query.orderId() != null) {
            builder.filter(filter -> filter.ids(ids -> ids.values(query.orderId())));
        }

        if (query.orderState() != null) {
            builder.filter(filter -> filter.term(term -> term.field("state").value(query.orderState())));
        }

        if (query.startTime() != null || query.endTime() != null) {
            builder.filter(filter -> filter.range(range -> {
                range.field("order_time");

                if (query.startTime() != null) {
                    range.gte(JsonData.of(DateUtil.yyyy_MM_dd_HH_mm_ss.format(query.startTime())));
                }

                if (query.endTime() != null) {
                    range.lt(JsonData.of(DateUtil.yyyy_MM_dd_HH_mm_ss.format(query.endTime())));
                }

                return range;
            }));
        }

        return builder.build();
    }

    /**
     * 将单条 Elasticsearch 命中转换为应用查询结果。
     *
     * @param hit Elasticsearch 命中记录
     * @return 活动订单查询结果项
     * @throws NullPointerException 命中不包含文档标识或 {@code _source} 时抛出
     */
    private ActivityOrderSearchItem toSearchItem(Hit<ActivityOrderDocument> hit) {
        String documentId = Objects.requireNonNull(hit.id(), "Elasticsearch hit id must not be null");
        ActivityOrderDocument source = Objects.requireNonNull(
                hit.source(),
                "Elasticsearch hit source must not be null"
        );
        return documentAssembler.toSearchItem(documentId, source);
    }
}
