package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.UserAwardRecordAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IUserAwardRecordDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.UserAwardRecordPO;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 用户中奖记录仓储测试。
 *
 * <p>重点验证奖品解锁次数查询始终带有用户分片键和活动条件，
 * 并按活动配置正确生成日范围或累计范围查询。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AwardRepositoryTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100301L;
    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 10, 17);

    @Mock
    private IUserAwardRecordDao userAwardRecordDao;

    @Mock
    private ITaskDao taskDao;

    @Mock
    private UserAwardRecordAssembler userAwardRecordAssembler;

    @Mock
    private TaskAssembler taskAssembler;

    @Mock
    private IntegrationEventCodec eventCodec;

    private AwardRepository repository;

    /**
     * Given 可控的持久化依赖，When 初始化仓储，Then 后续用例只验证查询构造行为。
     */
    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "AwardRepositoryTest"),
                UserAwardRecordPO.class
        );
        repository = new AwardRepository(
                userAwardRecordDao,
                taskDao,
                userAwardRecordAssembler,
                taskAssembler,
                eventCodec
        );
    }

    /**
     * Given 活动使用按日解锁，When 统计已完成抽奖次数，
     * Then 应使用用户和活动条件，并构造左闭右开的当日时间范围。
     */
    @Test
    void shouldCountCompletedRafflesWithinDayRange() {
        // Given
        when(userAwardRecordDao.selectCount(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<UserAwardRecordPO> wrapper = invocation.getArgument(0);
            String sqlSegment = wrapper.getSqlSegment();
            Map<String, Object> parameters = wrapper.getParamNameValuePairs();

            assertAll(
                    () -> assertTrue(sqlSegment.contains("user_id")),
                    () -> assertTrue(sqlSegment.contains("activity_id")),
                    () -> assertTrue(sqlSegment.contains("award_time")),
                    () -> assertTrue(sqlSegment.contains(">=")),
                    () -> assertTrue(sqlSegment.contains("<")),
                    () -> assertTrue(parameters.containsValue(USER_ID)),
                    () -> assertTrue(parameters.containsValue(ACTIVITY_ID)),
                    () -> assertTrue(parameters.containsValue(LocalDateTime.of(2026, 10, 17, 0, 0))),
                    () -> assertTrue(parameters.containsValue(LocalDateTime.of(2026, 10, 18, 0, 0)))
            );
            return 3L;
        });
        AwardUnlockCountQueryVO query = new AwardUnlockCountQueryVO(
                USER_ID,
                ACTIVITY_ID,
                AwardUnlockScopeVO.DAY,
                STATISTICS_DATE
        );

        // When
        long count = repository.countCompletedRaffles(query);

        // Then
        assertEquals(3L, count);
    }

    /**
     * Given 活动使用累计解锁，When 统计已完成抽奖次数，
     * Then 应保留用户和活动条件，但不应限制中奖时间。
     */
    @Test
    void shouldCountAllCompletedRafflesForTotalScope() {
        // Given
        when(userAwardRecordDao.selectCount(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<UserAwardRecordPO> wrapper = invocation.getArgument(0);
            String sqlSegment = wrapper.getSqlSegment();
            Map<String, Object> parameters = wrapper.getParamNameValuePairs();

            assertAll(
                    () -> assertTrue(sqlSegment.contains("user_id")),
                    () -> assertTrue(sqlSegment.contains("activity_id")),
                    () -> assertFalse(sqlSegment.contains("award_time")),
                    () -> assertTrue(parameters.containsValue(USER_ID)),
                    () -> assertTrue(parameters.containsValue(ACTIVITY_ID)),
                    () -> assertEquals(2, parameters.size())
            );
            return 12L;
        });
        AwardUnlockCountQueryVO query = new AwardUnlockCountQueryVO(
                USER_ID,
                ACTIVITY_ID,
                AwardUnlockScopeVO.TOTAL,
                STATISTICS_DATE
        );

        // When
        long count = repository.countCompletedRaffles(query);

        // Then
        assertEquals(12L, count);
    }

    /**
     * Given 统计条件为空，When 查询已完成抽奖次数，Then 应快速失败且不访问 DAO。
     */
    @Test
    void shouldRejectNullCountQuery() {
        // When & Then
        assertThrows(NullPointerException.class, () -> repository.countCompletedRaffles(null));
        verifyNoInteractions(userAwardRecordDao);
    }
}
