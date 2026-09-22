package com.lavyoung.marketforge.domain.activity.model.aggregate;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import lombok.Builder;

/**
 * 创建活动账户额度订单的聚合对象。
 * <p>
 * 保存 SKU 充值产生的次数额度和对应活动订单，用于一次性写入订单并累计用户账户额度。
 *
 * @param userId        用户标识
 * @param activityId    活动标识
 * @param totalCount    本次增加的总次数
 * @param dayCount      本次增加的日次数
 * @param monthCount    本次增加的月次数
 * @param activityOrder 活动额度订单
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Builder
public record CreateQuotaOrderAggregate(
        String userId,
        Long activityId,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount,
        ActivityOrderEntity activityOrder
) {
}
