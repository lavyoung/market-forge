package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AwardGrantHandlerRegistry} 单元测试。
 *
 * <p>验证注册表能够稳定地按奖品类型路由处理器，并在类型缺失、重复注册或处理器配置非法时
 * 尽早失败。这样可以把奖品处理器装配错误阻断在应用启动或消息消费入口，避免发奖流程选择
 * 到不确定的实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
class AwardGrantHandlerRegistryTest {

    /**
     * 验证注册表可以按奖品类型返回原始处理器实例。
     */
    @Test
    void shouldReturnRegisteredHandlerByAwardType() {
        // Given
        AwardGrantHandler handler = new StubAwardGrantHandler("internal_credit");
        AwardGrantHandlerRegistry registry = new AwardGrantHandlerRegistry(List.of(handler));

        // When
        AwardGrantHandler actual = registry.getRequired("internal_credit");

        // Then
        assertSame(handler, actual);
        assertTrue(registry.supports("internal_credit"));
    }

    /**
     * 验证未注册的奖品类型使用统一业务错误码拒绝发奖。
     */
    @Test
    void shouldRejectUnregisteredAwardType() {
        // Given
        AwardGrantHandlerRegistry registry = new AwardGrantHandlerRegistry(List.of());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> registry.getRequired("coupon"));

        // Then
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, exception.getResponseCode());
        assertFalse(registry.supports("coupon"));
    }

    /**
     * 验证空奖品类型不会进入处理器查找，避免配置错误被误判为普通未命中。
     */
    @Test
    void shouldRejectNullAndBlankAwardType() {
        // Given
        AwardGrantHandlerRegistry registry = new AwardGrantHandlerRegistry(List.of());

        // When
        BusinessException nullTypeException = assertThrows(BusinessException.class, () -> registry.getRequired(null));
        BusinessException blankTypeException = assertThrows(BusinessException.class, () -> registry.getRequired(" "));

        // Then
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, nullTypeException.getResponseCode());
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, blankTypeException.getResponseCode());
        assertFalse(registry.supports(null));
        assertFalse(registry.supports(" "));
    }

    /**
     * 验证相同奖品类型不能注册多个处理器，防止运行时路由结果依赖 Spring Bean 顺序。
     */
    @Test
    void shouldRejectDuplicateAwardType() {
        // Given
        AwardGrantHandler first = new StubAwardGrantHandler("internal_credit");
        AwardGrantHandler second = new StubAwardGrantHandler("internal_credit");

        // When
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new AwardGrantHandlerRegistry(List.of(first, second)));

        // Then
        assertEquals("duplicate award grant handler type: internal_credit", exception.getMessage());
    }

    /**
     * 验证空处理器元素会在注册阶段失败，避免生成包含不完整实现的注册表。
     */
    @Test
    void shouldRejectNullHandler() {
        // Given
        List<AwardGrantHandler> handlers = new java.util.ArrayList<>();
        handlers.add(null);

        // When
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> new AwardGrantHandlerRegistry(handlers));

        // Then
        assertEquals("award grant handler must not be null", exception.getMessage());
    }

    /**
     * 验证处理器声明的奖品类型不能为空白，保证注册键是稳定的业务类型编码。
     */
    @Test
    void shouldRejectBlankHandlerType() {
        // Given
        AwardGrantHandler handler = new StubAwardGrantHandler(" ");

        // When
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new AwardGrantHandlerRegistry(List.of(handler)));

        // Then
        assertEquals("award grant handler type must not be blank", exception.getMessage());
    }

    /**
     * 验证处理器支持指定领取模式时通过启用能力校验。
     */
    @Test
    void shouldAcceptSupportedClaimMode() {
        // Given
        AwardGrantHandler handler = new StubAwardGrantHandler("internal_credit");
        AwardGrantHandlerRegistry registry = new AwardGrantHandlerRegistry(List.of(handler));

        // When / Then
        assertDoesNotThrow(() -> registry.validateCapability("internal_credit", "automatic"));
    }

    /**
     * 验证处理器不支持人工领取或领取模式为空时拒绝启用。
     */
    @Test
    void shouldRejectUnsupportedClaimMode() {
        // Given
        AwardGrantHandler handler = new StubAwardGrantHandler("internal_credit");
        AwardGrantHandlerRegistry registry = new AwardGrantHandlerRegistry(List.of(handler));

        // When
        BusinessException manualException = assertThrows(
                BusinessException.class,
                () -> registry.validateCapability("internal_credit", "manual"));
        BusinessException blankException = assertThrows(
                BusinessException.class,
                () -> registry.validateCapability("internal_credit", " "));

        // Then
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, manualException.getResponseCode());
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, blankException.getResponseCode());
    }

    /**
     * 只用于注册表测试的最小处理器实现。
     *
     * @param awardType 当前测试处理器声明支持的奖品类型
     */
    private record StubAwardGrantHandler(String awardType) implements AwardGrantHandler {

        @Override
        public boolean supportsClaimMode(String claimMode) {
            return "automatic".equals(claimMode);
        }

        @Override
        public void grant(com.lavyoung.marketforge.application.award.model.GrantAwardCommand command) {
            // 注册表测试不执行具体履约逻辑。
        }
    }
}
