package com.lavyoung.marketforge.application.strategy.model;

/**
 * 变更策略定义生命周期命令。
 *
 * <p>命令不接收调用方声明的当前状态。应用服务会重新读取策略聚合，
 * 使用数据库中的真实状态判断生命周期迁移是否合法。</p>
 *
 * <p>当目标状态为 {@code active} 时，应用服务还必须先执行发布就绪检查和
 * 概率快照预热；只有运行时数据准备成功后才能将数据库状态改为已生效。</p>
 *
 * @param strategyId      策略业务标识
 * @param targetState     目标生命周期状态编码
 * @param expectedVersion 调用方最近一次查询到的乐观锁版本
 * @param operator        执行发布或停用的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record ChangeStrategyDefinitionStateCommand(
        Long strategyId,
        String targetState,
        int expectedVersion,
        String operator
) {
}
