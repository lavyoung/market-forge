package com.lavyoung.marketforge.types.model;

/**
 * 通用分页请求参数。
 *
 * <p>统一约束项目内基于页码的分页查询，并提供安全的偏移量计算。
 * 每页最多查询 100 条，避免无边界查询给数据库或搜索服务造成压力。</p>
 *
 * @param pageNumber 页码，从 1 开始
 * @param pageSize   每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record PageRequest(int pageNumber, int pageSize) {

    /**
     * 单次分页查询允许返回的最大记录数。
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 校验分页边界。
     *
     * @throws IllegalArgumentException 页码或每页记录数不合法时抛出
     */
    public PageRequest {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than or equal to 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * 计算当前页对应的数据库记录偏移量。
     *
     * @return 从零开始的记录偏移量
     */
    public long offset() {
        return ((long) pageNumber - 1L) * pageSize;
    }
}
