package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.messaging.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.UserAwardRecordEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证消息任务和用户中奖记录的持久化转换契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class MessagingAndAwardAssemblerTest {

    /**
     * 验证用户中奖记录在领域实体与持久化对象之间双向转换时保留业务字段。
     */
    @Test
    void shouldConvertUserAwardRecordInBothDirections() {
        // Given
        UserAwardRecordAssembler assembler = Mappers.getMapper(UserAwardRecordAssembler.class);
        UserAwardRecordEntity expected = UserAwardRecordEntity.builder()
                .userId("user-001")
                .activityId(100001L)
                .strategyId(100006L)
                .orderId("order-001")
                .awardId(100011L)
                .awardTitle("测试奖品")
                .awardTime(LocalDateTime.of(2026, 9, 22, 12, 0))
                .awardState("create")
                .build();

        // When
        UserAwardRecordEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证消息任务在领域实体与持久化对象之间双向转换时保留发布字段。
     */
    @Test
    void shouldConvertTaskInBothDirections() {
        // Given
        TaskAssembler assembler = Mappers.getMapper(TaskAssembler.class);
        TaskEntity expected = TaskEntity.builder()
                .topic("market-forge.exchange")
                .eventId("8f8d3cbb-4dda-4570-88ab-21956a65e83f")
                .eventType("activity.sku.stock.deduct")
                .messageBody("{\"sku\":100001}")
                .occurredAt(LocalDateTime.of(2026, 9, 22, 12, 0))
                .state("create")
                .build();

        // When
        TaskEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }
}
