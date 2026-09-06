package com.lavyoung.marketforge.types.domain.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link RuleModel} 的编码查询与执行阶段判断行为。
 */
class RuleModelTest {

    /**
     * Given 已知与未知规则编码，When 查询规则模型，Then 返回对应枚举或空值。
     */
    @Test
    void shouldResolveKnownCodeAndIgnoreUnknownCode() {
        // Given & When & Then
        assertEquals(RuleModel.WEIGHT, RuleModel.get("rule_weight"));
        assertNull(RuleModel.get("unknown"));
        assertNull(RuleModel.get(null));
    }

    /**
     * Given 各阶段规则编码，When 判断执行阶段，Then 仅匹配所属阶段。
     */
    @Test
    void shouldIdentifyRuleExecutionStage() {
        // Given & When & Then
        assertTrue(RuleModel.isBeforeModel(RuleModel.WEIGHT.getCode()));
        assertTrue(RuleModel.isExecutingModel(RuleModel.LOCK.getCode()));
        assertTrue(RuleModel.isAfterModel(RuleModel.RANDOM.getCode()));
        assertFalse(RuleModel.isBeforeModel(RuleModel.DEFAULT.getCode()));
        assertFalse(RuleModel.isExecutingModel("unknown"));
        assertFalse(RuleModel.isAfterModel(null));
    }

    /**
     * Given 黑名单、权重和默认节点，When 比较责任链优先级，Then 黑名单最先且默认节点最后。
     */
    @Test
    void shouldDefineExpectedChainPriority() {
        // Given & When & Then
        assertTrue(RuleModel.RULE_BLACKLIST.getOrder() > RuleModel.WEIGHT.getOrder());
        assertTrue(RuleModel.WEIGHT.getOrder() > RuleModel.DEFAULT.getOrder());
    }
}
