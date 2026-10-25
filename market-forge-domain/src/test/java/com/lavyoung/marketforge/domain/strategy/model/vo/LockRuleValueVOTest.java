package com.lavyoung.marketforge.domain.strategy.model.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 次数锁规则配置值对象测试。
 *
 * <p>验证无门槛和正常次数门槛语义，并确保负数、空值及非数字配置不会进入运行时。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class LockRuleValueVOTest {

    /**
     * Given 合法次数锁配置，When 判断已完成次数，Then 应以大于等于门槛作为解锁条件。
     */
    @Test
    void shouldParseSerializeAndEvaluateLockRuleValue() {
        // Given & When
        LockRuleValueVO ruleValue = LockRuleValueVO.parse(" 3 ");

        // Then
        assertAll(
                () -> assertEquals(3, ruleValue.requiredCount()),
                () -> assertFalse(ruleValue.isSatisfiedBy(2)),
                () -> assertTrue(ruleValue.isSatisfiedBy(3)),
                () -> assertTrue(ruleValue.isSatisfiedBy(4)),
                () -> assertEquals("3", ruleValue.toConfigValue())
        );
    }

    /**
     * Given 次数锁为零，When 判断零次完成量，Then 应表达无门槛并直接满足。
     */
    @Test
    void shouldAllowZeroAsNoThreshold() {
        LockRuleValueVO ruleValue = LockRuleValueVO.parse("0");

        assertTrue(ruleValue.isSatisfiedBy(0));
    }

    /**
     * Given 非法次数锁配置，When 解析或判断，Then 应抛出明确的参数异常。
     */
    @Test
    void shouldRejectInvalidLockRuleValue() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> LockRuleValueVO.parse(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> LockRuleValueVO.parse("")),
                () -> assertThrows(IllegalArgumentException.class, () -> LockRuleValueVO.parse("not-a-number")),
                () -> assertThrows(IllegalArgumentException.class, () -> LockRuleValueVO.parse("-1")),
                () -> assertThrows(IllegalArgumentException.class, () -> new LockRuleValueVO(1).isSatisfiedBy(-1))
        );
    }
}
