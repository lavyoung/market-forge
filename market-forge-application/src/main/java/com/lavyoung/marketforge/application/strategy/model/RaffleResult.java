package com.lavyoung.marketforge.application.strategy.model;

import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;

import java.util.Objects;

/**
 * 抽奖应用用例的执行结果。
 *
 * <p>抽奖结果除了提供前端展示所需的奖品标题和说明，还携带中奖发生时确定的
 * 奖品履约快照。后续结算流程必须使用该快照创建可靠消息，不得在消息消费时
 * 重新查询可能已经变化的奖品定义。</p>
 *
 * @param strategyId          抽奖策略标识
 * @param awardId             命中奖品标识
 * @param fulfillmentSnapshot 奖品履约快照
 * @param awardTitle          当前策略配置的奖品展示标题
 * @param awardDesc           奖品说明；可以为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record RaffleResult(
        Long strategyId,
        Long awardId,
        AwardFulfillmentSnapshot fulfillmentSnapshot,
        String awardTitle,
        String awardDesc
) {

    /**
     * 创建并校验抽奖应用结果。
     *
     * @throws NullPointerException     标识或履约快照为空时抛出
     * @throws IllegalArgumentException 标识非法或奖品标题为空白时抛出
     */
    public RaffleResult {
        requirePositive(strategyId, "strategyId");
        requirePositive(awardId, "awardId");
        Objects.requireNonNull(fulfillmentSnapshot, "fulfillmentSnapshot must not be null");

        if (awardTitle == null || awardTitle.isBlank()) {
            throw new IllegalArgumentException("awardTitle must not be blank");
        }
    }

    /**
     * 兼容现有抽奖响应中的奖品业务编码。
     *
     * <p>该方法只用于迁移期间兼容现有调用方。奖品业务编码不再用于选择发奖处理器。</p>
     *
     * @return 奖品业务编码
     */
    @Deprecated(forRemoval = true)
    public String awardKey() {
        return fulfillmentSnapshot.awardCode();
    }

    /**
     * 兼容现有抽奖响应中的奖品配置字段。
     *
     * <p>履约配置不应长期暴露给客户端，后续调整 API 响应时将删除该方法。</p>
     *
     * @return 奖品履约配置 JSON
     */
    @Deprecated(forRemoval = true)
    public String awardConfig() {
        return fulfillmentSnapshot.configContent();
    }

    private static void requirePositive(Long value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}