package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.WeightRuleValueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.WeightTierVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 权重抽奖责任链节点。
 * <p>
 * 根据用户在当前策略下的累计抽奖次数，匹配不高于当前次数的最高权重档位；
 * 命中时使用该档位对应的权重奖池执行抽奖，未命中或未配置规则时传递给后继节点。
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
     * 按用户累计抽奖次数选择最高可用权重档位。
     *
     * <p>权重规则为空时继续责任链。存在规则配置时，通过
     * {@link WeightRuleValueVO} 选择不高于用户累计次数的最高档位，
     * 并使用档位门槛访问预先装配的概率快照。</p>
     *
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @return 权重概率快照选中的奖品，或者后继节点的处理结果
     * @throws BusinessException    权重规则值非法或对应概率快照尚未装配时抛出
     * @throws NullPointerException 没有命中档位且没有后继节点时抛出
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-权重策略处理 userId={} strategyId={} ruleModel={}", userId, strategyId, ruleModel());
        String weightRuleValueStr = repository.queryStrategyRuleValue(strategyId, RuleModel.WEIGHT.getCode());
        if (weightRuleValueStr == null || weightRuleValueStr.isBlank()) {
            return next().logic(userId, strategyId);
        }
        WeightRuleValueVO weightRuleValue;
        try {
            weightRuleValue = WeightRuleValueVO.parse(weightRuleValueStr);
        } catch (IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID,
                    exception, strategyId, RuleModel.WEIGHT.getCode(), weightRuleValueStr);
        }
        long userRaffleCount = repository.queryUserRaffleCount(userId, strategyId);
        Optional<WeightTierVO> matchedTier = weightRuleValue.match(userRaffleCount);
        if (matchedTier.isEmpty()) {
            return next().logic(userId, strategyId);
        }
        WeightTierVO weightTierVO = matchedTier.orElseThrow();
        String weightThreshold = Long.toString(weightTierVO.threshold());
        long awardId = strategyDispatch.getRandomAwardIdAndWeight(strategyId, weightThreshold);
        log.info("抽奖责任链-权重策略命中 userId={} strategyId={} userRaffleCount={} weightThreshold={} awardId={}",
                userId, strategyId, userRaffleCount, weightThreshold, awardId);
        return DefaultChainFactory.StrategyAwardVO.builder()
                .awardId(awardId)
                .ruleModel(ruleModel())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleModel ruleModel() {
        return RuleModel.WEIGHT;
    }
}
