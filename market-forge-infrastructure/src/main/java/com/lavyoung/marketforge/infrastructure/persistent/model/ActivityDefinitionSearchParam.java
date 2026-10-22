package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 活动定义 MyBatis 分页查询参数。
 *
 * <p>该对象仅在 Infrastructure 层承载 SQL 过滤条件和分页窗口，防止 Application
 * 查询模型依赖数据库偏移量等实现细节。</p>
 *
 * @param activityId   活动业务标识
 * @param activityName 活动名称前缀
 * @param state        活动生命周期状态
 * @param offset       从零开始的分页偏移量
 * @param pageSize     当前页最大记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityDefinitionSearchParam(Long activityId, String activityName, String state, long offset,
                                            int pageSize) {

    /**
     * 校验数据库分页窗口。
     *
     * @throws IllegalArgumentException 偏移量或每页记录数非法时抛出
     */
    public ActivityDefinitionSearchParam {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
