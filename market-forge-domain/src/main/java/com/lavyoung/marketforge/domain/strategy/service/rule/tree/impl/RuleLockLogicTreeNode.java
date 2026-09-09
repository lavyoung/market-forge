package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 奖品次数锁规则树节点。
 * <p>
 * 从奖品规则配置中读取解锁所需次数；未达到门槛时接管流程，由后继节点提供兜底奖品。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component("rule_lock")
@RequiredArgsConstructor
public class RuleLockLogicTreeNode implements ILogicTreeNode {

    /**
     * 当前阶段尚未接入用户抽奖次数账户时使用的开发基准值。
     */
    private static final int DEVELOPMENT_USER_RAFFLE_COUNT = 2;

    /**
     * 抽奖策略仓储端口。
     */
    private final IStrategyRepository repository;

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId, String ruleValue) {
        String lockRuleValue = repository.queryStrategyRuleValue(strategyId, awardId, ruleModel().getCode());
        int requiredRaffleCount = parseRequiredRaffleCount(lockRuleValue, userId, strategyId, awardId);
        if (requiredRaffleCount > DEVELOPMENT_USER_RAFFLE_COUNT) {
            return DefaultTreeFactory.TreeActionEntity
                    .builder()
                    .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.TAKE_OVER)
                    .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                            .awardId(null) // 此奖励不允许
                            .ruleModel(RuleModel.LOCK)
                            .build()
                    )
                    .build();
        }
        log.info("抽奖策略-规则树，次数锁通过 userId={} strategyId={} ruleModel={} ruleValue={}",
                userId, strategyId, ruleModel(), lockRuleValue);
        return DefaultTreeFactory.TreeActionEntity
                .builder()
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO
                        .builder()
                        .awardId(awardId)
                        .build())
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }

    @Override
    public RuleModel ruleModel() {
        return RuleModel.LOCK;
    }

    /**
     * 解析奖品解锁所需的累计抽奖次数。
     *
     * @param lockRuleValue 奖品次数锁规则值
     * @param userId        用户标识，仅用于错误日志
     * @param strategyId    策略标识，仅用于错误日志
     * @param awardId       奖品标识，仅用于错误日志
     * @return 非负的解锁次数
     * @throws BusinessException 规则值为空、非整数或小于零时抛出
     */
    private int parseRequiredRaffleCount(String lockRuleValue, String userId, Long strategyId, Long awardId) {
        if (StringUtils.isBlank(lockRuleValue)) {
            log.error("抽奖策略-规则树，次数锁规则值为空 userId={} strategyId={} awardId={}", userId, strategyId, awardId);
            throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID);
        }
        try {
            int requiredRaffleCount = Integer.parseInt(lockRuleValue);
            if (requiredRaffleCount < 0) {
                throw new NumberFormatException("negative raffle count");
            }
            return requiredRaffleCount;
        } catch (NumberFormatException exception) {
            log.error("抽奖策略-规则树，次数锁规则值非法 userId={} strategyId={} awardId={} ruleValue={}",
                    userId, strategyId, awardId, lockRuleValue);
            throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, exception);
        }
    }
}
