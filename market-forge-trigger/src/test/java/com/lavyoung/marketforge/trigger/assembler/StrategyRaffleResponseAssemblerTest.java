package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证策略抽奖接口层装配器的双向边界模型转换。
 */
class StrategyRaffleResponseAssemblerTest {

    private static final long UNSAFE_JAVASCRIPT_INTEGER = 9_007_199_254_740_993L;
    private static final StrategyRaffleResponseAssembler ASSEMBLER =
            Mappers.getMapper(StrategyRaffleResponseAssembler.class);

    /**
     * Given 完整的 API 请求，When 转换为命令，Then 所有应用层入参保持一致。
     */
    @Test
    void shouldConvertRequestToCommand() {
        // Given
        StrategyRaffleRequest request = new StrategyRaffleRequest("user-001", 100_301L, 9_011L);

        // When
        ActivityRaffleCommand command = ASSEMBLER.toCommand(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), command.userId()),
                () -> assertEquals(request.activityId(), command.activityId()),
                () -> assertEquals(request.sku(), command.sku())
        );
    }

    /**
     * Given 包含超出 JavaScript 安全整数范围标识的应用结果，
     * When 转换为响应，Then 三个业务标识均转换为无精度损失的字符串。
     */
    @Test
    void shouldConvertResultToResponse() {
        // Given
        ActivityRaffleResult result = new ActivityRaffleResult(
                "RO1003010001", UNSAFE_JAVASCRIPT_INTEGER, UNSAFE_JAVASCRIPT_INTEGER + 1,
                UNSAFE_JAVASCRIPT_INTEGER + 2, "random_ore", "quantity=1", "随机矿石", "随机矿石");

        // When
        StrategyRaffleResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(result.orderId(), response.orderId()),
                () -> assertEquals(String.valueOf(result.activityId()), response.activityId()),
                () -> assertEquals(String.valueOf(result.strategyId()), response.strategyId()),
                () -> assertEquals(String.valueOf(result.awardId()), response.awardId()),
                () -> assertEquals(result.awardKey(), response.awardKey()),
                () -> assertEquals(result.awardConfig(), response.awardConfig()),
                () -> assertEquals(result.awardDesc(), response.awardDesc())
        );
    }
}
