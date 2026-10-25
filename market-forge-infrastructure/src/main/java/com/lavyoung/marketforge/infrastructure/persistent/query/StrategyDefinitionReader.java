package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchQuery;
import com.lavyoung.marketforge.application.strategy.query.IStrategyDefinitionReader;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.StrategyDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 基于策略主表的运营查询读取适配器。
 *
 * <p>先统计满足条件的记录数量。没有数据时直接返回空页，避免继续执行分页 SQL；
 * 存在数据时只读取策略主表并转换成轻量列表项，不加载奖品和规则子表。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Repository
@RequiredArgsConstructor
public class StrategyDefinitionReader implements IStrategyDefinitionReader {

    private final IStrategyDao strategyDao;
    private final StrategyDefinitionReadModelAssembler readModelAssembler;

    @Override
    public PageResult<StrategyDefinitionSearchItem> search(StrategyDefinitionSearchQuery query) {
        StrategyDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        StrategyDefinitionSearchParam searchParam = new StrategyDefinitionSearchParam(validQuery.strategyId(), validQuery.strategyDesc(), validQuery.state(), validQuery.page().offset(), validQuery.page().pageSize());
        long total = strategyDao.countStrategyDefinitions(searchParam);
        if (total == 0) {
            return PageResult.empty(validQuery.page().pageNumber(), validQuery.page().pageSize());
        }

        List<StrategyPO> strategies = Objects.requireNonNull(strategyDao.queryStrategyDefinitionPage(searchParam), "strategy definition query result must not be null");
        List<StrategyDefinitionSearchItem> records = readModelAssembler.toItems(strategies);
        return new PageResult<>(records, total, validQuery.page().pageNumber(), validQuery.page().pageSize());
    }
}
