package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证概率抽奖算法类型的持久化编码解析行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class ProbabilityAlgorithmTypeTest {

    /**
     * Given 已注册的算法编码，When 解析算法类型，Then 返回对应的枚举值。
     */
    @Test
    void shouldResolveAlgorithmTypeByCode() {
        // Given
        String code = ProbabilityAlgorithmType.ALIAS_METHOD.getCode();

        // When
        Optional<ProbabilityAlgorithmType> algorithmType = ProbabilityAlgorithmType.fromCode(code);

        // Then
        assertEquals(Optional.of(ProbabilityAlgorithmType.ALIAS_METHOD), algorithmType);
    }

    /**
     * Given 空白或未知算法编码，When 解析算法类型，Then 返回空结果而不是错误匹配。
     */
    @Test
    void shouldReturnEmptyWhenAlgorithmCodeIsInvalid() {
        // Given & When
        Optional<ProbabilityAlgorithmType> nullResult = ProbabilityAlgorithmType.fromCode(null);
        Optional<ProbabilityAlgorithmType> blankResult = ProbabilityAlgorithmType.fromCode("  ");
        Optional<ProbabilityAlgorithmType> unknownResult = ProbabilityAlgorithmType.fromCode("unknown");

        // Then
        assertTrue(nullResult.isEmpty());
        assertTrue(blankResult.isEmpty());
        assertTrue(unknownResult.isEmpty());
    }
}
