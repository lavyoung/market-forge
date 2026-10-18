package com.lavyoung.marketforge.application.activity.query;

import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动订单运营查询读取端口。
 *
 * <p>该接口声明应用用例所需的只读查询能力。Infrastructure 层负责使用
 * Elasticsearch 实现该接口，但不能向应用层暴露索引、DSL 或客户端类型。</p>
 *
 * <p>该端口不同于领域仓储。领域仓储负责聚合的加载和保存，而该端口负责
 * 面向运营页面的跨分库分页读模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
public interface IActivityOrderReader {

    /**
     * 分页检索活动订单。
     *
     * <p>结果应按照下单时间倒序排列，并继续使用稳定字段排序，
     * 保证相同下单时间下的分页顺序不会漂移。</p>
     *
     * @param query 活动订单查询条件
     * @return 活动订单分页查询结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     外部搜索服务不可用或结果解析失败时抛出
     */
    PageResult<ActivityOrderSearchItem> search(ActivityOrderSearchQuery query);
}
