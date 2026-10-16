package com.lavyoung.marketforge.types.model;

import java.util.List;
import java.util.Objects;

/**
 * 通用分页结果。
 *
 * <p>用于在应用层、接口层之间传递分页查询结果。该对象只表达分页语义，
 * 不包含 Elasticsearch、MyBatis、数据库游标等具体技术实现信息。</p>
 *
 * @param records    当前页记录
 * @param total      满足条件的总记录数
 * @param pageNumber 当前页码，从 1 开始
 * @param pageSize   每页记录数
 * @param <T>        当前页记录类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int pageNumber,
        int pageSize
) {

    /**
     * 校验分页元数据并对当前页记录执行防御性复制。
     *
     * @param records    当前页记录
     * @param total      总记录数
     * @param pageNumber 当前页码
     * @param pageSize   每页记录数
     * @throws NullPointerException     当前页记录为空时抛出
     * @throws IllegalArgumentException 总记录数、页码或每页记录数非法时抛出
     */
    public PageResult {
        records = List.copyOf(Objects.requireNonNull(records, "records must not be null"));
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than or equal to 1");
        }
    }

    /**
     * 创建空分页结果。
     *
     * @param pageNumber 当前页码
     * @param pageSize   每页记录数
     * @param <T>        记录类型
     * @return 不包含记录的分页结果
     * @throws IllegalArgumentException 页码或每页记录数非法时抛出
     */
    public static <T> PageResult<T> empty(int pageNumber, int pageSize) {
        return new PageResult<>(List.of(), 0, pageNumber, pageSize);
    }
}