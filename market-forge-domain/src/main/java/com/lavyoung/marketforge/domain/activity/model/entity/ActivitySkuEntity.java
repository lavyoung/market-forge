package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 抽奖活动 SKU 领域实体。
 *
 * <p>该实体描述一个可被签到返利、积分兑换或其他营销行为使用的活动商品。
 * SKU 决定用户会进入哪个抽奖活动、获得哪种次数配置，以及兑换时需要消耗多少积分。</p>
 *
 * <p>{@code stockCount} 表示运营配置的 SKU 初始库存总量；
 * {@code stockCountSurplus} 表示当前还可用于兑换或发放的剩余库存。
 * 查询商品列表时，前端应展示剩余库存；兑换下单时，也应以剩余库存判断是否还能继续兑换。</p>
 *
 * <p>{@code creditAmount} 是服务端配置的兑换价格。积分兑换时必须以该字段为准，
 * 不能信任客户端传入的积分数量，否则调用方能够篡改价格并以更低积分兑换商品。</p>
 *
 * @param sku               商品 SKU
 * @param activityId        抽奖活动标识
 * @param activityCountId   活动参与次数配置标识
 * @param stockCount        商品初始库存总量
 * @param stockCountSurplus 商品当前剩余库存
 * @param creditAmount      兑换一个该 SKU 需要消耗的积分数量；非积分兑换场景可为 {@code null}
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivitySkuEntity(
        Long sku,
        Long activityId,
        Long activityCountId,
        Integer stockCount,
        Integer stockCountSurplus,
        Long creditAmount
) {
}
