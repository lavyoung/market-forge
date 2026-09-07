package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认抽奖责任链尾节点。
 * <p>
 * 当前置规则均未直接返回奖品时，从策略默认概率表中随机选择奖品。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRuleChain extends AbstractLogicChain {

    /**
     * 已装配策略的随机调度服务。
     */
    private final IStrategyDispatch strategyDispatch;

    /**
     * 执行默认概率抽奖。
     *
     * @param userId     参与抽奖的用户标识，仅用于记录日志
     * @param strategyId 抽奖策略标识
     * @return 默认概率表随机选中的奖品及默认规则模型
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        long awardId = strategyDispatch.getRandomAwardId(strategyId);
        log.info("抽奖责任链-默认处理 userId={} strategyId={} ruleModel={} awardId={}", userId, strategyId, ruleModel(), awardId);
        return DefaultChainFactory.StrategyAwardVO.builder().awardId(awardId).ruleModel(ruleModel()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleModel ruleModel() {
        return RuleModel.DEFAULT;
    }
}
