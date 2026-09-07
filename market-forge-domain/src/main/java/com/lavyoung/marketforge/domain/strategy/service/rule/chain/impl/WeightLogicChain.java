package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
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
 * 权重抽奖责任链节点。
 * <p>
 * 根据用户分值匹配不高于当前分值的最高权重档位；命中时使用对应权重概率表抽奖，
 * 未命中或未配置规则时传递给后继节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeightLogicChain extends AbstractLogicChain {

    /**
     * 查询权重规则配置的策略仓储端口。
     */
    private final IStrategyRepository repository;

    /**
     * 已装配策略的随机调度服务。
     */
    private final IStrategyDispatch strategyDispatch;

    /**
     * 当前用于规则判断的用户分值；后续应由真实用户账户数据替换。
     */
    private Long userScore = 4500L;

    /**
     * 按用户分值执行权重抽奖。
     *
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @return 权重概率表或后继节点选中的奖品及命中规则模型
     * @throws IllegalArgumentException 权重规则配置格式非法时抛出
     * @throws NullPointerException     未命中权重且未装配后继节点时抛出
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-权重策略处理 userId={} strategyId={} ruleModel={} ", userId, strategyId, ruleModel());
        String weightRuleValue = repository.queryStrategyRuleValue(strategyId, RuleModel.WEIGHT.getCode());
        Map<Long, String> ruleValueMap = ruleValueMap(weightRuleValue);
        if (CollectionUtils.isEmpty(ruleValueMap)) {
            return this.next().logic(userId, strategyId);
        }
        // 排序比对操作
        Long weightKey = ruleValueMap.keySet().stream()
                .filter(weight -> weight <= userScore)
                .max(Long::compareTo)
                .orElse(null);
        if (weightKey != null) {
            long awardIdAndWeight = strategyDispatch.getRandomAwardIdAndWeight(strategyId, ruleValueMap.get(weightKey));
            log.info("抽奖责任链-权重策略命中 userId={} strategyId={} ruleModel={} userScore={} weightKey={} awardId={}", userId, strategyId, ruleModel(), userScore, weightKey, awardIdAndWeight);
            return DefaultChainFactory.StrategyAwardVO.builder().awardId(awardIdAndWeight).ruleModel(ruleModel()).build();
        }
        return this.next().logic(userId, strategyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleModel ruleModel() {
        return RuleModel.WEIGHT;
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
