package com.lavyoung.marketforge.types.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ModelPreconditions} 单元测试。
 *
 * <p>验证公共模型前置条件保持原始文本，并对空值、空白、长度越界和非法约束参数
 * 给出稳定的标准异常，防止不同领域模型重新产生不一致的文本校验语义。</p>
 */
class ModelPreconditionsTest {

    @Test
    void shouldReturnOriginalTextWithoutTrimming() {
        // Given
        String value = new String("  activity-name  ");

        // When
        String result = ModelPreconditions.requireText(value, "activityName");

        // Then
        assertSame(value, result);
        assertEquals("  activity-name  ", result);
    }

    @Test
    void shouldAcceptTextAtMaximumLength() {
        // Given
        String value = "123456";

        // When
        String result = ModelPreconditions.requireText(value, "code", 6);

        // Then
        assertSame(value, result);
    }

    @Test
    void shouldRejectNullTextWithFieldContext() {
        // Given / When
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> ModelPreconditions.requireText(null, "activityName"));

        // Then
        assertEquals("activityName must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectBlankTextWithFieldContext() {
        // Given / When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ModelPreconditions.requireText(" \t\r\n", "activityName"));

        // Then
        assertEquals("activityName must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectTextExceedingMaximumLength() {
        // Given / When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ModelPreconditions.requireText("1234567", "code", 6));

        // Then
        assertEquals("code length must not exceed 6", exception.getMessage());
    }

    @Test
    void shouldRejectNonPositiveMaximumLength() {
        // Given / When / Then
        IllegalArgumentException zeroException = assertThrows(IllegalArgumentException.class,
                () -> ModelPreconditions.requireText("value", "field", 0));
        IllegalArgumentException negativeException = assertThrows(IllegalArgumentException.class,
                () -> ModelPreconditions.requireText("value", "field", -1));
        assertEquals("maximumLength must be positive", zeroException.getMessage());
        assertEquals("maximumLength must be positive", negativeException.getMessage());
    }

    @Test
    void shouldRejectInvalidFieldName() {
        // Given / When / Then
        NullPointerException nullException = assertThrows(NullPointerException.class,
                () -> ModelPreconditions.requireText("value", null));
        IllegalArgumentException blankException = assertThrows(IllegalArgumentException.class,
                () -> ModelPreconditions.requireText("value", " "));
        assertEquals("fieldName must not be null", nullException.getMessage());
        assertEquals("fieldName must not be blank", blankException.getMessage());
    }
}
