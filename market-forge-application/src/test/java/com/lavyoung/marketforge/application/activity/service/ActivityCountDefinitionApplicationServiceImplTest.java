package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityCountDefinitionApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 活动次数配置运营应用服务测试。
 *
 * <p>覆盖创建、领域校验、开放活动引用保护、乐观锁预检和条件更新竞争失败。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityCountDefinitionApplicationServiceImplTest {

    @Mock
    private IActivityCountDefinitionRepository repository;

    private ActivityCountDefinitionApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityCountDefinitionApplicationServiceImpl(repository);
    }

    @Test
    void shouldCreateDefinitionWithGeneratedBusinessId() {
        // Given
        CreateActivityCountDefinitionCommand command = new CreateActivityCountDefinitionCommand(
                "签到次数包", 100, 10, 50, "admin");
        when(repository.queryByActivityCountId(anyLong())).thenAnswer(invocation -> Optional.of(
                definition(invocation.getArgument(0), 1, "admin")));

        // When
        ActivityCountDefinitionResult result = service.create(command);

        // Then
        ArgumentCaptor<ActivityCountDefinitionEntity> captor = ArgumentCaptor.forClass(ActivityCountDefinitionEntity.class);
        verify(repository).create(captor.capture());
        assertAll(
                () -> assertEquals(18, captor.getValue().activityCountId().toString().length()),
                () -> assertEquals("签到次数包", captor.getValue().activityCountName()),
                () -> assertEquals(1, captor.getValue().version()),
                () -> assertEquals(captor.getValue().activityCountId(), result.activityCountId())
        );
    }

    @Test
    void shouldTranslateInvalidCountRelationship() {
        // Given
        CreateActivityCountDefinitionCommand command = new CreateActivityCountDefinitionCommand(
                "错误次数包", 20, 10, 30, "admin");

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_INVALID, exception.getResponseCode());
        verify(repository, never()).create(any());
    }

    @Test
    void shouldRejectStaleVersionBeforeReferenceQuery() {
        // Given
        when(repository.queryByActivityCountId(100301L)).thenReturn(Optional.of(definition(100301L, 3, "admin")));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_CONFLICT, exception.getResponseCode());
        verify(repository, never()).existsOpenActivityReference(anyLong());
        verify(repository, never()).updateIfNotReferencedByOpenActivity(any());
    }

    @Test
    void shouldRejectDefinitionReferencedByOpenActivity() {
        // Given
        when(repository.queryByActivityCountId(100301L)).thenReturn(Optional.of(definition(100301L, 2, "admin")));
        when(repository.existsOpenActivityReference(100301L)).thenReturn(true);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_REFERENCED, exception.getResponseCode());
        verify(repository, never()).updateIfNotReferencedByOpenActivity(any());
    }

    @Test
    void shouldReportConditionalUpdateFailureAsConflict() {
        // Given
        when(repository.queryByActivityCountId(100301L)).thenReturn(Optional.of(definition(100301L, 2, "admin")));
        when(repository.existsOpenActivityReference(100301L)).thenReturn(false);
        when(repository.updateIfNotReferencedByOpenActivity(any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_CONFLICT, exception.getResponseCode());
    }

    @Test
    void shouldUpdateUnreferencedDefinition() {
        // Given
        ActivityCountDefinitionEntity current = definition(100301L, 2, "admin");
        ActivityCountDefinitionEntity persisted = new ActivityCountDefinitionEntity(
                100301L, "积分兑换次数包", 200, 20, 100, 3, "admin", "operator", null, null);
        when(repository.queryByActivityCountId(100301L)).thenReturn(Optional.of(current), Optional.of(persisted));
        when(repository.existsOpenActivityReference(100301L)).thenReturn(false);
        when(repository.updateIfNotReferencedByOpenActivity(any())).thenReturn(true);

        // When
        ActivityCountDefinitionResult result = service.update(updateCommand(2));

        // Then
        ArgumentCaptor<ActivityCountDefinitionEntity> captor = ArgumentCaptor.forClass(ActivityCountDefinitionEntity.class);
        verify(repository).updateIfNotReferencedByOpenActivity(captor.capture());
        assertAll(
                () -> assertEquals("积分兑换次数包", captor.getValue().activityCountName()),
                () -> assertEquals(2, captor.getValue().version()),
                () -> assertEquals("operator", captor.getValue().updatedBy()),
                () -> assertEquals(3, result.version())
        );
    }

    private UpdateActivityCountDefinitionCommand updateCommand(int version) {
        return new UpdateActivityCountDefinitionCommand(
                100301L, "积分兑换次数包", 200, 20, 100, version, "operator");
    }

    private ActivityCountDefinitionEntity definition(Long activityCountId, int version, String operator) {
        return new ActivityCountDefinitionEntity(
                activityCountId, "签到次数包", 100, 10, 50, version, "admin", operator, null, null);
    }
}
