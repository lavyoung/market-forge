package com.lavyoung.marketforge.domain.strategy.model.vo;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 {@link StrategyAwardRuleModelVO} 对抽奖执行阶段规则的筛选行为。
 */
class StrategyAwardRuleModelVOTest {

    /**
     * Given 混合阶段及未知规则编码，When 获取执行中规则，Then 仅保留有效执行中规则及原顺序。
     */
    @Test
    void shouldKeepOnlyExecutingRuleModelsInConfigurationOrder() {
        // Given
        StrategyAwardRuleModelVO ruleModels = new StrategyAwardRuleModelVO(
                "rule_weight/rule_lock/unknown/rule_luck_award/rule_random"
        );

        // When
        List<RuleModel> result = ruleModels.raffleExecutingRuleModelsList();

        // Then
        assertEquals(List.of(RuleModel.LOCK, RuleModel.LUCK_AWARD), result);
    }

    /**
     * Given 规则配置为空，When 获取执行中规则，Then 抛出空指针异常以暴露非法持久化数据。
     */
    @Test
    void shouldRejectNullRuleModels() {
        // Given
        StrategyAwardRuleModelVO ruleModels = new StrategyAwardRuleModelVO(null);

        // When & Then
        assertThrows(NullPointerException.class, ruleModels::raffleExecutingRuleModelsList);
    }
}
