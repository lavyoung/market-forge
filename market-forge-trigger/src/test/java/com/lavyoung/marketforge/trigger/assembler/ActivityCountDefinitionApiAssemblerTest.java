package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityCountDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityCountDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 活动次数配置 API 装配器测试。
 */
class ActivityCountDefinitionApiAssemblerTest {

    private static final Long ACTIVITY_COUNT_ID = 9_007_199_254_740_993L;
    private static final ActivityCountDefinitionApiAssembler ASSEMBLER = Mappers.getMapper(ActivityCountDefinitionApiAssembler.class);

    @Test
    void shouldMapRequestsToApplicationModels() {
        // Given
        ActivityCountDefinitionSearchRequest searchRequest = new ActivityCountDefinitionSearchRequest(
                ACTIVITY_COUNT_ID.toString(), "签到", 2, 20);
        CreateActivityCountDefinitionRequest createRequest = new CreateActivityCountDefinitionRequest(
                "签到次数包", 100, 10, 50);
        UpdateActivityCountDefinitionRequest updateRequest = new UpdateActivityCountDefinitionRequest(
                "积分兑换次数包", 200, 20, 100, 3);

        // When
        ActivityCountDefinitionSearchQuery query = ASSEMBLER.toSearchQuery(searchRequest);
        CreateActivityCountDefinitionCommand createCommand = ASSEMBLER.toCreateCommand(createRequest, "admin");
        UpdateActivityCountDefinitionCommand updateCommand = ASSEMBLER.toUpdateCommand(
                ACTIVITY_COUNT_ID, updateRequest, "operator");

        // Then
        assertAll(
                () -> assertEquals(ACTIVITY_COUNT_ID, query.activityCountId()),
                () -> assertEquals(2, query.page().pageNumber()),
                () -> assertEquals("admin", createCommand.operator()),
                () -> assertEquals(ACTIVITY_COUNT_ID, updateCommand.activityCountId()),
                () -> assertEquals(3, updateCommand.expectedVersion()),
                () -> assertEquals("operator", updateCommand.operator())
        );
    }

    @Test
    void shouldPreserveLongIdentifierAsString() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 10, 26, 10, 0);
        ActivityCountDefinitionResult result = new ActivityCountDefinitionResult(
                ACTIVITY_COUNT_ID, "签到次数包", 100, 10, 50, 1, "admin", "admin", now, now);

        // When
        ActivityCountDefinitionResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(ACTIVITY_COUNT_ID.toString(), response.activityCountId()),
                () -> assertEquals("签到次数包", response.activityCountName()),
                () -> assertEquals(100, response.totalCount())
        );
    }
}
