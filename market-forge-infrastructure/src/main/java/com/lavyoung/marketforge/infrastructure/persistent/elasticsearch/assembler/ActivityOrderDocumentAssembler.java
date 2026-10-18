package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler;

import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.ActivityOrderDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 活动订单 Elasticsearch 文档装配器。
 *
 * <p>负责将 Elasticsearch 文档和命中元数据转换为 Application 查询结果，
 * 防止索引字段、文档类型和时间偏移量泄漏到应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Component
public class ActivityOrderDocumentAssembler {

    /**
     * 将 Elasticsearch 命中文档转换为活动订单查询结果。
     *
     * <p>订单号存储在 Elasticsearch 命中的 {@code _id} 中，因此由 Reader
     * 单独传入。索引中的带偏移时间在此转换为应用层使用的本地时间。</p>
     *
     * @param documentId Elasticsearch 文档标识，即全业务唯一订单号
     * @param document   活动订单索引文档
     * @return 不包含搜索引擎技术类型的应用层查询结果
     * @throws NullPointerException 文档标识或文档为空时抛出
     */
    public ActivityOrderSearchItem toSearchItem(String documentId, ActivityOrderDocument document) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        Objects.requireNonNull(document, "document must not be null");

        return new ActivityOrderSearchItem(
                document.userId(),
                document.activityId(),
                document.sku(),
                document.activityName(),
                document.strategyId(),
                documentId,
                toLocalDateTime(document.orderTime()),
                document.totalCount(),
                document.dayCount(),
                document.monthCount(),
                document.state(),
                document.outBusinessNo(),
                toLocalDateTime(document.createTime()),
                toLocalDateTime(document.updateTime())
        );
    }

    /**
     * 将带时区偏移的索引时间转换为应用层本地时间。
     *
     * @param value Elasticsearch 日期时间
     * @return 本地时间；原值为空时返回 {@code null}
     */
    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
