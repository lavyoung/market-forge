package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;

/**
 * 活动 SKU 库存服务端口。
 * <p>
 * 负责活动 SKU 库存消息的队列消费、延迟补偿以及数据库库存同步。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IRaffleActivitySkuStockService {

    /**
     * 获取一条待处理的活动 SKU 库存消息。
     *
     * @return 活动 SKU 库存消息键
     * @throws Exception 当队列读取失败时抛出
     */
    ActivitySkuStockKeyVO takeQueueValue() throws Exception;

    /**
     * 清空待处理库存消息队列。
     */
    void clearQueueValue();

    /**
     * 根据延迟队列消息更新活动 SKU 数据库库存。
     *
     * @param sku 商品 SKU
     */
    void updateActivitySkuStock(Long sku);

    /**
     * 缓存库存耗尽后清空数据库侧活动 SKU 库存。
     *
     * @param sku 商品 SKU
     */
    void clearActivitySkuStock(Long sku);
}
