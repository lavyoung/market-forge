package com.lavyoung.marketforge.application.strategy.query;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 策略定义运营查询读取端口。
 *
 * <p>该端口描述运营页面需要的策略分页读取能力，由 Infrastructure 层提供
 * MySQL 实现。Application 层不得感知 MyBatis、PO、Mapper 或数据库分页参数。</p>
 *
 * <p>Reader 与领域 Repository 的职责不同：Repository 为领域规则装载完整聚合；
 * Reader 为查询页面生成轻量读模型，不执行领域决策。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public interface IStrategyDefinitionReader {

    /**
     * 分页查询策略定义摘要。
     *
     * <p>实现必须按照更新时间倒序、数据库主键倒序进行稳定排序。
     * 没有匹配记录时返回空分页，不得返回 {@code null}。</p>
     *
     * @param query 策略定义查询条件
     * @return 策略定义分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     数据库查询或结果转换失败时抛出
     */
    PageResult<StrategyDefinitionSearchItem> search(StrategyDefinitionSearchQuery query);
}
