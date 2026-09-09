package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证策略抽奖接口层装配器的双向边界模型转换。
 */
class StrategyRaffleAssemblerTest {

    private static final StrategyRaffleAssembler ASSEMBLER =
            Mappers.getMapper(StrategyRaffleAssembler.class);

    /**
     * Given 完整的 API 请求，When 转换为命令，Then 所有应用层入参保持一致。
     */
    @Test
    void shouldConvertRequestToCommand() {
        // Given
        StrategyRaffleRequest request = new StrategyRaffleRequest("user-001", 100_001L);

        // When
        RaffleCommand command = ASSEMBLER.toCommand(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), command.userId()),
                () -> assertEquals(request.strategyId(), command.strategyId())
        );
    }

    /**
     * Given 完整的应用结果，When 转换为响应，Then API 契约字段均不会丢失。
     */
    @Test
    void shouldConvertResultToResponse() {
        // Given
        RaffleResult result = new RaffleResult(
                100_001L, 100_011L, "random_ore", "quantity=1", "随机矿石");

        // When
        StrategyRaffleResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(result.strategyId(), response.strategyId()),
                () -> assertEquals(result.awardId(), response.awardId()),
                () -> assertEquals(result.awardKey(), response.awardKey()),
                () -> assertEquals(result.awardConfig(), response.awardConfig()),
                () -> assertEquals(result.awardDesc(), response.awardDesc())
        );
    }
}
