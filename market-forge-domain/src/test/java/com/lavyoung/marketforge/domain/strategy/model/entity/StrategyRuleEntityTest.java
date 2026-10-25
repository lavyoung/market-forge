package com.lavyoung.marketforge.domain.strategy.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 {@link StrategyRuleEntity} 只保存不可变规则数据，不再承担具体规则值解析职责。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
class StrategyRuleEntityTest {

    /**
     * Given 完整规则数据，When 使用构建器创建实体，Then 应原样保留规则归属和持久化配置。
     */
    @Test
    void shouldRetainImmutableRuleData() {
        // Given & When
        StrategyRuleEntity rule = StrategyRuleEntity.builder()
                .strategyId(100_001L)
                .awardId(200_001L)
                .ruleType(2)
                .ruleModel("rule_lock")
                .ruleValue("3")
                .ruleDesc("累计抽奖三次解锁")
                .build();

        // Then
        assertAll(
                () -> assertEquals(100_001L, rule.strategyId()),
                () -> assertEquals(200_001L, rule.awardId()),
                () -> assertEquals(2, rule.ruleType()),
                () -> assertEquals("rule_lock", rule.ruleModel()),
                () -> assertEquals("3", rule.ruleValue()),
                () -> assertEquals("累计抽奖三次解锁", rule.ruleDesc())
        );
    }
}
