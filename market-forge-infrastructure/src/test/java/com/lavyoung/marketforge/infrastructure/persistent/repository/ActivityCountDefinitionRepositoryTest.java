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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void shouldQueryDefinitionsInOneBatchAndReturnImmutableList() {
        // Given
        Set<Long> activityCountIds = Set.of(100301L, 100302L);
        ActivityCountPO firstPO = new ActivityCountPO();
        ActivityCountPO secondPO = new ActivityCountPO();
        List<ActivityCountPO> poList = List.of(firstPO, secondPO);
        ActivityCountDefinitionEntity first = definition();
        ActivityCountDefinitionEntity second = new ActivityCountDefinitionEntity(
                100302L, "高级次数包", 200, 20, 100, 1, "admin", "admin", null, null);
        when(activityCountDao.queryByActivityCountIds(activityCountIds)).thenReturn(poList);
        when(assembler.toEntities(poList)).thenReturn(List.of(first, second));

        // When
        List<ActivityCountDefinitionEntity> actual = repository.queryByActivityCountIds(activityCountIds);

        // Then
        assertAll(
                () -> assertEquals(List.of(first, second), actual),
                () -> assertThrows(UnsupportedOperationException.class, () -> actual.add(first)),
                () -> verify(activityCountDao).queryByActivityCountIds(activityCountIds),
                () -> verify(assembler).toEntities(poList)
        );
    }

    @Test
    void shouldReturnEmptyListWithoutQueryingDatabase() {
        // When
        List<ActivityCountDefinitionEntity> actual = repository.queryByActivityCountIds(Set.of());

        // Then
        assertTrue(actual.isEmpty());
        verifyNoInteractions(activityCountDao, assembler);
    }

    @Test
    void shouldRejectInvalidBatchIdentifiersBeforeQueryingDatabase() {
        // Given
        Set<Long> idsWithNull = new HashSet<>();
        idsWithNull.add(100301L);
        idsWithNull.add(null);

        // When
        NullPointerException nullSetException = assertThrows(NullPointerException.class,
                () -> repository.queryByActivityCountIds(null));
        NullPointerException nullElementException = assertThrows(NullPointerException.class,
                () -> repository.queryByActivityCountIds(idsWithNull));
        IllegalArgumentException nonPositiveException = assertThrows(IllegalArgumentException.class,
                () -> repository.queryByActivityCountIds(Set.of(0L)));

        // Then
        assertAll(
                () -> assertEquals("activityCountIds must not be null", nullSetException.getMessage()),
                () -> assertNotNull(nullElementException),
                () -> assertEquals("activityCountIds must contain only positive identifiers", nonPositiveException.getMessage()),
                () -> verifyNoInteractions(activityCountDao, assembler)
        );
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
