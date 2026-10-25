package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionStateChange;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.AwardDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 奖品定义仓储测试。
 *
 * <p>验证奖品定义查询和写入编排、数据库唯一键异常翻译、条件更新结果以及
 * 生命周期状态到 MyBatis 参数对象的转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AwardDefinitionRepositoryTest {

    @Mock
    private IAwardDao awardDao;

    @Mock
    private AwardDefinitionAssembler awardDefinitionAssembler;

    private AwardDefinitionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AwardDefinitionRepository(awardDao, awardDefinitionAssembler);
    }

    /**
     * Given 数据库存在奖品，When 按业务标识查询，Then 应转换并返回领域实体。
     */
    @Test
    void shouldQueryAwardDefinitionByAwardId() {
        // Given
        AwardPO po = new AwardPO();
        AwardDefinitionEntity expected = definition();
        when(awardDao.queryAwardByAwardId(1001L)).thenReturn(Optional.of(po));
        when(awardDefinitionAssembler.toEntity(po)).thenReturn(expected);

        // When
        Optional<AwardDefinitionEntity> actual = repository.queryByAwardId(1001L);

        // Then
        assertEquals(Optional.of(expected), actual);
        verify(awardDefinitionAssembler).toEntity(po);
    }

    /**
     * Given 数据库不存在对应编码，When 查询奖品，Then 应返回空且不调用转换器。
     */
    @Test
    void shouldReturnEmptyWhenAwardCodeDoesNotExist() {
        // Given
        when(awardDao.queryAwardByAwardKey("credit_100")).thenReturn(Optional.empty());

        // When
        Optional<AwardDefinitionEntity> actual = repository.queryByAwardCode("credit_100");

        // Then
        assertTrue(actual.isEmpty());
        verify(awardDefinitionAssembler, never()).toEntity(null);
    }

    /**
     * Given 奖品标识包含重复值，When 批量查询，Then 应去重后只执行一次 DAO 查询并转换结果。
     */
    @Test
    void shouldQueryAwardDefinitionsInOneBatch() {
        // Given
        AwardPO first = new AwardPO();
        AwardPO second = new AwardPO();
        List<AwardPO> persistentObjects = List.of(first, second);
        List<AwardDefinitionEntity> expected = List.of(definition(), definition());
        when(awardDao.queryAwardByAwardIds(List.of(1001L, 1002L))).thenReturn(persistentObjects);
        when(awardDefinitionAssembler.toEntities(persistentObjects)).thenReturn(expected);

        // When
        List<AwardDefinitionEntity> actual = repository.queryByAwardIds(List.of(1001L, 1002L, 1001L));

        // Then
        assertEquals(expected, actual);
        verify(awardDao).queryAwardByAwardIds(List.of(1001L, 1002L));
        verify(awardDefinitionAssembler).toEntities(persistentObjects);
    }

    /**
     * Given 空奖品标识集合，When 批量查询，Then 应直接返回空集合且不生成 IN 空集合 SQL。
     */
    @Test
    void shouldSkipDaoWhenAwardIdsAreEmpty() {
        // When
        List<AwardDefinitionEntity> actual = repository.queryByAwardIds(List.of());

        // Then
        assertEquals(List.of(), actual);
        verifyNoInteractions(awardDao, awardDefinitionAssembler);
    }

    /**
     * Given 批量查询参数或集合元素为空，When 查询，Then 应在访问 DAO 前拒绝。
     */
    @Test
    void shouldRejectNullBatchQueryArguments() {
        // Given
        List<Long> awardIdsWithNull = new ArrayList<>();
        awardIdsWithNull.add(1001L);
        awardIdsWithNull.add(null);

        // When & Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> repository.queryByAwardIds(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.queryByAwardIds(awardIdsWithNull))
        );
        verifyNoInteractions(awardDao, awardDefinitionAssembler);
    }

    /**
     * Given 合法奖品定义，When 数据库成功写入一行，Then 创建操作应正常完成。
     */
    @Test
    void shouldCreateAwardDefinition() {
        // Given
        AwardDefinitionEntity definition = definition();
        AwardPO po = new AwardPO();
        when(awardDefinitionAssembler.toPO(definition)).thenReturn(po);
        when(awardDao.insert(po)).thenReturn(1);

        // When
        repository.create(definition);

        // Then
        verify(awardDao).insert(po);
    }

    /**
     * Given 数据库没有成功插入记录，When 创建奖品，Then 应抛出稳定的创建失败业务异常。
     */
    @Test
    void shouldRejectCreateWhenNoRowIsInserted() {
        // Given
        AwardDefinitionEntity definition = definition();
        AwardPO po = new AwardPO();
        when(awardDefinitionAssembler.toPO(definition)).thenReturn(po);
        when(awardDao.insert(po)).thenReturn(0);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(definition));

        // Then
        assertEquals(BusinessResponseCode.AWARD_DEFINITION_CREATE_FAILED, exception.getResponseCode());
    }

    /**
     * Given 奖品标识或编码违反唯一键，When 创建奖品，Then 应保留原因并转换为重复业务异常。
     */
    @Test
    void shouldTranslateDuplicateKeyWhenCreatingAwardDefinition() {
        // Given
        AwardDefinitionEntity definition = definition();
        AwardPO po = new AwardPO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate award");
        when(awardDefinitionAssembler.toPO(definition)).thenReturn(po);
        when(awardDao.insert(po)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(definition));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.AWARD_DEFINITION_DUPLICATED, exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    /**
     * Given 草稿更新影响一行或零行，When 更新奖品，Then 应分别返回成功或版本状态冲突。
     */
    @Test
    void shouldReportDraftUpdateResult() {
        // Given
        AwardDefinitionEntity definition = definition();
        AwardPO po = new AwardPO();
        when(awardDefinitionAssembler.toPO(definition)).thenReturn(po);
        when(awardDao.updateDraftDefinition(po)).thenReturn(1, 0);

        // When
        boolean firstResult = repository.updateDraft(definition);
        boolean secondResult = repository.updateDraft(definition);

        // Then
        assertAll(
                () -> assertTrue(firstResult),
                () -> assertFalse(secondResult)
        );
    }

    /**
     * Given 状态变更请求，When 更新状态，Then 应向 DAO 传递状态编码、版本和操作人。
     */
    @Test
    void shouldChangeDefinitionStateWithOptimisticLockParameters() {
        // Given
        AwardDefinitionStateChange stateChange = new AwardDefinitionStateChange(
                1001L,
                AwardDefinitionStateVO.DRAFT,
                AwardDefinitionStateVO.ACTIVE,
                3,
                "operator-002"
        );
        when(awardDao.changeDefinitionState(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        // When
        boolean result = repository.changeState(stateChange);

        // Then
        ArgumentCaptor<AwardDefinitionStateUpdateParam> captor =
                ArgumentCaptor.forClass(AwardDefinitionStateUpdateParam.class);
        verify(awardDao).changeDefinitionState(captor.capture());
        AwardDefinitionStateUpdateParam actual = captor.getValue();
        assertAll(
                () -> assertTrue(result),
                () -> assertEquals(1001L, actual.awardId()),
                () -> assertEquals("draft", actual.currentState()),
                () -> assertEquals("active", actual.targetState()),
                () -> assertEquals(3, actual.expectedVersion()),
                () -> assertEquals("operator-002", actual.operator())
        );
    }

    /**
     * Given 仓储收到空参数，When 调用公开方法，Then 应在访问 DAO 前立即拒绝。
     */
    @Test
    void shouldRejectNullRepositoryArguments() {
        // When & Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> repository.queryByAwardId(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.queryByAwardCode(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.create(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.updateDraft(null)),
                () -> assertThrows(NullPointerException.class, () -> repository.changeState(null))
        );
    }

    private AwardDefinitionEntity definition() {
        return new AwardDefinitionEntity(
                1001L,
                "credit_100",
                "100积分",
                AwardTypeVO.INTERNAL_CREDIT,
                AwardClaimModeVO.AUTOMATIC,
                null,
                new AwardConfigSnapshot("{\"amount\":100}", 1),
                "抽奖后发放100积分",
                AwardDefinitionStateVO.DRAFT,
                1,
                "operator-001",
                "operator-001",
                LocalDateTime.of(2026, 10, 21, 10, 0),
                LocalDateTime.of(2026, 10, 21, 10, 0)
        );
    }
}
