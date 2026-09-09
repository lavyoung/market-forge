package com.lavyoung.marketforge.domain.strategy.service;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;

import java.util.Optional;

/**
 * 奖品库存接口
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/08
 */
public interface IRaffleStock {

    /**
     * 立即获取一条已经到期的库存扣减消息。
     *
     * @return 已到期消息；当前队列为空时返回空
     */
    Optional<StrategyAwardStockKeyVO> pollQueueValue();

    /**
     * 将处理失败的库存消息重新延迟投递。
     *
     * @param message 待重试的库存消息
     * @throws NullPointerException 消息为空
     */
    void requeueStockUpdate(StrategyAwardStockKeyVO message);

    /**
     * 将已消费的库存变化同步到持久化存储。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     */
    boolean updateStrategyAwardStock(Long strategyId, Long awardId);
}
