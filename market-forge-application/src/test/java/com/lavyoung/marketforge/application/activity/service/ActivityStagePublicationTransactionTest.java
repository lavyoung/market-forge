package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.service.impl.ActivityStagePublicationTransaction;
import com.lavyoung.marketforge.application.tx.ITransactionExecutor;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动展台发布事务测试。
 *
 * <p>验证同一渠道来源下旧配置失效与新配置激活位于同一事务回调中，
 * 并覆盖目标快照过期和条件更新冲突。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@ExtendWith(MockitoExtension.class)
class ActivityStagePublicationTransactionTest {

    @Mock
    private IActivityStageRepository activityStageRepository;

    @Mock
    private ITransactionExecutor transactionExecutor;

    private ActivityStagePublicationTransaction publicationTransaction;

    @BeforeEach
    void setUp() {
        publicationTransaction = new ActivityStagePublicationTransaction(activityStageRepository, transactionExecutor);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionExecutor).execute(any(Runnable.class));
    }

    /**
     * 验证发布时先失效旧 ACTIVE，再将目标 CREATE 推进为 ACTIVE。
     */
    @Test
    void shouldSwitchActiveStageInsideTransaction() {
        // Given
        ActivityStageEntity oldActive = stage(900001L, 100001L, ActivityStageStateVO.ACTIVE, 4);
        ActivityStageEntity target = stage(900002L, 100002L, ActivityStageStateVO.CREATE, 2);
        when(activityStageRepository.lockByChannelAndSource("wechat", "campaign"))
                .thenReturn(List.of(oldActive, target));
        when(activityStageRepository.changeState(any())).thenReturn(true);

        // When
        publicationTransaction.publish(target, 2, "operator-002");

        // Then
        ArgumentCaptor<ActivityStageStateChange> captor = ArgumentCaptor.forClass(ActivityStageStateChange.class);
        verify(activityStageRepository, org.mockito.Mockito.times(2)).changeState(captor.capture());
        List<ActivityStageStateChange> changes = captor.getAllValues();
        assertAll(
                () -> assertEquals(ActivityStageStateVO.EXPIRED, changes.get(0).targetState()),
                () -> assertEquals(900001L, changes.get(0).stageId()),
                () -> assertEquals(ActivityStageStateVO.ACTIVE, changes.get(1).targetState()),
                () -> assertEquals(900002L, changes.get(1).stageId())
        );
        verify(transactionExecutor).execute(any(Runnable.class));
    }

    /**
     * 验证预热后的目标配置版本发生变化时拒绝发布。
     */
    @Test
    void shouldRejectStalePreparedStage() {
        // Given
        ActivityStageEntity prepared = stage(900002L, 100002L, ActivityStageStateVO.CREATE, 2);
        ActivityStageEntity changed = stage(900002L, 100003L, ActivityStageStateVO.CREATE, 3);
        when(activityStageRepository.lockByChannelAndSource("wechat", "campaign")).thenReturn(List.of(changed));

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> publicationTransaction.publish(prepared, 2, "operator-002"));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, exception.getResponseCode());
    }

    /**
     * 验证旧活动状态条件更新失败时终止整个切换流程。
     */
    @Test
    void shouldAbortWhenExpiringCurrentStageFails() {
        // Given
        ActivityStageEntity oldActive = stage(900001L, 100001L, ActivityStageStateVO.ACTIVE, 4);
        ActivityStageEntity target = stage(900002L, 100002L, ActivityStageStateVO.CREATE, 2);
        when(activityStageRepository.lockByChannelAndSource("wechat", "campaign"))
                .thenReturn(List.of(oldActive, target));
        when(activityStageRepository.changeState(any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> publicationTransaction.publish(target, 2, "operator-002"));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, exception.getResponseCode());
    }

    private ActivityStageEntity stage(Long stageId, Long activityId, ActivityStageStateVO state, int version) {
        return new ActivityStageEntity(
                stageId, "wechat", "campaign", activityId, state, version,
                "operator-001", "operator-001", null, null);
    }
}
