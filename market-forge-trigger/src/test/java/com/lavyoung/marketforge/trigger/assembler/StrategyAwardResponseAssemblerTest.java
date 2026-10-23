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
class StrategyAwardResponseAssemblerTest {

    private static final long UNSAFE_JAVASCRIPT_INTEGER = 9_007_199_254_740_993L;
    private static final StrategyAwardResponseAssembler ASSEMBLER =
            Mappers.getMapper(StrategyAwardResponseAssembler.class);

    /**
     * Given 包含超出 JavaScript 安全整数范围标识的应用层奖品列表，
     * When 批量转换，Then 标识转换为无精度损失的字符串且业务数值保持原类型。
     */
    @Test
    void shouldConvertAwardEntitiesToResponses() {
        // Given
        StrategyAwardResult result = new StrategyAwardResult(
                UNSAFE_JAVASCRIPT_INTEGER,
                UNSAFE_JAVASCRIPT_INTEGER + 1,
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
                () -> assertEquals(String.valueOf(result.strategyId()), response.strategyId()),
                () -> assertEquals(String.valueOf(result.awardId()), response.awardId()),
                () -> assertEquals(result.awardTitle(), response.awardTitle()),
                () -> assertEquals(result.awardCount(), response.awardCount()),
                () -> assertEquals(result.awardCountSurplus(), response.awardCountSurplus()),
                () -> assertEquals(result.awardRate(), response.awardRate()),
                () -> assertEquals(result.sort(), response.sort())
        );
    }
}
