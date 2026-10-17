package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyAwardStockMessageRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.AbstractRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStock;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
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
public class DefaultRaffleStrategy extends AbstractRaffleStrategy implements IRaffleStock, IRaffleAward {

    public DefaultRaffleStrategy(IStrategyRepository repository, IStrategyAwardStockMessageRepository strategyAwardStockMessageRepository, IRuleTreeRepository ruleTreeRepository, IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory, DefaultTreeFactory defaultTreeFactory) {
        super(repository, strategyAwardStockMessageRepository, ruleTreeRepository, strategyDispatch, defaultChainFactory, defaultTreeFactory);
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
     *
     * <p>奖品没有配置执行阶段规则时直接保留候选奖品；
     * 存在规则配置时组装规则树，并将完整抽奖因子交给执行引擎。</p>
     *
     * @throws BusinessException 规则树配置缺失或无法完成组装时抛出
     */
    @Override
    protected DefaultTreeFactory.StrategyAwardVO raffleLogicTree(RaffleFactorEntity raffleFactorEntity) {
        Long strategyId = raffleFactorEntity.strategyId();
        Long awardId = raffleFactorEntity.awardId();

        StrategyAwardRuleModelVO ruleModels = repository.queryStrategyAwardRuleModels(strategyId, awardId);
        if (ruleModels == null) {
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }

        RuleTreeVO ruleTreeVO = ruleTreeRepository.queryRuleTreeVOByTreeId(ruleModels.toModelList())
                .orElseThrow(() -> {
                    log.error("抽奖策略规则树配置不完整 strategyId={} awardId={} ruleModels={}",
                            strategyId, awardId, ruleModels.ruleModels());
                    return BusinessException.of(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, strategyId, awardId);
                });

        return defaultTreeFactory.openLogicTree(ruleTreeVO).process(raffleFactorEntity);
    }

    @Override
    public Optional<StrategyAwardStockKeyVO> pollQueueValue() {
        return strategyAwardStockMessageRepository.poll();
    }

    @Override
    public void requeueStockUpdate(StrategyAwardStockKeyVO message) {
        strategyAwardStockMessageRepository.send(message);
    }

    @Override
    public boolean updateStrategyAwardStock(Long strategyId, Long awardId) {
        return repository.updateStrategyAwardStock(strategyId, awardId);
    }

    @Override
    public List<StrategyAwardEntity> queryRaffleStrategyAwardList(Long strategyId) {
        return repository.queryStrategyAwardList(strategyId);
    }
}
