package com.lavyoung.marketforge.application.activity.model;

/**
 * 修改活动 SKU 普通配置命令。
 *
 * <p>该命令只允许修改活动关联、次数配置和积分价格，不包含任何库存字段。
 * 库存必须通过独立库存调整用例变更。</p>
 *
 * @param sku             SKU 业务标识
 * @param activityId      新的活动业务标识
 * @param activityCountId 新的次数配置标识
 * @param creditAmount    新的积分兑换价格；为空表示关闭积分兑换
 * @param expectedVersion 页面读取到的乐观锁版本
 * @param operator        当前运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record UpdateActivitySkuDefinitionCommand(
        Long sku,
        Long activityId,
        Long activityCountId,
        Long creditAmount,
        int expectedVersion,
        String operator
) {
}