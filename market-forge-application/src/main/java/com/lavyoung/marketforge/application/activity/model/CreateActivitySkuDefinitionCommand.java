package com.lavyoung.marketforge.application.activity.model;

/**
 * 创建活动 SKU 命令。
 *
 * <p>创建时剩余库存由系统初始化为库存总量，调用方不能自行指定剩余库存，
 * 避免创建出总库存与剩余库存不一致的数据。</p>
 *
 * @param activityId      所属活动业务标识
 * @param activityCountId 兑换后获得的次数配置标识
 * @param stockCount      初始库存总量
 * @param creditAmount    兑换一个 SKU 所需积分；为空表示不支持积分兑换
 * @param operator        当前运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record CreateActivitySkuDefinitionCommand(
        Long activityId,
        Long activityCountId,
        long stockCount,
        Long creditAmount,
        String operator
) {
}