package com.lavyoung.marketforge.domain.strategy.service;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;

/**
 * 策略定义发布就绪校验器。
 *
 * <p>负责判断一个完整策略定义是否具备进入已生效状态的条件，包括奖池结构、
 * 概率空间、规则归属以及被引用奖品定义的可用状态。</p>
 *
 * <p>该服务只校验发布业务规则，不修改策略状态、不写入数据库，
 * 也不生成 Redis 概率快照。运行时预热由 Application 在校验通过后单独编排。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public interface IStrategyDefinitionPublicationValidator {

    /**
     * 校验策略定义是否允许发布。
     *
     * @param aggregate 待发布的完整策略定义聚合
     * @throws NullPointerException                                       待校验聚合为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖池、奖品定义、规则或概率配置不满足发布要求时抛出
     */
    void validate(StrategyDefinitionAggregate aggregate);
}
