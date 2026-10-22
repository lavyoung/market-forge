package com.lavyoung.marketforge.application.activity.query;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动定义运营查询读取端口。
 *
 * <p>该端口只表达运营页面需要的活动分页读取能力。Infrastructure 层负责使用
 * MySQL 实现查询，但不得向 Application 层暴露 MyBatis、PO 或分页插件类型。</p>
 *
 * <p>该 Reader 不替代领域 Repository。Repository 为领域规则加载聚合和实体；
 * Reader 则生成不参与领域决策的后台页面读模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IActivityDefinitionReader {

    /**
     * 分页查询活动定义。
     *
     * <p>实现方必须按照更新时间倒序、数据库主键倒序进行稳定排序，
     * 并确保每页查询数量受 {@link com.lavyoung.marketforge.types.model.PageRequest} 限制。</p>
     *
     * @param query 活动定义查询条件
     * @return 活动定义分页结果；没有匹配记录时返回空分页，不返回 {@code null}
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     数据库查询或结果转换失败时抛出
     */
    PageResult<ActivityDefinitionResult> search(ActivityDefinitionSearchQuery query);
}
