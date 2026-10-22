package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动定义仓储测试。
 */
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionRepositoryTest {
    @Mock
    private IActivityDao activityDao;
    @Mock
    private ActivityDefinitionAssembler assembler;
    @Mock
    private IRedisService redisService;

    private ActivityDefinitionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ActivityDefinitionRepository(activityDao, assembler, redisService);
    }

    @Test
    void shouldTranslateDuplicateKeyOnCreate() {
        // Given
        ActivityDefinitionEntity definition = definition(ActivityDefinitionStateVO.DRAFT);
        ActivityPO po = new ActivityPO();
        when(assembler.toPO(definition)).thenReturn(po);
        when(activityDao.insert(po)).thenThrow(new DuplicateKeyException("duplicate"));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(definition));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_DEFINITION_DUPLICATED, exception.getResponseCode());
    }

    @Test
    void shouldEvictCacheOnlyAfterSuccessfulDraftUpdate() {
        // Given
        ActivityDefinitionEntity definition = definition(ActivityDefinitionStateVO.DRAFT);
        ActivityPO po = new ActivityPO();
        when(assembler.toPO(definition)).thenReturn(po);
        when(activityDao.updateDraftDefinition(po)).thenReturn(1, 0);

        // When
        boolean first = repository.updateDraft(definition);
        boolean second = repository.updateDraft(definition);

        // Then
        assertAll(() -> assertTrue(first), () -> assertFalse(second));
        verify(redisService, times(1)).delete(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + 100301L);
    }

    @Test
    void shouldPassStateAndVersionToDaoAndEvictCache() {
        // Given
        ActivityDefinitionStateChange change = new ActivityDefinitionStateChange(100301L,
                ActivityDefinitionStateVO.DRAFT, ActivityDefinitionStateVO.OPEN, 2, "admin");
        when(activityDao.changeDefinitionState(any())).thenReturn(1);

        // When
        boolean result = repository.changeState(change);

        // Then
        ArgumentCaptor<ActivityDefinitionStateUpdateParam> captor = ArgumentCaptor.forClass(ActivityDefinitionStateUpdateParam.class);
        verify(activityDao).changeDefinitionState(captor.capture());
        assertAll(() -> assertTrue(result),
                () -> assertEquals("draft", captor.getValue().currentState()),
                () -> assertEquals("open", captor.getValue().targetState()),
                () -> assertEquals(2, captor.getValue().expectedVersion()));
        verify(redisService).delete(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + 100301L);
    }

    private ActivityDefinitionEntity definition(ActivityDefinitionStateVO state) {
        return new ActivityDefinitionEntity(100301L, "活动", "活动说明",
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 30, 23, 59),
                1000, 1000, 100301L, 100001L, AwardUnlockScopeVO.TOTAL, state, 1,
                "admin", "admin", null, null);
    }
}
