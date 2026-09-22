package com.lavyoung.marketforge.domain.activity.model.aggregate;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountDayEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountMonthEntity;
import lombok.Builder;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 创建抽奖参与订单的聚合对象。
 * <p>
 * 聚合用户总账户、日账户和月账户的校验结果，供仓储层在同一事务内扣减额度并创建抽奖单。
 *
 * @param userId                     用户标识
 * @param activityId                 活动标识
 * @param activityAccountEntity      用户活动总账户
 * @param isExistAccountDay          是否已存在当日账户
 * @param activityAccountDayEntity   用户活动日账户
 * @param isExistAccountMonth        是否已存在当月账户
 * @param activityAccountMonthEntity 用户活动月账户
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Builder
public record CreatePartakeOrderAggregate(
        String userId,
        Long activityId,
        ActivityAccountEntity activityAccountEntity,
        @DefaultValue("true") boolean isExistAccountDay,
        ActivityAccountDayEntity activityAccountDayEntity,
        @DefaultValue("true") boolean isExistAccountMonth,
        ActivityAccountMonthEntity activityAccountMonthEntity
) {
}
