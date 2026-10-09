package com.lavyoung.marketforge.domain.behavior.model.aggregate;

import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;

import java.util.List;

/**
 * 用户行为返利聚合。
 *
 * <p>该聚合承载一次用户行为入账产生的返利订单和待投递返利事件。
 * 仓储实现应保证订单记录和本地消息任务在同一事务内落库，
 * 避免返利订单存在但 MQ 消息丢失。</p>
 *
 * @param userId 用户标识，同时作为用户返利订单和本地消息任务的分片键
 * @param orders 用户行为返利订单集合
 * @param events 待投递的用户行为返利事件集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
public record UserBehaviorRebateAggregate(
        String userId,
        List<UserBehaviorRebateOrderEntity> orders,
        List<SendRebateEvent> events
) {
}
