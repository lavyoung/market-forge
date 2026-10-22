package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 活动定义运营查询结果。
 *
 * <p>用于 Application 层向 Trigger 层返回完整的活动配置。该模型面向运营查询，
 * 不向外层暴露领域实体、持久化对象或 MyBatis 类型。</p>
 *
 * @param activityId        活动业务标识
 * @param activityName      活动名称
 * @param activityDesc      活动说明
 * @param beginDateTime     活动开始时间
 * @param endDateTime       活动结束时间
 * @param stockCount        活动库存总量
 * @param stockCountSurplus 活动剩余库存
 * @param activityCountId   活动次数配置标识
 * @param strategyId        抽奖策略标识
 * @param awardUnlockScope  奖品解锁次数统计范围
 * @param state             活动生命周期状态
 * @param version           乐观锁版本
 * @param createdBy         创建人标识
 * @param updatedBy         最后修改人标识
 * @param createTime        创建时间
 * @param updateTime        最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityDefinitionResult(
        Long activityId,
        String activityName,
        String activityDesc,
        LocalDateTime beginDateTime,
        LocalDateTime endDateTime,
        int stockCount,
        int stockCountSurplus,
        Long activityCountId,
        Long strategyId,
        String awardUnlockScope,
        String state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
