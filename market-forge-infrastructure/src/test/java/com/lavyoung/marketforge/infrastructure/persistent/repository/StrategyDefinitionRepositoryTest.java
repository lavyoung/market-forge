package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyRuleAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyRuleDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.StrategyDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 策略定义运营仓储测试。
 *
 * <p>验证三表聚合查询与写入编排、乐观锁冲突的短路行为、批量写入完整性、
 * 数据库唯一键异常翻译、状态更新参数以及只读事务契约。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionRepositoryTest {

    @Mock
    private IStrategyDao strategyDao;
    @Mock
    private IStrategyAwardDao strategyAwardDao;
    @Mock
    private IStrategyRuleDao strategyRuleDao;
    @Mock
    private StrategyDefinitionAssembler definitionAssembler;
    @Mock
    private StrategyAwardAssembler awardAssembler;
    @Mock
    private StrategyRuleAssembler ruleAssembler;

    private StrategyDefinitionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new StrategyDefinitionRepository(strategyDao, strategyAwardDao, strategyRuleDao,
                definitionAssembler, awardAssembler, ruleAssembler);
    }

    /**
     * Given 策略根记录不存在，When 查询策略定义，Then 应返回空且不查询两张子表。
     */
    @Test
    void shouldReturnEmptyWithoutQueryingChildrenWhenRootDoesNotExist() {
        // Given
        when(strategyDao.queryStrategyByStrategyId(100001L)).thenReturn(Optional.empty());

        // When
        Optional<StrategyDefinitionAggregate> actual = repository.queryByStrategyId(100001L);

        // Then
        assertTrue(actual.isEmpty());
        verifyNoInteractions(strategyAwardDao, strategyRuleDao, definitionAssembler, awardAssembler, ruleAssembler);
    }

    /**
     * Given 三张表都存在配置，When 查询策略定义，Then 应批量查询子表并组装完整聚合。
     */
    @Test
    void shouldQueryCompleteStrategyDefinition() {
        // Given
        StrategyPO strategyPO = new StrategyPO();
        List<StrategyAwardPO> awardPOS = List.of(new StrategyAwardPO());
        List<StrategyRulePO> rulePOS = List.of(new StrategyRulePO());
        StrategyDefinitionAggregate expected = aggregate();
        when(strategyDao.queryStrategyByStrategyId(100001L)).thenReturn(Optional.of(strategyPO));
        when(strategyAwardDao.queryStrategyAwardList(100001L)).thenReturn(awardPOS);
        when(strategyRuleDao.queryStrategyRuleList(100001L)).thenReturn(rulePOS);
        when(awardAssembler.toEntities(awardPOS)).thenReturn(expected.awards());
        when(ruleAssembler.toEntities(rulePOS)).thenReturn(expected.rules());
        when(definitionAssembler.toAggregate(strategyPO, expected.awards(), expected.rules())).thenReturn(expected);

        // When
        Optional<StrategyDefinitionAggregate> actual = repository.queryByStrategyId(100001L);

        // Then
        assertEquals(Optional.of(expected), actual);
        verify(strategyAwardDao).queryStrategyAwardList(100001L);
        verify(strategyRuleDao).queryStrategyRuleList(100001L);
    }

    /**
     * Given 查询方法需要获得一致快照，When 检查事务元数据，Then 应声明只读事务。
     *
     * @throws NoSuchMethodException 方法签名被意外修改时抛出
     */
    @Test
    void shouldReadCompleteDefinitionInReadOnlyTransaction() throws NoSuchMethodException {
        // Given
        Method method = StrategyDefinitionRepository.class.getMethod("queryByStrategyId", Long.class);

        // When
        Transactional transactional = method.getAnnotation(Transactional.class);

        // Then
        assertNotNull(transactional, "三表聚合读取必须处于同一个只读事务");
        assertTrue(transactional.readOnly(), "聚合查询事务必须标记为只读");
    }

    /**
     * Given 包含奖品和规则的新草稿，When 创建策略，Then 应在根记录后批量写入两类子配置。
     */
    @Test
    void shouldCreateCompleteStrategyDefinition() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyPO strategyPO = new StrategyPO();
        List<StrategyAwardPO> awardPOS = List.of(new StrategyAwardPO());
        List<StrategyRulePO> rulePOS = List.of(new StrategyRulePO());
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.insert(strategyPO)).thenReturn(1);
        when(awardAssembler.toPOs(aggregate.awards())).thenReturn(awardPOS);
        when(ruleAssembler.toPOs(aggregate.rules())).thenReturn(rulePOS);
        when(strategyAwardDao.insertBatch(awardPOS)).thenReturn(1);
        when(strategyRuleDao.insertBatch(rulePOS)).thenReturn(1);

        // When
        repository.create(aggregate);

        // Then
        InOrder inOrder = inOrder(strategyDao, strategyAwardDao, strategyRuleDao);
        inOrder.verify(strategyDao).insert(strategyPO);
        inOrder.verify(strategyAwardDao).insertBatch(awardPOS);
        inOrder.verify(strategyRuleDao).insertBatch(rulePOS);
    }

    /**
     * Given 草稿没有奖品和规则，When 创建策略，Then 应跳过会生成非法 SQL 的空批量写入。
     */
    @Test
    void shouldSkipEmptyChildBatchesWhenCreatingDraft() {
        // Given
        StrategyDefinitionAggregate aggregate = emptyAggregate();
        StrategyPO strategyPO = new StrategyPO();
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.insert(strategyPO)).thenReturn(1);
        when(awardAssembler.toPOs(aggregate.awards())).thenReturn(List.of());
        when(ruleAssembler.toPOs(aggregate.rules())).thenReturn(List.of());

        // When
        repository.create(aggregate);

        // Then
        verify(strategyAwardDao, never()).insertBatch(anyList());
        verify(strategyRuleDao, never()).insertBatch(anyList());
    }

    /**
     * Given 策略根记录写入失败，When 创建策略，Then 应抛出保存失败且不写入子配置。
     */
    @Test
    void shouldRejectCreateWhenRootInsertIsIncomplete() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyPO strategyPO = new StrategyPO();
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.insert(strategyPO)).thenReturn(0);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(aggregate));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_DEFINITION_SAVE_FAILED, exception.getResponseCode());
        verifyNoInteractions(strategyAwardDao, strategyRuleDao, awardAssembler, ruleAssembler);
    }

    /**
     * Given 策略标识违反唯一约束，When 创建策略，Then 应保留原因并翻译为稳定业务异常。
     */
    @Test
    void shouldTranslateDuplicateKeyWhenCreatingDefinition() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyPO strategyPO = new StrategyPO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate strategy");
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.insert(strategyPO)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(aggregate));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.STRATEGY_DEFINITION_DUPLICATED, exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    /**
     * Given 根记录的状态或版本已变化，When 更新草稿，Then 应短路返回且不得删除历史子配置。
     */
    @Test
    void shouldNotReplaceChildrenWhenDraftUpdateConflicts() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyPO strategyPO = new StrategyPO();
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.updateDraft(strategyPO)).thenReturn(0);

        // When
        boolean actual = repository.updateDraft(aggregate);

        // Then
        assertFalse(actual);
        verifyNoInteractions(strategyAwardDao, strategyRuleDao, awardAssembler, ruleAssembler);
    }

    /**
     * Given 草稿状态与版本匹配，When 更新完整聚合，Then 应先锁定根记录再替换两类子配置。
     */
    @Test
    void shouldReplaceChildrenAfterSuccessfulDraftUpdate() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyPO strategyPO = new StrategyPO();
        List<StrategyAwardPO> awardPOS = List.of(new StrategyAwardPO());
        List<StrategyRulePO> rulePOS = List.of(new StrategyRulePO());
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.updateDraft(strategyPO)).thenReturn(1);
        when(awardAssembler.toPOs(aggregate.awards())).thenReturn(awardPOS);
        when(ruleAssembler.toPOs(aggregate.rules())).thenReturn(rulePOS);
        when(strategyAwardDao.insertBatch(awardPOS)).thenReturn(1);
        when(strategyRuleDao.insertBatch(rulePOS)).thenReturn(1);

        // When
        boolean actual = repository.updateDraft(aggregate);

        // Then
        assertTrue(actual);
        InOrder inOrder = inOrder(strategyDao, strategyAwardDao, strategyRuleDao);
        inOrder.verify(strategyDao).updateDraft(strategyPO);
        inOrder.verify(strategyAwardDao).deleteByStrategyId(100001L);
        inOrder.verify(strategyRuleDao).deleteByStrategyId(100001L);
        inOrder.verify(strategyAwardDao).insertBatch(awardPOS);
        inOrder.verify(strategyRuleDao).insertBatch(rulePOS);
    }

    /**
     * Given 子配置批量写入条数不完整，When 保存聚合，Then 应抛出异常触发外层事务回滚。
     */
    @Test
    void shouldRejectIncompleteChildBatch() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyPO strategyPO = new StrategyPO();
        List<StrategyAwardPO> awardPOS = List.of(new StrategyAwardPO());
        when(definitionAssembler.toPO(aggregate)).thenReturn(strategyPO);
        when(strategyDao.insert(strategyPO)).thenReturn(1);
        when(awardAssembler.toPOs(aggregate.awards())).thenReturn(awardPOS);
        when(strategyAwardDao.insertBatch(awardPOS)).thenReturn(0);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(aggregate));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_DEFINITION_SAVE_FAILED, exception.getResponseCode());
        verifyNoInteractions(strategyRuleDao, ruleAssembler);
    }

    /**
     * Given 合法状态迁移，When 更新生命周期，Then 应传递状态编码、版本与操作人。
     */
    @Test
    void shouldChangeStateWithOptimisticLockParameters() {
        // Given
        StrategyDefinitionStateChange stateChange = new StrategyDefinitionStateChange(100001L,
                StrategyDefinitionStateVO.DRAFT, StrategyDefinitionStateVO.ACTIVE, 3, "operator-003");
        when(strategyDao.changeState(any())).thenReturn(1);

        // When
        boolean actual = repository.changeState(stateChange);

        // Then
        ArgumentCaptor<StrategyDefinitionStateUpdateParam> captor = ArgumentCaptor.forClass(StrategyDefinitionStateUpdateParam.class);
        verify(strategyDao).changeState(captor.capture());
        StrategyDefinitionStateUpdateParam parameter = captor.getValue();
        assertAll(
                () -> assertTrue(actual),
                () -> assertEquals(100001L, parameter.strategyId()),
                () -> assertEquals("draft", parameter.currentState()),
                () -> assertEquals("active", parameter.targetState()),
                () -> assertEquals(3, parameter.expectedVersion()),
                () -> assertEquals("operator-003", parameter.operator())
        );
    }

    /**
     * Given 仓储公开方法收到空参数，When 调用，Then 应在访问 DAO 前立即拒绝。
     */
    @Test
    void shouldRejectNullArguments() {
        // When & Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> repository.queryByStrategyId(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.create(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.updateDraft(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.changeState(null))
        );
        verifyNoInteractions(strategyDao, strategyAwardDao, strategyRuleDao);
    }

    private StrategyDefinitionAggregate aggregate() {
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 25, 11, 0);
        StrategyAwardEntity award = new StrategyAwardEntity(100001L, 101L, "100积分", "积分自动到账", 1000, 900,
                new BigDecimal("0.1000"), "rule_lock", 1, createTime, updateTime);
        StrategyRuleEntity rule = new StrategyRuleEntity(100001L, null, 1, "rule_weight", "100:101",
                "累计积分达到100时命中", createTime, updateTime);
        return new StrategyDefinitionAggregate(100001L, "九宫格抽奖策略", "rule_weight,rule_blacklist",
                StrategyDefinitionStateVO.DRAFT, 3, "operator-001", "operator-002", createTime, updateTime,
                List.of(award), List.of(rule));
    }

    private StrategyDefinitionAggregate emptyAggregate() {
        return new StrategyDefinitionAggregate(100002L, "空草稿策略", null, StrategyDefinitionStateVO.DRAFT, 1,
                "operator-001", "operator-001", null, null, List.of(), List.of());
    }
}
