package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ChangeActivityDefinitionStateCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityDefinitionApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 活动定义应用服务测试。
 */
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionApplicationServiceImplTest {
    @Mock
    private IActivityDefinitionRepository repository;

    @Mock
    private IActivityCountDefinitionRepository activityCountDefinitionRepository;

    @Mock
    private IStrategyDefinitionRepository strategyDefinitionRepository;

    private ActivityDefinitionApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityDefinitionApplicationServiceImpl(repository, activityCountDefinitionRepository,
                strategyDefinitionRepository);
    }

    @Test
    void shouldCreateDraftWithoutActivityLevelStock() {
        // Given
        CreateActivityDefinitionCommand command = new CreateActivityDefinitionCommand("新活动", "活动说明",
                begin(), end(), 100301L, 100001L, "total", "admin");
        mockExistingReferences();
        when(repository.queryByActivityId(anyLong())).thenAnswer(invocation -> Optional.of(
                definition(invocation.getArgument(0), ActivityDefinitionStateVO.DRAFT, 1)));

        // When
        ActivityDefinitionResult result = service.create(command);

        // Then
        ArgumentCaptor<ActivityDefinitionEntity> captor = ArgumentCaptor.forClass(ActivityDefinitionEntity.class);
        verify(repository).create(captor.capture());
        assertAll(() -> assertEquals(18, captor.getValue().activityId().toString().length()),
                () -> assertEquals(100301L, captor.getValue().activityCountId()),
                () -> assertEquals(100001L, captor.getValue().strategyId()),
                () -> assertEquals(ActivityDefinitionStateVO.DRAFT, captor.getValue().state()),
                () -> assertEquals("draft", result.state()));
    }

    @Test
    void shouldRejectCreatingWhenActivityCountDefinitionDoesNotExist() {
        // Given
        CreateActivityDefinitionCommand command = createCommand();
        when(activityCountDefinitionRepository.queryByActivityCountId(100301L)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, exception.getResponseCode());
        verifyNoInteractions(strategyDefinitionRepository);
        verify(repository, never()).create(any());
    }

    @Test
    void shouldRejectCreatingWhenStrategyDefinitionDoesNotExist() {
        // Given
        CreateActivityDefinitionCommand command = createCommand();
        when(activityCountDefinitionRepository.queryByActivityCountId(100301L))
                .thenReturn(Optional.of(activityCountDefinition()));
        when(strategyDefinitionRepository.queryByStrategyId(100001L)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(command));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_NOT_FOUND, exception.getResponseCode());
        verify(repository, never()).create(any());
    }

    @Test
    void shouldRejectEditingOpenActivity() {
        // Given
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(
                definition(100301L, ActivityDefinitionStateVO.OPEN, 2)));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR, exception.getResponseCode());
        verify(repository, never()).updateDraft(any());
    }

    @Test
    void shouldRejectStaleVersionBeforeUpdate() {
        // Given
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(
                definition(100301L, ActivityDefinitionStateVO.DRAFT, 3)));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_DEFINITION_CONFLICT, exception.getResponseCode());
        verify(repository, never()).updateDraft(any());
    }

    @Test
    void shouldOpenDraftWithOptimisticLock() {
        // Given
        ActivityDefinitionEntity draft = definition(100301L, ActivityDefinitionStateVO.DRAFT, 1);
        ActivityDefinitionEntity opened = definition(100301L, ActivityDefinitionStateVO.OPEN, 2);
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(draft), Optional.of(opened));
        when(activityCountDefinitionRepository.lockByActivityCountId(100301L)).thenReturn(Optional.of(
                new ActivityCountDefinitionEntity(100301L, "基础次数包", 100, 10, 50, 1,
                        "admin", "admin", null, null)));
        when(repository.changeState(any())).thenReturn(true);

        // When
        ActivityDefinitionResult result = service.changeState(
                new ChangeActivityDefinitionStateCommand(100301L, "open", 1, "admin"));

        // Then
        ArgumentCaptor<ActivityDefinitionStateChange> captor = ArgumentCaptor.forClass(ActivityDefinitionStateChange.class);
        verify(repository).changeState(captor.capture());
        assertAll(() -> assertEquals(ActivityDefinitionStateVO.OPEN, captor.getValue().targetState()),
                () -> assertEquals(1, captor.getValue().expectedVersion()),
                () -> assertEquals("open", result.state()));
    }

    @Test
    void shouldRejectOpeningWhenActivityCountDefinitionDoesNotExist() {
        // Given
        ActivityDefinitionEntity draft = definition(100301L, ActivityDefinitionStateVO.DRAFT, 1);
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(draft));
        when(activityCountDefinitionRepository.lockByActivityCountId(100301L)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.changeState(
                new ChangeActivityDefinitionStateCommand(100301L, "open", 1, "admin")));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, exception.getResponseCode());
        verify(repository, never()).changeState(any());
    }

    @Test
    void shouldReportLostConditionalUpdateAsConflict() {
        // Given
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(
                definition(100301L, ActivityDefinitionStateVO.DRAFT, 1)));
        mockExistingReferences();
        when(repository.updateDraft(any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(1)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_DEFINITION_CONFLICT, exception.getResponseCode());
    }

    @Test
    void shouldUpdateDraftWhenReferencesExist() {
        // Given
        ActivityDefinitionEntity current = definition(100301L, ActivityDefinitionStateVO.DRAFT, 1);
        ActivityDefinitionEntity updated = definition(100301L, ActivityDefinitionStateVO.DRAFT, 2);
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(current), Optional.of(updated));
        mockExistingReferences();
        when(repository.updateDraft(any())).thenReturn(true);

        // When
        ActivityDefinitionResult result = service.updateDraft(updateCommand(1));

        // Then
        assertAll(
                () -> assertEquals(100301L, result.activityCountId()),
                () -> assertEquals(100001L, result.strategyId()),
                () -> assertEquals(2, result.version())
        );
        verify(repository).updateDraft(any(ActivityDefinitionEntity.class));
    }

    @Test
    void shouldRejectUpdatingWhenActivityCountDefinitionDoesNotExist() {
        // Given
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(
                definition(100301L, ActivityDefinitionStateVO.DRAFT, 1)));
        when(activityCountDefinitionRepository.queryByActivityCountId(100301L)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(1)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, exception.getResponseCode());
        verifyNoInteractions(strategyDefinitionRepository);
        verify(repository, never()).updateDraft(any());
    }

    @Test
    void shouldRejectUpdatingWhenStrategyDefinitionDoesNotExist() {
        // Given
        when(repository.queryByActivityId(100301L)).thenReturn(Optional.of(
                definition(100301L, ActivityDefinitionStateVO.DRAFT, 1)));
        when(activityCountDefinitionRepository.queryByActivityCountId(100301L))
                .thenReturn(Optional.of(activityCountDefinition()));
        when(strategyDefinitionRepository.queryByStrategyId(100001L)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateDraft(updateCommand(1)));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_NOT_FOUND, exception.getResponseCode());
        verify(repository, never()).updateDraft(any());
    }

    private CreateActivityDefinitionCommand createCommand() {
        return new CreateActivityDefinitionCommand("新活动", "活动说明", begin(), end(),
                100301L, 100001L, "total", "admin");
    }

    private void mockExistingReferences() {
        when(activityCountDefinitionRepository.queryByActivityCountId(100301L))
                .thenReturn(Optional.of(activityCountDefinition()));
        when(strategyDefinitionRepository.queryByStrategyId(100001L))
                .thenReturn(Optional.of(strategyDefinition()));
    }

    private ActivityCountDefinitionEntity activityCountDefinition() {
        return new ActivityCountDefinitionEntity(100301L, "基础次数包", 100, 10, 50, 1,
                "admin", "admin", null, null);
    }

    private StrategyDefinitionAggregate strategyDefinition() {
        return new StrategyDefinitionAggregate(100001L, "测试策略", null, StrategyDefinitionStateVO.DRAFT,
                1, "admin", "admin", null, null, List.of(), List.of());
    }

    private UpdateActivityDefinitionCommand updateCommand(int version) {
        return new UpdateActivityDefinitionCommand(100301L, "新活动", "活动说明", begin(), end(),
                100301L, 100001L, "total", version, "admin");
    }

    private ActivityDefinitionEntity definition(Long activityId, ActivityDefinitionStateVO state, int version) {
        return new ActivityDefinitionEntity(activityId, "新活动", "活动说明", begin(), end(), 100301L,
                100001L, AwardUnlockScopeVO.TOTAL, state, version, "admin", "admin", null, null);
    }

    private LocalDateTime begin() {
        return LocalDateTime.of(2026, 11, 1, 0, 0);
    }

    private LocalDateTime end() {
        return LocalDateTime.of(2026, 11, 30, 23, 59);
    }
}
