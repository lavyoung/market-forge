package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.service.impl.ActivityStagePublicationTransaction;
import com.lavyoung.marketforge.application.tx.ITransactionExecutor;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStagePublicationRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动展台发布事务测试。
 *
 * <p>验证同一渠道来源下旧配置失效、新配置激活与发布记录成功状态位于同一事务回调中，
 * 并覆盖展台快照过期、发布记录条件冲突和非法成功快照。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@ExtendWith(MockitoExtension.class)
class ActivityStagePublicationTransactionTest {

    @Mock
    private IActivityStageRepository activityStageRepository;

    @Mock
    private IActivityStagePublicationRepository activityStagePublicationRepository;

    @Mock
    private ITransactionExecutor transactionExecutor;

    private ActivityStagePublicationTransaction publicationTransaction;

    @BeforeEach
    void setUp() {
        publicationTransaction = new ActivityStagePublicationTransaction(
                activityStageRepository, activityStagePublicationRepository, transactionExecutor);
    }

    /**
     * 验证发布时先失效旧 ACTIVE，再将目标 CREATE 推进为 ACTIVE。
     */
    @Test
    void shouldSwitchActiveStageInsideTransaction() {
        // Given
        executeTransactionCallback();
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
        verifyNoInteractions(activityStagePublicationRepository);
    }

    /**
     * 验证新发布入口会在切换展台后把发布记录推进为成功终态。
     */
    @Test
    void shouldSwitchStageAndCompletePublicationInsideTransaction() {
        // Given
        executeTransactionCallback();
        ActivityStageEntity oldActive = stage(900001L, 100001L, ActivityStageStateVO.ACTIVE, 4);
        ActivityStageEntity target = stage(900002L, 100002L, ActivityStageStateVO.CREATE, 2);
        ActivityStagePublicationEntity switching = switchingPublication();
        ActivityStagePublicationEntity succeeded = switching.succeed(
                "operator-002", LocalDateTime.of(2026, 11, 9, 10, 5));
        when(activityStageRepository.lockByChannelAndSource("wechat", "campaign"))
                .thenReturn(List.of(oldActive, target));
        when(activityStageRepository.changeState(any())).thenReturn(true);
        when(activityStagePublicationRepository.transition(switching, succeeded)).thenReturn(true);

        // When
        publicationTransaction.publish(target, switching, succeeded);

        // Then
        ArgumentCaptor<ActivityStageStateChange> captor = ArgumentCaptor.forClass(ActivityStageStateChange.class);
        verify(activityStageRepository, times(2)).changeState(captor.capture());
        List<ActivityStageStateChange> changes = captor.getAllValues();
        assertAll(
                () -> assertEquals(ActivityStageStateVO.EXPIRED, changes.get(0).targetState()),
                () -> assertEquals(ActivityStageStateVO.ACTIVE, changes.get(1).targetState()),
                () -> verify(activityStagePublicationRepository).transition(switching, succeeded),
                () -> verify(transactionExecutor).execute(any(Runnable.class))
        );
    }

    /**
     * 验证发布记录条件更新冲突会终止发布事务。
     */
    @Test
    void shouldAbortWhenCompletingPublicationConflicts() {
        // Given
        executeTransactionCallback();
        ActivityStageEntity target = stage(900002L, 100002L, ActivityStageStateVO.CREATE, 2);
        ActivityStagePublicationEntity switching = switchingPublication();
        ActivityStagePublicationEntity succeeded = switching.succeed(
                "operator-002", LocalDateTime.of(2026, 11, 9, 10, 5));
        when(activityStageRepository.lockByChannelAndSource("wechat", "campaign")).thenReturn(List.of(target));
        when(activityStageRepository.changeState(any())).thenReturn(true);
        when(activityStagePublicationRepository.transition(switching, succeeded)).thenReturn(false);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> publicationTransaction.publish(target, switching, succeeded));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CONFLICT, exception.getResponseCode());
        verify(activityStagePublicationRepository).transition(switching, succeeded);
    }

    /**
     * 验证展台与发布任务快照不一致时不会开启事务。
     */
    @Test
    void shouldRejectMismatchedStageSnapshotBeforeTransaction() {
        // Given
        ActivityStageEntity mismatchedStage = stage(900002L, 100003L, ActivityStageStateVO.CREATE, 2);
        ActivityStagePublicationEntity switching = switchingPublication();
        ActivityStagePublicationEntity succeeded = switching.succeed(
                "operator-002", LocalDateTime.of(2026, 11, 9, 10, 5));

        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> publicationTransaction.publish(mismatchedStage, switching, succeeded)),
                () -> verify(transactionExecutor, never()).execute(any(Runnable.class)),
                () -> verifyNoInteractions(activityStagePublicationRepository)
        );
    }

    /**
     * 验证调用方不能通过公开构造器伪造发布成功终态。
     */
    @Test
    void shouldRejectForgedSucceededPublicationBeforeTransaction() {
        // Given
        ActivityStageEntity target = stage(900002L, 100002L, ActivityStageStateVO.CREATE, 2);
        ActivityStagePublicationEntity switching = switchingPublication();
        LocalDateTime publishedAt = LocalDateTime.of(2026, 11, 9, 10, 5);
        ActivityStagePublicationEntity forged = new ActivityStagePublicationEntity(
                switching.publicationId(), switching.stageId(), switching.stageVersion(), switching.activityId(),
                switching.channel(), switching.source(), ActivityStagePublicationStateVO.SUCCEEDED,
                ActivityStagePublicationPhaseVO.COMPLETED, switching.attemptCount() + 1, "operator-002",
                null, null, publishedAt, switching.createTime(), publishedAt);

        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> publicationTransaction.publish(target, switching, forged)),
                () -> verify(transactionExecutor, never()).execute(any(Runnable.class)),
                () -> verifyNoInteractions(activityStagePublicationRepository)
        );
    }

    /**
     * 验证预热后的目标配置版本发生变化时拒绝发布。
     */
    @Test
    void shouldRejectStalePreparedStage() {
        // Given
        executeTransactionCallback();
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
        executeTransactionCallback();
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

    private void executeTransactionCallback() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionExecutor).execute(any(Runnable.class));
    }

    private ActivityStagePublicationEntity switchingPublication() {
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 9, 10, 0);
        return new ActivityStagePublicationEntity(
                910001L, 900002L, 2, 100002L, "wechat", "campaign",
                ActivityStagePublicationStateVO.PROCESSING, ActivityStagePublicationPhaseVO.SWITCHING,
                1, "operator-001", null, null, null, createTime, createTime.plusMinutes(4));
    }
}
