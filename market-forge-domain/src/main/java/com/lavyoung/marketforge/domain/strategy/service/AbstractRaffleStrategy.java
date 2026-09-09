package com.lavyoung.marketforge.domain.strategy.service;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
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

    /**
     * 抽奖策略仓储端口。
     */
    protected IStrategyRepository repository;

    /**
     * 查询并组装规则树的仓储端口。
     */
    protected IRuleTreeRepository ruleTreeRepository;

    /**
     * 已装配策略的随机抽奖调度服务。
     */
    protected IStrategyDispatch strategyDispatch;

    /**
     * 根据策略配置装配抽奖前责任链的工厂。
     */
    protected DefaultChainFactory defaultChainFactory;

    /**
     * 根据规则树配置创建决策引擎的工厂。
     */
    protected DefaultTreeFactory defaultTreeFactory;

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

        // 2. 责任链抽奖模式 - 黑名单 - 权重 - 兜底
        DefaultChainFactory.StrategyAwardVO chainStrategyAwardVO = this.raffleLogicChain(userId, strategyId);
        log.info("抽奖策略计算-责任链 userId={} strategyId={} awardId={} ruleModel={}", userId, strategyId, chainStrategyAwardVO.awardId(), chainStrategyAwardVO.ruleModel());
        // 没到默认的策略 说明其他策略捕获处理 直接返回结果
        if (!Objects.equals(RuleModel.DEFAULT, chainStrategyAwardVO.ruleModel())) {
            return RaffleAwardEntity.builder()
                    .awardId(chainStrategyAwardVO.awardId())
                    .build();
        }
        // 3. 默认兜底处理  继续执行-类似构建“解锁放行—库存接管—幸运奖兜底”的规则树
        DefaultTreeFactory.StrategyAwardVO treeStrategyAwardVO = this.raffleLogicTree(userId, strategyId, chainStrategyAwardVO.awardId());
        log.info("抽奖策略计算-规则树 userId={} strategyId={} awardId={} ruleModel={} ruleValue={}", userId, strategyId, treeStrategyAwardVO.awardId(),
                treeStrategyAwardVO.ruleModel(), treeStrategyAwardVO.awardRuleValue());
        return RaffleAwardEntity.builder()
                .awardId(treeStrategyAwardVO.awardId())
                .awardConfig(treeStrategyAwardVO.awardRuleValue())
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
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @param awardId    责任链随机命中的奖品标识
     * @return 规则树最终确定的奖品、命中规则及奖品规则配置
     * @throws BusinessException 规则树缺失或节点流转配置无效时抛出
     */
    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Long awardId);

}
