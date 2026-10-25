package com.lavyoung.marketforge.application.strategy.model;

import java.util.List;

/**
 * 修改草稿策略定义命令。
 *
 * <p>该命令以完整聚合为单位替换策略基础信息、奖品和规则。
 * 应用服务会先校验当前策略仍为草稿，再使用 {@code expectedVersion}
 * 执行数据库条件更新，防止并发运营修改互相覆盖。</p>
 *
 * @param strategyId      待修改的策略业务标识
 * @param strategyDesc    最新策略描述
 * @param ruleModels      最新策略级责任链规则编码；未配置时可以为空
 * @param awards          最新策略奖品完整集合
 * @param rules           最新策略规则完整集合
 * @param expectedVersion 调用方最近一次查询到的乐观锁版本
 * @param operator        执行修改的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record UpdateStrategyDefinitionCommand(
        Long strategyId,
        String strategyDesc,
        String ruleModels,
        List<StrategyAwardConfigCommand> awards,
        List<StrategyRuleConfigCommand> rules,
        int expectedVersion,
        String operator
) {
}
