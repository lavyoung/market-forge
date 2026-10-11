package com.lavyoung.marketforge.application.activity.model;

/**
 * 用户活动账户额度查询结果。
 *
 * <p>应用层用该对象承接活动账户查询用例结果，并向 trigger 层输出稳定的数据结构。
 * 它不暴露 domain 层账户实体，避免接口层直接依赖领域模型。</p>
 *
 * @param totalCount        累计获得的总抽奖次数
 * @param totalCountSurplus 当前剩余总抽奖次数
 * @param dayCount          当日可用抽奖次数
 * @param dayCountSurplus   当日剩余抽奖次数
 * @param monthCount        当月可用抽奖次数
 * @param monthCountSurplus 当月剩余抽奖次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record ActivityAccountResult(
        Integer totalCount,
        Integer totalCountSurplus,
        Integer dayCount,
        Integer dayCountSurplus,
        Integer monthCount,
        Integer monthCountSurplus
) {

    public static ActivityAccountResult zero() {
        return new ActivityAccountResult(0, 0, 0, 0, 0, 0);
    }

}
