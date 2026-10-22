package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.application.award.query.IUserAwardRecordReader;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler.UserAwardRecordDocumentAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.UserAwardRecordDocument;
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
 * 基于 Elasticsearch 的用户中奖记录读取器。
 *
 * <p>负责把应用层查询条件转换为 Elasticsearch DSL，访问统一索引别名，
 * 并将搜索文档转换为应用层分页结果。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ElasticsearchUserAwardRecordReader implements IUserAwardRecordReader {
    private static final String INDEX_ALIAS = "market_forge_user_award_record";

    private final ElasticsearchClient client;
    private final UserAwardRecordDocumentAssembler documentAssembler;

    @Override
    public PageResult<UserAwardRecordSearchItem> search(UserAwardRecordSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        try {
            SearchResponse<UserAwardRecordDocument> response = executeSearch(query);

            List<UserAwardRecordSearchItem> records = response.hits().hits().stream().map(this::toSearchItem).toList();

            long total = response.hits().total() == null
                    ? records.size()
                    : response.hits().total().value();

            return new PageResult<>(
                    records,
                    total,
                    query.page().pageNumber(),
                    query.page().pageSize()
            );

        } catch (IOException e) {
            log.error("Elasticsearch 用户中奖记录查询失败，query={}", query, e);
            throw BusinessException.of(CommonResponseCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 执行 Elasticsearch 分页查询并返回原始搜索响应。
     *
     * @param query 已通过应用层校验的查询条件
     * @return 包含命中文档和总数的搜索响应
     * @throws IOException Elasticsearch 网络访问或响应解析失败时抛出
     */
    private SearchResponse<UserAwardRecordDocument> executeSearch(UserAwardRecordSearchQuery query) throws IOException {
        BoolQuery boolQuery = buildQuery(query);

        return client.search(builder -> builder
                        .index(INDEX_ALIAS)
                        .from(Math.toIntExact(query.page().offset()))
                        .size(query.page().pageSize())
                        .trackTotalHits(value -> value.enabled(true))
                        .query(value -> value.bool(boolQuery))
                        .sort(value -> value.field(field -> field
                                .field("award_time").order(SortOrder.Desc)))
                        .sort(value -> value.field(field -> field
                                .field("source_database")
                                .order(SortOrder.Asc)))
                        .sort(value -> value.field(field -> field
                                .field("id")
                                .order(SortOrder.Desc))),
                UserAwardRecordDocument.class
        );
    }

    /**
     * 将应用层查询条件转换为 Elasticsearch 布尔过滤条件。
     *
     * <p>时间区间保持应用模型约定的左闭右开语义，即开始时间使用
     * {@code gte}，结束时间使用 {@code lt}。</p>
     *
     * @param query 用户中奖记录查询条件
     * @return 可交给 Elasticsearch 客户端执行的布尔查询
     */
    private BoolQuery buildQuery(UserAwardRecordSearchQuery query) {
        BoolQuery.Builder builder = new BoolQuery.Builder();

        if (query.userId() != null && !query.userId().isBlank()) {
            builder.filter(filter -> filter.term(term -> term.field("user_id").value(query.userId())));
        }

        if (query.awardId() != null) {
            builder.filter(filter -> filter.term(term -> term.field("award_id").value(query.awardId())));
        }

        if (query.awardState() != null && !query.awardState().isBlank()) {
            builder.filter(filter -> filter.term(term -> term.field("award_state").value(query.awardState())));
        }

        if (query.startTime() != null || query.endTime() != null) {
            builder.filter(filter -> filter.range(range -> {
                range.field("award_time");

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
     * 将单条 Elasticsearch 命中转换为应用层查询结果。
     *
     * @param hit Elasticsearch 命中记录
     * @return 用户中奖记录查询结果项
     * @throws NullPointerException 命中文档不包含 {@code _source} 时抛出
     */
    private UserAwardRecordSearchItem toSearchItem(Hit<UserAwardRecordDocument> hit) {
        UserAwardRecordDocument source = Objects.requireNonNull(hit.source(), "Elasticsearch hit source must not be null");
        return documentAssembler.toSearchItem(hit.id(), source);
    }
}
