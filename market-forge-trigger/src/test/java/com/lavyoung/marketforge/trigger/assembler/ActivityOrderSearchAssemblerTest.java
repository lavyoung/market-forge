package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityOrderSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityOrderResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ActivityOrderSearchAssembler} 单元测试。
 *
 * <p>验证 HTTP 查询请求、Application 查询模型和 API 分页响应之间的转换契约，
 * 特别保护 Java 长整型标识向前端字符串转换时不会发生精度丢失。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
class ActivityOrderSearchAssemblerTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 10, 18, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 10, 19, 0, 0);
    private static final ActivityOrderSearchAssembler ASSEMBLER = Mappers.getMapper(ActivityOrderSearchAssembler.class);

    /**
     * 验证 API 请求的全部查询条件都能进入 Application 查询模型。
     */
    @Test
    void shouldMapRequestToApplicationQuery() {
        // Given
        ActivityOrderSearchRequest request = new ActivityOrderSearchRequest(
                "user-001", 100301L, "order-001", "used", START_TIME, END_TIME, 2, 20
        );

        // When
        ActivityOrderSearchQuery query = ASSEMBLER.toQuery(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), query.userId()),
                () -> assertEquals(request.activityId(), query.activityId()),
                () -> assertEquals(request.orderId(), query.orderId()),
                () -> assertEquals(request.orderState(), query.orderState()),
                () -> assertEquals(request.startTime(), query.startTime()),
                () -> assertEquals(request.endTime(), query.endTime()),
                () -> assertEquals(request.pageNumber(), query.pageNumber()),
                () -> assertEquals(request.pageSize(), query.pageSize())
        );
    }

    /**
     * 验证分页转换保留元数据，并将超过 JavaScript 安全整数范围的标识完整转为字符串。
     */
    @Test
    void shouldMapApplicationPageAndPreserveLongIdentifiers() {
        // Given
        ActivityOrderSearchItem item = searchItem();
        PageResult<ActivityOrderSearchItem> source = new PageResult<>(List.of(item), 41, 2, 20);

        // When
        PageResult<ActivityOrderResponse> target = ASSEMBLER.toPageResponse(source);

        // Then
        ActivityOrderResponse response = target.records().get(0);
        assertAll(
                () -> assertEquals(41, target.total()),
                () -> assertEquals(2, target.pageNumber()),
                () -> assertEquals(20, target.pageSize()),
                () -> assertEquals(1, target.records().size()),
                () -> assertEquals("9007199254740993", response.activityId()),
                () -> assertEquals("9007199254740994", response.sku()),
                () -> assertEquals("9007199254740995", response.strategyId()),
                () -> assertEquals(item.orderId(), response.orderId()),
                () -> assertEquals(item.orderState(), response.orderState()),
                () -> assertEquals(item.orderTime(), response.orderTime())
        );
    }

    /**
     * 验证空记录页仍能转换为合法 API 分页结果。
     */
    @Test
    void shouldMapEmptyPage() {
        // Given
        PageResult<ActivityOrderSearchItem> source = PageResult.empty(1, 20);

        // When
        PageResult<ActivityOrderResponse> target = ASSEMBLER.toPageResponse(source);

        // Then
        assertAll(
                () -> assertEquals(List.of(), target.records()),
                () -> assertEquals(0, target.total()),
                () -> assertEquals(1, target.pageNumber()),
                () -> assertEquals(20, target.pageSize())
        );
    }

    /**
     * 验证空分页结果会在协议转换边界被立即拒绝。
     */
    @Test
    void shouldRejectNullPageResult() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> ASSEMBLER.toPageResponse(null));
    }

    /**
     * 构造活动订单应用查询结果。
     *
     * @return 字段完整且标识超过 JavaScript 安全整数范围的活动订单
     */
    private ActivityOrderSearchItem searchItem() {
        return new ActivityOrderSearchItem(
                "user-001",
                9_007_199_254_740_993L,
                9_007_199_254_740_994L,
                "周末幸运抽奖",
                9_007_199_254_740_995L,
                "order-001",
                LocalDateTime.of(2026, 10, 18, 10, 8, 5),
                10,
                1,
                10,
                "used",
                "raffle-001",
                LocalDateTime.of(2026, 10, 18, 10, 8, 5),
                LocalDateTime.of(2026, 10, 18, 10, 8, 7)
        );
    }
}
