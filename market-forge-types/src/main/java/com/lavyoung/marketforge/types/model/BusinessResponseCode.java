package com.lavyoung.marketforge.types.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 业务编码
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/02
 */
@Getter
@AllArgsConstructor
public enum BusinessResponseCode implements IResponseCode {

    /*
     * 编码结构：AAA_BBB_CCC
     * AAA：业务域；BBB：业务场景；CCC：具体状态。
     * 公共域编码在展示为九位编码时需要在左侧补零，源码中不使用前导零以避免被 Java 解析为八进制。
     */

    // 100 - 策略域
    STRATEGY_NOT_FOUND(100_001_001, "strategy.not-found", "抽奖策略不存在"),
    STRATEGY_AWARD_NOT_CONFIGURED(100_001_002, "strategy.award.not-configured", "抽奖策略未配置奖品"),
    STRATEGY_RATE_INVALID(100_001_003, "strategy.rate.invalid", "抽奖策略概率配置无效"),
    STRATEGY_RATE_TOTAL_INVALID(100_001_004, "strategy.rate.total-invalid", "抽奖策略概率总和无效"),
    STRATEGY_NOT_ASSEMBLED(100_002_001, "strategy.not-assembled", "抽奖策略尚未装配"),
    STRATEGY_ASSEMBLY_FAILED(100_002_002, "strategy.assembly.failed", "抽奖策略装配失败"),
    STRATEGY_RULE_NOT_FOUND(100_003_001, "strategy.rule.not-found", "抽奖策略规则不存在"),
    STRATEGY_RULE_VALUE_INVALID(100_003_002, "strategy.rule.value-invalid", "抽奖策略规则值格式无效"),
    STRATEGY_WEIGHT_NOT_MATCHED(100_003_003, "strategy.weight.not-matched", "未匹配到对应的积分权重范围"),

    // 200 - 奖品域
    AWARD_NOT_FOUND(200_001_001, "award.not-found", "奖品不存在"),
    AWARD_NOT_AVAILABLE(200_001_002, "award.not-available", "奖品当前不可用"),
    AWARD_OUT_OF_STOCK(200_002_001, "award.out-of-stock", "奖品库存不足"),
    AWARD_STOCK_DEDUCTION_FAILED(200_002_002, "award.stock.deduction-failed", "奖品库存扣减失败"),
    AWARD_GRANT_FAILED(200_003_001, "award.grant.failed", "奖品发放失败"),
    AWARD_GRANT_DUPLICATED(200_003_002, "award.grant.duplicated", "奖品已发放，请勿重复领取"),

    // 300 - 抽奖域
    LOTTERY_ACTIVITY_NOT_FOUND(300_001_001, "lottery.activity.not-found", "抽奖活动不存在"),
    LOTTERY_ACTIVITY_NOT_STARTED(300_001_002, "lottery.activity.not-started", "抽奖活动尚未开始"),
    LOTTERY_ACTIVITY_ENDED(300_001_003, "lottery.activity.ended", "抽奖活动已结束"),
    LOTTERY_ACTIVITY_CLOSED(300_001_004, "lottery.activity.closed", "抽奖活动已关闭"),
    LOTTERY_CHANCE_NOT_ENOUGH(300_002_001, "lottery.chance.not-enough", "抽奖次数不足"),
    LOTTERY_DRAW_DUPLICATED(300_002_002, "lottery.draw.duplicated", "抽奖请求正在处理中"),
    LOTTERY_DRAW_FAILED(300_002_003, "lottery.draw.failed", "抽奖失败，请稍后重试"),
    LOTTERY_AWARD_LOCKED(300_003_001, "lottery.award.locked", "奖品尚未解锁"),

    // 400 - 积分账户域
    POINTS_ACCOUNT_NOT_FOUND(400_001_001, "points.account.not-found", "积分账户不存在"),
    POINTS_BALANCE_NOT_ENOUGH(400_002_001, "points.balance.not-enough", "积分余额不足"),
    POINTS_DEDUCTION_FAILED(400_002_002, "points.deduction.failed", "积分扣减失败"),
    POINTS_TRANSACTION_DUPLICATED(400_003_001, "points.transaction.duplicated", "积分流水已处理");


    /**
     * 业务编码
     */
    private final int code;

    /**
     * 国际化key
     */
    private final String i18nKey;

    /**
     * 默认信息
     */
    private final String msg;

    /**
     * 根据业务编码获取对应的枚举定义。
     *
     * @param code 业务编码
     * @return 匹配的业务响应编码；不存在时返回空
     */
    public static Optional<BusinessResponseCode> fromCode(int code) {
        return Arrays.stream(values())
                .filter(responseCode -> responseCode.code == code)
                .findFirst();
    }
}
