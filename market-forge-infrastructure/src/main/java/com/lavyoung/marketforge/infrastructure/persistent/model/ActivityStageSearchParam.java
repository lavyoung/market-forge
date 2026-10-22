package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 活动展台 MyBatis 分页查询参数。
 *
 * <p>该对象只在基础设施层承载 SQL 过滤条件和分页窗口，不向 Application 层暴露
 * 数据库实现细节。</p>
 *
 * @param channel  投放渠道
 * @param source   流量来源
 * @param state    生命周期状态
 * @param offset   从零开始的分页偏移量
 * @param pageSize 当前页最大记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityStageSearchParam(String channel, String source, String state, long offset, int pageSize) {

    /**
     * 校验数据库分页窗口。
     *
     * @throws IllegalArgumentException 偏移量或每页记录数非法时抛出
     */
    public ActivityStageSearchParam {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
