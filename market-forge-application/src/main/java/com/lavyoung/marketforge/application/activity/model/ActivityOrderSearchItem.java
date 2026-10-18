package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 活动订单应用层查询结果项。
 *
 * <p>该模型是查询用例的稳定输出，不暴露 Elasticsearch Document、
 * 分片来源或搜索客户端类型。基础设施发生变化时，上层接口无需跟随调整。</p>
 *
 * @param userId        用户标识
 * @param activityId    活动标识
 * @param sku           活动商品 SKU
 * @param activityName  活动名称快照
 * @param strategyId    抽奖策略标识
 * @param orderId       全业务唯一订单号
 * @param orderTime     业务下单时间
 * @param totalCount    总抽奖次数
 * @param dayCount      日抽奖次数
 * @param monthCount    月抽奖次数
 * @param orderState    订单状态
 * @param outBusinessNo 外部业务幂等号
 * @param createTime    数据创建时间
 * @param updateTime    数据最后更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
public record ActivityOrderSearchItem(
        String userId,
        Long activityId,
        Long sku,
        String activityName,
        Long strategyId,
        String orderId,
        LocalDateTime orderTime,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount,
        String orderState,
        String outBusinessNo,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
