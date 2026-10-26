package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.application.strategy.service.impl.StrategyDefinitionApplicationServiceImpl;
import com.lavyoung.marketforge.application.strategy.service.impl.StrategyDefinitionStateTransaction;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionPublicationValidator;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 策略定义运营应用服务测试。
 *
 * <p>验证策略创建、查询、草稿修改、历史策略复制、发布预热、停用以及乐观锁冲突。
 * 发布测试特别锁定“校验、预热、状态更新”的调用顺序。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionApplicationServiceImplTest {

    private static final Long STRATEGY_ID = 100001L;
    private static final Long FIRST_AWARD_ID = 200001L;
    private static final Long SECOND_AWARD_ID = 200002L;

    @Mock
    private IStrategyDefinitionRepository strategyDefinitionRepository;
    @Mock
    private IStrategyDefinitionPublicationValidator publicationValidator;
    @Mock
    private IStrategyArmory strategyArmory;
    @Mock
    private StrategyDefinitionStateTransaction stateTransaction;

    private StrategyDefinitionApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StrategyDefinitionApplicationServiceImpl(
                strategyDefinitionRepository, publicationValidator, strategyArmory, stateTransaction);
    }

    /**
     * Given 完整创建命令，When 创建策略，Then 应生成草稿聚合、初始化库存并回查持久化结果。
     */
    @Test
    void shouldCreateDraftAndReturnPersistedDefinition() {
        // Given
        AtomicReference<StrategyDefinitionAggregate> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            StrategyDefinitionAggregate created = invocation.getArgument(0);
            stored.set(withPersistenceTimes(created));
            return null;
        }).when(strategyDefinitionRepository).create(any());
        when(strategyDefinitionRepository.queryByStrategyId(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));

        // When
        StrategyDefinitionResult result = service.create(createCommand());

        // Then
        StrategyDefinitionAggregate created = stored.get();
        assertAll(
                () -> assertNotNull(created),
                () -> assertTrue(created.strategyId() > 0),
                () -> assertEquals(StrategyDefinitionStateVO.DRAFT, created.state()),
                () -> assertEquals(1, created.version()),
                () -> assertEquals(created.awards().get(0).awardCount(), created.awards().get(0).awardCountSurplus()),
                () -> assertEquals(created.strategyId(), created.awards().get(0).strategyId()),
                () -> assertEquals(created.strategyId(), created.rules().get(0).strategyId()),
                () -> assertEquals(created.strategyId(), result.strategyId()),
                () -> assertNotNull(result.createTime())
        );
        verify(strategyDefinitionRepository).queryByStrategyId(created.strategyId());
    }

    /**
     * Given 策略不存在，When 查询完整定义，Then 应返回稳定的策略不存在业务异常。
     */
    @Test
    void shouldRejectQueryWhenStrategyDoesNotExist() {
        // Given
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.queryByStrategyId(STRATEGY_ID));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_NOT_FOUND, exception.getResponseCode());
    }

    /**
     * Given 草稿版本匹配，When 修改完整配置，Then 应重建子配置、更新操作人并返回数据库结果。
     */
    @Test
    void shouldUpdateCompleteDraftDefinition() {
        // Given
        StrategyDefinitionAggregate current = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "原策略");
        AtomicReference<StrategyDefinitionAggregate> updatedReference = new AtomicReference<>();
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID))
                .thenReturn(Optional.of(current))
                .thenAnswer(invocation -> Optional.of(withPersistenceTimes(updatedReference.get())));
        when(strategyDefinitionRepository.updateDraft(any())).thenAnswer(invocation -> {
            updatedReference.set(invocation.getArgument(0));
            return true;
        });

        // When
        StrategyDefinitionResult result = service.updateDraft(updateCommand(3));

        // Then
        StrategyDefinitionAggregate updated = updatedReference.get();
        assertAll(
                () -> assertEquals("修改后的策略", updated.strategyDesc()),
                () -> assertEquals(3, updated.version()),
                () -> assertEquals("operator-002", updated.updatedBy()),
                () -> assertEquals(150, updated.awards().get(0).awardCount()),
                () -> assertEquals(150, updated.awards().get(0).awardCountSurplus()),
                () -> assertEquals("修改后的策略", result.strategyDesc())
        );
    }

    /**
     * Given 策略已生效，When 尝试原地修改，Then 应拒绝并且不调用更新仓储。
     */
    @Test
    void shouldRejectInPlaceUpdateForActiveStrategy() {
        // Given
        StrategyDefinitionAggregate current = aggregate(StrategyDefinitionStateVO.ACTIVE, 3, "已生效策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(current));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(3)));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_STATE_ERROR, exception.getResponseCode());
        verify(strategyDefinitionRepository, never()).updateDraft(any());
    }

    /**
     * Given 调用方版本已经过期，When 修改草稿，Then 应在数据库更新前返回并发冲突。
     */
    @Test
    void shouldRejectStaleDraftUpdateBeforeWriting() {
        // Given
        StrategyDefinitionAggregate current = aggregate(StrategyDefinitionStateVO.DRAFT, 4, "当前策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(current));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(3)));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT, exception.getResponseCode());
        verify(strategyDefinitionRepository, never()).updateDraft(any());
    }

    /**
     * Given 应用层版本检查通过但数据库条件更新失败，When 修改草稿，Then 应转换最终并发冲突。
     */
    @Test
    void shouldRejectDraftUpdateWhenDatabaseConditionChanges() {
        // Given
        StrategyDefinitionAggregate current = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "当前策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(current));
        when(strategyDefinitionRepository.updateDraft(any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(3)));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT, exception.getResponseCode());
    }

    /**
     * Given 已生效策略且库存已经消耗，When 复制为新草稿，Then 应生成新标识并重置库存后回查。
     */
    @Test
    void shouldCopyActiveStrategyAsIndependentDraft() {
        // Given
        StrategyDefinitionAggregate source = aggregate(StrategyDefinitionStateVO.ACTIVE, 3, "历史策略");
        AtomicReference<StrategyDefinitionAggregate> copiedReference = new AtomicReference<>();
        doAnswer(invocation -> {
            copiedReference.set(invocation.getArgument(0));
            return null;
        }).when(strategyDefinitionRepository).create(any());
        when(strategyDefinitionRepository.queryByStrategyId(anyLong())).thenAnswer(invocation -> {
            Long strategyId = invocation.getArgument(0);
            if (STRATEGY_ID.equals(strategyId)) {
                return Optional.of(source);
            }
            return Optional.of(withPersistenceTimes(copiedReference.get()));
        });

        // When
        StrategyDefinitionResult result = service.copyAsDraft(new CopyStrategyDefinitionCommand(STRATEGY_ID, "operator-002"));

        // Then
        StrategyDefinitionAggregate copied = copiedReference.get();
        assertAll(
                () -> assertNotEquals(STRATEGY_ID, copied.strategyId()),
                () -> assertEquals(StrategyDefinitionStateVO.DRAFT, copied.state()),
                () -> assertEquals(1, copied.version()),
                () -> assertEquals(copied.awards().get(0).awardCount(), copied.awards().get(0).awardCountSurplus()),
                () -> assertEquals(copied.strategyId(), copied.awards().get(0).strategyId()),
                () -> assertEquals(copied.strategyId(), result.strategyId()),
                () -> assertNotNull(result.createTime())
        );
    }

    /**
     * Given 当前策略仍是草稿，When 请求复制，Then 应提示配置操作非法且不创建新策略。
     */
    @Test
    void shouldRejectCopyingEditableDraft() {
        // Given
        StrategyDefinitionAggregate source = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "草稿策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(source));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.copyAsDraft(new CopyStrategyDefinitionCommand(STRATEGY_ID, "operator-002")));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception.getResponseCode());
        verify(strategyDefinitionRepository, never()).create(any());
    }

    /**
     * Given 可发布草稿，When 发布，Then 必须依次完成校验、预热和状态短事务。
     */
    @Test
    void shouldValidateAndWarmUpBeforePublishingStrategy() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "待发布策略");
        StrategyDefinitionAggregate active = aggregate(StrategyDefinitionStateVO.ACTIVE, 4, "待发布策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID))
                .thenReturn(Optional.of(draft), Optional.of(active));
        when(strategyArmory.assembleLotteryStrategy(STRATEGY_ID)).thenReturn(true);

        // When
        StrategyDefinitionResult result = service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 3, "operator-002"));

        // Then
        InOrder inOrder = inOrder(publicationValidator, strategyArmory, stateTransaction);
        inOrder.verify(publicationValidator).validate(draft);
        inOrder.verify(strategyArmory).assembleLotteryStrategy(STRATEGY_ID);
        ArgumentCaptor<StrategyDefinitionStateChange> captor = ArgumentCaptor.forClass(StrategyDefinitionStateChange.class);
        inOrder.verify(stateTransaction).changeState(captor.capture());
        assertAll(
                () -> assertEquals("active", result.state()),
                () -> assertEquals(StrategyDefinitionStateVO.DRAFT, captor.getValue().currentState()),
                () -> assertEquals(StrategyDefinitionStateVO.ACTIVE, captor.getValue().targetState()),
                () -> assertEquals(3, captor.getValue().expectedVersion())
        );
    }

    /**
     * Given 概率快照预热失败，When 发布策略，Then 不得将数据库状态推进为 active。
     */
    @Test
    void shouldNotPublishWhenRuntimeWarmUpFails() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "待发布策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(draft));
        when(strategyArmory.assembleLotteryStrategy(STRATEGY_ID)).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 3, "operator-002")));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, exception.getResponseCode());
        verify(publicationValidator).validate(draft);
        verify(stateTransaction, never()).changeState(any());
    }

    /**
     * Given 发布配置校验失败，When 发布策略，Then 不得装配概率快照或推进数据库状态。
     */
    @Test
    void shouldNotWarmUpOrPublishWhenPublicationValidationFails() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "待发布策略");
        BusinessException failure = BusinessException.of(BusinessResponseCode.STRATEGY_CONFIG_INVALID, STRATEGY_ID);
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(draft));
        doThrow(failure).when(publicationValidator).validate(draft);

        // When
        BusinessException actual = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 3, "operator-002")));

        // Then
        assertSame(failure, actual);
        verifyNoInteractions(strategyArmory, stateTransaction);
    }

    /**
     * Given 概率快照装配过程抛出系统异常，When 发布策略，Then 原异常向上暴露且数据库状态保持草稿。
     */
    @Test
    void shouldNotPublishWhenRuntimeWarmUpThrowsException() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "待发布策略");
        IllegalStateException failure = new IllegalStateException("redis unavailable");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(draft));
        when(strategyArmory.assembleLotteryStrategy(STRATEGY_ID)).thenThrow(failure);

        // When
        IllegalStateException actual = assertThrows(IllegalStateException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 3, "operator-002")));

        // Then
        assertSame(failure, actual);
        verify(publicationValidator).validate(draft);
        verifyNoInteractions(stateTransaction);
    }

    /**
     * Given 快照已完成预热但数据库状态或版本被并发修改，When 执行状态条件更新，Then 返回冲突且不伪造发布成功。
     */
    @Test
    void shouldExposeConflictWhenStateChangesAfterRuntimeWarmUp() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "待发布策略");
        BusinessException conflict = BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT, STRATEGY_ID, 3);
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(draft));
        when(strategyArmory.assembleLotteryStrategy(STRATEGY_ID)).thenReturn(true);
        doThrow(conflict).when(stateTransaction).changeState(any());

        // When
        BusinessException actual = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 3, "operator-002")));

        // Then
        assertSame(conflict, actual);
        InOrder inOrder = inOrder(publicationValidator, strategyArmory, stateTransaction);
        inOrder.verify(publicationValidator).validate(draft);
        inOrder.verify(strategyArmory).assembleLotteryStrategy(STRATEGY_ID);
        inOrder.verify(stateTransaction).changeState(any());
        verify(strategyDefinitionRepository).queryByStrategyId(STRATEGY_ID);
    }

    /**
     * Given 调用方持有过期版本，When 发布草稿策略，Then 应在校验和预热之前返回乐观锁冲突。
     */
    @Test
    void shouldRejectStalePublishBeforeValidationAndWarmUp() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 4, "待发布策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(draft));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 3, "operator-002")));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT, exception.getResponseCode());
        verifyNoInteractions(publicationValidator, strategyArmory, stateTransaction);
    }

    /**
     * Given 策略已经处于生效状态，When 重复提交发布请求，Then 拒绝非法状态迁移且不重复刷新快照。
     */
    @Test
    void shouldRejectRepeatedPublishWithoutRefreshingSnapshot() {
        // Given
        StrategyDefinitionAggregate active = aggregate(StrategyDefinitionStateVO.ACTIVE, 4, "已生效策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(active));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "active", 4, "operator-002")));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_STATE_ERROR, exception.getResponseCode());
        verifyNoInteractions(publicationValidator, strategyArmory, stateTransaction);
    }

    /**
     * Given 已生效策略，When 停用，Then 应只执行状态短事务而不重复校验和预热。
     */
    @Test
    void shouldDisableActiveStrategyWithoutWarmUp() {
        // Given
        StrategyDefinitionAggregate active = aggregate(StrategyDefinitionStateVO.ACTIVE, 4, "已生效策略");
        StrategyDefinitionAggregate disabled = aggregate(StrategyDefinitionStateVO.DISABLED, 5, "已生效策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID))
                .thenReturn(Optional.of(active), Optional.of(disabled));

        // When
        StrategyDefinitionResult result = service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "disabled", 4, "operator-002"));

        // Then
        assertEquals("disabled", result.state());
        verifyNoInteractions(publicationValidator, strategyArmory);
        verify(stateTransaction).changeState(any());
    }

    /**
     * Given 目标状态编码未知，When 变更生命周期，Then 应在校验和预热之前拒绝。
     */
    @Test
    void shouldRejectUnknownTargetState() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, 3, "草稿策略");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(draft));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeStrategyDefinitionStateCommand(STRATEGY_ID, "archived", 3, "operator-002")));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_STATE_ERROR, exception.getResponseCode());
        verifyNoInteractions(publicationValidator, strategyArmory, stateTransaction);
    }

    private CreateStrategyDefinitionCommand createCommand() {
        return new CreateStrategyDefinitionCommand("新建策略", "rule_weight", awardCommands(100), ruleCommands(), "operator-001");
    }

    private UpdateStrategyDefinitionCommand updateCommand(int expectedVersion) {
        return new UpdateStrategyDefinitionCommand(STRATEGY_ID, "修改后的策略", "rule_weight",
                awardCommands(150), ruleCommands(), expectedVersion, "operator-002");
    }

    private List<StrategyAwardConfigCommand> awardCommands(int count) {
        return List.of(
                new StrategyAwardConfigCommand(FIRST_AWARD_ID, "100积分", null, count,
                        new BigDecimal("30"), "rule_stock", 1),
                new StrategyAwardConfigCommand(SECOND_AWARD_ID, "优惠券", null, count,
                        new BigDecimal("70"), "rule_lock/rule_stock", 2)
        );
    }

    private List<StrategyRuleConfigCommand> ruleCommands() {
        return List.of(
                new StrategyRuleConfigCommand(null, 1, "rule_weight", "100:200001/200002", "权重奖池"),
                new StrategyRuleConfigCommand(SECOND_AWARD_ID, 2, "rule_lock", "1", "抽奖一次后解锁")
        );
    }

    private StrategyDefinitionAggregate aggregate(StrategyDefinitionStateVO state, int version, String description) {
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 25, 11, 0);
        List<StrategyAwardEntity> awards = List.of(
                new StrategyAwardEntity(STRATEGY_ID, FIRST_AWARD_ID, "100积分", null, 100, 80,
                        new BigDecimal("30"), "rule_stock", 1, createTime, updateTime),
                new StrategyAwardEntity(STRATEGY_ID, SECOND_AWARD_ID, "优惠券", null, 100, 60,
                        new BigDecimal("70"), "rule_lock/rule_stock", 2, createTime, updateTime)
        );
        List<StrategyRuleEntity> rules = List.of(
                new StrategyRuleEntity(STRATEGY_ID, null, 1, "rule_weight", "100:200001/200002",
                        "权重奖池", createTime, updateTime),
                new StrategyRuleEntity(STRATEGY_ID, SECOND_AWARD_ID, 2, "rule_lock", "1",
                        "抽奖一次后解锁", createTime, updateTime)
        );
        return new StrategyDefinitionAggregate(STRATEGY_ID, description, "rule_weight", state, version,
                "operator-001", "operator-001", createTime, updateTime, awards, rules);
    }

    private StrategyDefinitionAggregate withPersistenceTimes(StrategyDefinitionAggregate aggregate) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0);
        return new StrategyDefinitionAggregate(aggregate.strategyId(), aggregate.strategyDesc(), aggregate.ruleModels(),
                aggregate.state(), aggregate.version(), aggregate.createdBy(), aggregate.updatedBy(), now, now,
                aggregate.awards(), aggregate.rules());
    }
}
