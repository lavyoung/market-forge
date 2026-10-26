package com.lavyoung.marketforge.application.activity.query;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动次数配置运营查询端口。
 *
 * <p>该端口只负责后台分页读模型，不承担配置创建和受保护更新。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IActivityCountDefinitionReader {

    /**
     * 分页查询活动次数配置。
     *
     * @param query 已规范化的查询条件
     * @return 按更新时间和数据库主键倒序排列的分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     数据库读取或结果转换失败时抛出
     */
    PageResult<ActivityCountDefinitionResult> search(ActivityCountDefinitionSearchQuery query);
}
