package com.lavyoung.marketforge.application.strategy.model;

import java.time.LocalDateTime;

/**
 * 策略定义运营列表项。
 *
 * <p>该模型仅包含策略主表中的摘要信息，不包含策略奖品和策略规则。
 * 分页查询使用轻量结果可以避免为每一条策略额外查询子表，从根源上规避 N+1。</p>
 *
 * @param strategyId   策略业务标识
 * @param strategyDesc 策略描述
 * @param ruleModels   策略级规则模型编码
 * @param state        生命周期状态
 * @param version      乐观锁版本
 * @param createdBy    创建人标识
 * @param updatedBy    最后修改人标识
 * @param createTime   创建时间
 * @param updateTime   最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyDefinitionSearchItem(Long strategyId, String strategyDesc, String ruleModels, String state,
                                           int version,
                                           String createdBy, String updatedBy, LocalDateTime createTime,
                                           LocalDateTime updateTime) {
}
