package com.lavyoung.marketforge.domain.strategy.service;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 抽奖策略执行模板。
 * <p>
 * 统一完成入参校验、责任链抽奖和抽奖中规则判断，具体的阶段规则编排由子类实现。
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
     * 根据策略配置装配抽奖前责任链的工厂。
     */
    private DefaultChainFactory defaultChainFactory;

    /**
     * {@inheritDoc}
     *
     * @param raffleFactorEntity 包含用户和策略标识的抽奖因子，不可为 {@code null}
     * @return 规则指定或随机选中的奖品信息
     * @throws BusinessException    用户标识为空或策略标识为空时抛出
     * @throws NullPointerException 抽奖因子为 {@code null} 时抛出
     */
    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        // 1. 参数校验
        Long strategyId = raffleFactorEntity.strategyId();
        String userId = raffleFactorEntity.userId();
        if (strategyId == null || StringUtils.isBlank(userId)) {
            throw new BusinessException(CommonResponseCode.PARAM_INVALID);
        }

        // 责任链抽奖模式
        ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
        Long awardId = logicChain.logic(userId, strategyId);

        // 抽奖中处理
        RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> executingEntityRuleActionEntity = this.doCheckRaffleExecutingLogic(
                RaffleFactorEntity.builder().strategyId(strategyId).userId(userId).awardId(awardId).build(),
                Optional.ofNullable(repository.queryStrategyAwardRuleModels(strategyId, awardId)).map(StrategyAwardRuleModelVO::raffleExecutingRuleModelsList).orElse(null));
        // 规则结果处理 使用兜底
        if (executingEntityRuleActionEntity.code().equals(RuleLogicCheckTypeVO.TAKE_OVER.getCode())) {
            // 返回null 直接获取兜底奖励返回即可
            return RaffleAwardEntity.builder().awardId(null).build();
        }

        return RaffleAwardEntity.builder().awardId(awardId).build();
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

    /**
     * 按奖品配置执行抽奖中规则。
     * <p>
     * 规则可根据已随机命中的奖品决定放行、接管或改写后续抽奖结果。
     *
     * @param factorEntity 包含用户、策略及已命中奖品标识的抽奖因子
     * @param logics       待执行的抽奖中规则模型列表；可为空
     * @return 抽奖中规则动作；具体的空值语义由实现类约定
     */
    protected abstract RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> doCheckRaffleExecutingLogic(RaffleFactorEntity factorEntity, List<RuleModel> logics);

    /**
     * 按奖品配置执行抽奖后规则。
     * <p>
     * 用于在奖品结果确定后执行需要补充处理的规则链。
     *
     * @param factorEntity 包含用户、策略及奖品标识的抽奖因子
     * @param logics       待执行的抽奖后规则模型列表；可为空
     * @return 抽奖后规则动作；具体的空值语义由实现类约定
     */
    protected abstract RuleActionEntity<RuleActionEntity.RaffleAfterEntity> doCheckRaffleAfterLogic(RaffleFactorEntity factorEntity, List<RuleModel> logics);
}
