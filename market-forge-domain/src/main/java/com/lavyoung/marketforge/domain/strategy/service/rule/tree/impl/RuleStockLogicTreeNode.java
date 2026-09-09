package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private final IStrategyRepository repository;

    /**
     * 策略库存调度端口。
     */
    private final IStrategyDispatch strategyDispatch;

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId, String ruleValue) {
        boolean status = strategyDispatch.subtractAwardStock(strategyId, awardId);
        if (status) {
            log.info("抽奖策略-规则树，获得奖，库存扣减 userId={} strategyId={} ruleModel={} ruleValue={}", userId, strategyId, ruleModel(), ruleValue);
            repository.awardStockConsumeSendQueue(StrategyAwardStockKeyVO.builder()
                    .awardId(awardId)
                    .strategyId(strategyId)
                    .build()
            );
            return DefaultTreeFactory.TreeActionEntity.builder()
                    .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                            .awardId(awardId)
                            .build())
                    .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                    .build();
        }
        return DefaultTreeFactory.TreeActionEntity
                .builder()
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(null)
                        .ruleModel(ruleModel())
                        .build())
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.TAKE_OVER)
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
