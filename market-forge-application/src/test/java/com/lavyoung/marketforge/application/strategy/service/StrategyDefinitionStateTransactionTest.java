package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.service.impl.StrategyDefinitionStateTransaction;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 策略定义状态短事务测试。
 *
 * <p>验证状态条件更新成功时正常提交，状态或版本冲突时转换为稳定业务异常，
 * 并确保空参数不会进入仓储。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionStateTransactionTest {

    @Mock
    private IStrategyDefinitionRepository strategyDefinitionRepository;

    private StrategyDefinitionStateTransaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new StrategyDefinitionStateTransaction(strategyDefinitionRepository);
    }

    /**
     * Given 状态与版本条件匹配，When 推进生命周期，Then 应正常完成数据库更新。
     */
    @Test
    void shouldChangeStateWhenDatabaseConditionMatches() {
        // Given
        StrategyDefinitionStateChange stateChange = stateChange();
        when(strategyDefinitionRepository.changeState(stateChange)).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> transaction.changeState(stateChange));
        verify(strategyDefinitionRepository).changeState(stateChange);
    }

    /**
     * Given 数据库状态或版本已经改变，When 推进生命周期，Then 应抛出策略定义冲突。
     */
    @Test
    void shouldReportConflictWhenDatabaseConditionChanges() {
        // Given
        StrategyDefinitionStateChange stateChange = stateChange();
        when(strategyDefinitionRepository.changeState(stateChange)).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> transaction.changeState(stateChange));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT, exception.getResponseCode());
    }

    /**
     * Given 状态变更请求为空，When 调用短事务，Then 应在访问仓储前立即拒绝。
     */
    @Test
    void shouldRejectNullStateChange() {
        // When & Then
        assertThrows(NullPointerException.class, () -> transaction.changeState(null));
        verifyNoInteractions(strategyDefinitionRepository);
    }

    private StrategyDefinitionStateChange stateChange() {
        return new StrategyDefinitionStateChange(100001L, StrategyDefinitionStateVO.DRAFT,
                StrategyDefinitionStateVO.ACTIVE, 3, "operator-002");
    }
}
