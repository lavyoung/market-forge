package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

/**
 * 修改活动 SKU 普通配置请求。
 *
 * <p>该请求不包含库存字段。库存只能通过独立库存调整接口修改，避免普通编辑覆盖
 * 已经消费的库存。</p>
 *
 * @param activityId      新的所属活动业务标识
 * @param activityCountId 新的次数配置业务标识
 * @param creditAmount    新的积分价格；为空表示关闭积分兑换
 * @param expectedVersion 页面读取到的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Schema(description = "修改活动 SKU 普通配置请求")
public record UpdateActivitySkuDefinitionRequest(
        @Schema(description = "所属活动业务标识", example = "100000000000000001", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "所属活动不能为空")
        @Pattern(regexp = "^[1-9][0-9]{0,17}$", message = "活动标识格式不合法")
        String activityId,
        @Schema(description = "次数配置业务标识", example = "300000000000000001", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "次数配置不能为空")
        @Pattern(regexp = "^[1-9][0-9]{0,17}$", message = "次数配置标识格式不合法")
        String activityCountId,
        @Schema(description = "兑换所需积分；为空表示关闭积分兑换", example = "100", minimum = "1", nullable = true)
        @Positive(message = "积分兑换价格必须为正数")
        Long creditAmount,
        @Schema(description = "乐观锁版本", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "乐观锁版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
