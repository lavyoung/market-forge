package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 活动开放就绪检查问题响应。
 *
 * @param code    供管理端稳定识别的问题编码
 * @param path    问题对应的活动配置路径
 * @param message 面向运营人员展示的问题说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Schema(description = "活动开放就绪检查问题")
public record ActivityReadinessIssueResponse(
        @Schema(description = "稳定的问题编码", example = "activity.sku.missing") String code,
        @Schema(description = "问题对应的配置路径", example = "skus") String path,
        @Schema(description = "面向运营人员的问题说明", example = "活动至少需要配置一个额度 SKU") String message
) {
}
