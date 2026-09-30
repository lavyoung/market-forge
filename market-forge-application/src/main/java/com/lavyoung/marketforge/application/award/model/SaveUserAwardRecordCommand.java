package com.lavyoung.marketforge.application.award.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 保存用户中奖记录的应用命令。
 *
 * @param userId     用户标识
 * @param activityId 活动标识
 * @param strategyId 策略标识
 * @param orderId    抽奖订单标识
 * @param awardId    奖品标识
 * @param awardTitle 奖品标题
 * @param awardTime  中奖时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record SaveUserAwardRecordCommand(
        String userId,
        Long activityId,
        Long strategyId,
        String orderId,
        Long awardId,
        String awardTitle,
        LocalDateTime awardTime
) {

    /**
     * 校验保存中奖记录用例所需的基本参数。
     *
     * @throws IllegalArgumentException 字符串参数为空白或标识不是正数时抛出
     * @throws NullPointerException     必填参数为空时抛出
     */
    public SaveUserAwardRecordCommand {
        requireText(userId, "userId");
        requireText(orderId, "orderId");
        requireText(awardTitle, "awardTitle");
        requirePositive(activityId, "activityId");
        requirePositive(strategyId, "strategyId");
        requirePositive(awardId, "awardId");
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
