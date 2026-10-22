package com.lavyoung.marketforge.application.award.model;

import com.lavyoung.marketforge.types.model.PageRequest;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户中奖记录搜索条件。
 *
 * <p>该对象描述运营端查询中奖记录时允许使用的过滤条件和分页范围。
 * 查询条件属于应用层输入模型，不包含 Elasticsearch 查询语法，避免上层用例
 * 与具体搜索引擎实现耦合。分页边界统一由 {@link PageRequest} 负责。</p>
 *
 * @param userId     用户标识；为空时不限制用户
 * @param awardId    奖品标识；为空时不限制奖品
 * @param awardState 发奖状态；为空或空白时不限制状态
 * @param startTime  中奖时间起点，包含边界；为空时不限制起点
 * @param endTime    中奖时间终点，不包含边界；为空时不限制终点
 * @param page        分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
public record UserAwardRecordSearchQuery(
        String userId,
        Long awardId,
        String awardState,
        LocalDateTime startTime,
        LocalDateTime endTime,
        PageRequest page
) {
    /**
     * 校验中奖记录查询参数。
     *
     * <p>空白用户标识和发奖状态统一转换为空，避免基础设施层重复处理空白值。</p>
     *
     * @throws NullPointerException     分页参数为空时抛出
     * @throws IllegalArgumentException 参数不满足查询约束时抛出
     */
    public UserAwardRecordSearchQuery {
        userId = normalize(userId);
        awardState = normalize(awardState);
        Objects.requireNonNull(page, "page must not be null");
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be earlier than endTime");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
