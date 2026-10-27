package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 活动 SKU MyBatis 分页查询参数。
 *
 * @param sku             SKU 业务标识
 * @param activityId      所属活动业务标识
 * @param activityCountId 次数配置业务标识
 * @param offset          从零开始的分页偏移量
 * @param pageSize        当前页最大记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public record ActivitySkuDefinitionSearchParam(Long sku, Long activityId, Long activityCountId, long offset,
                                               int pageSize) {

    /**
     * 校验数据库分页窗口。
     *
     * @throws IllegalArgumentException 偏移量或每页记录数非法时抛出
     */
    public ActivitySkuDefinitionSearchParam {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
