package com.lavyoung.marketforge.application.strategy.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 策略定义应用结果。
 *
 * <p>该结果是策略运营用例向 Trigger 返回的完整聚合视图，包括策略根配置、
 * 奖品配置和规则配置。外层不能直接依赖 Domain 聚合或 Infrastructure PO。</p>
 *
 * @param strategyId   策略业务标识
 * @param strategyDesc 策略描述
 * @param ruleModels   策略级责任链规则编码
 * @param state        生命周期状态编码
 * @param version      乐观锁版本
 * @param createdBy    创建人标识
 * @param updatedBy    最后修改人标识
 * @param createTime   创建时间
 * @param updateTime   最后修改时间
 * @param awards       策略奖品完整列表
 * @param rules        策略规则完整列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record StrategyDefinitionResult(
        Long strategyId,
        String strategyDesc,
        String ruleModels,
        String state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<StrategyAwardConfigResult> awards,
        List<StrategyRuleConfigResult> rules
) {

    /**
     * 创建策略定义结果并保护子集合。
     *
     * <p>复制集合可以防止 Controller 或序列化过程意外修改应用服务已经生成的结果。</p>
     *
     * @throws NullPointerException 奖品集合或规则集合为空时抛出
     */
    public StrategyDefinitionResult {
        awards = List.copyOf(Objects.requireNonNull(awards, "awards must not be null"));
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
    }
}
