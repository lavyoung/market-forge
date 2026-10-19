package com.lavyoung.marketforge.types.model;

import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证统一响应编码、业务异常和响应载体之间的公共契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
class ResponseContractTest {

    /**
     * Given 公共成功编码，When 创建成功响应，Then 返回统一编码、文案和业务数据。
     */
    @Test
    void shouldCreateSuccessResponseFromCommonResponseCode() {
        // Given
        String data = "payload";

        // When
        Response<String> response = Response.success(data);

        // Then
        assertEquals(CommonResponseCode.SUCCESS.getCode(), response.code());
        assertEquals(CommonResponseCode.SUCCESS.getMsg(), response.message());
        assertEquals(data, response.data());
    }

    /**
     * Given 运营后台认证协议，When 读取公共认证错误码，Then 返回稳定的编码、国际化键和默认文案。
     */
    @Test
    void shouldExposeStableAuthenticationResponseCodes() {
        // Given & When
        CommonResponseCode authenticationRequired = CommonResponseCode.AUTHENTICATION_REQUIRED;
        CommonResponseCode authenticationFailed = CommonResponseCode.AUTHENTICATION_FAILED;
        CommonResponseCode accessDenied = CommonResponseCode.ACCESS_DENIED;
        CommonResponseCode csrfTokenInvalid = CommonResponseCode.CSRF_TOKEN_INVALID;

        // Then
        assertAll(
                () -> assertResponseCode(authenticationRequired, 1_005, "common.authentication.required", "请先登录"),
                () -> assertResponseCode(authenticationFailed, 1_006, "common.authentication.failed", "用户名或密码错误"),
                () -> assertResponseCode(accessDenied, 1_007, "common.access.denied", "无权访问该资源"),
                () -> assertResponseCode(csrfTokenInvalid, 1_008, "common.csrf-token.invalid", "请求安全校验失败")
        );
    }

    /**
     * Given 全部公共响应编码，When 检查编码和国际化键，Then 每个定义都保持唯一。
     */
    @Test
    void shouldKeepCommonResponseCodesUnique() {
        // Given
        CommonResponseCode[] responseCodes = CommonResponseCode.values();

        // When
        long uniqueCodeCount = Arrays.stream(responseCodes).mapToInt(CommonResponseCode::getCode).distinct().count();
        long uniqueI18nKeyCount = Arrays.stream(responseCodes).map(CommonResponseCode::getI18nKey).distinct().count();

        // Then
        assertAll(
                () -> assertEquals(responseCodes.length, uniqueCodeCount),
                () -> assertEquals(responseCodes.length, uniqueI18nKeyCount)
        );
    }

    /**
     * Given 项目自定义响应编码，When 创建失败响应，Then 无需公共模块识别具体枚举。
     */
    @Test
    void shouldCreateFailureResponseFromProjectResponseCode() {
        // Given
        IResponseCode responseCode = TestResponseCode.RESOURCE_NOT_FOUND;

        // When
        Response<Void> response = Response.fail(responseCode);

        // Then
        assertEquals(700_001, response.code());
        assertEquals("资源不存在", response.message());
        assertNull(response.data());
    }

    /**
     * Given 带有业务数据泛型的接口，When 创建失败响应，Then 保留接口泛型且数据为空。
     */
    @Test
    void shouldCreateGenericFailureResponse() {
        // Given
        IResponseCode responseCode = TestResponseCode.RESOURCE_NOT_FOUND;

        // When
        Response<String> response = Response.fail(responseCode);

        // Then
        assertAll(
                () -> assertEquals(700_001, response.code()),
                () -> assertEquals("资源不存在", response.message()),
                () -> assertNull(response.data())
        );
    }

    /**
     * Given 项目自定义响应编码，When 创建业务异常，Then 保留编码、国际化键和默认文案。
     */
    @Test
    void shouldCreateBusinessExceptionFromResponseCode() {
        // Given
        IResponseCode responseCode = TestResponseCode.RESOURCE_NOT_FOUND;

        // When
        BusinessException exception = new BusinessException(responseCode);

        // Then
        assertEquals(700_001, exception.getCode());
        assertEquals("resource.not-found", exception.getI18nKey());
        assertEquals("资源不存在", exception.getMessage());
    }

    /**
     * Given 底层异常，When 包装为业务异常，Then 保留原始异常作为原因。
     */
    @Test
    void shouldRetainCauseWhenWrappingBusinessException() {
        // Given
        IllegalStateException cause = new IllegalStateException("dependency failed");

        // When
        BusinessException exception = new BusinessException(
                TestResponseCode.RESOURCE_NOT_FOUND,
                cause
        );

        // Then
        assertSame(cause, exception.getCause());
        assertEquals("资源不存在", exception.getMessage());
    }

    /**
     * Given 带占位符的响应编码，When 创建带参数业务异常，Then 使用参数格式化响应文案。
     */
    @Test
    void shouldFormatBusinessExceptionMessageWithArgs() {
        // Given
        IResponseCode responseCode = TestResponseCode.RESOURCE_NOT_FOUND_WITH_ID;

        // When
        BusinessException exception = BusinessException.of(responseCode, "SKU-1001");

        // Then
        assertEquals(700_002, exception.getCode());
        assertEquals("资源 SKU-1001 不存在", exception.getMessage());
        assertArrayEquals(new Object[]{"SKU-1001"}, exception.getArgs());
    }

    /**
     * Given 不带占位符的响应编码，When 创建带参数业务异常，Then 保留动态参数作为排障上下文。
     */
    @Test
    void shouldAppendArgsWhenMessageHasNoPlaceholder() {
        // Given
        IResponseCode responseCode = TestResponseCode.RESOURCE_NOT_FOUND;

        // When
        BusinessException exception = BusinessException.of(responseCode, "SKU-1001");

        // Then
        assertEquals("资源不存在，参数：[SKU-1001]", exception.getMessage());
        assertArrayEquals(new Object[]{"SKU-1001"}, exception.getArgs());
    }

    /**
     * Given 空响应编码，When 创建业务异常，Then 立即拒绝非法调用。
     */
    @Test
    void shouldRejectNullResponseCode() {
        // Given & When & Then
        assertThrows(NullPointerException.class, () -> new BusinessException((IResponseCode) null));
    }

    /**
     * 断言响应编码的完整协议内容。
     *
     * @param responseCode 待校验的响应编码
     * @param code         预期数值编码
     * @param i18nKey      预期国际化键
     * @param message      预期默认文案
     */
    private static void assertResponseCode(IResponseCode responseCode, int code, String i18nKey, String message) {
        assertAll(
                () -> assertEquals(code, responseCode.getCode()),
                () -> assertEquals(i18nKey, responseCode.getI18nKey()),
                () -> assertEquals(message, responseCode.getMsg())
        );
    }

    /**
     * 测试项目自定义的业务响应编码。
     */
    private enum TestResponseCode implements IResponseCode {

        RESOURCE_NOT_FOUND(700_001, "resource.not-found", "资源不存在"),
        RESOURCE_NOT_FOUND_WITH_ID(700_002, "resource.not-found-with-id", "资源 {0} 不存在");

        private final int code;
        private final String i18nKey;
        private final String msg;

        TestResponseCode(int code, String i18nKey, String msg) {
            this.code = code;
            this.i18nKey = i18nKey;
            this.msg = msg;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getI18nKey() {
            return i18nKey;
        }

        @Override
        public String getMsg() {
            return msg;
        }
    }
}
