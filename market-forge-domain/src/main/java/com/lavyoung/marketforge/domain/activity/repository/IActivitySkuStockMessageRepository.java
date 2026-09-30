package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;

import java.util.Optional;

/**
 * 活动 SKU 库存消息端口。
 * <p>
 * 屏蔽 MQ 发布和 Redis 延迟补偿队列细节，为领域服务提供库存同步消息投递与补偿消息读取能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
public interface IActivitySkuStockMessageRepository {

    /**
     * 投递活动 SKU 库存扣减消息。
     *
     * @param activitySkuStockKeyVO 活动 SKU 库存扣减消息
     * @throws NullPointerException 当活动 SKU 库存扣减消息为空时抛出
     */
    void send(ActivitySkuStockKeyVO activitySkuStockKeyVO);

    /**
     * 获取一条待补偿的活动 SKU 库存扣减消息。
     *
     * @return 待补偿消息；队列为空时返回空
     */
    Optional<ActivitySkuStockKeyVO> poll();
}
