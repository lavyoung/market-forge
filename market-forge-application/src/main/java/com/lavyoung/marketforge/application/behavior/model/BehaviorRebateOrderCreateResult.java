package com.lavyoung.marketforge.application.behavior.model;

import java.util.List;

/**
 * 用户行为返利订单创建结果。
 * <p>
 * 该结果对象是 application 层对外返回的用例结果，用来隔离领域层结果模型。
 * trigger 层只依赖这个对象，不直接依赖 domain 层的 {@code BehaviorRebateOrderResult}。
 *
 * @param orderIds 本次行为创建的返利订单号列表
 * @param eventIds 本次行为创建的可靠消息事件标识列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/10
 */
public record BehaviorRebateOrderCreateResult(
        List<String> orderIds,
        List<String> eventIds
) {
}
