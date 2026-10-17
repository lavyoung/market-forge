package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyAwardStockMessageRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 奖品库存规则树节点。
 * <p>
 * 校验命中奖品的库存规则，并返回是否接管后续规则树流程的判断结果。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component("rule_stock")
@RequiredArgsConstructor
public class RuleStockLogicTreeNode implements ILogicTreeNode {

    /**
     * 抽奖策略仓储端口，用于投递数据库库存同步消息。
     */
    private final IStrategyAwardStockMessageRepository repository;

    /**
     * 策略库存调度端口。
     */
    private final IStrategyDispatch strategyDispatch;

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(RaffleFactorEntity raffleFactorEntity, String ruleValue) {
        RaffleFactorEntity factor = Objects.requireNonNull(raffleFactorEntity, "raffleFactorEntity must not be null");
        boolean deducted = strategyDispatch.subtractAwardStock(factor.strategyId(), factor.awardId(), factor.endDateTime());

        if (!deducted) {
            return DefaultTreeFactory.TreeActionEntity.builder()
                    .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder().awardId(null).ruleModel(ruleModel()).build())
                    .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.TAKE_OVER)
                    .build();
        }

        repository.send(StrategyAwardStockKeyVO.builder()
                .awardId(factor.awardId()).strategyId(factor.strategyId()).userId(factor.userId()).build());

        return DefaultTreeFactory.TreeActionEntity.builder()
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder().awardId(factor.awardId()).build())
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public RuleModel ruleModel() {
        return RuleModel.RULE_STOCK;
    }
}
