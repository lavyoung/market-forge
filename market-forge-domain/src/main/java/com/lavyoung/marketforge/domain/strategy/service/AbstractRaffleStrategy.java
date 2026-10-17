package com.lavyoung.marketforge.domain.strategy.service;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyAwardStockMessageRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 抽奖策略执行模板。
 * <p>
 * 统一完成入参校验、责任链抽奖和规则树决策，具体的责任链与规则树编排由子类实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    protected IStrategyRepository repository;
    protected IStrategyAwardStockMessageRepository strategyAwardStockMessageRepository;
    protected IRuleTreeRepository ruleTreeRepository;

    protected IStrategyDispatch strategyDispatch;

    protected DefaultChainFactory defaultChainFactory;
    protected DefaultTreeFactory defaultTreeFactory;

    /**
     * {@inheritDoc}
     *
     * @param raffleFactorEntity 本次抽奖所需的完整领域上下文
     * @return 责任链和规则树最终确定的奖品
     * @throws BusinessException    用户标识为空或策略标识为空时抛出
     * @throws NullPointerException 抽奖因子为空时抛出
     */
    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        RaffleFactorEntity factor = Objects.requireNonNull(raffleFactorEntity, "raffleFactorEntity must not be null");
        Long strategyId = factor.strategyId();
        String userId = factor.userId();

        if (strategyId == null || StringUtils.isBlank(userId)) {
            throw new BusinessException(CommonResponseCode.PARAM_INVALID);
        }

        DefaultChainFactory.StrategyAwardVO chainResult = raffleLogicChain(userId, strategyId);
        log.info("抽奖策略计算-责任链 userId={} strategyId={} awardId={} ruleModel={}",
                userId, strategyId, chainResult.awardId(), chainResult.ruleModel());

        if (!Objects.equals(RuleModel.DEFAULT, chainResult.ruleModel())) {
            return RaffleAwardEntity.builder()
                    .strategyId(strategyId)
                    .awardId(chainResult.awardId())
                    .build();
        }

        RaffleFactorEntity treeFactor = factor.withAwardId(chainResult.awardId());
        DefaultTreeFactory.StrategyAwardVO treeResult = raffleLogicTree(treeFactor);

        log.info("抽奖策略计算-规则树 userId={} activityId={} strategyId={} awardId={} ruleModel={} ruleValue={}",
                userId, factor.activityId(), strategyId, treeResult.awardId(),
                treeResult.ruleModel(), treeResult.awardRuleValue());

        return RaffleAwardEntity.builder()
                .strategyId(strategyId)
                .awardId(treeResult.awardId())
                .awardConfig(treeResult.awardRuleValue())
                .build();
    }

    /**
     * 执行策略的抽奖责任链。
     *
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @return 责任链命中的奖品及规则模型
     * @throws BusinessException 策略不存在或责任链配置不可用时抛出
     */
    protected abstract DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId);

    /**
     * 对责任链随机命中的奖品执行规则树决策。
     *
     * <p>抽奖因子中的 {@code awardId} 已由责任链写入，同时保留活动标识、
     * 解锁范围、统计日期和活动结束时间，供不同规则节点使用。</p>
     *
     * @param raffleFactorEntity 携带候选奖品和完整活动上下文的抽奖因子
     * @return 规则树最终确定的奖品、命中规则及奖品规则配置
     * @throws BusinessException 规则树缺失或节点流转配置无效时抛出
     */
    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(RaffleFactorEntity raffleFactorEntity);
}
