package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityDefinitionCommand;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ActivityDefinitionApiAssembler} 单元测试。
 *
 * <p>验证查询分页转换以及超过 JavaScript 安全整数范围的活动、次数配置和策略标识
 * 均以字符串无损输出。</p>
 */
class ActivityDefinitionApiAssemblerTest {

    private static final Long ACTIVITY_ID = 9_007_199_254_740_993L;
    private static final Long ACTIVITY_COUNT_ID = 9_007_199_254_740_994L;
    private static final Long STRATEGY_ID = 9_007_199_254_740_995L;
    private static final ActivityDefinitionApiAssembler ASSEMBLER = Mappers.getMapper(ActivityDefinitionApiAssembler.class);

    @Test
    void shouldMapSearchRequestToApplicationQuery() {
        // Given
        ActivityDefinitionSearchRequest request = new ActivityDefinitionSearchRequest(ACTIVITY_ID, "九宫格", "open", 2, 20);

        // When
        ActivityDefinitionSearchQuery query = ASSEMBLER.toSearchQuery(request);

        // Then
        assertAll(
                () -> assertEquals(ACTIVITY_ID, query.activityId()),
                () -> assertEquals("九宫格", query.activityName()),
                () -> assertEquals("open", query.state()),
                () -> assertEquals(2, query.page().pageNumber()),
                () -> assertEquals(20, query.page().pageSize())
        );
    }

    @Test
    void shouldMapCreateRequestAndTrustedOperatorToCommand() {
        // Given
        CreateActivityDefinitionRequest request = new CreateActivityDefinitionRequest("新活动", "活动说明",
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 30, 23, 59),
                1000, 100301L, 100001L, "total");

        // When
        CreateActivityDefinitionCommand command = ASSEMBLER.toCreateCommand(request, "admin");

        // Then
        assertAll(() -> assertEquals("新活动", command.activityName()),
                () -> assertEquals(1000, command.stockCount()),
                () -> assertEquals(100301L, command.activityCountId()),
                () -> assertEquals("admin", command.operator()));
    }

    @Test
    void shouldPreserveLongIdentifiersAsStringsInResponseAndPage() {
        // Given
        ActivityDefinitionResult result = result();
        PageResult<ActivityDefinitionResult> page = new PageResult<>(List.of(result), 21, 2, 20);

        // When
        ActivityDefinitionResponse response = ASSEMBLER.toResponse(result);
        PageResult<ActivityDefinitionResponse> pageResponse = ASSEMBLER.toPageResponse(page);

        // Then
        assertAll(
                () -> assertEquals(ACTIVITY_ID.toString(), response.activityId()),
                () -> assertEquals(ACTIVITY_COUNT_ID.toString(), response.activityCountId()),
                () -> assertEquals(STRATEGY_ID.toString(), response.strategyId()),
                () -> assertEquals(result.activityName(), response.activityName()),
                () -> assertEquals(result.stockCountSurplus(), response.stockCountSurplus()),
                () -> assertEquals(result.version(), response.version()),
                () -> assertEquals(21L, pageResponse.total()),
                () -> assertEquals(response, pageResponse.records().get(0))
        );
    }

    @Test
    void shouldRejectNullPageResult() {
        // When / Then
        assertThrows(NullPointerException.class, () -> ASSEMBLER.toPageResponse(null));
    }

    private ActivityDefinitionResult result() {
        return new ActivityDefinitionResult(ACTIVITY_ID, "九宫格活动抽奖", "九宫格抽奖活动演示数据", LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2036, 12, 31, 23, 59, 59), 100000, 99999, ACTIVITY_COUNT_ID, STRATEGY_ID, "total", "open", 2, "admin", "operator", LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 10, 30));
    }
}
