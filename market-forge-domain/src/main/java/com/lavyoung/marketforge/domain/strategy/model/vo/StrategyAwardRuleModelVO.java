package com.lavyoung.marketforge.domain.strategy.model.vo;

import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;

import java.util.Arrays;
import java.util.List;

/**
 * 策略奖品规则模型值对象。
 * <p>
 * 封装奖品配置的规则模型字符串，并提供按抽奖执行阶段筛选规则的能力。
 *
 * @param ruleModels 以 {@link Constants#SPLIT} 分隔的规则模型编码
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Builder
public record StrategyAwardRuleModelVO(
        String ruleModels
) {

    /**
     * 获取需要在抽奖执行阶段处理的规则模型。
     * <p>
     * 无法识别或不属于执行阶段的规则编码会被忽略，结果顺序与配置顺序一致。
     *
     * @return 抽奖执行阶段的规则模型列表；没有匹配项时返回空列表
     * @throws NullPointerException 当规则模型配置为 {@code null} 时抛出
     */
    public List<RuleModel> raffleExecutingRuleModelsList() {
        String[] ruleModelValues = ruleModels.split(Constants.SPLIT);
        return Arrays.stream(ruleModelValues).filter(RuleModel::isExecutingModel).map(RuleModel::get).toList();
    }
}
