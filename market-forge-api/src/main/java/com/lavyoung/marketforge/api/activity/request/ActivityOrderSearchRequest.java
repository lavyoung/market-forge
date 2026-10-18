package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动订单运营分页查询请求。
 *
 * <p>该请求用于运营后台检索跨分库汇总后的活动订单。用户、活动、订单号、
 * 状态和下单时间均为可选过滤条件，分页参数为必填项。</p>
 *
 * <p>用户标识和订单号采用精确匹配，避免在 Elasticsearch 的 keyword 字段上
 * 执行高成本通配符查询。下单时间采用左闭右开区间，即包含开始时间，
 * 不包含结束时间。</p>
 *
 * @param userId     用户标识；为空时不限制用户
 * @param activityId 活动标识；为空时不限制活动
 * @param orderId    全业务唯一订单号；为空时不限制订单
 * @param orderState 订单状态；为空时不限制状态
 * @param startTime  下单时间起点，包含该时间
 * @param endTime    下单时间终点，不包含该时间
 * @param pageNumber 页码，从 1 开始
 * @param pageSize   每页记录数，最大 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Schema(description = "活动订单运营分页查询条件")
public record ActivityOrderSearchRequest(
        @Schema(description = "用户标识，精确匹配", example = "user-001", maxLength = 64)
        @Size(max = 64, message = "用户标识长度不能超过 64 个字符")
        String userId,

        @Schema(description = "活动标识", example = "100301", minimum = "1")
        @Min(value = 1, message = "活动标识不能小于 1")
        Long activityId,

        @Schema(description = "全业务唯一订单号，精确匹配", example = "ROPmv8av9hj0002z38fvn2949wettup", maxLength = 64)
        @Size(max = 64, message = "订单号长度不能超过 64 个字符")
        String orderId,

        @Schema(description = "订单状态", example = "used", allowableValues = {"create", "used", "cancel", "wait_pay", "complete"})
        @Pattern(regexp = "^(?:create|used|cancel|wait_pay|complete)?$", message = "订单状态不合法")
        String orderState,

        @Schema(description = "下单时间起点，包含该时间", example = "2026-10-18T00:00:00", type = "string", format = "date-time")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startTime,

        @Schema(description = "下单时间终点，不包含该时间", example = "2026-10-19T00:00:00", type = "string", format = "date-time")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endTime,

        @Schema(description = "页码，从 1 开始", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "页码不能小于 1")
        int pageNumber,

        @Schema(description = "每页记录数", example = "20", minimum = "1", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "每页记录数不能小于 1")
        @Max(value = 100, message = "每页记录数不能超过 100")
        int pageSize
) implements Serializable {

    /**
     * 校验下单时间查询区间。
     *
     * <p>只提供一个时间边界时表示开放区间；同时提供两个边界时，
     * 开始时间必须严格早于结束时间。</p>
     *
     * @return 时间范围合法时返回 {@code true}
     */
    @Schema(hidden = true)
    @AssertTrue(message = "下单时间起点必须早于终点")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || startTime.isBefore(endTime);
    }
}
