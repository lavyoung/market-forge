package com.lavyoung.marketforge.application.strategy.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略定义应用结果测试。
 *
 * <p>验证完整策略结果不会暴露可变子集合，并在调用方传入空集合引用时立即失败，
 * 防止不完整结果继续进入 REST 响应装配过程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionResultTest {

    /**
     * Given 调用方传入可变集合，When 创建结果后修改原集合，Then 结果内容应保持不变且只读。
     */
    @Test
    void shouldProtectChildCollections() {
        // Given
        List<StrategyAwardConfigResult> awards = new ArrayList<>();
        List<StrategyRuleConfigResult> rules = new ArrayList<>();

        // When
        StrategyDefinitionResult result = result(awards, rules);
        awards.add(null);
        rules.add(null);

        // Then
        assertAll(
                () -> assertEquals(List.of(), result.awards()),
                () -> assertEquals(List.of(), result.rules()),
                () -> assertThrows(UnsupportedOperationException.class, () -> result.awards().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> result.rules().clear())
        );
    }

    /**
     * Given 奖品或规则集合引用为空，When 创建结果，Then 应立即拒绝不完整应用结果。
     */
    @Test
    void shouldRejectNullChildCollections() {
        // When & Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> result(null, List.of())),
                () -> assertThrows(NullPointerException.class, () -> result(List.of(), null))
        );
    }

    private StrategyDefinitionResult result(List<StrategyAwardConfigResult> awards, List<StrategyRuleConfigResult> rules) {
        return new StrategyDefinitionResult(100001L, "测试策略", null, "draft", 1,
                "operator-001", "operator-001", null, null, awards, rules);
    }
}
