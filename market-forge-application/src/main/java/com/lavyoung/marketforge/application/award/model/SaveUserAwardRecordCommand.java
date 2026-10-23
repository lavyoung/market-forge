package com.lavyoung.marketforge.application.award.model;

import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 保存用户中奖记录的应用命令。
 *
 * <p>该命令来自活动抽奖结算流程，用于同时创建中奖记录和可靠消息任务。
 * 履约快照在抽奖命中时已经固定，后续保存和消息投递必须原样传递，
 * 不得重新查询可能发生变化的奖品定义。</p>
 *
 * @param userId             用户标识
 * @param activityId         活动标识
 * @param strategyId         策略标识
 * @param orderId            抽奖订单标识，也是发奖幂等业务号
 * @param awardId            奖品标识
 * @param fulfillmentSnapshot 中奖时确定的奖品履约快照
 * @param awardTitle         命中奖品展示标题
 * @param awardTime          中奖时间
 */
public record SaveUserAwardRecordCommand(
        String userId,
        Long activityId,
        Long strategyId,
        String orderId,
        Long awardId,
        AwardFulfillmentSnapshot fulfillmentSnapshot,
        String awardTitle,
        LocalDateTime awardTime
) {

    /**
     * 校验保存中奖记录所需的完整业务参数。
     *
     * @throws NullPointerException     标识、快照或中奖时间为空时抛出
     * @throws IllegalArgumentException 字符串为空白或业务标识不是正数时抛出
     */
    public SaveUserAwardRecordCommand {
        requireText(userId, "userId");
        requireText(orderId, "orderId");
        requireText(awardTitle, "awardTitle");
        requirePositive(activityId, "activityId");
        requirePositive(strategyId, "strategyId");
        requirePositive(awardId, "awardId");
        Objects.requireNonNull(fulfillmentSnapshot, "fulfillmentSnapshot must not be null");
        Objects.requireNonNull(awardTime, "awardTime must not be null");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
