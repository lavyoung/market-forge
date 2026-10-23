package com.lavyoung.marketforge.application.award.model;

import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;

import java.util.Objects;

/**
 * 发放用户中奖奖品的应用命令。
 *
 * <p>该命令由中奖记录可靠消息转换而来。履约快照保存中奖发生时已经确定的
 * 奖品类型、领取模式、供应商、配置内容和配置版本。发奖应用服务根据
 * {@code fulfillmentSnapshot.awardType()} 选择处理器。</p>
 *
 * <p>奖品业务编码只用于识别具体奖品，不能用于选择发奖处理器。
 * 配置内容只能由对应奖品类型的处理器按照配置版本解析。</p>
 *
 * @param userId              用户标识
 * @param orderId             抽奖订单标识，也是发奖幂等业务号
 * @param awardId             奖品标识
 * @param fulfillmentSnapshot 中奖时确定的不可变履约快照
 * @param awardTitle          奖品标题，用于日志和运营排查
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
public record GrantAwardCommand(
        String userId,
        String orderId,
        Long awardId,
        AwardFulfillmentSnapshot fulfillmentSnapshot,
        String awardTitle
) {

    /**
     * 创建并校验发奖命令。
     *
     * @throws NullPointerException     奖品标识或履约快照为空时抛出
     * @throws IllegalArgumentException 用户、订单、标题为空白或奖品标识非法时抛出
     */
    public GrantAwardCommand {
        requireText(userId, "userId");
        requireText(orderId, "orderId");
        requireText(awardTitle, "awardTitle");
        Objects.requireNonNull(awardId, "awardId must not be null");
        Objects.requireNonNull(fulfillmentSnapshot, "fulfillmentSnapshot must not be null");

        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be positive");
        }
    }

    /**
     * 校验必要文本字段。
     *
     * @param value     待校验内容
     * @param fieldName 字段名称
     * @throws IllegalArgumentException 内容为空或空白时抛出
     */
    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}