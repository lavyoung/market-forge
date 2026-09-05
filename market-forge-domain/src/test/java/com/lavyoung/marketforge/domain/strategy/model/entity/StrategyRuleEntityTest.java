package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link StrategyRuleEntity} 的不可变数据和权重规则解析行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
class StrategyRuleEntityTest {

    /**
     * Given 权重规则 record，When 解析规则值，Then 返回每个门槛对应的奖品列表。
     */
    @Test
    void shouldParseWeightRuleValues() {
        // Given
        StrategyRuleEntity rule = StrategyRuleEntity.builder()
                .strategyId(100_001L)
                .ruleModel(RuleModel.WEIGHT.getCode())
                .ruleValue("4000:100011/100012;5000:100013")
                .build();

        // When
        Map<String, List<Long>> weightValues = rule.ruleWeightValues();

        // Then
        assertEquals(List.of(100_011L, 100_012L), weightValues.get("4000"));
        assertEquals(List.of(100_013L), weightValues.get("5000"));
    }

    /**
     * Given 非权重规则，When 请求权重配置，Then 返回空映射。
     */
    @Test
    void shouldReturnEmptyWeightValuesForOtherRuleModels() {
        // Given
        StrategyRuleEntity rule = StrategyRuleEntity.builder()
                .ruleModel(RuleModel.LOCK.getCode())
                .ruleValue("1")
                .build();

        // When
        Map<String, List<Long>> weightValues = rule.ruleWeightValues();

        // Then
        assertTrue(weightValues.isEmpty());
    }
}
