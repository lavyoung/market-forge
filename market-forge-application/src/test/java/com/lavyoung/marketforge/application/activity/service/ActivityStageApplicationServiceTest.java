package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityStageApplicationServiceImpl;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityStagePublicationTransaction;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 活动展台应用服务测试。
 *
 * <p>验证草稿创建与修改、发布前活动校验、预热和事务切换顺序，
 * 以及用户端按渠道来源解析当前活动。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@ExtendWith(MockitoExtension.class)
class ActivityStageApplicationServiceTest {

    @Mock
    private IActivityStageRepository activityStageRepository;

    @Mock
    private IActivityRepository activityRepository;

    @Mock
    private IActivityRaffleApplicationService activityRaffleApplicationService;

    @Mock
    private ActivityStagePublicationTransaction publicationTransaction;

    private ActivityStageApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ActivityStageApplicationServiceImpl(
                activityStageRepository, activityRepository, activityRaffleApplicationService, publicationTransaction);
    }

    /**
     * 验证创建命令会生成业务标识并以 CREATE 状态保存。
     */
    @Test
    void shouldCreateDraftStage() {
        // Given
        AtomicReference<ActivityStageEntity> savedStage = new AtomicReference<>();
        doAnswer(invocation -> {
            savedStage.set(invocation.getArgument(0, ActivityStageEntity.class));
            return null;
        }).when(activityStageRepository).create(any());
        when(activityStageRepository.queryByStageId(anyLong()))
                .thenAnswer(invocation -> Optional.of(savedStage.get()));

        // When
        ActivityStageResult result = applicationService.create(
                new CreateActivityStageCommand("wechat", "campaign", 100001L, "operator-001"));

        // Then
        assertAll(
                () -> assertTrue(result.stageId() > 0),
                () -> assertEquals("create", result.state()),
                () -> assertEquals(1, result.version()),
                () -> assertEquals(100001L, result.activityId())
        );
    }

    /**
     * 验证草稿版本不一致时不会执行数据库更新。
     */
    @Test
    void shouldRejectDraftUpdateWithStaleVersion() {
        // Given
        when(activityStageRepository.queryByStageId(900001L))
                .thenReturn(Optional.of(stage(ActivityStageStateVO.CREATE, 2)));
        UpdateActivityStageCommand command = new UpdateActivityStageCommand(
                900001L, "app", "home", 100002L, 1, "operator-002");

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> applicationService.updateDraft(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, exception.getResponseCode());
        verify(activityStageRepository, never()).updateDraft(any());
    }

    /**
     * 验证活动预热成功后才执行展台发布事务。
     */
    @Test
    void shouldPreheatBeforePublishingStage() {
        // Given
        ActivityStageEntity draft = stage(ActivityStageStateVO.CREATE, 1);
        ActivityStageEntity active = stage(ActivityStageStateVO.ACTIVE, 2);
        when(activityStageRepository.queryByStageId(900001L))
                .thenReturn(Optional.of(draft), Optional.of(active));
        when(activityRepository.getActivityEntityByIdActivityId(100001L)).thenReturn(openActivity());

        // When
        ActivityStageResult result = applicationService.publish(
                new PublishActivityStageCommand(900001L, 1, "operator-002"));

        // Then
        InOrder inOrder = inOrder(activityRaffleApplicationService, publicationTransaction);
        inOrder.verify(activityRaffleApplicationService).armory(100001L);
        inOrder.verify(publicationTransaction).publish(draft, 1, "operator-002");
        assertEquals("active", result.state());
    }

    /**
     * 验证预热失败时不能执行数据库生效切换。
     */
    @Test
    void shouldNotPublishWhenPreheatingFails() {
        // Given
        ActivityStageEntity draft = stage(ActivityStageStateVO.CREATE, 1);
        when(activityStageRepository.queryByStageId(900001L)).thenReturn(Optional.of(draft));
        when(activityRepository.getActivityEntityByIdActivityId(100001L)).thenReturn(openActivity());
        doThrow(BusinessException.of(BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED, 100001L))
                .when(activityRaffleApplicationService).armory(100001L);

        // When & Then
        assertThrows(BusinessException.class, () -> applicationService.publish(
                new PublishActivityStageCommand(900001L, 1, "operator-002")));
        verify(publicationTransaction, never()).publish(any(), anyInt(), any());
    }

    /**
     * 验证操作人非法时必须在活动查询和缓存预热之前失败，避免产生无审计主体的副作用。
     */
    @Test
    void shouldRejectBlankOperatorBeforePreheating() {
        // Given
        PublishActivityStageCommand command = new PublishActivityStageCommand(900001L, 1, " ");

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> applicationService.publish(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception.getResponseCode());
        verify(activityStageRepository, never()).queryByStageId(anyLong());
        verify(activityRaffleApplicationService, never()).armory(anyLong());
        verify(publicationTransaction, never()).publish(any(), anyInt(), any());
    }

    /**
     * 验证用户端可以根据 SC 获取当前生效活动。
     */
    @Test
    void shouldResolveActiveActivityId() {
        // Given
        when(activityStageRepository.queryActive("wechat", "campaign"))
                .thenReturn(Optional.of(stage(ActivityStageStateVO.ACTIVE, 2)));

        // When
        Long activityId = applicationService.queryActiveActivityId("wechat", "campaign");

        // Then
        assertEquals(100001L, activityId);
    }

    private ActivityStageEntity stage(ActivityStageStateVO state, int version) {
        return new ActivityStageEntity(
                900001L, "wechat", "campaign", 100001L, state, version,
                "operator-001", "operator-002", null, null);
    }

    private ActivityEntity openActivity() {
        return new ActivityEntity(
                100001L, "测试活动", "发布测试", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7),
                1000, 1000, 200001L, 300001L, AwardUnlockScopeVO.TOTAL, ActivityStateVO.OPEN.getCode());
    }
}
