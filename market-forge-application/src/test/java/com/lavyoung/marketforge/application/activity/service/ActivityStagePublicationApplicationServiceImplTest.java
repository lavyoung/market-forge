package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityReadinessIssueResult;
import com.lavyoung.marketforge.application.activity.model.ActivityReadinessResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStagePublicationResult;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityStagePublicationApplicationServiceImpl;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityStagePublicationTransaction;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.*;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动展台发布应用服务测试。
 *
 * <p>验证首次发布、成功重放、处理中幂等、失败重试、并发竞争以及检查、预热、切换
 * 三个阶段的失败留痕，确保只有取得发布任务执行权的请求才能产生副作用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@ExtendWith(MockitoExtension.class)
class ActivityStagePublicationApplicationServiceImplTest {

    private static final Long STAGE_ID = 900001L;
    private static final Long PUBLICATION_ID = 910001L;
    private static final Long ACTIVITY_ID = 100001L;
    private static final int STAGE_VERSION = 1;
    private static final String OPERATOR = "operator-002";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-09T04:00:00Z"), BUSINESS_ZONE);
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 11, 9, 12, 0);

    @Mock
    private IActivityStageRepository activityStageRepository;

    @Mock
    private IActivityStagePublicationRepository activityStagePublicationRepository;

    @Mock
    private IActivityRepository activityRepository;

    @Mock
    private IActivityReadinessApplicationService activityReadinessApplicationService;

    @Mock
    private IActivityRaffleApplicationService activityRaffleApplicationService;

    @Mock
    private ActivityStagePublicationTransaction publicationTransaction;

    private ActivityStagePublicationApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ActivityStagePublicationApplicationServiceImpl(
                activityStageRepository, activityStagePublicationRepository, activityRepository,
                activityReadinessApplicationService, activityRaffleApplicationService, publicationTransaction,
                FIXED_CLOCK);
    }

    /**
     * 验证首次请求会创建任务、按顺序推进阶段并通过原子事务完成发布。
     */
    @Test
    void shouldCreateAndCompleteFirstPublication() {
        // Given
        ActivityStageEntity stage = draftStage();
        stubNewExecutablePublication(stage);
        when(activityStagePublicationRepository.transition(any(), any())).thenReturn(true);

        // When
        ActivityStagePublicationResult result = applicationService.publish(command());

        // Then
        ArgumentCaptor<ActivityStagePublicationEntity> switchingCaptor =
                ArgumentCaptor.forClass(ActivityStagePublicationEntity.class);
        ArgumentCaptor<ActivityStagePublicationEntity> succeededCaptor =
                ArgumentCaptor.forClass(ActivityStagePublicationEntity.class);
        verify(publicationTransaction).publish(any(ActivityStageEntity.class), switchingCaptor.capture(),
                succeededCaptor.capture());
        assertAll(
                () -> assertEquals("succeeded", result.state()),
                () -> assertEquals("completed", result.phase()),
                () -> assertEquals(1, result.attemptCount()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.SWITCHING, switchingCaptor.getValue().phase()),
                () -> assertEquals(ActivityStagePublicationStateVO.SUCCEEDED, succeededCaptor.getValue().state()),
                () -> verify(activityRaffleApplicationService).armory(ACTIVITY_ID),
                () -> verify(activityStagePublicationRepository, times(2)).transition(any(), any())
        );
    }

    /**
     * 验证成功任务重放时，即使展台已经激活并递增版本，也直接返回原成功结果。
     */
    @Test
    void shouldReturnSucceededPublicationWithoutRecheckingActiveStage() {
        // Given
        ActivityStagePublicationEntity succeeded = succeededPublication();
        when(activityStageRepository.queryByStageId(STAGE_ID)).thenReturn(Optional.of(activeStage()));
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.of(succeeded));

        // When
        ActivityStagePublicationResult result = applicationService.publish(command());

        // Then
        assertAll(
                () -> assertEquals("succeeded", result.state()),
                () -> assertEquals(PUBLICATION_ID, result.publicationId()),
                () -> verifyNoInteractions(activityRepository, activityReadinessApplicationService,
                        activityRaffleApplicationService, publicationTransaction),
                () -> verify(activityStagePublicationRepository, never()).createIfAbsent(any()),
                () -> verify(activityStagePublicationRepository, never()).transition(any(), any())
        );
    }

    /**
     * 验证发现其他请求正在处理时只返回进度，不重复执行发布副作用。
     */
    @Test
    void shouldReturnProcessingPublicationWithoutDuplicatingSideEffects() {
        // Given
        ActivityStagePublicationEntity processing = processingPublication();
        when(activityStageRepository.queryByStageId(STAGE_ID)).thenReturn(Optional.of(draftStage()));
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.of(processing));

        // When
        ActivityStagePublicationResult result = applicationService.publish(command());

        // Then
        assertAll(
                () -> assertEquals("processing", result.state()),
                () -> assertEquals("checking", result.phase()),
                () -> verifyNoInteractions(activityRepository, activityReadinessApplicationService,
                        activityRaffleApplicationService, publicationTransaction),
                () -> verify(activityStagePublicationRepository, never()).transition(any(), any())
        );
    }

    /**
     * 验证失败任务复用原记录重试，并在成功取得执行权后完成第二次发布尝试。
     */
    @Test
    void shouldRetryFailedPublicationAndIncrementAttemptCount() {
        // Given
        ActivityStagePublicationEntity failed = failedPublication();
        when(activityStageRepository.queryByStageId(STAGE_ID)).thenReturn(Optional.of(draftStage()));
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.of(failed));
        when(activityStagePublicationRepository.transition(any(), any())).thenReturn(true);
        stubPublishDependencies();

        // When
        ActivityStagePublicationResult result = applicationService.publish(command());

        // Then
        assertAll(
                () -> assertEquals("succeeded", result.state()),
                () -> assertEquals(2, result.attemptCount()),
                () -> verify(activityStagePublicationRepository, times(3)).transition(any(), any()),
                () -> verify(publicationTransaction).publish(any(ActivityStageEntity.class),
                        any(ActivityStagePublicationEntity.class), any(ActivityStagePublicationEntity.class))
        );
    }

    /**
     * 验证失败任务重试竞争失败时返回其他请求推进后的最新进度。
     */
    @Test
    void shouldReturnLatestProgressWhenRetryOwnershipIsLost() {
        // Given
        ActivityStagePublicationEntity failed = failedPublication();
        ActivityStagePublicationEntity retried = failed.retry("operator-003", FIXED_TIME.plusMinutes(1));
        when(activityStageRepository.queryByStageId(STAGE_ID)).thenReturn(Optional.of(draftStage()));
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.of(failed), Optional.of(retried));
        when(activityStagePublicationRepository.transition(failed, failed.retry(OPERATOR, FIXED_TIME)))
                .thenReturn(false);

        // When
        ActivityStagePublicationResult result = applicationService.publish(command());

        // Then
        assertAll(
                () -> assertEquals("processing", result.state()),
                () -> assertEquals(2, result.attemptCount()),
                () -> assertEquals("operator-003", result.operator()),
                () -> verifyNoInteractions(activityRepository, activityReadinessApplicationService,
                        activityRaffleApplicationService, publicationTransaction)
        );
    }

    /**
     * 验证首次任务创建竞争失败后复用并发请求创建的任务，不重复执行副作用。
     */
    @Test
    void shouldReuseConcurrentPublicationWhenCreateOwnershipIsLost() {
        // Given
        ActivityStagePublicationEntity concurrent = processingPublication();
        when(activityStageRepository.queryByStageId(STAGE_ID)).thenReturn(Optional.of(draftStage()));
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(activityStagePublicationRepository.createIfAbsent(any())).thenReturn(false);

        // When
        ActivityStagePublicationResult result = applicationService.publish(command());

        // Then
        assertAll(
                () -> assertEquals(PUBLICATION_ID, result.publicationId()),
                () -> assertEquals("processing", result.state()),
                () -> verifyNoInteractions(activityRepository, activityReadinessApplicationService,
                        activityRaffleApplicationService, publicationTransaction)
        );
    }

    /**
     * 验证配置检查失败时记录在 checking 阶段，且不执行预热和切换。
     */
    @Test
    void shouldRecordCheckingFailureWhenActivityIsNotReady() {
        // Given
        ActivityStageEntity stage = draftStage();
        stubNewPublication(stage);
        when(activityRepository.getActivityEntityByIdActivityId(ACTIVITY_ID)).thenReturn(openActivity());
        when(activityReadinessApplicationService.check(ACTIVITY_ID)).thenReturn(notReadyReport());
        when(activityStagePublicationRepository.transition(any(), any())).thenReturn(true);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.publish(command()));

        // Then
        ArgumentCaptor<ActivityStagePublicationEntity> targetCaptor =
                ArgumentCaptor.forClass(ActivityStagePublicationEntity.class);
        verify(activityStagePublicationRepository).transition(any(), targetCaptor.capture());
        ActivityStagePublicationEntity failed = targetCaptor.getValue();
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_DEFINITION_NOT_READY, exception.getResponseCode()),
                () -> assertEquals(ActivityStagePublicationStateVO.FAILED, failed.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.CHECKING, failed.phase()),
                () -> assertEquals(BusinessResponseCode.ACTIVITY_DEFINITION_NOT_READY.getI18nKey(),
                        failed.failureCode()),
                () -> verifyNoInteractions(activityRaffleApplicationService, publicationTransaction)
        );
    }

    /**
     * 验证预热失败时记录在 preheating 阶段，不调用展台切换事务。
     */
    @Test
    void shouldRecordPreheatingFailureWhenArmoryFails() {
        // Given
        ActivityStageEntity stage = draftStage();
        stubNewExecutablePublication(stage);
        BusinessException assemblyFailure =
                BusinessException.of(BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED, ACTIVITY_ID);
        doThrow(assemblyFailure).when(activityRaffleApplicationService).armory(ACTIVITY_ID);
        when(activityStagePublicationRepository.transition(any(), any())).thenReturn(true);

        // When
        BusinessException actual = assertThrows(BusinessException.class,
                () -> applicationService.publish(command()));

        // Then
        ArgumentCaptor<ActivityStagePublicationEntity> targetCaptor =
                ArgumentCaptor.forClass(ActivityStagePublicationEntity.class);
        verify(activityStagePublicationRepository, times(2)).transition(any(), targetCaptor.capture());
        ActivityStagePublicationEntity failed = targetCaptor.getAllValues().get(1);
        assertAll(
                () -> assertSame(assemblyFailure, actual),
                () -> assertEquals(ActivityStagePublicationStateVO.FAILED, failed.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.PREHEATING, failed.phase()),
                () -> verify(publicationTransaction, never()).publish(any(ActivityStageEntity.class),
                        any(ActivityStagePublicationEntity.class), any(ActivityStagePublicationEntity.class))
        );
    }

    /**
     * 验证展台切换失败时记录在 switching 阶段，并保留原业务异常。
     */
    @Test
    void shouldRecordSwitchingFailureWhenTransactionFails() {
        // Given
        ActivityStageEntity stage = draftStage();
        stubNewExecutablePublication(stage);
        BusinessException switchFailure =
                BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, STAGE_ID, STAGE_VERSION);
        doThrow(switchFailure).when(publicationTransaction).publish(any(ActivityStageEntity.class),
                any(ActivityStagePublicationEntity.class), any(ActivityStagePublicationEntity.class));
        when(activityStagePublicationRepository.transition(any(), any())).thenReturn(true);

        // When
        BusinessException actual = assertThrows(BusinessException.class,
                () -> applicationService.publish(command()));

        // Then
        ArgumentCaptor<ActivityStagePublicationEntity> targetCaptor =
                ArgumentCaptor.forClass(ActivityStagePublicationEntity.class);
        verify(activityStagePublicationRepository, times(3)).transition(any(), targetCaptor.capture());
        ActivityStagePublicationEntity failed = targetCaptor.getAllValues().get(2);
        assertAll(
                () -> assertSame(switchFailure, actual),
                () -> assertEquals(ActivityStagePublicationStateVO.FAILED, failed.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.SWITCHING, failed.phase()),
                () -> assertEquals(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT.getI18nKey(),
                        failed.failureCode())
        );
    }

    /**
     * 验证阶段条件更新发生并发冲突时不能用旧快照覆盖为失败状态。
     */
    @Test
    void shouldNotOverwritePublicationWhenPhaseAdvanceConflicts() {
        // Given
        ActivityStageEntity stage = draftStage();
        stubNewExecutablePublication(stage);
        when(activityStagePublicationRepository.transition(any(), any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.publish(command()));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CONFLICT,
                        exception.getResponseCode()),
                () -> verify(activityStagePublicationRepository).transition(any(), any()),
                () -> verifyNoInteractions(activityRaffleApplicationService, publicationTransaction)
        );
    }

    /**
     * 验证查询不存在的发布任务时返回发布任务专属错误码。
     */
    @Test
    void shouldRejectMissingPublicationQuery() {
        // Given
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.query(STAGE_ID, STAGE_VERSION));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_NOT_FOUND,
                exception.getResponseCode());
    }

    private void stubNewPublication(ActivityStageEntity stage) {
        when(activityStageRepository.queryByStageId(STAGE_ID)).thenReturn(Optional.of(stage));
        when(activityStagePublicationRepository.queryByStageAndVersion(STAGE_ID, STAGE_VERSION))
                .thenReturn(Optional.empty());
        when(activityStagePublicationRepository.createIfAbsent(any())).thenReturn(true);
    }

    private void stubNewExecutablePublication(ActivityStageEntity stage) {
        stubNewPublication(stage);
        stubPublishDependencies();
    }

    private void stubPublishDependencies() {
        when(activityRepository.getActivityEntityByIdActivityId(ACTIVITY_ID)).thenReturn(openActivity());
        when(activityReadinessApplicationService.check(ACTIVITY_ID)).thenReturn(readyReport());
    }

    private PublishActivityStageCommand command() {
        return new PublishActivityStageCommand(STAGE_ID, STAGE_VERSION, OPERATOR);
    }

    private ActivityStageEntity draftStage() {
        return stage(ActivityStageStateVO.CREATE, STAGE_VERSION);
    }

    private ActivityStageEntity activeStage() {
        return stage(ActivityStageStateVO.ACTIVE, STAGE_VERSION + 1);
    }

    private ActivityStageEntity stage(ActivityStageStateVO state, int version) {
        return new ActivityStageEntity(
                STAGE_ID, "wechat", "campaign", ACTIVITY_ID, state, version,
                "operator-001", OPERATOR, FIXED_TIME.minusDays(1), FIXED_TIME);
    }

    private ActivityStagePublicationEntity processingPublication() {
        return ActivityStagePublicationEntity.start(PUBLICATION_ID, draftStage(), OPERATOR, FIXED_TIME.minusMinutes(5));
    }

    private ActivityStagePublicationEntity failedPublication() {
        return processingPublication().fail(
                BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED.getI18nKey(),
                BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED.getMsg(), OPERATOR, FIXED_TIME.minusMinutes(1));
    }

    private ActivityStagePublicationEntity succeededPublication() {
        ActivityStagePublicationEntity preheating = processingPublication().advance(
                ActivityStagePublicationPhaseVO.PREHEATING, OPERATOR, FIXED_TIME.minusMinutes(4));
        ActivityStagePublicationEntity switching = preheating.advance(
                ActivityStagePublicationPhaseVO.SWITCHING, OPERATOR, FIXED_TIME.minusMinutes(3));
        return switching.succeed(OPERATOR, FIXED_TIME.minusMinutes(2));
    }

    private ActivityEntity openActivity() {
        return new ActivityEntity(
                ACTIVITY_ID, "测试活动", "展台发布测试", FIXED_TIME.minusDays(1), FIXED_TIME.plusDays(7),
                200001L, 300001L, AwardUnlockScopeVO.TOTAL, ActivityStateVO.OPEN.getCode());
    }

    private ActivityReadinessResult readyReport() {
        return new ActivityReadinessResult(ACTIVITY_ID, FIXED_TIME, List.of(), List.of(), List.of());
    }

    private ActivityReadinessResult notReadyReport() {
        ActivityReadinessIssueResult issue = new ActivityReadinessIssueResult(
                "activity.sku.missing", "skus", "活动至少需要配置一个额度 SKU");
        return new ActivityReadinessResult(ACTIVITY_ID, FIXED_TIME, List.of(issue), List.of(), List.of());
    }
}
