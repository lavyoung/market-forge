package com.lavyoung.marketforge.domain.award.service;

/**
 * 用户奖品状态领域服务。
 * <p>
 * 封装中奖记录状态机的业务规则，不负责事务、消息发布或接口协议转换。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IAwardStateService {

    /**
     * 将中奖记录从创建状态幂等推进到待领取状态。
     *
     * @param userId  用户标识
     * @param orderId 抽奖订单标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 中奖记录不存在或状态推进失败时抛出
     */
    void prepareClaim(String userId, String orderId);
}
