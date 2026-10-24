package com.lavyoung.marketforge.application.award.model;

import java.util.Map;
import java.util.Objects;

/**
 * 单个奖品配置字段的元数据。
 *
 * <p>该模型描述配置字段的业务名称、展示名称、输入类型和约束。运营后台
 * 根据这些信息生成结构化表单，不再要求运营人员直接编辑 JSON。</p>
 *
 * <p>{@code constraints} 使用不可变字符串映射保存可扩展约束。例如整数
 * 字段可以使用 {@code minimum=1} 和 {@code step=1}，选择字段可以在后续
 * 版本中通过独立选项模型进行扩展。</p>
 *
 * @param name        JSON 配置中的字段名称
 * @param label       运营后台展示名称
 * @param fieldType   字段的数据和输入类型
 * @param required    是否必须填写
 * @param description 字段的业务含义
 * @param constraints 字段约束，不存在额外约束时使用空映射
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
public record AwardConfigFieldMetadata(
        String name,
        String label,
        AwardConfigFieldType fieldType,
        boolean required,
        String description,
        Map<String, String> constraints) {

    /**
     * 创建经过完整性校验的配置字段元数据。
     *
     * @throws NullPointerException     字段类型或约束集合为空时抛出
     * @throws IllegalArgumentException 字段名称、展示名称或说明为空白时抛出
     */
    public AwardConfigFieldMetadata {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("award config field name must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("award config field label must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("award config field description must not be blank");
        }

        Objects.requireNonNull(fieldType, "fieldType must not be null");
        Objects.requireNonNull(constraints, "constraints must not be null");
        constraints = Map.copyOf(constraints);
    }
}
