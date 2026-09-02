package com.lavyoung.marketforge.domain.strategy.model;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 策略实体
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/08/31
 */
@Data
@Builder
public class StrategyEntity {

    /**
     * 策略id
     */
    private Long strategyId;
    /**
     * 描述
     */
    private String strategyDesc;
    /**
     * 规则模型
     */
    private String ruleModels;

    public List<RuleModel> toRuleModes() {
        if (StringUtils.isBlank(ruleModels)) {
            return List.of();
        }
        return Arrays.stream(ruleModels.split(",")).map(RuleModel::get).filter(Objects::nonNull).toList();
    }

    /**
     * 是否包含
     *
     * @param ruleModel 权重模型
     * @return true
     */
    public boolean containsModel(String ruleModel) {
        RuleModel model = RuleModel.get(ruleModel);
        if (model == null) {
            return false;
        }
        return toRuleModes().contains(model);
    }
}
