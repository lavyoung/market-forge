package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 策略定义运营查询应用服务。
 *
 * <p>为 Trigger 层提供稳定的策略分页查询用例。HTTP Request 必须先转换成
 * {@link StrategyDefinitionSearchQuery}，不得直接传入 Application 层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public interface IStrategyDefinitionSearchService {

    /**
     * 分页查询策略定义摘要。
     *
     * @param query 策略定义查询条件
     * @return 策略定义分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     策略定义读取失败时抛出
     */
    PageResult<StrategyDefinitionSearchItem> search(StrategyDefinitionSearchQuery query);
}
