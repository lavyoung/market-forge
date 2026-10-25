/**
 * 抽奖策略领域仓储端口。
 *
 * <p>{@code IStrategyRepository} 面向抽奖运行时，提供规则、库存和概率快照访问；
 * {@code IStrategyDefinitionRepository} 面向运营配置，提供策略聚合的事务性读写。
 * 两类端口共享策略领域，但拥有不同的访问频率和一致性边界。</p>
 *
 * <p>本包只定义领域需要的能力，不依赖 MyBatis、PO、Redis 或其他基础设施类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
package com.lavyoung.marketforge.domain.strategy.repository;