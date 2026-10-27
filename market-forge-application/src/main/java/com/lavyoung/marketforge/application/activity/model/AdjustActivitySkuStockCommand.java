package com.lavyoung.marketforge.application.activity.model;

/**
 * 调整活动 SKU 库存命令。
 *
 * <p>正数表示补充库存，负数表示收回未消费库存。调用方只提交调整量，
 * 调整后的总库存和剩余库存由领域模型根据数据库当前快照计算，不能由客户端指定。</p>
 *
 * @param sku             SKU 业务标识
 * @param quantityDelta   调整数量；正数增加，负数减少
 * @param reason          调整原因
 * @param expectedVersion 页面读取到的乐观锁版本
 * @param operator        当前运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record AdjustActivitySkuStockCommand(
        Long sku,
        long quantityDelta,
        String reason,
        int expectedVersion,
        String operator
) {
}