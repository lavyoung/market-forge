package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略定义状态变更请求测试。
 *
 * <p>验证数据库条件更新所需的当前状态、目标状态、
 * 乐观锁版本和操作人均符合领域约束。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionStateChangeTest {

    /**
     * Given 合法的草稿发布请求，When 创建状态变更对象，Then 应保留全部条件更新参数。
     */
    @Test
    void shouldCreateValidStateChange() {
        // Given & When
        StrategyDefinitionStateChange change = new StrategyDefinitionStateChange(
                100_001L, StrategyDefinitionStateVO.DRAFT,
                StrategyDefinitionStateVO.ACTIVE, 3, "operator-001"
        );

        // Then
        assertAll(
                () -> assertEquals(100_001L, change.strategyId()),
                () -> assertEquals(StrategyDefinitionStateVO.DRAFT, change.currentState()),
                () -> assertEquals(StrategyDefinitionStateVO.ACTIVE, change.targetState()),
                () -> assertEquals(3, change.expectedVersion()),
                () -> assertEquals("operator-001", change.operator())
        );
    }

    /**
     * Given 无效标识、版本、操作人或状态迁移，When 创建变更对象，Then 应拒绝进入仓储。
     */
    @Test
    void shouldRejectInvalidStateChange() {
        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StrategyDefinitionStateChange(0L, StrategyDefinitionStateVO.DRAFT,
                                StrategyDefinitionStateVO.ACTIVE, 1, "operator-001")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StrategyDefinitionStateChange(100_001L, StrategyDefinitionStateVO.DRAFT,
                                StrategyDefinitionStateVO.ACTIVE, 0, "operator-001")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StrategyDefinitionStateChange(100_001L, StrategyDefinitionStateVO.DRAFT,
                                StrategyDefinitionStateVO.ACTIVE, 1, " ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StrategyDefinitionStateChange(100_001L, StrategyDefinitionStateVO.DRAFT,
                                StrategyDefinitionStateVO.DISABLED, 1, "operator-001")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new StrategyDefinitionStateChange(100_001L, StrategyDefinitionStateVO.ACTIVE,
                                StrategyDefinitionStateVO.DRAFT, 1, "operator-001")),
                () -> assertThrows(NullPointerException.class,
                        () -> new StrategyDefinitionStateChange(100_001L, null,
                                StrategyDefinitionStateVO.ACTIVE, 1, "operator-001"))
        );
    }
}
