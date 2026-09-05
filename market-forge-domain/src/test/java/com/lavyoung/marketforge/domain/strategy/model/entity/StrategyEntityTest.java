package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link StrategyEntity} 的规则模型转换行为。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
class StrategyEntityTest {

    /**
     * Given 配置包含有效和未知编码，When 转换规则模型，Then 忽略未知编码并保留配置顺序。
     */
    @Test
    void shouldConvertKnownRuleModelsAndIgnoreUnknownValues() {
        // Given
        StrategyEntity strategy = StrategyEntity.builder()
                .ruleModels("rule_weight,unknown,rule_blacklist")
                .build();

        // When
        List<RuleModel> models = strategy.toRuleModes();

        // Then
        assertEquals(List.of(RuleModel.WEIGHT, RuleModel.RULE_BLACKLIST), models);
    }

    /**
     * Given 规则配置为空白，When 转换规则模型，Then 返回空列表。
     */
    @Test
    void shouldReturnEmptyRuleModelsWhenConfigurationIsBlank() {
        // Given
        StrategyEntity strategy = StrategyEntity.builder().ruleModels(" ").build();

        // When
        List<RuleModel> models = strategy.toRuleModes();

        // Then
        assertTrue(models.isEmpty());
    }

    /**
     * Given 策略已配置权重规则，When 判断已知和未知规则，Then 返回对应包含结果。
     */
    @Test
    void shouldReportWhetherRuleModelIsConfigured() {
        // Given
        StrategyEntity strategy = StrategyEntity.builder().ruleModels("rule_weight").build();

        // When & Then
        assertTrue(strategy.containsModel(RuleModel.WEIGHT.getCode()));
        assertFalse(strategy.containsModel("unknown"));
    }
}
