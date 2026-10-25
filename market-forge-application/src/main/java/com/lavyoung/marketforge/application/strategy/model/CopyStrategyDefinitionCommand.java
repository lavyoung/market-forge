package com.lavyoung.marketforge.application.strategy.model;

/**
 * 复制历史策略为新草稿的命令。
 *
 * <p>已生效或已停用策略不能原地修改。需要调整配置时，通过本命令生成一个
 * 拥有新策略标识的独立草稿，原策略继续服务已有活动和历史抽奖记录。</p>
 *
 * @param sourceStrategyId 被复制的历史策略业务标识
 * @param operator         创建新草稿的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record CopyStrategyDefinitionCommand(
        Long sourceStrategyId,
        String operator
) {
}
