package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 策略定义生命周期状态事务。
 *
 * <p>该组件只负责最终的数据库条件更新，使策略预热与数据库事务保持隔离。
 * 发布应用服务先在事务外完成配置校验和 Redis 概率快照预热，再调用本组件
 * 通过一个短事务推进生命周期。</p>
 *
 * <p>状态、乐观锁版本或策略记录已经发生变化时，仓储返回 {@code false}，
 * 本组件将其转换为稳定的并发冲突业务异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Component
@RequiredArgsConstructor
public class StrategyDefinitionStateTransaction {

    private final IStrategyDefinitionRepository strategyDefinitionRepository;

    /**
     * 使用当前状态和乐观锁版本推进策略生命周期。
     *
     * @param stateChange 已完成领域合法性校验的状态变更请求
     * @throws NullPointerException 状态变更请求为空时抛出
     * @throws BusinessException    数据库状态或版本已经变化时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeState(StrategyDefinitionStateChange stateChange) {
        Objects.requireNonNull(stateChange, "stateChange must not be null");
        if (!strategyDefinitionRepository.changeState(stateChange)) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT,
                    stateChange.strategyId(),
                    stateChange.expectedVersion()
            );
        }
    }
}
