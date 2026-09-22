package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 活动领域实体与持久化对象转换器测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
class ActivityAssemblerTest {

    /**
     * 验证活动实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivityInBothDirections() {
        // Given
        ActivityAssembler assembler = Mappers.getMapper(ActivityAssembler.class);
        ActivityEntity expected = ActivityEntity.builder()
                .activityId(100001L)
                .activityName("九宫格抽奖")
                .activityDesc("测试活动")
                .beginDateTime(LocalDateTime.of(2026, 9, 18, 10, 0))
                .endDateTime(LocalDateTime.of(2026, 10, 18, 10, 0))
                .stockCount(1000)
                .stockCountSurplus(900)
                .activityCountId(100001L)
                .strategyId(100006L)
                .state("open")
                .build();

        // When
        ActivityEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证活动次数配置实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivityCountInBothDirections() {
        // Given
        ActivityCountAssembler assembler = Mappers.getMapper(ActivityCountAssembler.class);
        ActivityCountEntity expected = ActivityCountEntity.builder()
                .activityCountId(100001L)
                .totalCount(10)
                .dayCount(2)
                .monthCount(5)
                .build();

        // When
        ActivityCountEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证用户活动总账户实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivityAccountInBothDirections() {
        // Given
        ActivityAccountAssembler assembler = Mappers.getMapper(ActivityAccountAssembler.class);
        ActivityAccountEntity expected = ActivityAccountEntity.builder()
                .userId("user-001")
                .activityId(100001L)
                .totalCount(10)
                .totalCountSurplus(8)
                .dayCount(2)
                .dayCountSurplus(1)
                .monthCount(5)
                .monthCountSurplus(3)
                .build();

        // When
        ActivityAccountEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证用户活动日账户实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivityAccountDayInBothDirections() {
        // Given
        ActivityAccountDayAssembler assembler = Mappers.getMapper(ActivityAccountDayAssembler.class);
        ActivityAccountDayEntity expected = ActivityAccountDayEntity.builder()
                .userId("user-001")
                .activityId(100001L)
                .day(LocalDate.of(2026, 9, 22))
                .dayCount(2)
                .dayCountSurplus(1)
                .build();

        // When
        ActivityAccountDayEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证用户活动月账户实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivityAccountMonthInBothDirections() {
        // Given
        ActivityAccountMonthAssembler assembler = Mappers.getMapper(ActivityAccountMonthAssembler.class);
        ActivityAccountMonthEntity expected = ActivityAccountMonthEntity.builder()
                .userId("user-001")
                .activityId(100001L)
                .month(YearMonth.of(2026, 9))
                .monthCount(5)
                .monthCountSurplus(3)
                .build();

        // When
        ActivityAccountMonthEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证活动 SKU 实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivitySkuInBothDirections() {
        // Given
        ActivitySkuAssembler assembler = Mappers.getMapper(ActivitySkuAssembler.class);
        ActivitySkuEntity expected = ActivitySkuEntity.builder()
                .sku(901001L)
                .activityId(100001L)
                .activityCountId(100001L)
                .stockCount(1000)
                .build();

        // When
        ActivitySkuEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }

    /**
     * 验证活动订单实体与持久化对象之间的双向转换契约。
     */
    @Test
    void shouldConvertActivityOrderInBothDirections() {
        // Given
        ActivityOrderAssembler assembler = Mappers.getMapper(ActivityOrderAssembler.class);
        ActivityOrderEntity expected = ActivityOrderEntity.builder()
                .userId("user-001")
                .activityId(100001L)
                .sku(901001L)
                .activityName("九宫格抽奖")
                .strategyId(100006L)
                .orderId("order-001")
                .orderTime(LocalDateTime.of(2026, 9, 18, 12, 0))
                .totalCount(10)
                .dayCount(2)
                .monthCount(5)
                .state("not_used")
                .build();

        // When
        ActivityOrderEntity actual = assembler.toEntity(assembler.toPO(expected));

        // Then
        assertEquals(expected, actual);
    }
}
