package com.lavyoung.marketforge.trigger.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动接口保护场景。
 *
 * <p>每个场景对应独立的 Resilience4j 实例名称，使抽奖和查询接口能够配置
 * 不同的超时时间、统计窗口与熔断阈值，避免不同业务流量相互影响。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
@Getter
@AllArgsConstructor
public enum ActivityProtectionScene {

    /**
     * 活动抽奖场景。
     */
    DRAW("activity-draw"),

    /**
     * 活动账户查询场景。
     */
    QUERY_ACCOUNT("activity-query-account"),

    /**
     * 活动奖品列表查询场景。
     */
    QUERY_AWARD_LIST("activity-query-award-list");

    private final String circuitBreakerName;
}
