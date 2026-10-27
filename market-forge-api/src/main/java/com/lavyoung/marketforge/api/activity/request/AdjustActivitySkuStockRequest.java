package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 调整活动 SKU 库存请求。
 *
 * @param quantityDelta   调整数量；正数补充库存，负数收回未消费库存，不能为零
 * @param reason          调整原因
 * @param expectedVersion 页面读取到的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Schema(description = "调整活动 SKU 库存请求")
public record AdjustActivitySkuStockRequest(
        @Schema(description = "库存调整量；正数增加，负数减少，不能为零", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        long quantityDelta,
        @Schema(description = "调整原因", example = "追加活动预算", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "库存调整原因不能为空")
        @Size(max = 256, message = "库存调整原因不能超过 256 个字符")
        String reason,
        @Schema(description = "乐观锁版本", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "乐观锁版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
