package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityStageSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.PublishActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityStageResponse;
import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ActivityStageApiAssembler} 单元测试。
 *
 * <p>保护服务端操作人、路径标识和十八位业务编号的协议转换契约。</p>
 */
class ActivityStageApiAssemblerTest {

    private static final Long STAGE_ID = 9_007_199_254_740_993L;
    private static final Long ACTIVITY_ID = 9_007_199_254_740_994L;
    private static final ActivityStageApiAssembler ASSEMBLER = Mappers.getMapper(ActivityStageApiAssembler.class);

    @Test
    void shouldMapRequestsToApplicationModels() {
        // Given
        ActivityStageSearchRequest searchRequest = new ActivityStageSearchRequest("web", "official", "active", 2, 20);
        CreateActivityStageRequest createRequest = new CreateActivityStageRequest("web", "official", ACTIVITY_ID.toString());
        UpdateActivityStageRequest updateRequest = new UpdateActivityStageRequest("app", "campaign", ACTIVITY_ID.toString(), 3);
        PublishActivityStageRequest publishRequest = new PublishActivityStageRequest(4);

        // When
        ActivityStageSearchQuery query = ASSEMBLER.toSearchQuery(searchRequest);
        CreateActivityStageCommand createCommand = ASSEMBLER.toCreateCommand(createRequest, "admin");
        UpdateActivityStageCommand updateCommand = ASSEMBLER.toUpdateCommand(STAGE_ID, updateRequest, "operator");
        PublishActivityStageCommand publishCommand = ASSEMBLER.toPublishCommand(STAGE_ID, publishRequest, "publisher");

        // Then
        assertAll(
                () -> assertEquals(2, query.page().pageNumber()),
                () -> assertEquals(ACTIVITY_ID, createCommand.activityId()),
                () -> assertEquals("admin", createCommand.operator()),
                () -> assertEquals(STAGE_ID, updateCommand.stageId()),
                () -> assertEquals(ACTIVITY_ID, updateCommand.activityId()),
                () -> assertEquals("operator", updateCommand.operator()),
                () -> assertEquals(STAGE_ID, publishCommand.stageId()),
                () -> assertEquals(4, publishCommand.expectedVersion()),
                () -> assertEquals("publisher", publishCommand.operator())
        );
    }

    @Test
    void shouldPreserveLongIdentifiersAsStringsInResponseAndPage() {
        // Given
        ActivityStageResult result = result();
        PageResult<ActivityStageResult> page = new PageResult<>(List.of(result), 21, 2, 20);

        // When
        ActivityStageResponse response = ASSEMBLER.toResponse(result);
        PageResult<ActivityStageResponse> pageResponse = ASSEMBLER.toPageResponse(page);

        // Then
        assertAll(
                () -> assertEquals(STAGE_ID.toString(), response.stageId()),
                () -> assertEquals(ACTIVITY_ID.toString(), response.activityId()),
                () -> assertEquals(21L, pageResponse.total()),
                () -> assertEquals(response, pageResponse.records().get(0))
        );
    }

    private ActivityStageResult result() {
        return new ActivityStageResult(STAGE_ID, "web", "official", ACTIVITY_ID, "active", 2, "admin", "admin", LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 10, 30));
    }
}
