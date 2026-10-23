package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 活动 SKU 商品查询响应。
 *
 * <p>该对象面向前端商品列表页面，描述用户可以使用积分兑换的活动商品。
 * SKU 和活动标识属于长整型业务标识，因此在 HTTP 响应中使用字符串；
 * 库存、次数和积分价格属于可计算业务数量，继续保留数值类型。</p>
 *
 * <p>商品是否可兑换、库存是否充足、用户积分是否足够等业务判断，
 * 仍由 application 层和 domain 层完成；本对象只负责将查询结果
 * 以稳定、易展示的结构返回给调用方。</p>
 *
 * @param sku               活动商品 SKU，是用户发起积分兑换时传入的商品标识
 * @param activityId        商品所属抽奖活动标识
 * @param stockCount        商品初始库存总量
 * @param stockCountSurplus 商品当前剩余库存；为零时前端应展示为不可兑换
 * @param totalCount        兑换成功后增加的总抽奖次数
 * @param dayCount          兑换成功后增加的单日可用抽奖次数
 * @param monthCount        兑换成功后增加的单月可用抽奖次数
 * @param creditAmount      兑换一个该 SKU 所需扣减的积分数量
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Schema(description = "活动 SKU 积分兑换商品")
public record ActivitySkuProductResponse(
        @Schema(description = "活动商品 SKU", example = "9007199254740993", type = "string")
        String sku,

        @Schema(description = "所属抽奖活动标识", example = "9007199254740994", type = "string")
        String activityId,

        @Schema(description = "商品库存总量", example = "100000")
        Integer stockCount,

        @Schema(description = "商品剩余库存", example = "99890")
        Integer stockCountSurplus,

        @Schema(description = "兑换后增加的总抽奖次数", example = "100")
        Integer totalCount,

        @Schema(description = "兑换后增加的单日抽奖次数", example = "10")
        Integer dayCount,

        @Schema(description = "兑换后增加的单月抽奖次数", example = "100")
        Integer monthCount,

        @Schema(description = "兑换该 SKU 所需积分", example = "100")
        Long creditAmount
) implements Serializable {
}
