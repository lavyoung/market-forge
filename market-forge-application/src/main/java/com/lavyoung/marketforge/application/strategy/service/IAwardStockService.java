package com.lavyoung.marketforge.application.strategy.service;

/**
 * 奖品库存异步同步用例入口。
 */
public interface IAwardStockService {

    /**
     * 批量同步当前已到期的库存扣减消息。
     */
    void synchronizePendingStock();
}
