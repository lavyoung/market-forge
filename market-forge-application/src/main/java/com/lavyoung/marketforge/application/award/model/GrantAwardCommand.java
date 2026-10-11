package com.lavyoung.marketforge.application.award.model;

import java.util.Objects;

/**
 * 发放用户中奖奖品的应用命令。
 * <p>
 * 该命令来自用户中奖记录创建消息。应用层会根据奖品业务键选择具体发奖通道，
 * 当前章节先支持积分奖品发放。
 *
 * @param userId      用户标识
 * @param orderId     抽奖订单标识，同时作为发奖幂等业务号
 * @param awardId     奖品标识
 * @param awardKey    奖品业务键，用于选择具体发奖策略
 * @param awardConfig 奖品发放配置，例如积分数量
 * @param awardTitle  奖品标题，用于日志和排查
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record GrantAwardCommand(
        String userId,
        String orderId,
        Long awardId,
        String awardKey,
        String awardConfig,
        String awardTitle
) {

    /**
     * 校验发奖命令的必要参数。
     *
     * @throws IllegalArgumentException 字符串参数为空白或奖品标识非法时抛出
     * @throws NullPointerException     必填对象为空时抛出
     */
    public GrantAwardCommand {
        requireText(userId, "userId");
        requireText(orderId, "orderId");
        requireText(awardKey, "awardKey");
        requireText(awardConfig, "awardConfig");
        requireText(awardTitle, "awardTitle");
        Objects.requireNonNull(awardId, "awardId must not be null");
        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
