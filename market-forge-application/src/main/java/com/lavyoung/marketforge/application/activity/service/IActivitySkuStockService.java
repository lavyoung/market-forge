package com.lavyoung.marketforge.application.activity.service;

/**
 * 活动 SKU 库存异步同步用例入口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IActivitySkuStockService {

    /**
     * 同步一条当前已到期的活动 SKU 库存扣减消息。
     *
     * @throws RuntimeException 当队列读取或库存同步失败时抛出
     */
    void synchronizePendingStock();
}
