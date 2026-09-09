package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.response.StrategyAwardResponse;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证策略奖品应用层结果到 API 响应的批量转换行为。
 */
class StrategyAwardAssemblerTest {

    private static final StrategyAwardAssembler ASSEMBLER =
            Mappers.getMapper(StrategyAwardAssembler.class);

    /**
     * Given 完整应用层奖品列表，When 批量转换，Then API 所需字段全部保留。
     */
    @Test
    void shouldConvertAwardEntitiesToResponses() {
        // Given
        StrategyAwardResult result = new StrategyAwardResult(
                100_001L,
                100_011L,
                "随机矿石",
                100,
                80,
                new BigDecimal("0.1000"),
                1
        );

        // When
        List<StrategyAwardResponse> responses = ASSEMBLER.toResponses(List.of(result));

        // Then
        StrategyAwardResponse response = responses.get(0);
        assertAll(
                () -> assertEquals(1, responses.size()),
                () -> assertEquals(result.strategyId(), response.strategyId()),
                () -> assertEquals(result.awardId(), response.awardId()),
                () -> assertEquals(result.awardTitle(), response.awardTitle()),
                () -> assertEquals(result.awardCount(), response.awardCount()),
                () -> assertEquals(result.awardCountSurplus(), response.awardCountSurplus()),
                () -> assertEquals(result.awardRate(), response.awardRate()),
                () -> assertEquals(result.sort(), response.sort())
        );
    }
}
