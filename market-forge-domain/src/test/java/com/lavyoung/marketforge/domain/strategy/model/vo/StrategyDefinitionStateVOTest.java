package com.lavyoung.marketforge.domain.strategy.model.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略定义生命周期状态测试。
 *
 * <p>验证持久化编码解析、编辑与引用能力，以及
 * {@code DRAFT -> ACTIVE -> DISABLED} 的单向状态机。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionStateVOTest {

    /**
     * Given 合法、未知和空状态编码，When 解析状态，Then 只返回可识别的生命周期。
     */
    @Test
    void shouldParseKnownStateCodeOnly() {
        // When & Then
        assertAll(
                () -> assertEquals(StrategyDefinitionStateVO.DRAFT,
                        StrategyDefinitionStateVO.of("draft").orElseThrow()),
                () -> assertEquals(StrategyDefinitionStateVO.ACTIVE,
                        StrategyDefinitionStateVO.of("active").orElseThrow()),
                () -> assertTrue(StrategyDefinitionStateVO.of("unknown").isEmpty()),
                () -> assertTrue(StrategyDefinitionStateVO.of(null).isEmpty())
        );
    }

    /**
     * Given 三种策略状态，When 查询编辑、引用和复制能力，Then 返回与生命周期一致的结果。
     */
    @Test
    void shouldExposeStateCapabilities() {
        // When & Then
        assertAll(
                () -> assertTrue(StrategyDefinitionStateVO.DRAFT.canEdit()),
                () -> assertFalse(StrategyDefinitionStateVO.DRAFT.canReference()),
                () -> assertFalse(StrategyDefinitionStateVO.DRAFT.requiresCopyForChange()),
                () -> assertFalse(StrategyDefinitionStateVO.ACTIVE.canEdit()),
                () -> assertTrue(StrategyDefinitionStateVO.ACTIVE.canReference()),
                () -> assertTrue(StrategyDefinitionStateVO.ACTIVE.requiresCopyForChange()),
                () -> assertFalse(StrategyDefinitionStateVO.DISABLED.canEdit()),
                () -> assertFalse(StrategyDefinitionStateVO.DISABLED.canReference()),
                () -> assertTrue(StrategyDefinitionStateVO.DISABLED.requiresCopyForChange())
        );
    }

    /**
     * Given 策略生命周期，When 判断状态迁移，Then 只允许草稿生效和生效停用。
     */
    @Test
    void shouldAllowForwardTransitionsOnly() {
        // When & Then
        assertAll(
                () -> assertTrue(StrategyDefinitionStateVO.DRAFT.canTransitionTo(StrategyDefinitionStateVO.ACTIVE)),
                () -> assertFalse(StrategyDefinitionStateVO.DRAFT.canTransitionTo(StrategyDefinitionStateVO.DISABLED)),
                () -> assertTrue(StrategyDefinitionStateVO.ACTIVE.canTransitionTo(StrategyDefinitionStateVO.DISABLED)),
                () -> assertFalse(StrategyDefinitionStateVO.ACTIVE.canTransitionTo(StrategyDefinitionStateVO.DRAFT)),
                () -> assertFalse(StrategyDefinitionStateVO.DISABLED.canTransitionTo(StrategyDefinitionStateVO.ACTIVE)),
                () -> assertThrows(NullPointerException.class,
                        () -> StrategyDefinitionStateVO.DRAFT.canTransitionTo(null))
        );
    }
}
