package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.AbstractRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 默认抽奖策略实现。
 * <p>
 * 使用责任链完成前置规则判断与默认奖品选择，并对默认奖品继续执行策略配置的规则树。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/05
 */
@Slf4j
@Component
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {

    /**
     * 创建默认抽奖策略。
     *
     * @param repository          抽奖策略仓储端口
     * @param ruleTreeRepository  规则树仓储端口
     * @param strategyDispatch    已装配策略的随机抽奖调度服务
     * @param defaultChainFactory 抽奖责任链工厂
     * @param defaultTreeFactory  规则树决策引擎工厂
     */
    public DefaultRaffleStrategy(IStrategyRepository repository, IRuleTreeRepository ruleTreeRepository,
                                 IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory,
                                 DefaultTreeFactory defaultTreeFactory) {
        super(repository, ruleTreeRepository, strategyDispatch, defaultChainFactory, defaultTreeFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
        ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
        return logicChain.logic(userId, strategyId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 奖品未配置执行阶段规则时保留原奖品；规则模型无法组装成规则树时终止抽奖。
     *
     * @throws BusinessException 规则树配置缺失、无法完成组装时抛出
     */
    @Override
    protected DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Long awardId) {
        StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModels(strategyId, awardId);
        // 组装规则树
        if (strategyAwardRuleModelVO == null) {
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }
        RuleTreeVO ruleTreeVO = ruleTreeRepository.queryRuleTreeVOByTreeId(strategyAwardRuleModelVO.toModelList())
                .orElseThrow(() -> {
                    log.error("存在抽奖策略 strategyId={} 配置的规则模型Key={} 未在库表中配置完整规则树", strategyId,
                            strategyAwardRuleModelVO.ruleModels());
                    return new BusinessException(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED);
                });
        return defaultTreeFactory.openLogicTree(ruleTreeVO).process(userId, strategyId, awardId);
    }

    @Override
    public Optional<StrategyAwardStockKeyVO> pollQueueValue() {
        return repository.pollQueueValue();
    }

    @Override
    public void requeueStockUpdate(StrategyAwardStockKeyVO message) {
        repository.awardStockConsumeSendQueue(message);
    }

    @Override
    public boolean updateStrategyAwardStock(Long strategyId, Long awardId) {
        return repository.updateStrategyAwardStock(strategyId, awardId);
    }
}
