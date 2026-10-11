package com.lavyoung.marketforge.application.award.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 保存用户中奖记录的应用命令。
 * <p>
 * 该命令来自活动抽奖结算流程，用于同时创建用户中奖记录和后续发奖消息。
 * 中奖记录表只保存中奖事实，{@code awardKey} 和 {@code awardConfig} 会进入发奖事件，
 * 作为消费者选择发奖策略和解析发奖参数的依据。
 *
 * @param userId      用户标识
 * @param activityId  活动标识
 * @param strategyId  策略标识
 * @param orderId     抽奖订单标识，作为中奖记录和发奖流程的幂等业务号
 * @param awardId     奖品标识
 * @param awardKey    奖品业务键，用于区分积分、实物、优惠券等发奖类型
 * @param awardConfig 奖品发放配置，例如积分数量、随机积分范围或实物 SKU
 * @param awardTitle  奖品标题
 * @param awardTime   中奖时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record SaveUserAwardRecordCommand(
        String userId,
        Long activityId,
        Long strategyId,
        String orderId,
        Long awardId,
        String awardKey,
        String awardConfig,
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
        requireText(awardKey, "awardKey");
        requireText(awardConfig, "awardConfig");
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
