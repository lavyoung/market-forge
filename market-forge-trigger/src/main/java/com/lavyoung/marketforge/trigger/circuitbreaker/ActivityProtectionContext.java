package com.lavyoung.marketforge.trigger.circuitbreaker;

/**
 * 活动接口保护上下文。
 *
 * <p>集中保存熔断器选择和降级日志所需的信息，不携带领域实体，
 * 避免入口保护组件依赖具体业务模型。</p>
 *
 * @param scene      接口保护场景
 * @param userId     当前用户标识
 * @param activityId 当前活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
public record ActivityProtectionContext(
        ActivityProtectionScene scene,
        String userId,
        Long activityId
) {
}
