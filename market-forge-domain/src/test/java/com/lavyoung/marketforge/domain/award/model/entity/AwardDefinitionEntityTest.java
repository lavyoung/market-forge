package com.lavyoung.marketforge.domain.award.model.entity;

import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品定义实体和状态变更对象测试。
 *
 * <p>验证运营后台维护奖品定义时的领域边界，包括基础字段合法性、领取方式、
 * 提供方识别、生命周期能力以及乐观锁状态变更参数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardDefinitionEntityTest {

    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 10, 21, 11, 0);

    /**
     * Given 合法的内部积分奖品草稿，When 创建实体，Then 应保留配置并允许运营编辑。
     */
    @Test
    void shouldCreateEditableInternalAwardDraft() {
        // Given & When
        AwardDefinitionEntity definition = createDefinition(
                AwardClaimModeVO.AUTOMATIC,
                null,
                AwardDefinitionStateVO.DRAFT
        );

        // Then
        assertAll(
                () -> assertEquals(1001L, definition.awardId()),
                () -> assertEquals("credit_100", definition.awardCode()),
                () -> assertTrue(definition.canEdit()),
                () -> assertFalse(definition.canReference()),
                () -> assertFalse(definition.requiresManualClaim()),
                () -> assertFalse(definition.hasProvider())
        );
    }

    /**
     * Given 启用的第三方手动领取奖品，When 查询业务能力，Then 应可被活动引用并标识提供方和领取要求。
     */
    @Test
    void shouldDescribeActiveManualExternalAward() {
        // Given & When
        AwardDefinitionEntity definition = createDefinition(
                AwardClaimModeVO.MANUAL,
                "coupon-center",
                AwardDefinitionStateVO.ACTIVE
        );

        // Then
        assertAll(
                () -> assertFalse(definition.canEdit()),
                () -> assertTrue(definition.canReference()),
                () -> assertTrue(definition.requiresManualClaim()),
                () -> assertTrue(definition.hasProvider())
        );
    }

    /**
     * Given 缺失或格式非法的奖品定义字段，When 创建实体，Then 应在进入仓储前拒绝保存。
     */
    @Test
    void shouldRejectInvalidAwardDefinitionFields() {
        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createDefinition(0L, "credit_100", "100积分", AwardClaimModeVO.AUTOMATIC, null, 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createDefinition(1001L, "Credit-100", "100积分", AwardClaimModeVO.AUTOMATIC, null, 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createDefinition(1001L, "credit_100", " ", AwardClaimModeVO.AUTOMATIC, null, 1)),
                () -> assertThrows(NullPointerException.class,
                        () -> createDefinition(1001L, "credit_100", "100积分", null, null, 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createDefinition(1001L, "credit_100", "100积分", AwardClaimModeVO.MANUAL, "Coupon Center", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> createDefinition(1001L, "credit_100", "100积分", AwardClaimModeVO.AUTOMATIC, null, 0))
        );
    }

    /**
     * Given 完整的状态变更命令，When 创建变更对象，Then 应保留乐观锁版本和操作人。
     */
    @Test
    void shouldCreateAwardDefinitionStateChange() {
        // Given & When
        AwardDefinitionStateChange change = new AwardDefinitionStateChange(
                1001L,
                AwardDefinitionStateVO.DRAFT,
                AwardDefinitionStateVO.ACTIVE,
                3,
                "operator-001"
        );

        // Then
        assertAll(
                () -> assertEquals(1001L, change.awardId()),
                () -> assertEquals(AwardDefinitionStateVO.DRAFT, change.currentState()),
                () -> assertEquals(AwardDefinitionStateVO.ACTIVE, change.targetState()),
                () -> assertEquals(3, change.expectedVersion()),
                () -> assertEquals("operator-001", change.operator())
        );
    }

    /**
     * Given 无效的状态变更参数，When 创建变更对象，Then 应拒绝空转变更和失效的乐观锁条件。
     */
    @Test
    void shouldRejectInvalidAwardDefinitionStateChange() {
        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardDefinitionStateChange(0L, AwardDefinitionStateVO.DRAFT,
                                AwardDefinitionStateVO.ACTIVE, 1, "operator-001")),
                () -> assertThrows(NullPointerException.class,
                        () -> new AwardDefinitionStateChange(1001L, null,
                                AwardDefinitionStateVO.ACTIVE, 1, "operator-001")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardDefinitionStateChange(1001L, AwardDefinitionStateVO.DRAFT,
                                AwardDefinitionStateVO.DRAFT, 1, "operator-001")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardDefinitionStateChange(1001L, AwardDefinitionStateVO.DRAFT,
                                AwardDefinitionStateVO.ACTIVE, 0, "operator-001")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardDefinitionStateChange(1001L, AwardDefinitionStateVO.DRAFT,
                                AwardDefinitionStateVO.ACTIVE, 1, " "))
        );
    }

    private AwardDefinitionEntity createDefinition(AwardClaimModeVO claimMode, String providerCode,
                                                   AwardDefinitionStateVO state) {
        return new AwardDefinitionEntity(
                1001L,
                "credit_100",
                "100积分",
                providerCode == null ? AwardTypeVO.INTERNAL_CREDIT : AwardTypeVO.THIRD_PARTY_COUPON,
                claimMode,
                providerCode,
                new AwardConfigSnapshot("{\"amount\":100}", 1),
                "抽奖后发放100积分",
                state,
                1,
                "operator-001",
                "operator-001",
                CREATE_TIME,
                UPDATE_TIME
        );
    }

    private AwardDefinitionEntity createDefinition(Long awardId, String awardCode, String awardName,
                                                   AwardClaimModeVO claimMode, String providerCode, int version) {
        return new AwardDefinitionEntity(
                awardId,
                awardCode,
                awardName,
                AwardTypeVO.INTERNAL_CREDIT,
                claimMode,
                providerCode,
                new AwardConfigSnapshot("{\"amount\":100}", 1),
                "抽奖后发放100积分",
                AwardDefinitionStateVO.DRAFT,
                version,
                "operator-001",
                "operator-001",
                CREATE_TIME,
                UPDATE_TIME
        );
    }
}
