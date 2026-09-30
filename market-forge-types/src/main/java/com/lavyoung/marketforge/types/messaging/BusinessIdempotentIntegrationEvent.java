package com.lavyoung.marketforge.types.messaging;

/**
 * 由业务聚合自身保证幂等的集成事件标记。
 * <p>
 * 此类事件不依赖通用消费记录表去重，消息处理器必须通过唯一业务键、
 * 状态机或条件更新保证重复消费不会产生额外业务副作用。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface BusinessIdempotentIntegrationEvent extends IntegrationEvent {
}
