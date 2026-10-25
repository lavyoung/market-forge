package com.lavyoung.marketforge.application.strategy.model;

import java.util.List;

/**
 * 创建策略定义命令。
 *
 * <p>策略标识、初始生命周期、乐观锁版本和审计时间由服务端控制。
 * 新策略固定以草稿状态创建，创建后可以继续完善奖品与规则配置。</p>
 *
 * <p>奖品和规则集合允许为空，以支持运营后台先建立策略基本信息、
 * 再逐步配置奖池的操作方式；但空奖池不能发布。</p>
 *
 * @param strategyDesc 策略描述
 * @param ruleModels   策略级责任链规则编码；未配置时可以为空
 * @param awards       策略奖品配置；可以为空集合
 * @param rules        策略规则配置；可以为空集合
 * @param operator     创建策略的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record CreateStrategyDefinitionCommand(
        String strategyDesc,
        String ruleModels,
        List<StrategyAwardConfigCommand> awards,
        List<StrategyRuleConfigCommand> rules,
        String operator
) {
}
