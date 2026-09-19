package com.lavyoung.marketforge.domain.activity.model.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 活动领域实体业务字段契约测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
class ActivityEntityContractTest {

    private static final Map<Class<?>, Set<String>> ENTITY_COMPONENTS = Map.of(
            ActivityEntity.class, Set.of(
                    "activityId", "activityName", "activityDesc", "beginDateTime", "endDateTime", "stockCount",
                    "stockCountSurplus", "activityCountId", "strategyId", "state"
            ),
            ActivityCountEntity.class, Set.of(
                    "activityCountId", "totalCount", "dayCount", "monthCount"
            ),
            ActivityAccountEntity.class, Set.of(
                    "userId", "activityId", "totalCount", "totalCountSurplus", "dayCount", "dayCountSurplus",
                    "monthCount", "monthCountSurplus"
            ),
            ActivityAccountFlowEntity.class, Set.of(
                    "userId", "activityId", "totalCount", "dayCount", "monthCount", "flowId", "flowChannel", "bizId"
            ),
            ActivityOrderEntity.class, Set.of(
                    "userId", "activityId", "activityName", "strategyId", "orderId", "orderTime", "state"
            )
    );

    @Test
    void shouldDefineActivityModelsAsImmutableRecords() {
        // Given
        Set<Class<?>> activityEntityTypes = ENTITY_COMPONENTS.keySet();

        // When & Then
        activityEntityTypes.forEach(entityType ->
                assertTrue(entityType.isRecord(), () -> entityType.getSimpleName() + " 应当使用 record 定义")
        );
    }

    @Test
    void shouldExposeOnlyBusinessComponents() {
        // Given
        ENTITY_COMPONENTS.forEach((entityType, expectedComponents) -> {
            // When
            Set<String> actualComponents = Arrays.stream(entityType.getRecordComponents())
                    .map(RecordComponent::getName)
                    .collect(Collectors.toUnmodifiableSet());

            // Then
            assertEquals(expectedComponents, actualComponents, () -> entityType.getSimpleName() + " 业务字段不完整");
        });
    }
}
