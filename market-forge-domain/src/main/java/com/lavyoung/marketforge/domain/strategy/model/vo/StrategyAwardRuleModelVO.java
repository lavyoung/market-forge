package com.lavyoung.marketforge.domain.strategy.model.vo;

import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
     */
    public List<RuleModel> raffleExecutingRuleModelsList() {
        return toModelList().stream().filter(RuleModel::isExecutingModel).toList();
    }

    /**
     * 将路径式规则编码转换为有效的规则模型列表。
     * <p>
     * 空配置返回空列表，无法识别的规则编码会被忽略，并保持其余规则的配置顺序。
     *
     * @return 有效规则模型的不可修改列表
     */
    public List<RuleModel> toModelList() {
        if (StringUtils.isBlank(ruleModels)) {
            return List.of();
        }
        return Arrays.stream(ruleModels.split(Constants.SPLIT))
                .map(String::trim)
                .map(RuleModel::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
