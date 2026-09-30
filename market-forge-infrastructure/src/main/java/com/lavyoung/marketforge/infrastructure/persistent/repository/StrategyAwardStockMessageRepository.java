package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyAwardStockMessageRepository;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * 策略奖品库存消息仓储实现。
 * <p>
 * 优先发布策略奖品库存扣减 MQ 事件；发布失败时写入 Redis 延迟队列，由调度补偿流程继续处理。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class StrategyAwardStockMessageRepository implements IStrategyAwardStockMessageRepository {

    private static final Duration AWARD_STOCK_QUEUE_DELAY = Duration.ofSeconds(3);

    private final IRedisService redisService;
    private final MessagePublisher messagePublisher;

    @Override
    public void send(StrategyAwardStockKeyVO awardStockKeyVO) {
        try {
            messagePublisher.publish(new AwardStockDeductedEvent(
                    awardStockKeyVO.strategyId(),
                    awardStockKeyVO.awardId(),
                    awardStockKeyVO.userId()
            ));
        } catch (RuntimeException e) {
            redisService.offerDelayed(
                    Constants.RedisKeys.STRATEGY_AWARD_STOCK_QUEUE,
                    awardStockKeyVO,
                    AWARD_STOCK_QUEUE_DELAY
            );
        }
    }

    @Override
    public Optional<StrategyAwardStockKeyVO> poll() {
        return redisService.pollDelayed(
                Constants.RedisKeys.STRATEGY_AWARD_STOCK_QUEUE,
                StrategyAwardStockKeyVO.class
        );
    }
}
