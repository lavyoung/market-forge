/**
 * 抽奖策略运行时与运营配置用例的输入契约。
 * <p>
 * 请求模型采用不可变 {@code record}，并使用 Jakarta Bean Validation 声明结构性约束。
 * 规则配置使用黑名单、权重档位和次数锁等结构化请求，禁止运营端直接拼装内部持久化字符串。
 */
package com.lavyoung.marketforge.api.strategy.request;
