package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler;

import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.UserAwardRecordDocument;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 用户中奖记录 Elasticsearch 文档转换器。
 *
 * <p>负责将 Infrastructure 文档模型转换为 Application 查询结果，
 * 防止 Elasticsearch 字段结构泄漏到上层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@Component
public class UserAwardRecordDocumentAssembler {

    /**
     * 将 Elasticsearch 命中文档转换为应用层查询结果。
     *
     * <p>订单号保存在 Elasticsearch 命中的 {@code _id} 中而不是 {@code _source} 中，
     * 因此由调用方单独传入。带偏移量的索引时间在边界处转换为项目统一使用的
     * {@link java.time.LocalDateTime}。</p>
     *
     * @param documentId Elasticsearch 文档标识，即业务订单号
     * @param document   Elasticsearch 用户中奖记录文档
     * @return 不包含搜索引擎技术类型的应用层查询结果
     * @throws NullPointerException 文档为空时抛出
     */
    public UserAwardRecordSearchItem toSearchItem(String documentId, UserAwardRecordDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        return new UserAwardRecordSearchItem(
                documentId,
                document.userId(),
                document.activityId(),
                document.strategyId(),
                document.awardId(),
                document.awardTitle(),
                document.awardState(),
                document.awardTime() == null ? null : document.awardTime().toLocalDateTime()
        );
    }
}
