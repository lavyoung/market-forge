package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 活动次数配置 MyBatis 分页查询参数。
 *
 * @param activityCountId   活动次数配置业务标识
 * @param activityCountName 配置名称前缀
 * @param offset            从零开始的分页偏移量
 * @param pageSize          当前页最大记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivityCountDefinitionSearchParam(Long activityCountId, String activityCountName, long offset,
                                                 int pageSize) {

    /**
     * 校验数据库分页窗口。
     *
     * @throws IllegalArgumentException 偏移量或每页记录数非法时抛出
     */
    public ActivityCountDefinitionSearchParam {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
