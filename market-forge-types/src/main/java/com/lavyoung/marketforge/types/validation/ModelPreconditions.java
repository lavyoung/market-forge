package com.lavyoung.marketforge.types.validation;

import java.util.Objects;

/**
 * 模型构造期的通用前置条件。
 *
 * <p>用于实体、值对象、事件和应用命令在构造时检查与具体业务无关的基础参数，
 * 避免每个模型重复实现字符校验方案。</p>
 *
 * <p>本类只检查参数，不负责：</p>
 *
 * <ul>
 *     <li>将异常转换为 {@code BusinessException}；</li>
 *     <li>执行库存、状态、概率等领域规则；</li>
 *     <li>自动修改或清洗调用方传入的业务数据。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
 */
public final class ModelPreconditions {

    private ModelPreconditions() {
    }

    /**
     * 要求文本不为 {@code null}、空字符串或纯空白字符。
     *
     * <p>方法返回原始文本，不会自动执行 {@link String#trim()}。是否去除首尾空格
     * 属于调用方的数据规范化决定，不应由基础校验工具静默处理。</p>
     *
     * @param value     待校验文本
     * @param fieldName 字段名称，用于构造异常信息
     * @return 未经修改的原始文本
     * @throws NullPointerException     文本或字段名称为空时抛出
     * @throws IllegalArgumentException 文本为空白，或字段名称为空白时抛出
     */
    public static String requireText(String value, String fieldName) {
        String validFieldName = requireFieldName(fieldName);
        Objects.requireNonNull(value, validFieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(validFieldName + " must not be blank");
        }
        return value;
    }

    /**
     * 要求文本非空白且长度不超过指定上限。
     *
     * <p>长度按照 Java 字符串的 {@link String#length()} 计算，与当前数据库
     * {@code varchar} 字段和项目已有领域模型的校验方式保持一致。</p>
     *
     * @param value         待校验文本
     * @param fieldName     字段名称，用于构造异常信息
     * @param maximumLength 允许的最大长度，必须大于零
     * @return 未经修改的原始文本
     * @throws NullPointerException     文本或字段名称为空时抛出
     * @throws IllegalArgumentException 文本为空白、超过最大长度、字段名称为空白，
     *                                  或最大长度不是正数时抛出
     */
    public static String requireText(String value, String fieldName, int maximumLength) {
        if (maximumLength <= 0) {
            throw new IllegalArgumentException("maximumLength must be positive");
        }

        String validValue = requireText(value, fieldName);
        if (validValue.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " length must not exceed " + maximumLength
            );
        }
        return validValue;
    }

    /**
     * 校验异常信息使用的字段名称。
     *
     * @param fieldName 字段名称
     * @return 有效字段名称
     * @throws NullPointerException     字段名称为空时抛出
     * @throws IllegalArgumentException 字段名称为空白时抛出
     */
    private static String requireFieldName(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        if (fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName must not be blank");
        }
        return fieldName;
    }
}
