package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 策略定义 MyBatis 分页查询参数。
 *
 * <p>该对象只在 Infrastructure 层承载 SQL 过滤条件和分页窗口，
 * 避免 Application 查询模型依赖数据库偏移量。</p>
 *
 * @param strategyId   策略业务标识
 * @param strategyDesc 策略描述前缀
 * @param state        策略生命周期状态
 * @param offset       从零开始的分页偏移量
 * @param pageSize     当前页最大记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyDefinitionSearchParam(Long strategyId, String strategyDesc, String state, long offset,
                                            int pageSize) {

    /**
     * 校验数据库分页窗口。
     *
     * @throws IllegalArgumentException 偏移量或每页记录数非法时抛出
     */
    public StrategyDefinitionSearchParam {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
