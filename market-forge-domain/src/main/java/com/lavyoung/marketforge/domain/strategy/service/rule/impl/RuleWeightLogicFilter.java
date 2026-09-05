package com.lavyoung.marketforge.domain.strategy.service.rule.impl;

import com.lavyoung.marketforge.domain.strategy.annotation.LogicStrategy;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.ILogicFilter;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 权重前置规则过滤器。
 * <p>
 * 根据用户分值匹配权重门槛，并返回相应权重策略的概率查找表键。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/05
 */
@Slf4j
@Component
@RequiredArgsConstructor
@LogicStrategy(logicModel = RuleModel.WEIGHT)
public class RuleWeightLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    /**
     * 抽奖策略仓储端口。
     */
    private final IStrategyRepository repository;

    /**
     * 当前用于规则判断的用户分值；后续应由真实用户账户数据替换。
     */
    public Long userScore = 4500L;

    /**
     * 根据权重规则筛选用户可参与的奖品范围。
     * <p>
     * 规则格式示例：{@code 4000:102/103/104/105;5000:102/103/104/105/106/107}。
     *
     * @param ruleMatterEntity 包含用户、策略及规则模型的规则物料
     * @return 命中权重门槛时返回接管动作，否则返回放行动作
     * @throws IllegalArgumentException 权重规则配置格式非法时抛出
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-权重 userId={} strategyId={} awardId={}, ruleModel={}", ruleMatterEntity.userId(), ruleMatterEntity.strategyId(), ruleMatterEntity.awardId(), ruleMatterEntity.ruleModel());
        String weightRuleValue = repository.queryStrategyRuleValue(ruleMatterEntity.strategyId(), ruleMatterEntity.awardId(), ruleMatterEntity.ruleModel());
        Map<Long, String> ruleValueMap = ruleValueMap(weightRuleValue);
        if (!CollectionUtils.isEmpty(ruleValueMap)) {
            // todo 这里需要实际的用户分数
            Long weightKey = ruleValueMap.keySet().stream()
                    .filter(weight -> weight <= userScore)
                    .max(Long::compareTo)
                    .orElse(null);
            if (weightKey != null) {
                return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                        .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                        .msg(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                        .ruleModel(RuleModel.WEIGHT.getCode())
                        .data(RuleActionEntity.RaffleBeforeEntity.builder()
                                .strategyId(ruleMatterEntity.strategyId())
                                .awardId(ruleMatterEntity.awardId())
                                .ruleWeightValueKey(ruleValueMap.get(weightKey))
                                .build()
                        )
                        .build();
            }
        }
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .msg(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

    /**
     * 将权重规则配置解析为“权重门槛—规则值”的映射。
     *
     * @param ruleValue 以分号分组、冒号分隔权重与奖品范围的规则配置
     * @return 权重门槛与规则值的映射；配置为空时返回空映射
     * @throws IllegalArgumentException 规则分组格式非法或权重门槛不是有效数字时抛出
     */
    private Map<Long, String> ruleValueMap(String ruleValue) {
        if (StringUtils.isBlank(ruleValue)) {
            return Map.of();
        }
        Map<Long, String> ruleValueMap = new HashMap<>();
        String[] splitRuleValue = ruleValue.split(Constants.SEMICOLON);
        for (String ruleWeightGroup : splitRuleValue) {
            if (StringUtils.isBlank(ruleWeightGroup)) {
                return ruleValueMap;
            }
            String[] split = ruleWeightGroup.split(Constants.COLON);
            if (split.length != 2) {
                throw new IllegalArgumentException("rule weight rule invalid input format: " + ruleWeightGroup);
            }
            ruleValueMap.put(Long.parseLong(split[0]), split[0]);
        }
        return ruleValueMap;
    }
}
