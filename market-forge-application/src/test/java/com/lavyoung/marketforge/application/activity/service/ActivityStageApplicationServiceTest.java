package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityStageApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 活动展台维护应用服务测试。
 *
 * <p>验证草稿创建与修改、当前生效活动解析，以及旧发布入口只负责委托新的发布应用服务。
 * 发布检查、预热、幂等与事务一致性由专门的发布应用服务测试覆盖。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/10
 */
@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
class ActivityStageApplicationServiceTest {

    @Mock
    private IActivityStageRepository activityStageRepository;

    @Mock
    private IActivityStagePublicationApplicationService activityStagePublicationApplicationService;

    private ActivityStageApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ActivityStageApplicationServiceImpl(
                activityStageRepository, activityStagePublicationApplicationService);
    }

    /**
     * 验证创建命令会生成业务标识并以待上架状态保存。
     */
    @Test
    void shouldCreateDraftStage() {
        // Given
        AtomicReference<ActivityStageEntity> savedStage = new AtomicReference<>();
        doAnswer(invocation -> {
            savedStage.set(invocation.getArgument(0, ActivityStageEntity.class));
            return null;
        }).when(activityStageRepository).create(any());
        when(activityStageRepository.queryByStageId(anyLong())).thenAnswer(invocation -> Optional.of(savedStage.get()));

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
        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.updateDraft(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, exception.getResponseCode());
        verify(activityStageRepository, never()).updateDraft(any());
    }

    /**
     * 验证旧发布入口只委托新的发布应用服务，并在执行后查询最新展台状态。
     */
    @Test
    void shouldDelegateLegacyPublishToPublicationApplicationService() {
        // Given
        PublishActivityStageCommand command = new PublishActivityStageCommand(900001L, 1, "operator-002");
        ActivityStageEntity active = stage(ActivityStageStateVO.ACTIVE, 2);
        when(activityStageRepository.queryByStageId(900001L)).thenReturn(Optional.of(active));

        // When
        ActivityStageResult result = applicationService.publish(command);

        // Then
        assertAll(
                () -> assertEquals("active", result.state()),
                () -> assertEquals(2, result.version()),
                () -> verify(activityStagePublicationApplicationService).publish(command),
                () -> verify(activityStageRepository).queryByStageId(900001L)
        );
    }

    /**
     * 验证发布服务失败时原异常直接向上抛出，并且不查询可能仍未变化的展台。
     */
    @Test
    void shouldPropagatePublicationFailureWithoutQueryingStage() {
        // Given
        PublishActivityStageCommand command = new PublishActivityStageCommand(900001L, 1, "operator-002");
        BusinessException failure = BusinessException.of(
                BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED, 100001L);
        when(activityStagePublicationApplicationService.publish(command)).thenThrow(failure);

        // When
        BusinessException actual = assertThrows(BusinessException.class,
                () -> applicationService.publish(command));

        // Then
        assertSame(failure, actual);
        verifyNoInteractions(activityStageRepository);
    }

    /**
     * 验证空发布命令在调用发布服务前被拒绝。
     */
    @Test
    void shouldRejectNullLegacyPublishCommand() {
        // When
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> applicationService.publish(null));

        // Then
        assertEquals("command must not be null", exception.getMessage());
        verifyNoInteractions(activityStageRepository, activityStagePublicationApplicationService);
    }

    /**
     * 验证用户端可以根据渠道和来源获取当前生效活动。
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
}
