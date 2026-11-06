package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityStagePublicationAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityStagePublicationDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStagePublicationTransitionParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePublicationPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动展台发布记录仓储测试。
 *
 * <p>验证发布记录查询转换、幂等创建、唯一键冲突分类、数据库异常翻译，
 * 以及使用状态、阶段和尝试次数进行并发条件迁移的完整行为。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
@ExtendWith(MockitoExtension.class)
class ActivityStagePublicationRepositoryTest {

    @Mock
    private IActivityStagePublicationDao activityStagePublicationDao;

    @Mock
    private ActivityStagePublicationAssembler activityStagePublicationAssembler;

    private ActivityStagePublicationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ActivityStagePublicationRepository(
                activityStagePublicationDao, activityStagePublicationAssembler);
    }

    /**
     * 验证展台版本查询会通过转换器返回领域发布记录。
     */
    @Test
    void shouldQueryPublicationByStageAndVersion() {
        // Given
        ActivityStagePublicationPO po = new ActivityStagePublicationPO();
        ActivityStagePublicationEntity expected = publication(910001L);
        when(activityStagePublicationDao.queryByStageAndVersion(900001L, 3)).thenReturn(Optional.of(po));
        when(activityStagePublicationAssembler.toEntity(po)).thenReturn(expected);

        // When
        Optional<ActivityStagePublicationEntity> actual = repository.queryByStageAndVersion(900001L, 3);

        // Then
        assertEquals(Optional.of(expected), actual);
    }

    /**
     * 验证首次发布记录插入成功时返回 {@code true}。
     */
    @Test
    void shouldCreatePublicationOnFirstRequest() {
        // Given
        ActivityStagePublicationEntity publication = publication(910001L);
        ActivityStagePublicationPO po = new ActivityStagePublicationPO();
        when(activityStagePublicationAssembler.toPO(publication)).thenReturn(po);
        when(activityStagePublicationDao.insert(po)).thenReturn(1);

        // When
        boolean created = repository.createIfAbsent(publication);

        // Then
        assertTrue(created);
    }

    /**
     * 验证相同展台版本触发唯一键时按幂等重复处理。
     */
    @Test
    void shouldReturnFalseWhenStageVersionAlreadyHasPublication() {
        // Given
        ActivityStagePublicationEntity publication = publication(910001L);
        ActivityStagePublicationPO inserted = new ActivityStagePublicationPO();
        ActivityStagePublicationPO existing = new ActivityStagePublicationPO();
        when(activityStagePublicationAssembler.toPO(publication)).thenReturn(inserted);
        when(activityStagePublicationDao.insert(inserted)).thenThrow(new DuplicateKeyException("duplicate version"));
        when(activityStagePublicationDao.queryByStageAndVersion(900001L, 3)).thenReturn(Optional.of(existing));
        when(activityStagePublicationAssembler.toEntity(existing)).thenReturn(publication);

        // When
        boolean created = repository.createIfAbsent(publication);

        // Then
        assertFalse(created);
    }

    /**
     * 验证发布标识碰撞不能被误判为相同展台版本的幂等请求。
     */
    @Test
    void shouldTranslatePublicationIdCollision() {
        // Given
        ActivityStagePublicationEntity publication = publication(910001L);
        ActivityStagePublicationPO po = new ActivityStagePublicationPO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate publication id");
        when(activityStagePublicationAssembler.toPO(publication)).thenReturn(po);
        when(activityStagePublicationDao.insert(po)).thenThrow(cause);
        when(activityStagePublicationDao.queryByStageAndVersion(900001L, 3)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> repository.createIfAbsent(publication));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_ID_CONFLICT,
                        exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    /**
     * 验证普通数据库插入异常会转换为稳定的创建失败业务异常。
     */
    @Test
    void shouldTranslateDataAccessFailureWhenCreatingPublication() {
        // Given
        ActivityStagePublicationEntity publication = publication(910001L);
        ActivityStagePublicationPO po = new ActivityStagePublicationPO();
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("database unavailable");
        when(activityStagePublicationAssembler.toPO(publication)).thenReturn(po);
        when(activityStagePublicationDao.insert(po)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> repository.createIfAbsent(publication));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CREATE_FAILED,
                        exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    /**
     * 验证阶段迁移完整传递当前快照和目标状态。
     */
    @Test
    void shouldTransitionPublicationWithSnapshotConditions() {
        // Given
        ActivityStagePublicationEntity current = publication(910001L);
        LocalDateTime changedAt = LocalDateTime.of(2026, 11, 6, 10, 2);
        ActivityStagePublicationEntity target = current.advance(
                ActivityStagePublicationPhaseVO.SWITCHING, "operator-002", changedAt);
        when(activityStagePublicationDao.transition(any())).thenReturn(1);

        // When
        boolean updated = repository.transition(current, target);

        // Then
        ArgumentCaptor<ActivityStagePublicationTransitionParam> captor =
                ArgumentCaptor.forClass(ActivityStagePublicationTransitionParam.class);
        verify(activityStagePublicationDao).transition(captor.capture());
        ActivityStagePublicationTransitionParam actual = captor.getValue();
        assertAll(
                () -> assertTrue(updated),
                () -> assertEquals(910001L, actual.publicationId()),
                () -> assertEquals("processing", actual.expectedState()),
                () -> assertEquals("preheating", actual.expectedPhase()),
                () -> assertEquals(2, actual.expectedAttemptCount()),
                () -> assertEquals("switching", actual.targetPhase()),
                () -> assertEquals("operator-002", actual.operator()),
                () -> assertEquals(changedAt, actual.updateTime())
        );
    }

    /**
     * 验证原快照已过期时返回 {@code false}，交由 Application 决定重读或报冲突。
     */
    @Test
    void shouldReturnFalseWhenPublicationSnapshotIsStale() {
        // Given
        ActivityStagePublicationEntity current = publication(910001L);
        ActivityStagePublicationEntity target = current.advance(
                ActivityStagePublicationPhaseVO.SWITCHING, "operator-002", current.updateTime().plusMinutes(1));
        when(activityStagePublicationDao.transition(any())).thenReturn(0);

        // When
        boolean updated = repository.transition(current, target);

        // Then
        assertFalse(updated);
    }

    /**
     * 验证两个不同发布任务不能被错误拼接为一次状态迁移。
     */
    @Test
    void shouldRejectTransitionBetweenDifferentPublications() {
        // Given
        ActivityStagePublicationEntity current = publication(910001L);
        ActivityStagePublicationEntity target = publication(910002L);

        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> repository.transition(current, target)),
                () -> verify(activityStagePublicationDao, never()).transition(any())
        );
    }

    /**
     * 验证状态迁移数据库异常会转换为稳定的更新失败业务异常。
     */
    @Test
    void shouldTranslateDataAccessFailureWhenTransitioningPublication() {
        // Given
        ActivityStagePublicationEntity current = publication(910001L);
        ActivityStagePublicationEntity target = current.advance(
                ActivityStagePublicationPhaseVO.SWITCHING, "operator-002", current.updateTime().plusMinutes(1));
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("database unavailable");
        when(activityStagePublicationDao.transition(any())).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class, () -> repository.transition(current, target));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_UPDATE_FAILED,
                        exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    private ActivityStagePublicationEntity publication(Long publicationId) {
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 6, 10, 0);
        return new ActivityStagePublicationEntity(
                publicationId, 900001L, 3, 100001L, "wechat", "campaign",
                ActivityStagePublicationStateVO.PROCESSING, ActivityStagePublicationPhaseVO.PREHEATING,
                2, "operator-001", null, null, null, createTime, createTime.plusMinutes(1));
    }
}
