package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler;

import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.UserAwardRecordDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link UserAwardRecordDocumentAssembler} 单元测试。
 *
 * <p>验证 Elasticsearch 命中元数据与文档字段能够被完整转换为应用层读模型，
 * 同时覆盖空文档和可选时间字段边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
class UserAwardRecordDocumentAssemblerTest {

    private final UserAwardRecordDocumentAssembler assembler = new UserAwardRecordDocumentAssembler();

    /**
     * 验证订单号取自命中记录的文档标识，并正确移除中奖时间的时区偏移量。
     */
    @Test
    void shouldMapDocumentAndHitIdToApplicationItem() {
        // Given
        OffsetDateTime awardTime = OffsetDateTime.of(
                2026, 10, 16, 9, 30, 0, 0, ZoneOffset.ofHours(8)
        );
        UserAwardRecordDocument document = document(awardTime);

        // When
        UserAwardRecordSearchItem item = assembler.toSearchItem("order-001", document);

        // Then
        assertAll(
                () -> assertEquals("order-001", item.orderId()),
                () -> assertEquals("user-001", item.userId()),
                () -> assertEquals(1001L, item.activityId()),
                () -> assertEquals(2001L, item.strategyId()),
                () -> assertEquals(3001L, item.awardId()),
                () -> assertEquals("测试奖品", item.awardTitle()),
                () -> assertEquals("completed", item.awardState()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 16, 9, 30), item.awardTime())
        );
    }

    /**
     * 验证索引文档没有中奖时间时，应用读模型保留空值而不是映射失败。
     */
    @Test
    void shouldKeepAwardTimeNullWhenDocumentTimeIsAbsent() {
        // Given
        UserAwardRecordDocument document = document(null);

        // When
        UserAwardRecordSearchItem item = assembler.toSearchItem("order-002", document);

        // Then
        assertNull(item.awardTime());
    }

    /**
     * 验证空文档在基础设施映射边界被立即拒绝。
     */
    @Test
    void shouldRejectNullDocument() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> assembler.toSearchItem("order-003", null));
    }

    /**
     * 构造测试使用的 Elasticsearch 用户中奖记录文档。
     *
     * @param awardTime 可选的中奖时间
     * @return 字段固定、中奖时间可变的测试文档
     */
    private UserAwardRecordDocument document(OffsetDateTime awardTime) {
        return new UserAwardRecordDocument(
                1L,
                "market-forge-001",
                "user-001",
                1001L,
                2001L,
                3001L,
                "测试奖品",
                awardTime,
                "completed",
                OffsetDateTime.parse("2026-10-16T09:00:00+08:00"),
                OffsetDateTime.parse("2026-10-16T09:35:00+08:00")
        );
    }
}
