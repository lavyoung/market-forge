package com.lavyoung.marketforge.domain.credit.model.entity;

/**
 * 用户积分账户领域实体。
 * <p>
 * 该实体表示用户当前可用积分余额，是积分发奖、积分扣减和积分查询的核心账户对象。
 * 积分账户只保存当前余额快照，具体每一次积分来源和变化原因由积分账户订单记录。
 *
 * @param userId          用户标识
 * @param totalAmount     累计获得积分总额
 * @param availableAmount 当前可用积分余额
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record CreditAccountEntity(
        String userId,
        Long totalAmount,
        Long availableAmount
) {
}
