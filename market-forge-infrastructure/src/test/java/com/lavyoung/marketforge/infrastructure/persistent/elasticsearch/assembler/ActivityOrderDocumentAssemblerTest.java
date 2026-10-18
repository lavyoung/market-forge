package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.assembler;

import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document.ActivityOrderDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ActivityOrderDocumentAssembler} 单元测试。
 *
 * <p>验证 Elasticsearch 命中标识和文档字段能够完整转换为应用层查询结果，
 * 同时覆盖空时间、空文档标识和空文档边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
class ActivityOrderDocumentAssemblerTest {

    private final ActivityOrderDocumentAssembler assembler = new ActivityOrderDocumentAssembler();

    /**
     * 验证订单号取自 ES 命中标识，索引字段和带偏移时间被正确转换。
     */
    @Test
    void shouldMapDocumentAndHitIdToApplicationItem() {
        // Given
        ActivityOrderDocument document = document(
                OffsetDateTime.parse("2026-10-18T10:08:05+08:00"),
                OffsetDateTime.parse("2026-10-18T10:08:05+08:00"),
                OffsetDateTime.parse("2026-10-18T10:08:07+08:00")
        );

        // When
        ActivityOrderSearchItem item = assembler.toSearchItem("order-001", document);

        // Then
        assertAll(
                () -> assertEquals("user-001", item.userId()),
                () -> assertEquals(100301L, item.activityId()),
                () -> assertEquals(9011L, item.sku()),
                () -> assertEquals("周末幸运抽奖", item.activityName()),
                () -> assertEquals(100006L, item.strategyId()),
                () -> assertEquals("order-001", item.orderId()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 18, 10, 8, 5), item.orderTime()),
                () -> assertEquals(10, item.totalCount()),
                () -> assertEquals(1, item.dayCount()),
                () -> assertEquals(10, item.monthCount()),
                () -> assertEquals("used", item.orderState()),
                () -> assertEquals("raffle-001", item.outBusinessNo()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 18, 10, 8, 5), item.createTime()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 18, 10, 8, 7), item.updateTime())
        );
    }

    /**
     * 验证索引中可选时间为空时，应用查询结果保留空值。
     */
    @Test
    void shouldKeepOptionalTimesNull() {
        // Given
        ActivityOrderDocument document = document(null, null, null);

        // When
        ActivityOrderSearchItem item = assembler.toSearchItem("order-002", document);

        // Then
        assertAll(
                () -> assertNull(item.orderTime()),
                () -> assertNull(item.createTime()),
                () -> assertNull(item.updateTime())
        );
    }

    /**
     * 验证缺少 Elasticsearch 文档标识时立即拒绝转换。
     */
    @Test
    void shouldRejectNullDocumentId() {
        // Given
        ActivityOrderDocument document = document(null, null, null);

        // When / Then
        assertThrows(NullPointerException.class, () -> assembler.toSearchItem(null, document));
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
     * 构造活动订单索引测试文档。
     *
     * @param orderTime  可选的下单时间
     * @param createTime 可选的创建时间
     * @param updateTime 可选的更新时间
     * @return 字段固定、时间可变的测试文档
     */
    private ActivityOrderDocument document(OffsetDateTime orderTime, OffsetDateTime createTime, OffsetDateTime updateTime) {
        return new ActivityOrderDocument(
                1L,
                "market-forge-001",
                "user-001",
                100301L,
                9011L,
                "周末幸运抽奖",
                100006L,
                orderTime,
                10,
                1,
                10,
                "used",
                "raffle-001",
                createTime,
                updateTime
        );
    }
}
