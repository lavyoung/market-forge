package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivitySkuDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.AdjustActivitySkuStockRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivitySkuDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动 SKU API 装配器测试。
 */
class ActivitySkuDefinitionApiAssemblerTest {

    private static final Long SKU = 9_007_199_254_740_993L;
    private static final Long ACTIVITY_ID = 8_007_199_254_740_993L;
    private static final Long ACTIVITY_COUNT_ID = 7_007_199_254_740_993L;
    private static final ActivitySkuDefinitionApiAssembler ASSEMBLER =
            Mappers.getMapper(ActivitySkuDefinitionApiAssembler.class);

    @Test
    void shouldMapStringIdentifiersAndAuthenticatedOperator() {
        // Given
        ActivitySkuDefinitionSearchRequest searchRequest = new ActivitySkuDefinitionSearchRequest(
                SKU.toString(), ACTIVITY_ID.toString(), ACTIVITY_COUNT_ID.toString(), 2, 20);
        CreateActivitySkuDefinitionRequest createRequest = new CreateActivitySkuDefinitionRequest(
                ACTIVITY_ID.toString(), ACTIVITY_COUNT_ID.toString(), 100L, 500L);
        UpdateActivitySkuDefinitionRequest updateRequest = new UpdateActivitySkuDefinitionRequest(
                ACTIVITY_ID.toString(), ACTIVITY_COUNT_ID.toString(), 600L, 3);
        AdjustActivitySkuStockRequest adjustRequest = new AdjustActivitySkuStockRequest(50L, "追加预算", 3);

        // When
        ActivitySkuDefinitionSearchQuery query = ASSEMBLER.toSearchQuery(searchRequest);
        CreateActivitySkuDefinitionCommand createCommand = ASSEMBLER.toCreateCommand(createRequest, "admin");
        UpdateActivitySkuDefinitionCommand updateCommand = ASSEMBLER.toUpdateCommand(SKU, updateRequest, "operator");
        AdjustActivitySkuStockCommand adjustCommand = ASSEMBLER.toAdjustStockCommand(SKU, adjustRequest, "operator");

        // Then
        assertAll(() -> assertEquals(SKU, query.sku()),
                () -> assertEquals(ACTIVITY_ID, query.activityId()),
                () -> assertEquals(ACTIVITY_COUNT_ID, query.activityCountId()),
                () -> assertEquals(2, query.page().pageNumber()),
                () -> assertEquals("admin", createCommand.operator()),
                () -> assertEquals(600L, updateCommand.creditAmount()),
                () -> assertEquals(50L, adjustCommand.quantityDelta()),
                () -> assertEquals("operator", adjustCommand.operator()));
    }

    @Test
    void shouldPreserveLongIdentifiersAsStringsAndExposeDerivedCreditFlag() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 10, 0);
        ActivitySkuDefinitionResult result = new ActivitySkuDefinitionResult(SKU, ACTIVITY_ID, ACTIVITY_COUNT_ID,
                100L, 80L, 20L, 500L, 3, "admin", "operator", now, now);

        // When
        ActivitySkuDefinitionResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(() -> assertEquals(SKU.toString(), response.sku()),
                () -> assertEquals(ACTIVITY_ID.toString(), response.activityId()),
                () -> assertEquals(ACTIVITY_COUNT_ID.toString(), response.activityCountId()),
                () -> assertEquals(20L, response.consumedCount()),
                () -> assertTrue(response.supportsCreditExchange()));
    }
}
