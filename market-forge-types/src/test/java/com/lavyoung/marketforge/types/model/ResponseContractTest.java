package com.lavyoung.marketforge.types.model;

import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;

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
     * Given 空响应编码，When 创建业务异常，Then 立即拒绝非法调用。
     */
    @Test
    void shouldRejectNullResponseCode() {
        // Given & When & Then
        assertThrows(NullPointerException.class, () -> new BusinessException((IResponseCode) null));
    }

    /**
     * 测试项目自定义的业务响应编码。
     */
    private enum TestResponseCode implements IResponseCode {

        RESOURCE_NOT_FOUND(700_001, "resource.not-found", "资源不存在");

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
