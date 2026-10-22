package com.lavyoung.marketforge.application.activity.query;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动展台运营查询读取端口。
 *
 * <p>该端口面向后台列表读模型，不承担展台发布和状态流转。Infrastructure 层负责
 * 使用 MySQL 实现分页查询，但不能向应用层暴露 PO、MyBatis 或数据库分页类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IActivityStageReader {

    /**
     * 分页查询活动展台。
     *
     * @param query 已规范化的查询条件
     * @return 按更新时间和数据库主键倒序排列的分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     数据库读取或结果转换失败时抛出
     */
    PageResult<ActivityStageResult> search(ActivityStageSearchQuery query);
}
