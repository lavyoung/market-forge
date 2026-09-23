package com.lavyoung.marketforge.types.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证轻量级业务 ID 生成工具的公共契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class IdGeneratorTest {

    /**
     * Given 连续生成通用 ID，When 写入集合，Then 在常规调用规模下不应出现重复。
     */
    @Test
    void shouldGenerateDifferentGeneralIds() {
        // Given
        Set<String> ids = new HashSet<>();

        // When
        for (int i = 0; i < 1_000; i++) {
            ids.add(IdGenerator.nextId());
        }

        // Then
        assertEquals(1_000, ids.size());
    }

    /**
     * Given 业务前缀，When 生成前缀 ID，Then 返回值应以指定前缀开头。
     */
    @Test
    void shouldGenerateIdWithPrefix() {
        // Given
        String prefix = "ORD";

        // When
        String id = IdGenerator.nextId(prefix);

        // Then
        assertTrue(id.startsWith(prefix));
        assertTrue(id.length() > prefix.length());
    }

    /**
     * Given 指定数字长度，When 生成纯数字 ID，Then 返回值只包含数字且长度匹配。
     */
    @Test
    void shouldGenerateNumericIdWithExpectedLength() {
        // Given
        int length = 12;

        // When
        String id = IdGenerator.nextNumericId(length);

        // Then
        assertEquals(length, id.length());
        assertTrue(id.chars().allMatch(Character::isDigit));
    }

    /**
     * Given 指定前缀和数字长度，When 生成前缀数字 ID，Then 前缀和数字部分均符合约束。
     */
    @Test
    void shouldGenerateNumericIdWithPrefix() {
        // Given
        String prefix = "ACT";
        int numericLength = 12;

        // When
        String id = IdGenerator.nextNumericId(prefix, numericLength);

        // Then
        assertTrue(id.startsWith(prefix));
        String numericPart = id.substring(prefix.length());
        assertEquals(numericLength, numericPart.length());
        assertTrue(numericPart.chars().allMatch(Character::isDigit));
    }

    /**
     * Given 非法参数，When 生成 ID，Then 立即拒绝调用。
     */
    @Test
    void shouldRejectInvalidArguments() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.nextId(" "));
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.nextNumericId(7));
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.nextNumericId(33));
    }
}
