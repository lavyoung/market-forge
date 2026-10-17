package com.lavyoung.marketforge.domain.award.model.vo;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品解锁次数查询条件测试。
 *
 * <p>验证查询对象在进入仓储层前就能拦截无效用户、活动、
 * 统计范围和业务日期，避免基础设施层接收不完整的统计条件。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardUnlockCountQueryVOTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100301L;
    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 10, 17);

    /**
     * Given 完整的按日统计条件，When 创建查询值对象，Then 应完整保留所有业务字段。
     */
    @Test
    void shouldCreateValidDayScopeQuery() {
        // Given & When
        AwardUnlockCountQueryVO query = new AwardUnlockCountQueryVO(
                USER_ID,
                ACTIVITY_ID,
                AwardUnlockScopeVO.DAY,
                STATISTICS_DATE
        );

        // Then
        assertAll(
                () -> assertEquals(USER_ID, query.userId()),
                () -> assertEquals(ACTIVITY_ID, query.activityId()),
                () -> assertEquals(AwardUnlockScopeVO.DAY, query.unlockScope()),
                () -> assertEquals(STATISTICS_DATE, query.statisticsDate())
        );
    }

    /**
     * Given 缺少必填字段或活动标识非法，When 创建查询值对象，Then 应在进入仓储前立即拒绝。
     */
    @Test
    void shouldRejectInvalidQueryFields() {
        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardUnlockCountQueryVO(" ", ACTIVITY_ID, AwardUnlockScopeVO.DAY, STATISTICS_DATE)),
                () -> assertThrows(NullPointerException.class,
                        () -> new AwardUnlockCountQueryVO(USER_ID, null, AwardUnlockScopeVO.DAY, STATISTICS_DATE)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardUnlockCountQueryVO(USER_ID, 0L, AwardUnlockScopeVO.DAY, STATISTICS_DATE)),
                () -> assertThrows(NullPointerException.class,
                        () -> new AwardUnlockCountQueryVO(USER_ID, ACTIVITY_ID, null, STATISTICS_DATE)),
                () -> assertThrows(NullPointerException.class,
                        () -> new AwardUnlockCountQueryVO(USER_ID, ACTIVITY_ID, AwardUnlockScopeVO.TOTAL, null))
        );
    }
}
