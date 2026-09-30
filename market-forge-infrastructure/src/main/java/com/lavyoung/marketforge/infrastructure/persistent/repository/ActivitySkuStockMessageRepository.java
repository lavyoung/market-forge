package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuStockMessageRepository;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * 活动 SKU 库存消息仓储实现。
 * <p>
 * 优先发布活动 SKU 库存扣减 MQ 事件；发布失败时写入 Redis 延迟队列，由调度补偿流程继续处理。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ActivitySkuStockMessageRepository implements IActivitySkuStockMessageRepository {

    private static final Duration ACTIVITY_SKU_STOCK_QUEUE_DELAY = Duration.ofSeconds(3);

    private final IRedisService redisService;
    private final MessagePublisher messagePublisher;

    @Override
    public void send(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
        try {
            messagePublisher.publish(new ActivitySkuStockDeductedEvent(
                    activitySkuStockKeyVO.sku(),
                    activitySkuStockKeyVO.activityId()
            ));
        } catch (RuntimeException e) {
            redisService.offerDelayed(
                    Constants.RedisKeys.ACTIVITY_SKU_STOCK_QUEUE,
                    activitySkuStockKeyVO,
                    ACTIVITY_SKU_STOCK_QUEUE_DELAY
            );
        }
    }

    @Override
    public Optional<ActivitySkuStockKeyVO> poll() {
        return redisService.pollDelayed(
                Constants.RedisKeys.ACTIVITY_SKU_STOCK_QUEUE,
                ActivitySkuStockKeyVO.class
        );
    }
}
