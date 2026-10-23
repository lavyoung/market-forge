package com.lavyoung.marketforge.application.award.grant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link InternalCreditAwardConfig} 单元测试。
 *
 * <p>验证积分奖品的结构化配置只能携带正积分数量，防止空值、零值和负值进入
 * 积分账户入账链路。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
class InternalCreditAwardConfigTest {

    /**
     * 验证正积分数量能够创建有效配置。
     */
    @Test
    void shouldCreateConfigWithPositiveAmount() {
        // Given
        Long amount = 100L;

        // When
        InternalCreditAwardConfig config = new InternalCreditAwardConfig(amount);

        // Then
        assertEquals(amount, config.amount());
    }

    /**
     * 验证积分数量为空时立即拒绝创建配置。
     */
    @Test
    void shouldRejectNullAmount() {
        // When
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> new InternalCreditAwardConfig(null));

        // Then
        assertEquals("amount must not be null", exception.getMessage());
    }

    /**
     * 验证零积分和负积分都不能作为积分奖品配置。
     */
    @Test
    void shouldRejectNonPositiveAmount() {
        // When
        IllegalArgumentException zeroAmountException = assertThrows(
                IllegalArgumentException.class, () -> new InternalCreditAwardConfig(0L));
        IllegalArgumentException negativeAmountException = assertThrows(
                IllegalArgumentException.class, () -> new InternalCreditAwardConfig(-1L));

        // Then
        assertEquals("amount must be positive", zeroAmountException.getMessage());
        assertEquals("amount must be positive", negativeAmountException.getMessage());
    }
}
