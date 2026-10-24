package com.lavyoung.marketforge.api.award.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 奖品配置表单字段元数据响应。
 *
 * @param name        JSON 配置字段名称
 * @param label       运营后台展示名称
 * @param fieldType   前端输入控件类型
 * @param required    是否必须填写
 * @param description 字段业务说明
 * @param constraints 不同字段类型对应的输入约束
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Schema(description = "奖品配置表单字段元数据")
public record AwardConfigFieldMetadataResponse(
        @Schema(description = "JSON 配置字段名称", example = "amount") String name,
        @Schema(description = "字段展示名称", example = "积分数量") String label,
        @Schema(description = "输入类型", example = "integer",
                allowableValues = {"integer", "text", "select", "boolean"}) String fieldType,
        @Schema(description = "是否必填", example = "true") boolean required,
        @Schema(description = "字段业务说明", example = "中奖后发放的积分数量") String description,
        @Schema(description = "输入约束", example = "{\"minimum\":\"1\",\"step\":\"1\"}")
        Map<String, String> constraints) {

    /**
     * 冻结字段约束，避免序列化前被调用方修改。
     *
     * @throws NullPointerException 约束集合为空时抛出
     */
    public AwardConfigFieldMetadataResponse {
        constraints = Map.copyOf(constraints);
    }
}
