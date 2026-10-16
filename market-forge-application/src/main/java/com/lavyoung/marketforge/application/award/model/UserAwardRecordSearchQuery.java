package com.lavyoung.marketforge.application.award.model;

import java.time.LocalDateTime;

/**
 * 用户中奖记录搜索条件。
 *
 * <p>该对象描述运营端查询中奖记录时允许使用的过滤条件和分页范围。
 * 查询条件属于应用层输入模型，不包含 Elasticsearch 查询语法，避免上层用例
 * 与具体搜索引擎实现耦合。</p>
 *
 * @param userId     用户标识；为空时不限制用户
 * @param awardId    奖品标识；为空时不限制奖品
 * @param awardState 发奖状态；为空或空白时不限制状态
 * @param startTime  中奖时间起点，包含边界；为空时不限制起点
 * @param endTime    中奖时间终点，不包含边界；为空时不限制终点
 * @param pageNo     页码，从 1 开始
 * @param pageSize   每页记录数
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
        int pageNo,
        int pageSize
) {
    /**
     * 校验中奖记录查询参数。
     *
     * @param userId     用户标识
     * @param awardId    奖品标识
     * @param awardState 发奖状态
     * @param startTime  查询开始时间
     * @param endTime    查询结束时间
     * @param pageNo     页码
     * @param pageSize   每页记录数
     * @throws IllegalArgumentException 参数不满足查询约束时抛出
     */
    public UserAwardRecordSearchQuery {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than or equal to 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be earlier than endTime");
        }
    }

    /**
     * 计算 Elasticsearch 分页起始位置。
     *
     * @return 从零开始的结果偏移量
     * @throws ArithmeticException 页码与每页数量相乘发生整数溢出时抛出
     */
    public int offset() {
        return Math.multiplyExact(pageNo - 1, pageSize);
    }
}
