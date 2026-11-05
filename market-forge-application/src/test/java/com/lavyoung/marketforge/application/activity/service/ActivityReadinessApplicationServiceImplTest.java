package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityReadinessResult;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityReadinessApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessIssueVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessReportVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.service.IActivityDefinitionReadinessChecker;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动开放就绪检查应用服务测试。
 *
 * <p>验证固定业务时间、活动读取、领域检查调用、结果转换和异常边界。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityReadinessApplicationServiceImplTest {

    private static final Long ACTIVITY_ID = 100301L;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-05T04:00:00Z"), BUSINESS_ZONE);
    private static final LocalDateTime CHECKED_AT = LocalDateTime.of(2026, 11, 5, 12, 0);

    @Mock
    private IActivityDefinitionRepository activityDefinitionRepository;

    @Mock
    private IActivityDefinitionReadinessChecker readinessChecker;

    private ActivityReadinessApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityReadinessApplicationServiceImpl(
                activityDefinitionRepository, readinessChecker, FIXED_CLOCK);
    }

    @Test
    void shouldCheckActivityAtFixedTimeAndMapAllIssues() {
        // Given
        ActivityDefinitionEntity definition = definition();
        ActivityReadinessReportVO report = new ActivityReadinessReportVO(
                ACTIVITY_ID,
                List.of(new ActivityReadinessIssueVO("activity.sku.missing", "skus", "缺少活动 SKU")),
                List.of(new ActivityReadinessIssueVO("activity.time.started", "beginDateTime", "活动已经开始")),
                List.of(new ActivityReadinessIssueVO("activity.sku.credit", "skus[0].creditAmount", "建议配置积分价格")));
        when(activityDefinitionRepository.queryByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(definition));
        when(readinessChecker.check(definition, CHECKED_AT)).thenReturn(report);

        // When
        ActivityReadinessResult result = service.check(ACTIVITY_ID);

        // Then
        assertAll(
                () -> assertEquals(ACTIVITY_ID, result.activityId()),
                () -> assertEquals(CHECKED_AT, result.checkedAt()),
                () -> assertFalse(result.ready()),
                () -> assertEquals("activity.sku.missing", result.errors().get(0).code()),
                () -> assertEquals("beginDateTime", result.warnings().get(0).path()),
                () -> assertEquals("建议配置积分价格", result.suggestions().get(0).message()),
                () -> verify(readinessChecker).check(definition, CHECKED_AT)
        );
    }

    @Test
    void shouldRejectMissingActivityWithoutCallingDomainChecker() {
        // Given
        when(activityDefinitionRepository.queryByActivityId(ACTIVITY_ID)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.check(ACTIVITY_ID));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, exception.getResponseCode()),
                () -> verifyNoInteractions(readinessChecker)
        );
    }

    @Test
    void shouldRejectInvalidActivityIdBeforeReadingRepository() {
        // When
        NullPointerException nullException = assertThrows(NullPointerException.class, () -> service.check(null));
        IllegalArgumentException nonPositiveException = assertThrows(IllegalArgumentException.class,
                () -> service.check(0L));

        // Then
        assertAll(
                () -> assertEquals("activityId must not be null", nullException.getMessage()),
                () -> assertEquals("activityId must be positive", nonPositiveException.getMessage()),
                () -> verifyNoInteractions(activityDefinitionRepository, readinessChecker)
        );
    }

    @Test
    void shouldPropagateSystemFailureFromDomainChecker() {
        // Given
        ActivityDefinitionEntity definition = definition();
        IllegalStateException failure = new IllegalStateException("probability service unavailable");
        when(activityDefinitionRepository.queryByActivityId(ACTIVITY_ID)).thenReturn(Optional.of(definition));
        when(readinessChecker.check(definition, CHECKED_AT)).thenThrow(failure);

        // When
        IllegalStateException actual = assertThrows(IllegalStateException.class, () -> service.check(ACTIVITY_ID));

        // Then
        assertSame(failure, actual);
    }

    private ActivityDefinitionEntity definition() {
        return new ActivityDefinitionEntity(
                ACTIVITY_ID, "测试活动", "活动就绪检查", CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10),
                100302L, 100303L, AwardUnlockScopeVO.TOTAL, ActivityDefinitionStateVO.DRAFT, 1,
                "admin", "admin", null, null);
    }
}
