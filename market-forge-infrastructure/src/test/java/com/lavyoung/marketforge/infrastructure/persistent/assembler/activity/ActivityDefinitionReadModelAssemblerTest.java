package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ActivityDefinitionReadModelAssembler} 映射契约测试。
 *
 * <p>验证活动配置、库存、关联标识、生命周期和审计字段均能完整投影到应用读模型。</p>
 */
class ActivityDefinitionReadModelAssemblerTest {

    private static final ActivityDefinitionReadModelAssembler ASSEMBLER = Mappers.getMapper(ActivityDefinitionReadModelAssembler.class);

    @Test
    void shouldMapAllActivityDefinitionFields() {
        // Given
        ActivityPO source = activityPO();

        // When
        ActivityDefinitionResult result = ASSEMBLER.toResult(source);

        // Then
        assertAll(
                () -> assertEquals(source.getActivityId(), result.activityId()),
                () -> assertEquals(source.getActivityName(), result.activityName()),
                () -> assertEquals(source.getActivityDesc(), result.activityDesc()),
                () -> assertEquals(source.getBeginDateTime(), result.beginDateTime()),
                () -> assertEquals(source.getEndDateTime(), result.endDateTime()),
                () -> assertEquals(source.getStockCount(), result.stockCount()),
                () -> assertEquals(source.getStockCountSurplus(), result.stockCountSurplus()),
                () -> assertEquals(source.getActivityCountId(), result.activityCountId()),
                () -> assertEquals(source.getStrategyId(), result.strategyId()),
                () -> assertEquals(source.getAwardUnlockScope(), result.awardUnlockScope()),
                () -> assertEquals(source.getState(), result.state()),
                () -> assertEquals(source.getVersion(), result.version()),
                () -> assertEquals(source.getCreatedBy(), result.createdBy()),
                () -> assertEquals(source.getUpdatedBy(), result.updatedBy()),
                () -> assertEquals(source.getCreateTime(), result.createTime()),
                () -> assertEquals(source.getUpdateTime(), result.updateTime())
        );
    }

    private ActivityPO activityPO() {
        ActivityPO po = new ActivityPO();
        po.setActivityId(100301L);
        po.setActivityName("九宫格活动抽奖");
        po.setActivityDesc("九宫格抽奖活动演示数据");
        po.setBeginDateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        po.setEndDateTime(LocalDateTime.of(2036, 12, 31, 23, 59, 59));
        po.setStockCount(100000);
        po.setStockCountSurplus(99999);
        po.setActivityCountId(100301L);
        po.setStrategyId(900901001L);
        po.setAwardUnlockScope("total");
        po.setState("open");
        po.setVersion(2);
        po.setCreatedBy("admin");
        po.setUpdatedBy("operator");
        po.setCreateTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 10, 22, 11, 0));
        return po;
    }
}
