package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchQuery;
import com.lavyoung.marketforge.application.strategy.query.IStrategyDefinitionReader;
import com.lavyoung.marketforge.application.strategy.service.IStrategyDefinitionSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 策略定义运营查询应用服务默认实现。
 *
 * <p>负责校验查询输入并声明只读事务，实际数据库读取由
 * {@link IStrategyDefinitionReader} 完成。该服务不加载领域聚合，也不执行策略发布规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Service
@RequiredArgsConstructor
public class StrategyDefinitionSearchServiceImpl implements IStrategyDefinitionSearchService {

    private final IStrategyDefinitionReader strategyDefinitionReader;

    @Override
    @Transactional(readOnly = true)
    public PageResult<StrategyDefinitionSearchItem> search(StrategyDefinitionSearchQuery query) {
        StrategyDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        return strategyDefinitionReader.search(validQuery);
    }
}
