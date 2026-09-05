package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 抽奖策略基础配置。
 *
 * @param strategyId   策略标识
 * @param strategyDesc 策略描述
 * @param ruleModels   逗号分隔的规则模型编码集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/08/31
 */
@Builder
public record StrategyEntity(
        Long strategyId,
        String strategyDesc,
        String ruleModels
) {

    /**
     * 将策略配置中的规则模型编码转换为枚举列表。
     *
     * @return 有效的规则模型列表；未配置规则时返回空列表
     */
    public List<RuleModel> toRuleModes() {
        if (StringUtils.isBlank(ruleModels)) {
            return List.of();
        }
        return Arrays.stream(ruleModels.split(",")).map(RuleModel::get).filter(Objects::nonNull).toList();
    }

    /**
     * 判断策略是否配置了指定规则模型。
     *
     * @param ruleModel 待判断的规则模型编码
     * @return 配置中包含该规则模型时返回 {@code true}，否则返回 {@code false}
     */
    public boolean containsModel(String ruleModel) {
        RuleModel model = RuleModel.get(ruleModel);
        if (model == null) {
            return false;
        }
        return toRuleModes().contains(model);
    }
}
