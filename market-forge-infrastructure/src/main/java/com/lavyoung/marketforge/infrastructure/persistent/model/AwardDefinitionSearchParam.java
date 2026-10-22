package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 奖品定义 MyBatis 分页查询参数。
 *
 * <p>该对象属于基础设施层，只负责承载 SQL 过滤和分页参数，
 * 不向 Application 层暴露 MyBatis 或数据库实现细节。</p>
 *
 * @param awardCode 奖品业务编码
 * @param awardName 奖品名称前缀
 * @param awardType 发奖处理器类型
 * @param state     生命周期状态
 * @param offset    从零开始的分页偏移量
 * @param pageSize  当前页最大记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record AwardDefinitionSearchParam(
        String awardCode,
        String awardName,
        String awardType,
        String state,
        long offset,
        int pageSize
) {

    /**
     * 校验数据库分页参数。
     *
     * @throws IllegalArgumentException 偏移量或每页记录数非法时抛出
     */
    public AwardDefinitionSearchParam {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }
}
