package com.lavyoung.marketforge.domain.award.model.entity;

import com.lavyoung.marketforge.domain.award.model.valobj.AwardStateVO;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 用户中奖记录领域实体。
 *
 * @param userId     用户标识
 * @param activityId 活动标识
 * @param strategyId 策略标识
 * @param orderId    抽奖订单标识
 * @param awardId    奖品标识
 * @param awardTitle 奖品标题
 * @param awardTime  中奖时间
 * @param awardState 发奖状态
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Builder
public record UserAwardRecordEntity(
        String userId,
        Long activityId,
        Long strategyId,
        String orderId,
        Long awardId,
        String awardTitle,
        LocalDateTime awardTime,
        AwardStateVO awardState
) {
}
