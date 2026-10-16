package com.lavyoung.marketforge.application.award.query;

import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 用户中奖记录读取器。
 *
 * <p>该接口定义应用层执行运营查询所需的只读数据能力。它面向分页读模型，
 * 不承担领域聚合的加载与保存职责，也不暴露 Elasticsearch 索引、查询 DSL
 * 或客户端类型。</p>
 *
 * <p>基础设施层负责实现该读取器，并将外部搜索文档转换为应用层查询结果。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
public interface IUserAwardRecordReader {

    /**
     * 根据运营检索条件分页查询用户中奖记录。
     *
     * <p>结果应按照中奖时间倒序排列，并使用全局唯一的订单标识作为第二排序条件，
     * 保证相同中奖时间下的分页顺序稳定。</p>
     *
     * @param query 用户中奖记录检索条件
     * @return 满足条件的分页中奖记录
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     外部查询服务不可用或响应解析失败时抛出
     */
    PageResult<UserAwardRecordSearchItem> search(UserAwardRecordSearchQuery query);
}
