package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityCountDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityCountDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 活动次数配置运营仓储测试。
 */
@ExtendWith(MockitoExtension.class)
class ActivityCountDefinitionRepositoryTest {

    @Mock
    private IActivityCountDao activityCountDao;

    @Mock
    private ActivityCountDefinitionAssembler assembler;

    private ActivityCountDefinitionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ActivityCountDefinitionRepository(activityCountDao, assembler);
    }

    @Test
    void shouldQueryAndConvertDefinition() {
        // Given
        ActivityCountPO po = new ActivityCountPO();
        ActivityCountDefinitionEntity expected = definition();
        when(activityCountDao.queryByActivityCountId(100301L)).thenReturn(Optional.of(po));
        when(assembler.toEntity(po)).thenReturn(expected);

        // When
        Optional<ActivityCountDefinitionEntity> actual = repository.queryByActivityCountId(100301L);

        // Then
        assertEquals(Optional.of(expected), actual);
    }

    @Test
    void shouldLockAndConvertDefinition() {
        // Given
        ActivityCountPO po = new ActivityCountPO();
        ActivityCountDefinitionEntity expected = definition();
        when(activityCountDao.lockByActivityCountId(100301L)).thenReturn(Optional.of(po));
        when(assembler.toEntity(po)).thenReturn(expected);

        // When
        Optional<ActivityCountDefinitionEntity> actual = repository.lockByActivityCountId(100301L);

        // Then
        assertEquals(Optional.of(expected), actual);
    }

    @Test
    void shouldTranslateDuplicateKeyOnCreate() {
        // Given
        ActivityCountDefinitionEntity definition = definition();
        ActivityCountPO po = new ActivityCountPO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate activity count");
        when(assembler.toPO(definition)).thenReturn(po);
        when(activityCountDao.insert(po)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(definition));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_DUPLICATED, exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    void shouldDelegateOpenActivityReferenceCheck() {
        // Given
        when(activityCountDao.existsOpenActivityReference(100301L)).thenReturn(true);

        // When & Then
        assertTrue(repository.existsOpenActivityReference(100301L));
    }

    @Test
    void shouldReturnConditionalUpdateResult() {
        // Given
        ActivityCountDefinitionEntity definition = definition();
        ActivityCountPO po = new ActivityCountPO();
        when(assembler.toPO(definition)).thenReturn(po);
        when(activityCountDao.updateIfNotReferencedByOpenActivity(po)).thenReturn(1, 0);

        // When
        boolean first = repository.updateIfNotReferencedByOpenActivity(definition);
        boolean second = repository.updateIfNotReferencedByOpenActivity(definition);

        // Then
        assertAll(() -> assertTrue(first), () -> assertFalse(second));
    }

    private ActivityCountDefinitionEntity definition() {
        return new ActivityCountDefinitionEntity(
                100301L, "签到次数包", 100, 10, 50, 1, "admin", "admin", null, null);
    }
}
