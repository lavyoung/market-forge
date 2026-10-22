package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ActivityStageReadModelAssembler} 映射契约测试。
 */
class ActivityStageReadModelAssemblerTest {

    private static final ActivityStageReadModelAssembler ASSEMBLER = Mappers.getMapper(ActivityStageReadModelAssembler.class);

    @Test
    void shouldMapPersistentObjectToApplicationResult() {
        // Given
        ActivityStagePO source = new ActivityStagePO();
        source.setStageId(1001L);
        source.setChannel("web");
        source.setSource("official");
        source.setActivityId(2001L);
        source.setState("active");
        source.setVersion(2);
        source.setCreatedBy("admin");
        source.setUpdatedBy("operator");
        source.setCreateTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        source.setUpdateTime(LocalDateTime.of(2026, 10, 22, 10, 30));

        // When
        ActivityStageResult result = ASSEMBLER.toResults(java.util.List.of(source)).get(0);

        // Then
        assertAll(
                () -> assertEquals(source.getStageId(), result.stageId()),
                () -> assertEquals(source.getActivityId(), result.activityId()),
                () -> assertEquals(source.getState(), result.state()),
                () -> assertEquals(source.getVersion(), result.version()),
                () -> assertEquals(source.getUpdateTime(), result.updateTime())
        );
    }
}
