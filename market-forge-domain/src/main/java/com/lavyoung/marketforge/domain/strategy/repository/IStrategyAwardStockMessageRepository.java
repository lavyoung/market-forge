package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;

import java.util.Optional;

/**
 * 策略奖品库存消息端口。
 * <p>
 * 屏蔽 MQ 发布和 Redis 延迟补偿队列细节，为策略领域服务提供奖品库存同步消息投递与补偿消息读取能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
public interface IStrategyAwardStockMessageRepository {

    /**
     * 投递策略奖品库存扣减消息。
     *
     * @param awardStockKeyVO 策略奖品库存扣减消息
     * @throws NullPointerException 当策略奖品库存扣减消息为空时抛出
     */
    void send(StrategyAwardStockKeyVO awardStockKeyVO);

    /**
     * 获取一条待补偿的策略奖品库存扣减消息。
     *
     * @return 待补偿消息；队列为空时返回空
     */
    Optional<StrategyAwardStockKeyVO> poll();
}
