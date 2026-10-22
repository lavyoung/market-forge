package com.lavyoung.marketforge.application.award.service.impl;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.AwardDefinitionSearchQuery;
import com.lavyoung.marketforge.application.award.query.IAwardDefinitionReader;
import com.lavyoung.marketforge.application.award.service.IAwardDefinitionSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 奖品定义运营查询应用服务默认实现。
 *
 * <p>负责守护应用层查询输入和声明只读事务，再把实际数据读取委托给
 * {@link IAwardDefinitionReader}。MyBatis、分页 SQL 和 PO 转换由
 * Infrastructure 层负责。</p>
 *
 * <p>查询结果是面向运营页面的读模型，不参与奖品生命周期决策。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Service
@RequiredArgsConstructor
public class AwardDefinitionSearchServiceImpl implements IAwardDefinitionSearchService {

    private final IAwardDefinitionReader awardDefinitionReader;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AwardDefinitionResult> search(AwardDefinitionSearchQuery query) {
        AwardDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        return awardDefinitionReader.search(validQuery);
    }
}
