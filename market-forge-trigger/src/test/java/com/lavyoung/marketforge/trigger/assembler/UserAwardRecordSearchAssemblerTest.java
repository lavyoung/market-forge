package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.award.request.UserAwardRecordSearchRequest;
import com.lavyoung.marketforge.api.award.response.UserAwardRecordResponse;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link UserAwardRecordSearchAssembler} 单元测试。
 *
 * <p>验证 HTTP 查询请求、Application 查询模型和 API 分页响应之间的转换契约，
 * 防止后续增加字段时出现静默丢失。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
class UserAwardRecordSearchAssemblerTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 10, 16, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 10, 17, 0, 0);
    private static final UserAwardRecordSearchAssembler ASSEMBLER =
            Mappers.getMapper(UserAwardRecordSearchAssembler.class);

    /**
     * 验证 API 请求的全部查询条件都能进入 Application 查询模型。
     */
    @Test
    void shouldMapRequestToApplicationQuery() {
        // Given
        UserAwardRecordSearchRequest request = new UserAwardRecordSearchRequest(
                "user-001",
                101L,
                "completed",
                START_TIME,
                END_TIME,
                2,
                20
        );

        // When
        UserAwardRecordSearchQuery query = ASSEMBLER.toQuery(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), query.userId()),
                () -> assertEquals(request.awardId(), query.awardId()),
                () -> assertEquals(request.awardState(), query.awardState()),
                () -> assertEquals(request.startTime(), query.startTime()),
                () -> assertEquals(request.endTime(), query.endTime()),
                () -> assertEquals(request.pageNo(), query.page().pageNumber()),
                () -> assertEquals(request.pageSize(), query.page().pageSize())
        );
    }

    /**
     * 验证分页转换会转换记录类型，同时完整保留分页元数据。
     */
    @Test
    void shouldMapApplicationPageToApiPage() {
        // Given
        UserAwardRecordSearchItem item = searchItem();
        PageResult<UserAwardRecordSearchItem> source = new PageResult<>(List.of(item), 41, 2, 20);

        // When
        PageResult<UserAwardRecordResponse> target = ASSEMBLER.toPageResponse(source);

        // Then
        UserAwardRecordResponse response = target.records().get(0);
        assertAll(
                () -> assertEquals(41, target.total()),
                () -> assertEquals(2, target.pageNumber()),
                () -> assertEquals(20, target.pageSize()),
                () -> assertEquals(1, target.records().size()),
                () -> assertEquals(item.orderId(), response.orderId()),
                () -> assertEquals(item.userId(), response.userId()),
                () -> assertEquals(item.activityId(), response.activityId()),
                () -> assertEquals(item.strategyId(), response.strategyId()),
                () -> assertEquals(item.awardId(), response.awardId()),
                () -> assertEquals(item.awardTitle(), response.awardTitle()),
                () -> assertEquals(item.awardState(), response.awardState()),
                () -> assertEquals(item.awardTime(), response.awardTime())
        );
    }

    /**
     * 验证空记录页仍能转换为合法 API 分页结果。
     */
    @Test
    void shouldMapEmptyPage() {
        // Given
        PageResult<UserAwardRecordSearchItem> source = PageResult.empty(1, 20);

        // When
        PageResult<UserAwardRecordResponse> target = ASSEMBLER.toPageResponse(source);

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
     * 构造用户中奖记录应用查询结果。
     *
     * @return 字段完整的用户中奖记录
     */
    private UserAwardRecordSearchItem searchItem() {
        return new UserAwardRecordSearchItem(
                "ROPmv8av9hj0002z38fvn2949wettup",
                "user-001",
                100301L,
                100006L,
                101L,
                "用户积分",
                "completed",
                LocalDateTime.of(2026, 10, 16, 9, 30)
        );
    }
}
