package com.lavyoung.marketforge.application.activity.model;

/**
 * 活动 SKU 商品查询结果。
 *
 * <p>该对象是 application 层返回给 trigger 层的用例结果，
 * 用于描述一个可使用积分兑换的活动 SKU 商品。它不属于 HTTP 协议模型，
 * 因此不会直接绑定 Swagger 注解或 Web 校验注解。</p>
 *
 * <p>商品列表页需要同时展示 SKU 库存、兑换价格和兑换后可获得的抽奖次数。
 * 其中库存和价格来自活动 SKU 配置，次数来自活动次数配置。</p>
 *
 * @param sku               活动商品 SKU
 * @param activityId        商品所属活动标识
 * @param stockCount        商品初始库存总量
 * @param stockCountSurplus 商品当前剩余库存
 * @param totalCount        兑换后增加的总抽奖次数
 * @param dayCount          兑换后增加的日抽奖次数
 * @param monthCount        兑换后增加的月抽奖次数
 * @param creditAmount      兑换该 SKU 需要消耗的积分数量
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
public record ActivitySkuProductResult(
        Long sku,
        Long activityId,
        Integer stockCount,
        Integer stockCountSurplus,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount,
        Long creditAmount
) {
}
