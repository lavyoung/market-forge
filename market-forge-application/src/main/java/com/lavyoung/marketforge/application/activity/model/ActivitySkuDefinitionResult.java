package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 活动 SKU 运营查询结果。
 *
 * <p>用于向 Trigger 层返回完整 SKU 配置和库存状态，不向外层暴露领域实体、
 * 持久化对象或 MyBatis 类型。</p>
 *
 * @param sku               SKU 业务标识
 * @param activityId        所属活动业务标识
 * @param activityCountId   次数配置业务标识
 * @param stockCount        累计配置的库存总量
 * @param stockCountSurplus 当前剩余库存
 * @param consumedCount     已消费库存数量
 * @param creditAmount      积分兑换价格；为空表示不支持积分兑换
 * @param version           乐观锁版本
 * @param createdBy         创建人标识
 * @param updatedBy         最后修改人标识
 * @param createTime        创建时间
 * @param updateTime        最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivitySkuDefinitionResult(
        Long sku,
        Long activityId,
        Long activityCountId,
        long stockCount,
        long stockCountSurplus,
        long consumedCount,
        Long creditAmount,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}