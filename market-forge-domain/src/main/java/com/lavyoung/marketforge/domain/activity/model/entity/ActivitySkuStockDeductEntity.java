package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 活动 SKU 缓存库存预扣结果。
 * <p>
 * 用于表达 Redis 侧库存预扣是否成功，以及当前 SKU 是否已经进入零库存状态。调用方根据
 * {@code success} 判断是否继续创建额度订单，根据 {@code zeroStock} 判断是否触发零库存同步。
 *
 * @param success   预扣成功返回 {@code true}
 * @param zeroStock 当前缓存库存已经耗尽或被修正为零时返回 {@code true}
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Builder
public record ActivitySkuStockDeductEntity(
        boolean success,
        boolean zeroStock
) {
}
