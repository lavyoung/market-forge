package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAccountResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityAwardResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityAccountResult;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证活动抽奖接口层装配器的边界模型转换。
 */
class ActivityRaffleResponseAssemblerTest {

    private static final long UNSAFE_JAVASCRIPT_INTEGER = 9_007_199_254_740_993L;

    private static final ActivityRaffleResponseAssembler ASSEMBLER =
            Mappers.getMapper(ActivityRaffleResponseAssembler.class);

    /**
     * Given 活动页查询请求，When 转换为应用命令，Then 用户和活动标识保持一致。
     */
    @Test
    void shouldConvertAwardListRequestToCommand() {
        // Given
        ActivityAwardListRequest request = new ActivityAwardListRequest("user-001", 100_301L);

        // When
        ActivityAwardListCommand command = ASSEMBLER.toCommand(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), command.userId()),
                () -> assertEquals(request.activityId(), command.activityId())
        );
    }

    /**
     * Given 活动账户应用结果，When 转换为 API 响应，Then 总、日、月三组额度字段完整保留。
     */
    @Test
    void shouldConvertActivityAccountResultToResponse() {
        // Given
        ActivityAccountResult result = new ActivityAccountResult(10, 8, 3, 2, 7, 6);

        // When
        ActivityAccountResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(result.totalCount(), response.totalCount()),
                () -> assertEquals(result.totalCountSurplus(), response.totalCountSurplus()),
                () -> assertEquals(result.dayCount(), response.dayCount()),
                () -> assertEquals(result.dayCountSurplus(), response.dayCountSurplus()),
                () -> assertEquals(result.monthCount(), response.monthCount()),
                () -> assertEquals(result.monthCountSurplus(), response.monthCountSurplus())
        );
    }

    /**
     * Given 活动奖品应用结果，When 转换为 API 响应，Then 次数锁和权重奖池展示字段完整保留。
     */
    @Test
    void shouldConvertActivityAwardResultToResponse() {
        // Given
        ActivityAwardResult result = new ActivityAwardResult(
                UNSAFE_JAVASCRIPT_INTEGER,
                "1等奖",
                "抽奖2次后解锁",
                8,
                2,
                "total",
                false,
                1,
                4_000,
                false,
                3_999
        );

        // When
        ActivityAwardResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(String.valueOf(result.awardId()), response.awardId()),
                () -> assertEquals(result.awardTitle(), response.awardTitle()),
                () -> assertEquals(result.awardSubtitle(), response.awardSubtitle()),
                () -> assertEquals(result.sort(), response.sort()),
                () -> assertEquals(result.awardRuleLockCount(), response.awardRuleLockCount()),
                () -> assertEquals(result.awardUnlockScope(), response.awardUnlockScope()),
                () -> assertEquals(result.isAwardUnlock(), response.isAwardUnlock()),
                () -> assertEquals(result.waitUnLockCount(), response.waitUnLockCount()),
                () -> assertEquals(result.awardRuleWeightCount(), response.awardRuleWeightCount()),
                () -> assertEquals(result.isAwardWeightUnlock(), response.isAwardWeightUnlock()),
                () -> assertEquals(result.waitAwardWeightCount(), response.waitAwardWeightCount())
        );
    }

    /**
     * Given 超出 JavaScript 安全整数范围的活动、策略和奖品标识，
     * When 转换为抽奖 API 响应，Then 所有标识都应无损转换为十进制字符串。
     */
    @Test
    void shouldConvertLongIdentifiersToStringsInDrawResponse() {
        // Given
        ActivityRaffleResult result = new ActivityRaffleResult(
                "order-001",
                UNSAFE_JAVASCRIPT_INTEGER,
                UNSAFE_JAVASCRIPT_INTEGER + 1,
                UNSAFE_JAVASCRIPT_INTEGER + 2,
                "integral",
                "10",
                "10 积分",
                "自动发放积分"
        );

        // When
        ActivityDrawResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals("9007199254740993", response.activityId()),
                () -> assertEquals("9007199254740994", response.strategyId()),
                () -> assertEquals("9007199254740995", response.awardId()),
                () -> assertEquals(result.orderId(), response.orderId()),
                () -> assertEquals(result.awardKey(), response.awardKey()),
                () -> assertEquals(result.awardTitle(), response.awardTitle())
        );
    }
}
