package com.lavyoung.marketforge.domain.strategy.service.rule.filter.impl;

import com.lavyoung.marketforge.domain.strategy.annotation.LogicStrategy;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.filter.ILogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl.RuleLuckAwardLogicTreeNode;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 幸运奖兜底规则过滤器。
 * <p>
 * 在抽奖执行阶段比较用户抽奖次数与奖品规则阈值；达到阈值时放行，
 * 未达到阈值时接管当前流程，以便后续使用兜底奖品。
 *
 * @deprecated 幸运奖处理已迁移至规则树，请使用 {@link RuleLuckAwardLogicTreeNode}
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@LogicStrategy(logicModel = RuleModel.LUCK_AWARD)
@Deprecated(since = "1.0.0", forRemoval = false)
public class RuleLuckAwardLogicFilter implements ILogicFilter<RuleActionEntity.RaffleExecutingEntity> {

    /**
     * 查询奖品规则阈值的策略仓储端口。
     */
    private final IStrategyRepository repository;

    /**
     * 当前用户的累计抽奖次数。
     * <p>
     * 该字段目前为临时计数占位，默认从零开始。
     */
    private Long userRaffleCount = 0L;

    /**
     * 根据用户抽奖次数和规则阈值判断是否接管奖品结果。
     *
     * @param ruleMatterEntity 包含用户、策略、奖品和规则模型的校验上下文
     * @return 达到规则阈值时返回放行动作，否则返回接管动作
     * @throws NumberFormatException 当仓储返回的规则值为空或不是有效整数时抛出
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-次数锁 userId={} strategyId={} awardId={} ruleModel={}", ruleMatterEntity.userId(), ruleMatterEntity.strategyId(), ruleMatterEntity.awardId(), ruleMatterEntity.ruleModel());
        String ruleValueStr = repository.queryStrategyRuleValue(ruleMatterEntity.strategyId(), ruleMatterEntity.awardId(), ruleMatterEntity.ruleModel());
        Long ruleValue = Long.parseLong(ruleValueStr);
        if (userRaffleCount >= ruleValue) {
            return RuleActionEntity.<RuleActionEntity.RaffleExecutingEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .msg(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }
        return RuleActionEntity.<RuleActionEntity.RaffleExecutingEntity>builder()
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .msg(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .ruleModel(RuleModel.LUCK_AWARD.getCode())
                .data(RuleActionEntity.RaffleExecutingEntity
                        .builder()
                        .build())
                .build();
    }
}
