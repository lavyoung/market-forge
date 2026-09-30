package com.lavyoung.marketforge.application.activity.model;

/**
 * 活动抽奖应用结果。
 *
 * @param orderId     抽奖参与订单号
 * @param activityId  抽奖活动标识
 * @param strategyId  活动关联的抽奖策略标识
 * @param awardId     命中奖品标识
 * @param awardKey    奖品业务标识
 * @param awardConfig 奖品发放配置
 * @param awardTitle  奖品标题
 * @param awardDesc   奖品说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record ActivityRaffleResult(
        String orderId,
        Long activityId,
        Long strategyId,
        Long awardId,
        String awardKey,
        String awardConfig,
        String awardTitle,
        String awardDesc
) {
}
