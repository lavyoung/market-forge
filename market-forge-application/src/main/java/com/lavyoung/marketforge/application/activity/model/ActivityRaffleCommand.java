package com.lavyoung.marketforge.application.activity.model;

import java.util.Objects;

/**
 * 活动抽奖应用命令。
 *
 * @param userId     参与抽奖的用户标识
 * @param activityId 抽奖活动标识
 * @param sku        活动 SKU
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record ActivityRaffleCommand(
        String userId,
        Long activityId,
        Long sku
) {

    /**
     * 校验活动抽奖用例所需的基本参数。
     *
     * @throws IllegalArgumentException 字符串参数为空白或标识不是正数时抛出
     * @throws NullPointerException     必填参数为空时抛出
     */
    public ActivityRaffleCommand {
        requireText(userId, "userId");
        requirePositive(activityId, "activityId");
        requirePositive(sku, "sku");
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
