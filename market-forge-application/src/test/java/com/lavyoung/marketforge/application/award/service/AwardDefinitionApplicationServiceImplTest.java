package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.ChangeAwardDefinitionStateCommand;
import com.lavyoung.marketforge.application.award.model.CreateAwardDefinitionCommand;
import com.lavyoung.marketforge.application.award.model.UpdateAwardDefinitionCommand;
import com.lavyoung.marketforge.application.award.service.impl.AwardDefinitionApplicationServiceImpl;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionStateChange;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 奖品定义应用服务单元测试。
 *
 * <p>测试应用层对奖品定义生命周期的编排职责，包括创建草稿、查询、草稿编辑、状态迁移和乐观锁冲突处理。
 * Repository 使用 Mockito 隔离，确保测试只关注应用服务是否正确调用领域模型和持久化端口。</p>
 */
@ExtendWith(MockitoExtension.class)
class AwardDefinitionApplicationServiceImplTest {

    private static final Long AWARD_ID = 1001L;
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 10, 21, 10, 30);

    @Mock
    private IAwardDefinitionRepository awardDefinitionRepository;

    private AwardDefinitionApplicationServiceImpl awardDefinitionApplicationService;

    @BeforeEach
    void setUp() {
        awardDefinitionApplicationService = new AwardDefinitionApplicationServiceImpl(awardDefinitionRepository);
    }

    /**
     * 验证创建奖品时只生成草稿，并将领域实体交给 Repository 持久化。
     */
    @Test
    @DisplayName("创建奖品定义时生成十八位奖品编号并保存为草稿")
    void shouldCreateDraftAwardDefinition() {
        // Given
        CreateAwardDefinitionCommand command = new CreateAwardDefinitionCommand(
                "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "抽奖后自动发放积分", "operator-001");
        when(awardDefinitionRepository.queryByAwardId(anyLong())).thenAnswer(invocation -> {
            Long generatedAwardId = invocation.getArgument(0);
            return Optional.of(definition(generatedAwardId, AwardDefinitionStateVO.DRAFT, 1, "operator-001"));
        });

        // When
        AwardDefinitionResult result = awardDefinitionApplicationService.create(command);

        // Then
        ArgumentCaptor<AwardDefinitionEntity> entityCaptor = ArgumentCaptor.forClass(AwardDefinitionEntity.class);
        verify(awardDefinitionRepository).create(entityCaptor.capture());
        AwardDefinitionEntity createdEntity = entityCaptor.getValue();
        assertAll(
                () -> assertNotNull(createdEntity.awardId()),
                () -> assertEquals(18, createdEntity.awardId().toString().length()),
                () -> assertTrue(createdEntity.awardId() > 0),
                () -> assertEquals("credit_100", createdEntity.awardCode()),
                () -> assertEquals("100积分", createdEntity.awardName()),
                () -> assertEquals(AwardTypeVO.INTERNAL_CREDIT, createdEntity.awardType()),
                () -> assertEquals(AwardClaimModeVO.AUTOMATIC, createdEntity.claimMode()),
                () -> assertEquals("{\"amount\":100}", createdEntity.awardConfig().content()),
                () -> assertEquals(1, createdEntity.awardConfig().version()),
                () -> assertEquals(AwardDefinitionStateVO.DRAFT, createdEntity.state()),
                () -> assertEquals(1, createdEntity.version()),
                () -> assertEquals(createdEntity.awardId(), result.awardId()),
                () -> assertEquals("draft", result.state())
        );
    }

    /**
     * 验证按奖品编号查询时，应用层会把领域实体转换为稳定的应用结果模型。
     */
    @Test
    @DisplayName("按奖品编号查询并返回应用结果")
    void shouldQueryAwardDefinition() {
        // Given
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID))
                .thenReturn(Optional.of(definition(AWARD_ID, AwardDefinitionStateVO.ACTIVE, 2, "operator-002")));

        // When
        AwardDefinitionResult result = awardDefinitionApplicationService.queryByAwardId(AWARD_ID);

        // Then
        assertAll(
                () -> assertEquals(AWARD_ID, result.awardId()),
                () -> assertEquals("100积分", result.awardName()),
                () -> assertEquals("internal_credit", result.awardType()),
                () -> assertEquals("automatic", result.claimMode()),
                () -> assertEquals("active", result.state()),
                () -> assertEquals(2, result.version())
        );
    }

    /**
     * 验证查询不存在的奖品时返回业务错误，而不是向外层传播空值。
     */
    @Test
    @DisplayName("查询不存在的奖品定义时抛出奖品不存在异常")
    void shouldRejectMissingAwardDefinition() {
        // Given
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.queryByAwardId(AWARD_ID));

        // Then
        assertEquals(BusinessResponseCode.AWARD_NOT_FOUND.getCode(), exception.getCode());
    }

    /**
     * 验证草稿编辑会校验版本，并保留初始创建审计信息。
     */
    @Test
    @DisplayName("使用正确版本更新草稿奖品定义")
    void shouldUpdateEditableDraft() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 2, "operator-001");
        AwardDefinitionEntity persisted = definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 3, "operator-002");
        UpdateAwardDefinitionCommand command = new UpdateAwardDefinitionCommand(
                AWARD_ID, "credit_200", "200积分", "internal_credit", "automatic", null,
                "{\"amount\":200}", 1, "抽奖后自动发放二百积分", 2, "operator-002");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current), Optional.of(persisted));
        when(awardDefinitionRepository.updateDraft(any(AwardDefinitionEntity.class))).thenReturn(true);

        // When
        AwardDefinitionResult result = awardDefinitionApplicationService.updateDraft(command);

        // Then
        ArgumentCaptor<AwardDefinitionEntity> entityCaptor = ArgumentCaptor.forClass(AwardDefinitionEntity.class);
        verify(awardDefinitionRepository).updateDraft(entityCaptor.capture());
        AwardDefinitionEntity updatedEntity = entityCaptor.getValue();
        assertAll(
                () -> assertEquals("credit_200", updatedEntity.awardCode()),
                () -> assertEquals("200积分", updatedEntity.awardName()),
                () -> assertEquals("{\"amount\":200}", updatedEntity.awardConfig().content()),
                () -> assertEquals(2, updatedEntity.version()),
                () -> assertEquals("operator-001", updatedEntity.createdBy()),
                () -> assertEquals(CREATE_TIME, updatedEntity.createTime()),
                () -> assertEquals("operator-002", updatedEntity.updatedBy()),
                () -> assertEquals(3, result.version())
        );
    }

    /**
     * 验证生效中的奖品不能通过草稿编辑入口修改，避免运行中的活动配置被直接覆盖。
     */
    @Test
    @DisplayName("拒绝编辑已经生效的奖品定义")
    void shouldRejectUpdateWhenAwardIsActive() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.ACTIVE, 2, "operator-001");
        UpdateAwardDefinitionCommand command = updateCommand(2);
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.updateDraft(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_STATE_ERROR.getCode(), exception.getCode());
        verify(awardDefinitionRepository, never()).updateDraft(any(AwardDefinitionEntity.class));
    }

    /**
     * 验证请求携带的版本落后时在应用层提前拒绝，避免提交无效更新。
     */
    @Test
    @DisplayName("请求版本过期时拒绝更新草稿")
    void shouldRejectUpdateWhenExpectedVersionIsStale() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 3, "operator-001");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.updateDraft(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.AWARD_DEFINITION_CONFLICT.getCode(), exception.getCode());
        verify(awardDefinitionRepository, never()).updateDraft(any(AwardDefinitionEntity.class));
    }

    /**
     * 验证应用层检查通过后发生并发竞争时，Repository 的条件更新结果仍会转换为冲突异常。
     */
    @Test
    @DisplayName("草稿条件更新未命中时报告并发冲突")
    void shouldReportConflictWhenDraftConditionalUpdateLosesRace() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 2, "operator-001");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current));
        when(awardDefinitionRepository.updateDraft(any(AwardDefinitionEntity.class))).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.updateDraft(updateCommand(2)));

        // Then
        assertEquals(BusinessResponseCode.AWARD_DEFINITION_CONFLICT.getCode(), exception.getCode());
    }

    /**
     * 验证草稿到生效状态的合法迁移会形成独立状态变更对象，并携带期望版本。
     */
    @Test
    @DisplayName("将草稿奖品定义变更为生效状态")
    void shouldChangeStateFromDraftToActive() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 2, "operator-001");
        AwardDefinitionEntity persisted = definition(AWARD_ID, AwardDefinitionStateVO.ACTIVE, 3, "operator-002");
        ChangeAwardDefinitionStateCommand command = new ChangeAwardDefinitionStateCommand(AWARD_ID, "active", 2, "operator-002");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current), Optional.of(persisted));
        when(awardDefinitionRepository.changeState(any(AwardDefinitionStateChange.class))).thenReturn(true);

        // When
        AwardDefinitionResult result = awardDefinitionApplicationService.changeState(command);

        // Then
        ArgumentCaptor<AwardDefinitionStateChange> changeCaptor = ArgumentCaptor.forClass(AwardDefinitionStateChange.class);
        verify(awardDefinitionRepository).changeState(changeCaptor.capture());
        AwardDefinitionStateChange stateChange = changeCaptor.getValue();
        assertAll(
                () -> assertEquals(AWARD_ID, stateChange.awardId()),
                () -> assertEquals(AwardDefinitionStateVO.DRAFT, stateChange.currentState()),
                () -> assertEquals(AwardDefinitionStateVO.ACTIVE, stateChange.targetState()),
                () -> assertEquals(2, stateChange.expectedVersion()),
                () -> assertEquals("operator-002", stateChange.operator()),
                () -> assertEquals("active", result.state()),
                () -> assertEquals(3, result.version())
        );
    }

    /**
     * 验证领域状态机拒绝从生效状态退回草稿，避免历史配置被重新编辑。
     */
    @Test
    @DisplayName("拒绝从生效状态退回草稿状态")
    void shouldRejectIllegalStateTransition() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.ACTIVE, 2, "operator-001");
        ChangeAwardDefinitionStateCommand command = new ChangeAwardDefinitionStateCommand(AWARD_ID, "draft", 2, "operator-002");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.changeState(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_STATE_ERROR.getCode(), exception.getCode());
        verify(awardDefinitionRepository, never()).changeState(any(AwardDefinitionStateChange.class));
    }

    /**
     * 验证未知状态文本不会进入 Repository，防止非法状态落库。
     */
    @Test
    @DisplayName("拒绝未知的目标状态")
    void shouldRejectUnknownTargetState() {
        // Given
        ChangeAwardDefinitionStateCommand command = new ChangeAwardDefinitionStateCommand(AWARD_ID, "unknown", 2, "operator-002");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID))
                .thenReturn(Optional.of(definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 2, "operator-001")));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.changeState(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_STATE_ERROR.getCode(), exception.getCode());
        verify(awardDefinitionRepository, never()).changeState(any(AwardDefinitionStateChange.class));
    }

    /**
     * 验证状态条件更新遭遇并发竞争时，不把旧版本结果误报为操作成功。
     */
    @Test
    @DisplayName("状态条件更新未命中时报告并发冲突")
    void shouldReportConflictWhenStateConditionalUpdateLosesRace() {
        // Given
        AwardDefinitionEntity current = definition(AWARD_ID, AwardDefinitionStateVO.DRAFT, 2, "operator-001");
        ChangeAwardDefinitionStateCommand command = new ChangeAwardDefinitionStateCommand(AWARD_ID, "active", 2, "operator-002");
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(current));
        when(awardDefinitionRepository.changeState(any(AwardDefinitionStateChange.class))).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.changeState(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_DEFINITION_CONFLICT.getCode(), exception.getCode());
    }

    /**
     * 验证不受支持的奖品类型在领域对象创建阶段被拦截，不产生任何持久化副作用。
     */
    @Test
    @DisplayName("拒绝创建不受支持的奖品类型")
    void shouldRejectUnsupportedAwardType() {
        // Given
        CreateAwardDefinitionCommand command = new CreateAwardDefinitionCommand(
                "unknown_award", "未知奖品", "UNKNOWN", "automatic", null,
                "{\"amount\":100}", 1, null, "operator-001");

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> awardDefinitionApplicationService.create(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_TYPE_NOT_SUPPORTED.getCode(), exception.getCode());
        verify(awardDefinitionRepository, never()).create(any(AwardDefinitionEntity.class));
    }

    private UpdateAwardDefinitionCommand updateCommand(int expectedVersion) {
        return new UpdateAwardDefinitionCommand(
                AWARD_ID, "credit_200", "200积分", "internal_credit", "automatic", null,
                "{\"amount\":200}", 1, "抽奖后自动发放二百积分", expectedVersion, "operator-002");
    }

    private AwardDefinitionEntity definition(Long awardId, AwardDefinitionStateVO state, int version, String updatedBy) {
        return new AwardDefinitionEntity(
                awardId, "credit_100", "100积分", AwardTypeVO.INTERNAL_CREDIT, AwardClaimModeVO.AUTOMATIC,
                null, new AwardConfigSnapshot("{\"amount\":100}", 1), "抽奖后自动发放积分", state, version,
                "operator-001", updatedBy, CREATE_TIME, UPDATE_TIME);
    }
}
