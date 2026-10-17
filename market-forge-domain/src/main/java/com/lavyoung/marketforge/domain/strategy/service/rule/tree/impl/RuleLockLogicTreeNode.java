package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.domain.award.service.IAwardUnlockService;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 奖品次数锁规则树节点。
 *
 * <p>活动抽奖按照活动配置的日次数或累计次数判断奖品是否解锁。
 * 非活动抽奖没有活动上下文时，保留按策略累计抽奖次数判断的兼容行为。</p>
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
     * 抽奖策略仓储端口。
     */
    private final IStrategyRepository strategyRepository;
    private final IAwardUnlockService awardUnlockService;

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(RaffleFactorEntity raffleFactorEntity, String ruleValue) {
        RaffleFactorEntity factor = Objects.requireNonNull(raffleFactorEntity, "raffleFactorEntity must not be null");
        Long strategyId = Objects.requireNonNull(factor.strategyId(), "strategyId must not be null");
        Long awardId = Objects.requireNonNull(factor.awardId(), "awardId must not be null");

        int requiredCount = strategyRepository.queryAwardRuleLockCounts(strategyId, List.of(awardId)).get(awardId);
        AwardUnlockResultVO unlockResult = evaluateUnlock(factor, requiredCount);

        if (!unlockResult.unlocked()) {
            log.info("抽奖策略-次数锁未通过 userId={} activityId={} strategyId={} awardId={} scope={} requiredCount={} completedCount={}",
                    factor.userId(), factor.activityId(), strategyId, awardId, factor.awardUnlockScope(),
                    unlockResult.requiredCount(), unlockResult.completedCount());
            return takeOver();
        }

        log.info("抽奖策略-次数锁通过 userId={} activityId={} strategyId={} awardId={} scope={} requiredCount={} completedCount={}",
                factor.userId(), factor.activityId(), strategyId, awardId, factor.awardUnlockScope(),
                unlockResult.requiredCount(), unlockResult.completedCount());
        return allow(awardId);
    }

    /**
     * 按活动上下文或兼容口径计算奖品解锁结果。
     *
     * @param factor        本次抽奖因子
     * @param requiredCount 奖品要求完成的抽奖次数
     * @return 奖品解锁判定结果
     * @throws BusinessException 活动上下文不完整时抛出
     */
    private AwardUnlockResultVO evaluateUnlock(RaffleFactorEntity factor, int requiredCount) {
        if (factor.hasActivityContext()) {
            AwardUnlockCountQueryVO queryVO = new AwardUnlockCountQueryVO(
                    factor.userId(), factor.activityId(), factor.awardUnlockScope(), factor.statisticsDate());
            return awardUnlockService.evaluateUnlock(queryVO, requiredCount);
        }

        boolean hasPartialContext = factor.activityId() != null || factor.awardUnlockScope() != null || factor.statisticsDate() != null;
        if (hasPartialContext) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, factor.activityId(), factor.awardUnlockScope(), factor.statisticsDate());
        }

        long completedCount = strategyRepository.queryUserRaffleCount(factor.userId(), factor.strategyId());
        return AwardUnlockResultVO.evaluate(requiredCount, completedCount);
    }

    private DefaultTreeFactory.TreeActionEntity takeOver() {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.TAKE_OVER)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder().awardId(null).ruleModel(ruleModel()).build())
                .build();
    }

    private DefaultTreeFactory.TreeActionEntity allow(Long awardId) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleModel ruleModel() {
        return RuleModel.LOCK;
    }
}
