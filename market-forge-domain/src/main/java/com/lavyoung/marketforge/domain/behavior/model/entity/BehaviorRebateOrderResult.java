package com.lavyoung.marketforge.domain.behavior.model.entity;

import java.util.List;

/**
 * 用户行为返利订单创建结果。
 *
 * <p>领域服务创建返利订单时，同时会生成待投递的返利事件。
 * 应用层需要事件标识来触发本地消息任务的事务后即时投递，
 * 因此结果对象同时返回返利订单号和事件标识。</p>
 *
 * @param orderIds 创建成功的返利订单号集合
 * @param eventIds 待投递返利事件标识集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
public record BehaviorRebateOrderResult(
        List<String> orderIds,
        List<String> eventIds
) {
}
