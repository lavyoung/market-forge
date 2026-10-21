package com.lavyoung.marketforge.domain.award.model.vo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品定义值对象测试。
 *
 * <p>验证奖品类型、领取模式、生命周期状态和配置快照在进入领域实体前能够拒绝非法值，
 * 防止数据库编码、运营配置和领域模型之间出现无法识别的状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardDefinitionValueObjectTest {

    /**
     * Given 系统支持的奖品类型编码，When 创建奖品类型值对象，Then 应保留编码并正确判断内外部类型。
     */
    @Test
    void shouldCreateSupportedAwardType() {
        // Given & When
        AwardTypeVO internalType = AwardTypeVO.INTERNAL_CREDIT;
        AwardTypeVO externalType = AwardTypeVO.THIRD_PARTY_COUPON;

        // Then
        assertAll(
                () -> assertEquals("internal_credit", internalType.code()),
                () -> assertEquals(new AwardTypeVO("internal_credit"), internalType),
                () -> assertEquals("third_party_coupon", externalType.code()),
                () -> assertEquals(new AwardTypeVO("third_party_coupon"), externalType)
        );
    }

    /**
     * Given 不符合稳定编码规则的奖品类型，When 创建值对象，Then 应立即拒绝非法配置。
     */
    @Test
    void shouldRejectInvalidAwardType() {
        // When & Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new AwardTypeVO(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new AwardTypeVO(" ")),
                () -> assertThrows(IllegalArgumentException.class, () -> new AwardTypeVO("InternalCredit")),
                () -> assertThrows(IllegalArgumentException.class, () -> new AwardTypeVO("1_internal_credit")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardTypeVO("internal_credit_type_code_that_is_too_long"))
        );
    }

    /**
     * Given 持久化层读取的领取模式和状态编码，When 执行领域转换，Then 应返回对应枚举或空结果。
     */
    @Test
    void shouldParseClaimModeAndDefinitionState() {
        // Given & When
        Optional<AwardClaimModeVO> automatic = AwardClaimModeVO.of("automatic");
        Optional<AwardDefinitionStateVO> active = AwardDefinitionStateVO.of("active");

        // Then
        assertAll(
                () -> assertEquals(Optional.of(AwardClaimModeVO.AUTOMATIC), automatic),
                () -> assertEquals(Optional.of(AwardDefinitionStateVO.ACTIVE), active),
                () -> assertTrue(AwardClaimModeVO.of("unknown").isEmpty()),
                () -> assertTrue(AwardDefinitionStateVO.of(null).isEmpty())
        );
    }

    /**
     * Given 不同生命周期状态，When 判断运营能力，Then 只有草稿可编辑且只有启用状态可被活动引用。
     */
    @Test
    void shouldExposeStateCapabilities() {
        // When & Then
        assertAll(
                () -> assertTrue(AwardDefinitionStateVO.DRAFT.canEdit()),
                () -> assertFalse(AwardDefinitionStateVO.ACTIVE.canEdit()),
                () -> assertFalse(AwardDefinitionStateVO.DISABLED.canEdit()),
                () -> assertTrue(AwardDefinitionStateVO.ACTIVE.canReference()),
                () -> assertFalse(AwardDefinitionStateVO.DRAFT.canReference()),
                () -> assertFalse(AwardDefinitionStateVO.DISABLED.canReference())
        );
    }

    /**
     * Given 奖品定义的各个生命周期状态，When 判断目标状态，Then 只允许草稿启用和启用后停用。
     */
    @Test
    void shouldAllowOnlyForwardDefinitionStateTransitions() {
        // When & Then
        assertAll(
                () -> assertTrue(AwardDefinitionStateVO.DRAFT.canTransitionTo(AwardDefinitionStateVO.ACTIVE)),
                () -> assertTrue(AwardDefinitionStateVO.ACTIVE.canTransitionTo(AwardDefinitionStateVO.DISABLED)),
                () -> assertFalse(AwardDefinitionStateVO.DRAFT.canTransitionTo(AwardDefinitionStateVO.DISABLED)),
                () -> assertFalse(AwardDefinitionStateVO.ACTIVE.canTransitionTo(AwardDefinitionStateVO.DRAFT)),
                () -> assertFalse(AwardDefinitionStateVO.DISABLED.canTransitionTo(AwardDefinitionStateVO.ACTIVE)),
                () -> assertFalse(AwardDefinitionStateVO.DRAFT.canTransitionTo(AwardDefinitionStateVO.DRAFT)),
                () -> assertThrows(NullPointerException.class,
                        () -> AwardDefinitionStateVO.DRAFT.canTransitionTo(null))
        );
    }

    /**
     * Given 合法和非法的奖品配置快照，When 创建快照，Then 应保留完整配置并拒绝空内容或非法版本。
     */
    @Test
    void shouldValidateAwardConfigSnapshot() {
        // Given & When
        AwardConfigSnapshot snapshot = new AwardConfigSnapshot("{\"amount\":100}", 1);

        // Then
        assertAll(
                () -> assertEquals("{\"amount\":100}", snapshot.content()),
                () -> assertEquals(1, snapshot.version()),
                () -> assertThrows(NullPointerException.class, () -> new AwardConfigSnapshot(null, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new AwardConfigSnapshot(" ", 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new AwardConfigSnapshot("{}", 0))
        );
    }
}
