package com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;

/**
 * 规则树执行引擎契约。
 *
 * <p>负责从根节点开始执行规则树，并将完整抽奖上下文传递给每个节点，
 * 直到规则树抵达没有后继连线的叶子节点。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface IDecisionTreeEngine {

    /**
     * 执行本次抽奖对应的规则树。
     *
     * @param raffleFactorEntity 包含用户、活动、策略、候选奖品和解锁范围的抽奖因子
     * @return 规则树最终确定的奖品和命中规则信息
     * @throws NullPointerException 抽奖因子为空时抛出
     * @throws BusinessException    规则树配置不完整、节点未注册或节点流转失败时抛出
     */
    DefaultTreeFactory.StrategyAwardVO process(RaffleFactorEntity raffleFactorEntity);
}
