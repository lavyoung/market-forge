package com.lavyoung.marketforge.api.award.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户中奖记录分页查询请求。
 *
 * <p>该请求面向运营查询场景，可以按照用户、奖品、发奖状态和中奖时间进行组合过滤。
 * 所有过滤条件均为可选项，但分页参数必须满足约束。</p>
 *
 * <p>时间范围采用左闭右开语义，即包含 {@code startTime}，
 * 不包含 {@code endTime}。例如查询 2026 年 10 月 16 日的数据时，
 * 可以传入 {@code [2026-10-16T00:00:00, 2026-10-17T00:00:00)}。</p>
 *
 * @param userId     用户标识；为空时不限制用户
 * @param awardId    奖品标识；为空时不限制奖品
 * @param awardState 发奖状态；为空时不限制状态
 * @param startTime  中奖时间起点，包含该时间
 * @param endTime    中奖时间终点，不包含该时间
 * @param pageNo     页码，从 1 开始
 * @param pageSize   每页记录数，最大 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@Schema(description = "用户中奖记录分页查询条件")
public record UserAwardRecordSearchRequest(
        @Schema(
                description = "用户标识；不传时查询全部用户",
                example = "user-001",
                maxLength = 64
        )
        @Size(max = 64, message = "用户标识长度不能超过 64 个字符")
        String userId,

        @Schema(
                description = "奖品标识；不传时不限制奖品",
                example = "101",
                minimum = "1"
        )
        @Positive(message = "奖品标识必须为正数")
        Long awardId,

        @Schema(
                description = "发奖状态",
                example = "completed",
                allowableValues = {
                        "create",
                        "wait_claim",
                        "granting",
                        "grant_failed",
                        "completed"
                }
        )
        @Pattern(
                regexp = "^(?:create|wait_claim|granting|grant_failed|completed)?$",
                message = "发奖状态不合法"
        )
        String awardState,

        @Schema(
                description = "中奖时间起点，包含该时间",
                example = "2026-10-16T00:00:00",
                type = "string",
                format = "date-time"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startTime,

        @Schema(
                description = "中奖时间终点，不包含该时间",
                example = "2026-10-17T00:00:00",
                type = "string",
                format = "date-time"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endTime,

        @Schema(
                description = "页码，从 1 开始",
                example = "1",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1, message = "页码不能小于 1")
        int pageNo,

        @Schema(
                description = "每页记录数，范围为 1 到 100",
                example = "20",
                minimum = "1",
                maximum = "100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1, message = "每页记录数不能小于 1")
        @Max(value = 100, message = "每页记录数不能超过 100")
        int pageSize
) implements Serializable {

    /**
     * 判断中奖时间查询区间是否合法。
     *
     * <p>起点或终点未提供时，表示采用开放区间，因此校验通过。
     * 两个时间都存在时，起点必须严格早于终点，以维持左闭右开
     * {@code [startTime, endTime)} 的查询语义。</p>
     *
     * @return 时间区间合法时返回 {@code true}，否则返回 {@code false}
     */
    @Schema(hidden = true)
    @AssertTrue(message = "中奖时间起点必须早于终点")
    public boolean isTimeRangeValid() {
        return startTime == null
                || endTime == null
                || startTime.isBefore(endTime);
    }
}
