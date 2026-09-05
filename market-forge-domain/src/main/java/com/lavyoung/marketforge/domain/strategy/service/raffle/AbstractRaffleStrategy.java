package com.lavyoung.marketforge.domain.strategy.service.raffle;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 抽奖策略执行模板。
 * <p>
 * 统一完成入参校验、抽奖前规则判断和奖品随机选择，具体的前置规则编排由子类实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    /**
     * 抽奖策略仓储端口。
     */
    protected IStrategyRepository repository;

    /**
     * 已装配策略的随机调度服务。
     */
    protected IStrategyDispatch strategyDispatch;

    /**
     * {@inheritDoc}
     *
     * @param raffleFactorEntity 包含用户和策略标识的抽奖因子
     * @return 规则指定或随机选中的奖品信息
     * @throws BusinessException 用户标识为空或策略标识为空时抛出
     */
    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        // 1. 参数校验
        Long strategyId = raffleFactorEntity.strategyId();
        String userId = raffleFactorEntity.userId();
        if (strategyId == null || StringUtils.isBlank(userId)) {
            throw new BusinessException(CommonResponseCode.PARAM_INVALID);
        }

        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        // 抽奖前 - 规则
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity = this.doCheckRaffleBeforeLogic(
                RaffleFactorEntity.builder().userId(userId).strategyId(strategyId).build(),
                strategyEntity.toRuleModes()
        );
        if (ruleActionEntity != null && RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionEntity.code())) {
            if (RuleModel.RULE_BLACKLIST.getCode().equals(ruleActionEntity.ruleModel())) {
                return RaffleAwardEntity.builder()
                        .awardId(ruleActionEntity.data().awardId())
                        .build();
            }
            if (RuleModel.WEIGHT.getCode().equals(ruleActionEntity.ruleModel())) {
                // 根据返回的权重进行抽奖
                RuleActionEntity.RaffleBeforeEntity beforeEntity = ruleActionEntity.data();
                String ruleWeightValueKey = beforeEntity.ruleWeightValueKey();

                long awardId = strategyDispatch.getRandomAwardIdAndWeight(strategyId, ruleWeightValueKey);
                return RaffleAwardEntity.builder()
                        .awardId(awardId)
                        .build();
            }
        }
        // 执行抽奖 默认
        long awardId = strategyDispatch.getRandomAwardId(raffleFactorEntity.strategyId());
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .build();
    }

    /**
     * 按策略配置执行抽奖前规则。
     * <p>
     * 返回接管流程的首个规则结果；所有规则均放行时返回 {@code null}。
     *
     * @param factorEntity 抽奖因子
     * @param logics       待执行的规则模型列表
     * @return 接管抽奖流程的规则动作；全部放行时返回 {@code null}
     */
    protected abstract RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity factorEntity, List<RuleModel> logics);
}
