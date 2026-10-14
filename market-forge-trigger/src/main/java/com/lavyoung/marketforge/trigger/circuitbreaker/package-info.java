/**
 * 活动入口的熔断与超时保护适配器。
 *
 * <p>该包负责选择 Spring Cloud Circuit Breaker 实例、执行受保护调用并将
 * 技术故障转换为统一降级响应，不承载活动领域规则或基础设施访问逻辑。</p>
 */
package com.lavyoung.marketforge.trigger.circuitbreaker;
