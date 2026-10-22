package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityStageAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityStageDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStageStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动展台仓储测试。
 *
 * <p>验证查询和锁定结果转换、ACTIVE 唯一业务不变量、数据库唯一键异常翻译，
 * 以及草稿和状态更新的乐观锁结果。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@ExtendWith(MockitoExtension.class)
class ActivityStageRepositoryTest {

    @Mock
    private IActivityStageDao activityStageDao;

    @Mock
    private ActivityStageAssembler activityStageAssembler;

    private ActivityStageRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ActivityStageRepository(activityStageDao, activityStageAssembler);
    }

    /**
     * 验证业务标识查询会通过转换器返回领域实体。
     */
    @Test
    void shouldQueryStageByBusinessId() {
        // Given
        ActivityStagePO po = new ActivityStagePO();
        ActivityStageEntity expected = stage(ActivityStageStateVO.CREATE);
        when(activityStageDao.queryByStageId(900001L)).thenReturn(Optional.of(po));
        when(activityStageAssembler.toEntity(po)).thenReturn(expected);

        // When
        Optional<ActivityStageEntity> actual = repository.queryByStageId(900001L);

        // Then
        assertEquals(Optional.of(expected), actual);
    }

    /**
     * 验证当前 SC 没有生效配置时返回空且不执行对象转换。
     */
    @Test
    void shouldReturnEmptyWhenNoActiveStageExists() {
        // Given
        when(activityStageDao.queryActive("wechat", "campaign")).thenReturn(List.of());

        // When
        Optional<ActivityStageEntity> actual = repository.queryActive("wechat", "campaign");

        // Then
        assertTrue(actual.isEmpty());
        verify(activityStageAssembler, never()).toEntity(any());
    }

    /**
     * 验证数据库出现多个 ACTIVE 配置时立即暴露配置损坏问题。
     */
    @Test
    void shouldRejectMultipleActiveStagesForSameSc() {
        // Given
        when(activityStageDao.queryActive("wechat", "campaign"))
                .thenReturn(List.of(new ActivityStagePO(), new ActivityStagePO()));

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> repository.queryActive("wechat", "campaign"));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception.getResponseCode());
    }

    /**
     * 验证发布事务取得渠道锁后返回不可修改的领域快照。
     */
    @Test
    void shouldLockAndConvertChannelStages() {
        // Given
        ActivityStagePO po = new ActivityStagePO();
        ActivityStageEntity expected = stage(ActivityStageStateVO.ACTIVE);
        when(activityStageDao.lockByChannelAndSource("wechat", "campaign")).thenReturn(List.of(po));
        when(activityStageAssembler.toEntities(List.of(po))).thenReturn(List.of(expected));

        // When
        List<ActivityStageEntity> actual = repository.lockByChannelAndSource("wechat", "campaign");

        // Then
        assertAll(
                () -> assertEquals(List.of(expected), actual),
                () -> assertThrows(UnsupportedOperationException.class, () -> actual.add(expected))
        );
    }

    /**
     * 验证创建时的数据库唯一键异常会转换为稳定业务异常并保留原因。
     */
    @Test
    void shouldTranslateDuplicateKeyWhenCreatingStage() {
        // Given
        ActivityStageEntity stage = stage(ActivityStageStateVO.CREATE);
        ActivityStagePO po = new ActivityStagePO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate stage");
        when(activityStageAssembler.toPO(stage)).thenReturn(po);
        when(activityStageDao.insert(po)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(stage));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_STAGE_DUPLICATED, exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    /**
     * 验证草稿条件更新能够区分成功和状态版本冲突。
     */
    @Test
    void shouldReportDraftUpdateResult() {
        // Given
        ActivityStageEntity stage = stage(ActivityStageStateVO.CREATE);
        ActivityStagePO po = new ActivityStagePO();
        when(activityStageAssembler.toPO(stage)).thenReturn(po);
        when(activityStageDao.updateDraft(po)).thenReturn(1, 0);

        // When
        boolean firstResult = repository.updateDraft(stage);
        boolean secondResult = repository.updateDraft(stage);

        // Then
        assertAll(() -> assertTrue(firstResult), () -> assertFalse(secondResult));
    }

    /**
     * 验证状态变更向 DAO 完整传递当前状态、目标状态、版本和操作人。
     */
    @Test
    void shouldChangeStateWithOptimisticLockParameters() {
        // Given
        ActivityStageStateChange change = new ActivityStageStateChange(
                900001L, ActivityStageStateVO.CREATE, ActivityStageStateVO.ACTIVE, 3, "operator-002");
        when(activityStageDao.changeState(any())).thenReturn(1);

        // When
        boolean result = repository.changeState(change);

        // Then
        ArgumentCaptor<ActivityStageStateUpdateParam> captor = ArgumentCaptor.forClass(ActivityStageStateUpdateParam.class);
        verify(activityStageDao).changeState(captor.capture());
        ActivityStageStateUpdateParam actual = captor.getValue();
        assertAll(
                () -> assertTrue(result),
                () -> assertEquals(900001L, actual.stageId()),
                () -> assertEquals("create", actual.currentState()),
                () -> assertEquals("active", actual.targetState()),
                () -> assertEquals(3, actual.expectedVersion()),
                () -> assertEquals("operator-002", actual.operator())
        );
    }

    private ActivityStageEntity stage(ActivityStageStateVO state) {
        return new ActivityStageEntity(
                900001L, "wechat", "campaign", 100001L, state, 1, "operator-001", "operator-001",
                LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 10, 0));
    }
}
