package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.message.model.valobj.TaskStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.TaskPublishingClaim;
import io.github.lavyoung.lavshard.starter.internal.datasource.LavShardManagedDataSourceRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证本地消息任务仓储的投递权抢占参数转换与结果语义。
 */
@ExtendWith(MockitoExtension.class)
class TaskRepositoryTest {

    private static final String USER_ID = "demo-user-001";
    private static final String EVENT_ID = "event-001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 22, 0);
    private static final LocalDateTime EXPIRED_BEFORE = NOW.minusMinutes(5);

    @Mock
    private ITaskDao taskDao;

    @Mock
    private TaskAssembler taskAssembler;

    @Mock
    private LavShardManagedDataSourceRegistry dataSourceRegistry;

    @InjectMocks
    private TaskRepository taskRepository;

    /**
     * Given DAO 成功更新一条任务，When 仓储抢占投递权，Then 完整传递分片键、状态和时间边界并返回成功。
     */
    @Test
    void shouldBuildClaimAndReturnTrueWhenTaskIsAcquired() {
        // Given
        when(taskDao.markPublishing(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        ArgumentCaptor<TaskPublishingClaim> claimCaptor =
                ArgumentCaptor.forClass(TaskPublishingClaim.class);

        // When
        boolean acquired = taskRepository.markPublishing(
                USER_ID,
                EVENT_ID,
                NOW,
                EXPIRED_BEFORE
        );

        // Then
        verify(taskDao).markPublishing(claimCaptor.capture());
        TaskPublishingClaim claim = claimCaptor.getValue();
        assertAll(
                () -> assertTrue(acquired),
                () -> assertEquals(USER_ID, claim.userId()),
                () -> assertEquals(EVENT_ID, claim.eventId()),
                () -> assertEquals(TaskStateVO.PUBLISHING.getCode(), claim.publishingState()),
                () -> assertEquals(TaskStateVO.CREATE.getCode(), claim.createState()),
                () -> assertEquals(TaskStateVO.PUBLISH_FAILED.getCode(), claim.publishFailedState()),
                () -> assertEquals(TaskStateVO.PUBLISHING.getCode(), claim.publishingExpiredState()),
                () -> assertEquals(NOW, claim.now()),
                () -> assertEquals(EXPIRED_BEFORE, claim.publishingExpiredBefore())
        );
    }

    /**
     * Given 任务状态不满足抢占条件，When DAO 未更新任何记录，Then 仓储返回抢占失败。
     */
    @Test
    void shouldReturnFalseWhenTaskCannotBeAcquired() {
        // Given
        when(taskDao.markPublishing(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        // When
        boolean acquired = taskRepository.markPublishing(
                USER_ID,
                EVENT_ID,
                NOW,
                EXPIRED_BEFORE
        );

        // Then
        assertFalse(acquired);
    }
}
