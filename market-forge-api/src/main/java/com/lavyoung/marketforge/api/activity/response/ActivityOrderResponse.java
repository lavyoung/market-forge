package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动订单运营查询响应。
 *
 * <p>该对象描述运营后台展示的一条活动订单记录。数据库分片内自增主键和
 * Elasticsearch 来源库字段不会暴露，因为它们不具备稳定的业务含义。</p>
 *
 * <p>业务标识统一以字符串形式返回，防止 Java {@code long} 超过
 * JavaScript 安全整数范围后发生精度丢失。</p>
 *
 * @param userId        用户标识
 * @param activityId    活动标识
 * @param sku           活动商品 SKU
 * @param activityName  下单时保存的活动名称快照
 * @param strategyId    抽奖策略标识
 * @param orderId       全业务唯一订单号
 * @param orderTime     业务下单时间
 * @param totalCount    订单授予的总抽奖次数
 * @param dayCount      订单授予的日抽奖次数
 * @param monthCount    订单授予的月抽奖次数
 * @param orderState    订单状态
 * @param outBusinessNo 外部业务幂等号
 * @param createTime    数据创建时间
 * @param updateTime    数据最后更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Schema(description = "活动订单运营查询结果")
public record ActivityOrderResponse(
        @Schema(description = "用户标识", example = "user-001")
        String userId,

        @Schema(description = "活动标识", example = "100301")
        String activityId,

        @Schema(description = "活动商品 SKU", example = "9011")
        String sku,

        @Schema(description = "活动名称快照", example = "周末幸运抽奖")
        String activityName,

        @Schema(description = "抽奖策略标识", example = "100006")
        String strategyId,

        @Schema(description = "全业务唯一订单号", example = "ROPmv8av9hj0002z38fvn2949wettup")
        String orderId,

        @Schema(description = "业务下单时间", example = "2026-10-18T10:08:05", type = "string", format = "date-time")
        LocalDateTime orderTime,

        @Schema(description = "总抽奖次数", example = "10")
        Integer totalCount,

        @Schema(description = "日抽奖次数", example = "1")
        Integer dayCount,

        @Schema(description = "月抽奖次数", example = "10")
        Integer monthCount,

        @Schema(description = "订单状态", example = "used", allowableValues = {"create", "used", "cancel", "wait_pay", "complete"})
        String orderState,

        @Schema(description = "外部业务幂等号", example = "raffle-20261018-001")
        String outBusinessNo,

        @Schema(description = "数据创建时间", example = "2026-10-18T10:08:05", type = "string", format = "date-time")
        LocalDateTime createTime,

        @Schema(description = "数据最后更新时间", example = "2026-10-18T10:08:07", type = "string", format = "date-time")
        LocalDateTime updateTime
) implements Serializable {
}
